package com.fuzinewatch

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.webkit.CookieManager
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fuzinewatch.data.AppDatabase
import com.fuzinewatch.data.Listing
import com.fuzinewatch.data.ScrapeSettings
import com.fuzinewatch.data.ScrapeSettingsStore
import com.fuzinewatch.data.ScrapeStatus
import com.fuzinewatch.data.ScrapeStatusStore
import com.fuzinewatch.scraper.ScrapeRunner
import com.fuzinewatch.scraper.Site
import com.fuzinewatch.scraper.WebViewFetcher
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

class MainActivity : ComponentActivity() {
    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermission()
        requestBatteryExemption()
        setContent { FuzineWatchApp() }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun requestBatteryExemption() {
        if (Build.VERSION.SDK_INT < 23) return
        val powerManager = getSystemService(PowerManager::class.java)
        if (powerManager.isIgnoringBatteryOptimizations(packageName)) return
        runCatching {
            startActivity(
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                },
            )
        }
    }
}

@Composable
private fun FuzineWatchApp() {
    var unblockMode by remember { mutableStateOf(false) }
    MaterialTheme {
        if (unblockMode) {
            UnblockScreen(onBack = { unblockMode = false })
        } else {
            MainScreen(onOpenUnblock = { unblockMode = true })
        }
    }
}

@Composable
private fun MainScreen(onOpenUnblock: () -> Unit) {
    val context = LocalContext.current
    val listings by remember {
        AppDatabase.get(context).listingDao().recent()
    }.collectAsStateWithLifecycle(emptyList())
    val status by remember {
        ScrapeStatusStore.get(context).status
    }.collectAsStateWithLifecycle()
    val settingsStore = remember { ScrapeSettingsStore.get(context) }
    val settings by settingsStore.settings.collectAsStateWithLifecycle()
    var scraping by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("FuzineWatch", style = MaterialTheme.typography.headlineMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    enabled = !scraping,
                    onClick = {
                        scraping = true
                        scope.launch {
                            ScrapeRunner.scrapeAll(context)
                            scraping = false
                        }
                    },
                ) {
                    Text(if (scraping) "Scraping..." else "Scrape now")
                }
                OutlinedButton(onClick = onOpenUnblock) {
                    Text("Unblock site manually")
                }
            }
            StatusBlock(status)
            SettingsBlock(
                settings = settings,
                onIntervalChange = {
                    settingsStore.setIntervalMinutes(it)
                    ScrapeWorker.enqueue(context)
                },
            )
            Text("Recent listings", style = MaterialTheme.typography.titleLarge)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listings, key = { it.id }) { listing ->
                    ListingRow(listing)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingsBlock(settings: ScrapeSettings, onIntervalChange: (Long) -> Unit) {
    val options = listOf(15L, 30L, 60L, 120L, 180L, 360L)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Checks every ${formatInterval(settings.intervalMinutes)}")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEach { minutes ->
                val selected = settings.intervalMinutes == minutes
                if (selected) {
                    Button(onClick = { onIntervalChange(minutes) }) {
                        Text(formatInterval(minutes))
                    }
                } else {
                    OutlinedButton(onClick = { onIntervalChange(minutes) }) {
                        Text(formatInterval(minutes))
                    }
                }
            }
        }
        Text("Paused daily from 22:00 to 07:00. Manual scrape still works.")
    }
}

private fun formatInterval(minutes: Long): String = when {
    minutes < 60 -> "${minutes}m"
    minutes % 60L == 0L -> "${minutes / 60L}h"
    else -> "${minutes / 60L}h ${minutes % 60L}m"
}

@Composable
private fun StatusBlock(status: ScrapeStatus) {
    val lastRun = if (status.lastRunAt == 0L) {
        "Never"
    } else {
        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
            .format(Date(status.lastRunAt))
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Last scrape: $lastRun", style = MaterialTheme.typography.titleMedium)
        status.siteSummaries.forEach { (site, summary) ->
            Text("$site: $summary", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ListingRow(listing: Listing) {
    val uriHandler = LocalUriHandler.current
    Card(
        onClick = { uriHandler.openUri(listing.link) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(listing.title, style = MaterialTheme.typography.titleMedium)
            Text(
                listOf(listing.price, listing.size).filter { it.isNotBlank() }.joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(listing.source, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun UnblockScreen(onBack: () -> Unit) {
    var selectedSite by remember { mutableStateOf(Site.NEPREMICNINE) }
    var webView: WebView? by remember { mutableStateOf(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onBack) { Text("Back") }
                Site.entries.forEach { site ->
                    Button(onClick = {
                        selectedSite = site
                        webView?.loadUrl(site.url)
                    }) {
                        Text(site.label)
                    }
                }
            }
            Text("Complete any challenge here, then return to the main screen.")
        }
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            factory = { context ->
                WebView(context).also {
                    webView = it
                    WebViewFetcher.applySettings(it)
                    it.loadUrl(selectedSite.url)
                }
            },
            update = {
                CookieManager.getInstance().flush()
            },
        )
        Spacer(Modifier.height(8.dp))
    }
}

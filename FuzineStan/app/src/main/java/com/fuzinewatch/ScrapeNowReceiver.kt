package com.fuzinewatch

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.fuzinewatch.scraper.ScrapeRunner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ScrapeNowReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_SCRAPE_NOW) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            runCatching { ScrapeRunner.scrapeAll(context.applicationContext) }
            pendingResult.finish()
        }
    }

    companion object {
        const val ACTION_SCRAPE_NOW = "com.fuzinewatch.SCRAPE_NOW"
    }
}

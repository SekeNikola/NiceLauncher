package com.fuzinewatch.data

import android.content.Context
import com.fuzinewatch.scraper.Site
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ScrapeStatus(
    val lastRunAt: Long,
    val siteSummaries: Map<String, String>,
)

/** Last-scrape outcome per site, persisted so it survives process death between runs. */
class ScrapeStatusStore private constructor(context: Context) {

    private val prefs = context.getSharedPreferences("scrape_status", Context.MODE_PRIVATE)
    private val _status = MutableStateFlow(read())
    val status: StateFlow<ScrapeStatus> = _status.asStateFlow()

    fun update(site: Site, summary: String) {
        prefs.edit().putString(KEY_SITE_PREFIX + site.name, summary).apply()
        _status.value = read()
    }

    fun markRun() {
        prefs.edit().putLong(KEY_LAST_RUN, System.currentTimeMillis()).apply()
        _status.value = read()
    }

    fun hasSeeded(site: Site): Boolean =
        prefs.getBoolean(KEY_SEEDED_PREFIX + site.name, false)

    fun markSeeded(site: Site) {
        prefs.edit().putBoolean(KEY_SEEDED_PREFIX + site.name, true).apply()
    }

    private fun read(): ScrapeStatus = ScrapeStatus(
        lastRunAt = prefs.getLong(KEY_LAST_RUN, 0L),
        siteSummaries = Site.entries.associate {
            it.label to (prefs.getString(KEY_SITE_PREFIX + it.name, null) ?: "Not scraped yet")
        },
    )

    companion object {
        private const val KEY_LAST_RUN = "last_run"
        private const val KEY_SITE_PREFIX = "site_"
        private const val KEY_SEEDED_PREFIX = "seeded_"

        @Volatile
        private var instance: ScrapeStatusStore? = null

        fun get(context: Context): ScrapeStatusStore = instance ?: synchronized(this) {
            instance ?: ScrapeStatusStore(context.applicationContext).also { instance = it }
        }
    }
}

package com.fuzinewatch.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalTime

data class ScrapeSettings(
    val intervalMinutes: Long,
    val quietStartHour: Int,
    val quietEndHour: Int,
) {
    fun isQuietNow(): Boolean {
        val hour = LocalTime.now().hour
        return if (quietStartHour < quietEndHour) {
            hour in quietStartHour until quietEndHour
        } else {
            hour >= quietStartHour || hour < quietEndHour
        }
    }
}

class ScrapeSettingsStore private constructor(context: Context) {
    private val prefs = context.getSharedPreferences("scrape_settings", Context.MODE_PRIVATE)
    private val _settings = MutableStateFlow(read())
    val settings: StateFlow<ScrapeSettings> = _settings.asStateFlow()

    fun setIntervalMinutes(minutes: Long) {
        prefs.edit().putLong(KEY_INTERVAL_MINUTES, minutes.coerceAtLeast(15)).apply()
        _settings.value = read()
    }

    fun read(): ScrapeSettings = ScrapeSettings(
        intervalMinutes = prefs.getLong(KEY_INTERVAL_MINUTES, DEFAULT_INTERVAL_MINUTES),
        quietStartHour = prefs.getInt(KEY_QUIET_START, 22),
        quietEndHour = prefs.getInt(KEY_QUIET_END, 7),
    )

    companion object {
        private const val KEY_INTERVAL_MINUTES = "interval_minutes"
        private const val KEY_QUIET_START = "quiet_start"
        private const val KEY_QUIET_END = "quiet_end"
        private const val DEFAULT_INTERVAL_MINUTES = 60L

        @Volatile
        private var instance: ScrapeSettingsStore? = null

        fun get(context: Context): ScrapeSettingsStore = instance ?: synchronized(this) {
            instance ?: ScrapeSettingsStore(context.applicationContext).also { instance = it }
        }
    }
}

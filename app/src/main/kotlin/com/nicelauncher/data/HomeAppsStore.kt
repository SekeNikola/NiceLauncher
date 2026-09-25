package com.nicelauncher.data

import android.content.Context

class HomeAppsStore(context: Context) {
    private val prefs = context.getSharedPreferences("home_apps", Context.MODE_PRIVATE)

    fun getHomeApps(): List<HomeApp> {
        val raw = prefs.getString("apps", "") ?: return emptyList()
        if (raw.isBlank()) return emptyList()
        return raw.split(";;").filter { it.isNotBlank() }.map { item ->
            val parts = item.split("|", limit = 3)
            HomeApp(
                packageName = parts[0],
                customImageUri = parts.getOrNull(1)?.takeIf { it.isNotBlank() && it != "null" },
                backgroundMode = parts.getOrNull(2)?.let {
                    try { BackgroundMode.valueOf(it) } catch (_: Exception) { BackgroundMode.BLURRED }
                } ?: BackgroundMode.BLURRED,
            )
        }
    }

    fun saveHomeApps(apps: List<HomeApp>) {
        val encoded = apps.joinToString(";;") {
            "${it.packageName}|${it.customImageUri ?: ""}|${it.backgroundMode.name}"
        }
        prefs.edit().putString("apps", encoded).apply()
    }

    fun isDarkMode(): Boolean = prefs.getBoolean("dark_mode", true)

    fun setDarkMode(dark: Boolean) {
        prefs.edit().putBoolean("dark_mode", dark).apply()
    }

    fun getNoteText(): String = prefs.getString("note_text", "PROJECT\nASSIGNM\nENT") ?: "PROJECT\nASSIGNM\nENT"

    fun setNoteText(text: String) {
        prefs.edit().putString("note_text", text).apply()
    }

    fun getIconSizePercent(): Float = prefs.getFloat("icon_size_percent", 58f)

    fun setIconSizePercent(percent: Float) {
        prefs.edit().putFloat("icon_size_percent", percent).apply()
    }

    fun getUserAvatarUri(): String? = prefs.getString("user_avatar_uri", null)

    fun setUserAvatarUri(uri: String?) {
        prefs.edit().putString("user_avatar_uri", uri).apply()
    }

    fun getWallpaperUri(): String? = prefs.getString("wallpaper_uri", null)

    fun setWallpaperUri(uri: String?) {
        prefs.edit().putString("wallpaper_uri", uri).apply()
    }

    fun getUserName(): String = prefs.getString("user_name", "USER") ?: "USER"

    fun setUserName(name: String) {
        prefs.edit().putString("user_name", name).apply()
    }

    fun getWeatherCity(): String = prefs.getString("weather_city", "Ljubljana") ?: "Ljubljana"

    fun setWeatherCity(city: String) {
        prefs.edit().putString("weather_city", city).apply()
    }

    fun getClockAppPackage(): String? = prefs.getString("clock_app_package", null)

    fun setClockAppPackage(packageName: String?) {
        prefs.edit().putString("clock_app_package", packageName).apply()
    }

    fun getWeatherAppPackage(): String? = prefs.getString("weather_app_package", null)

    fun setWeatherAppPackage(packageName: String?) {
        prefs.edit().putString("weather_app_package", packageName).apply()
    }
}

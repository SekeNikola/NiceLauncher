package com.nicelauncher.data

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL

data class LauncherWeather(
    val city: String,
    val temperatureC: Int?,
    val conditionCode: Int?,
)

class WeatherRepository {
    fun fetchCityWeather(city: String): LauncherWeather {
        val encoded = URLEncoder.encode(city, "UTF-8")
        val geo = JSONObject(
            get("https://geocoding-api.open-meteo.com/v1/search?name=$encoded&count=1&language=en&format=json")
        )
        val first = geo.optJSONArray("results")?.optJSONObject(0)
            ?: return LauncherWeather(city.uppercase(), null, null)

        val lat = first.getDouble("latitude")
        val lon = first.getDouble("longitude")
        val label = first.optString("name", city).uppercase()
        val weather = JSONObject(
            get("https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current_weather=true")
        ).getJSONObject("current_weather")

        return LauncherWeather(
            city = label,
            temperatureC = weather.optDouble("temperature").toInt(),
            conditionCode = weather.optInt("weathercode"),
        )
    }

    private fun get(url: String): String {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 6000
            readTimeout = 6000
            requestMethod = "GET"
        }
        return connection.inputStream.bufferedReader().use { it.readText() }
    }
}

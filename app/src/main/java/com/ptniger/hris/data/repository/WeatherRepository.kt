package com.ptniger.hris.data.repository

import com.ptniger.hris.data.model.WeatherData
import okhttp3.OkHttpClient
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class WeatherRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    // In-memory cache per koordinat
    private val cache = mutableMapOf<String, WeatherData>()

    /**
     * Fetch cuaca dari OpenWeatherMap API.
     * Endpoint: https://api.openweathermap.org/data/2.5/weather
     *
     * @param lat Latitude lokasi
     * @param lon Longitude lokasi
     * @param apiKey OpenWeatherMap API key (dari Firestore app_configs)
     * @return WeatherData atau null jika gagal/no API key
     */
    suspend fun fetchCurrentWeather(
        lat: Double,
        lon: Double,
        apiKey: String? = null
    ): WeatherData? {
        val key = apiKey ?: AppConfigRepository().getWeatherApiKeyOrNull() ?: return null
        if (key.isBlank()) return null

        // Cek cache dulu (cache key = rounded koordinat 2 desimal)
        val cacheKey = "${String.format("%.2f", lat)}_${String.format("%.2f", lon)}"
        cache[cacheKey]?.let { if (it.isFresh()) return it }

        return try {
            val url = "https://api.openweathermap.org/data/2.5/weather" +
                    "?lat=$lat&lon=$lon" +
                    "&appid=$key" +
                    "&units=metric" +    // Celsius
                    "&lang=id"           // Bahasa Indonesia

            val request = okhttp3.Request.Builder().url(url).build()
            val response = kotlinx.coroutines.Dispatchers.IO.run {
                client.newCall(request).execute()
            }

            if (!response.isSuccessful) return null

            val json = JSONObject(response.body?.string() ?: return null)
            val weather = json.getJSONArray("weather").getJSONObject(0)
            val main = json.getJSONObject("main")
            val wind = json.getJSONObject("wind")

            val result = WeatherData(
                lat = lat,
                lon = lon,
                cityName = json.optString("name", ""),
                description = weather.optString("description", ""),
                iconCode = weather.optString("icon", "01d"),
                tempCelsius = main.optDouble("temp", 0.0),
                feelsLikeCelsius = main.optDouble("feels_like", 0.0),
                humidity = main.optInt("humidity", 0),
                windSpeedMs = wind.optDouble("speed", 0.0),
                fetchedAt = System.currentTimeMillis()
            )

            cache[cacheKey] = result
            result
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Bandingkan cuaca di dua titik lokasi.
     * Dipakai untuk anti-fraud: apakah lokasi GPS karyawan sinkron dengan lokasi kantor?
     *
     * Return: WeatherComparisonResult
     */
    suspend fun compareWeatherLocations(
        employeeLat: Double, employeeLon: Double,
        officeLat: Double, officeLon: Double
    ): WeatherComparisonResult {
        val empWeather    = fetchCurrentWeather(employeeLat, employeeLon) ?: return WeatherComparisonResult.UNAVAILABLE
        val officeWeather = fetchCurrentWeather(officeLat, officeLon)     ?: return WeatherComparisonResult.UNAVAILABLE

        // Bandingkan: suhu berbeda > 5°C atau kondisi sangat berbeda
        val tempDiff = Math.abs(empWeather.tempCelsius - officeWeather.tempCelsius)
        val iconDiff = empWeather.iconCode.take(2) != officeWeather.iconCode.take(2)

        return when {
            tempDiff > 8.0 && iconDiff -> WeatherComparisonResult.SUSPICIOUS    // sangat berbeda
            tempDiff > 5.0 || iconDiff -> WeatherComparisonResult.SLIGHTLY_OFF  // agak berbeda
            else                       -> WeatherComparisonResult.CONSISTENT    // normal
        }
    }

    enum class WeatherComparisonResult {
        CONSISTENT,     // cuaca sinkron → lokasi kemungkinan benar
        SLIGHTLY_OFF,   // sedikit berbeda → flag ringan
        SUSPICIOUS,     // sangat berbeda → kemungkinan GPS spoof atau beda kota
        UNAVAILABLE     // API gagal/no key → tidak bisa verifikasi
    }
}

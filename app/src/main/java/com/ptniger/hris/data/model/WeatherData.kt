package com.ptniger.hris.data.model

data class WeatherData(
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    val cityName: String = "",             // "Semarang"
    val countryCode: String = "ID",
    val description: String = "",          // "cerah berawan", "hujan ringan", dll
    val iconCode: String = "",             // kode icon OWM: "01d", "02d", "10d", dll
    val tempCelsius: Double = 0.0,
    val feelsLikeCelsius: Double = 0.0,
    val humidity: Int = 0,                 // %
    val windSpeedMs: Double = 0.0,
    val fetchedAt: Long = System.currentTimeMillis()
) {
    // Helper: apakah data masih fresh (max 30 menit cache)
    fun isFresh(): Boolean = (System.currentTimeMillis() - fetchedAt) < 30 * 60 * 1000L

    // Helper: icon yang sesuai untuk UI (Material Icon atau emoji fallback)
    fun getWeatherLabel(): String = when {
        iconCode.startsWith("01") -> "Cerah"
        iconCode.startsWith("02") -> "Cerah Berawan"
        iconCode.startsWith("03") -> "Berawan"
        iconCode.startsWith("04") -> "Mendung"
        iconCode.startsWith("09") -> "Hujan Ringan"
        iconCode.startsWith("10") -> "Hujan"
        iconCode.startsWith("11") -> "Badai"
        iconCode.startsWith("13") -> "Bersalju"
        iconCode.startsWith("50") -> "Berkabut"
        else -> description
    }

    // Helper: Material Icon ID berdasarkan kondisi cuaca
    fun getIconName(): String = when {
        iconCode.startsWith("01") -> "wb_sunny"
        iconCode.startsWith("02") || iconCode.startsWith("03") -> "partly_cloudy_day"
        iconCode.startsWith("04") -> "cloud"
        iconCode.startsWith("09") || iconCode.startsWith("10") -> "rainy"
        iconCode.startsWith("11") -> "thunderstorm"
        iconCode.startsWith("50") -> "foggy"
        else -> "wb_cloudy"
    }
}

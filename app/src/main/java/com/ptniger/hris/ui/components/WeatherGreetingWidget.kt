package com.ptniger.hris.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ptniger.hris.data.model.User
import com.ptniger.hris.data.model.WeatherData
import com.ptniger.hris.data.repository.EmployeeRepository
import com.ptniger.hris.data.repository.OfficeLocationRepository
import com.ptniger.hris.data.repository.WeatherRepository
import com.ptniger.hris.ui.theme.Blue
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Widget greeting "Selamat pagi, Budi! Hari ini cuaca akan cerah di Semarang"
 * Dipakai di SEMUA dashboard (Employee, Manager, HR, Finance, GM, Admin)
 * via DashboardLayout.
 *
 * Lokasi cuaca: koordinat kantor yang di-assign ke karyawan (employee.officeId)
 */
@Composable
fun WeatherGreetingWidget(
    user: User,
    modifier: Modifier = Modifier
) {
    var weather by remember { mutableStateOf<WeatherData?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(user.userId) {
        scope.launch {
            try {
                // Ambil koordinat kantor karyawan dari Firestore
                val empRepo = EmployeeRepository()
                val employee = empRepo.getByUserId(user.userId)
                val officeId = employee?.officeId ?: "office_main"
                val office = OfficeLocationRepository().getById(officeId)

                if (office != null) {
                    weather = WeatherRepository().fetchCurrentWeather(
                        lat = office.latitude,
                        lon = office.longitude
                    )
                }
            } catch (_: Exception) {}
            isLoading = false
        }
    }

    // Tentukan greeting berdasarkan jam
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when {
        hour < 11 -> "Selamat pagi"
        hour < 15 -> "Selamat siang"
        hour < 18 -> "Selamat sore"
        else      -> "Selamat malam"
    }
    val firstName = (user.fullName.ifEmpty { user.name })
        .split(" ").firstOrNull() ?: user.name

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Blue,
        shadowElevation = 2.dp
    ) {
        // Gradient background
        Box(
            Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Blue, Color(0xFF3B82F6))
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(20.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Kiri: greeting + cuaca text
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "$greeting,",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    Text(
                        firstName,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White
                    )
                    Spacer(Modifier.height(4.dp))

                    if (isLoading) {
                        // Loading state
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CircularProgressIndicator(
                                Modifier.size(12.dp),
                                color = Color.White.copy(alpha = 0.7f),
                                strokeWidth = 1.5.dp
                            )
                            Text("Memuat cuaca...",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.7f))
                        }
                    } else if (weather != null) {
                        val w = weather!!
                        Text(
                            "Hari ini cuaca akan ${w.getWeatherLabel().lowercase()} di ${w.cityName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                        Text(
                            "Terasa ${w.feelsLikeCelsius.toInt()}°C · Kelembapan ${w.humidity}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    } else {
                        // API key belum dikonfigurasi atau gagal
                        Text(
                            "Data cuaca tidak tersedia",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }

                // Kanan: suhu + ikon cuaca
                if (!isLoading && weather != null) {
                    val w = weather!!
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Ikon cuaca (Material Icon, profesional)
                        Icon(
                            imageVector = getWeatherIcon(w.iconCode),
                            contentDescription = w.description,
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            "${w.tempCelsius.toInt()}°C",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

/**
 * Mapping kode icon OWM ke Material Icon.
 * Tidak pakai emoji — semua pakai ikon vektor dari Compose.
 */
fun getWeatherIcon(iconCode: String): ImageVector = when {
    iconCode.startsWith("01") -> Icons.Default.WbSunny           // cerah
    iconCode.startsWith("02") -> Icons.Default.WbCloudy          // sedikit berawan
    iconCode.startsWith("03") -> Icons.Default.Cloud             // berawan
    iconCode.startsWith("04") -> Icons.Default.Cloud             // mendung
    iconCode.startsWith("09") -> Icons.Default.WaterDrop         // hujan ringan (pakai WaterDrop pengganti Grain)
    iconCode.startsWith("10") -> Icons.Default.WaterDrop         // hujan (pakai WaterDrop pengganti Umbrella)
    iconCode.startsWith("11") -> Icons.Default.FlashOn           // badai (pakai FlashOn pengganti Thunderstorm)
    iconCode.startsWith("13") -> Icons.Default.AcUnit            // salju
    iconCode.startsWith("50") -> Icons.Default.Dehaze            // kabut
    else                      -> Icons.Default.WbCloudy
}

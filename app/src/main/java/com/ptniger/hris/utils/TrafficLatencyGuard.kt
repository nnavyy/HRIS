package com.ptniger.hris.utils

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Traffic & Latency Guard:
 * Mengelola deteksi beban puncak (High Traffic / Server Overload) dan
 * memberikan indikator antrean ramah pengguna agar aplikasi terasa tenang,
 * aman, dan tidak membuat pengguna bingung saat terjadi latensi tinggi.
 */
object TrafficLatencyGuard {

    private val _isHighTraffic = MutableStateFlow(false)
    val isHighTraffic: StateFlow<Boolean> = _isHighTraffic.asStateFlow()

    private val _trafficMessage = MutableStateFlow(
        "Trafik sistem sedang padat. Permintaan Anda dalam antrean aman, mohon tunggu sebentar..."
    )
    val trafficMessage: StateFlow<String> = _trafficMessage.asStateFlow()

    // Ambang batas latensi dianggap "High Traffic" (dalam milidetik)
    private const val HIGH_LATENCY_THRESHOLD_MS = 3200L

    /**
     * Membungkus eksekusi fungsi suspend dengan pemantau durasi.
     * Jika request memakan waktu > 3.2 detik, aktifkan notifikasi high traffic.
     */
    suspend fun <T> runWithTrafficGuard(
        actionName: String = "Operasi",
        block: suspend () -> T
    ): T {
        val startTime = System.currentTimeMillis()
        return try {
            block()
        } finally {
            val duration = System.currentTimeMillis() - startTime
            if (duration > HIGH_LATENCY_THRESHOLD_MS) {
                _trafficMessage.value = "Trafik server sedang tinggi (respon ${duration / 1000}s). Sistem tetap memproses data Anda dengan aman."
                _isHighTraffic.value = true
            } else {
                _isHighTraffic.value = false
            }
        }
    }

    fun setHighTraffic(enabled: Boolean, customMessage: String? = null) {
        if (customMessage != null) _trafficMessage.value = customMessage
        _isHighTraffic.value = enabled
    }

    fun clear() {
        _isHighTraffic.value = false
    }
}

/**
 * Banner Composable elegan untuk memberi tahu pengguna jika server sedang antre / lag.
 */
@Composable
fun HighTrafficBanner(
    visible: Boolean,
    message: String = "Trafik sistem sedang padat. Permintaan Anda dalam antrean aman, mohon tunggu...",
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically() + fadeIn(),
        exit = slideOutVertically() + fadeOut()
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFFEF3C7), // Amber soft
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.5.dp,
                    color = Color(0xFFD97706) // Amber dark
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Antrean Server Aktif",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF92400E)
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFB45309)
                    )
                }
            }
        }
    }
}

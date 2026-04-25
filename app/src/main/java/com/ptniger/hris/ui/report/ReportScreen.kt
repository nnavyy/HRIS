package com.ptniger.hris.ui.report

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.ptniger.hris.data.model.User
import com.ptniger.hris.ui.theme.*

@Composable
fun ReportScreen(user: User) {
    Column(Modifier.fillMaxSize().background(Background).verticalScroll(rememberScrollState())) {
        Text("Laporan", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(18.dp))
        ReportItem(Icons.Default.Description, "Laporan Absensi", "Rekap hadir, telat, izin, alpha", "PDF", BlueSoft, Blue)
        ReportItem(Icons.Default.Payments, "Laporan Payroll", "Payroll, tunjangan, potongan", "Excel", GreenSoft, Green)
        ReportItem(Icons.Default.CalendarMonth, "Laporan Cuti", "Sisa kuota dan riwayat cuti", "PDF", OrangeSoft, Orange)
        ReportItem(Icons.Default.Star, "Laporan KPI", "Performa karyawan per departemen", "PDF", PurpleSoft, Purple)
        ReportItem(Icons.Default.Shield, "Audit Log", "Riwayat perubahan data", "Secure", RedSoft, Red)
        Spacer(Modifier.height(100.dp))
    }
}

@Composable
private fun ReportItem(icon: ImageVector, title: String, desc: String, badge: String, bg: Color, fg: Color) {
    Surface(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 4.dp), shape = RoundedCornerShape(22.dp), color = Surface, shadowElevation = 1.dp) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(16.dp)).background(bg), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = fg, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(desc, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            Surface(shape = RoundedCornerShape(999.dp), color = bg) {
                Text(badge, Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = fg)
            }
        }
    }
}

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

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.clickable

@Composable
fun ReportScreen(user: User, vm: ReportViewModel = viewModel()) {
    val context = LocalContext.current
    val message by vm.message.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val now = java.util.Calendar.getInstance()
    val month = now.get(java.util.Calendar.MONTH) + 1
    val year = now.get(java.util.Calendar.YEAR)

    // Auto-dismiss success message after 4 seconds
    LaunchedEffect(message) {
        if (message != null) {
            kotlinx.coroutines.delay(4000)
            vm.clearMessage()
        }
    }

    Column(Modifier.fillMaxSize().background(Background).statusBarsPadding().verticalScroll(rememberScrollState())) {
        Text("Laporan", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(start = 18.dp, end = 64.dp, top = 14.dp, bottom = 10.dp))
        if (message != null) {
            Spacer(Modifier.height(4.dp))
            Surface(
                Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                shape = RoundedCornerShape(12.dp),
                color = GreenSoft
            ) {
                Text(message!!, Modifier.padding(horizontal = 14.dp, vertical = 10.dp), color = Green, style = MaterialTheme.typography.bodySmall)
            }
        }
        if (isLoading) {
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 18.dp), color = Blue)
        }
        Spacer(Modifier.height(16.dp))

        ReportItem(Icons.Default.AccessTime, "Laporan Absensi", "Rekap harian karyawan (CSV)", "Download", GreenSoft, Green) {
            vm.downloadAttendanceReport(context, month, year)
        }
        ReportItem(Icons.Default.Payments, "Laporan Payroll", "Gaji dan bonus bulan ini (CSV)", "Download", BlueSoft, Blue) {
            vm.downloadPayrollReport(context, month, year)
        }
        ReportItem(Icons.Default.CalendarMonth, "Laporan Cuti", "Rekap semua pengajuan cuti (CSV)", "Download", OrangeSoft, Orange) {
            vm.downloadLeaveReport(context, month, year)
        }
        ReportItem(Icons.Default.Star, "Laporan KPI", "Performa karyawan (CSV)", "Download", PurpleSoft, Purple) {
            vm.downloadKpiReport(context, month, year)
        }
        Spacer(Modifier.height(100.dp))
    }
}

@Composable
private fun ReportItem(icon: ImageVector, title: String, desc: String, badge: String, bg: Color, fg: Color, onClick: () -> Unit = {}) {
    Surface(
        Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 4.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        color = Surface,
        shadowElevation = 1.dp
    ) {
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

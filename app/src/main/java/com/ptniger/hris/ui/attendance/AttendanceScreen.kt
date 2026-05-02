package com.ptniger.hris.ui.attendance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ptniger.hris.data.model.User
import com.ptniger.hris.ui.theme.*
import com.ptniger.hris.utils.DateUtils

@Composable
fun AttendanceScreen(user: User, vm: AttendanceViewModel = viewModel()) {
    val empId = user.employeeId.ifEmpty { user.userId }
    LaunchedEffect(Unit) { vm.loadTodayAttendance(empId) }
    val state by vm.state.collectAsState()

    Column(Modifier.fillMaxSize().background(Background).statusBarsPadding().verticalScroll(rememberScrollState())) {
        // Header with current date
        Column(Modifier.fillMaxWidth().padding(start = 18.dp, end = 64.dp, top = 14.dp, bottom = 10.dp)) {
            Text("Absensi", style = MaterialTheme.typography.headlineMedium)
            Text(
                "${DateUtils.formatDate(DateUtils.today())} · ${DateUtils.nowTime()}",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }

        // Today status card
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
            shape = RoundedCornerShape(28.dp),
            color = if (state.hasCheckedIn) GreenSoft else Surface, shadowElevation = 2.dp
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Absensi Hari Ini", style = MaterialTheme.typography.titleMedium)
                    Surface(shape = RoundedCornerShape(999.dp), color = if (state.hasCheckedIn) GreenSoft else OrangeSoft) {
                        Text(
                            if (state.hasCheckedIn) if (state.isLate) "Terlambat" else "Tepat Waktu" else "Belum Check-in",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (state.hasCheckedIn && !state.isLate) Green else if (state.isLate) Red else Orange
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                if (state.hasCheckedIn) {
                    InfoRow("Check-in", state.checkInTime, Green)
                    if (state.checkOutTime.isNotEmpty()) InfoRow("Check-out", state.checkOutTime, Blue)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Check-in / Check-out buttons
        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = { vm.simpleCheckIn(empId) },
                modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Green),
                enabled = !state.hasCheckedIn && !state.isLoading
            ) {
                Icon(Icons.Default.Login, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Check-in")
            }
            Button(
                onClick = { vm.simpleCheckOut(state.attendanceId, empId) },
                modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Blue),
                enabled = state.hasCheckedIn && state.checkOutTime.isEmpty() && !state.isLoading
            ) {
                Icon(Icons.Default.Logout, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Check-out")
            }
        }

        Spacer(Modifier.height(16.dp))

        // Attendance calendar
        Surface(Modifier.fillMaxWidth().padding(horizontal = 18.dp), shape = RoundedCornerShape(24.dp), color = Surface, shadowElevation = 1.dp) {
            Column(Modifier.padding(16.dp)) {
                Text("Kalender Kehadiran", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    listOf("S","S","R","K","J","S","M").forEach {
                        Text(it, style = MaterialTheme.typography.labelSmall, color = TextMuted, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                }
                Spacer(Modifier.height(8.dp))
                val days = state.monthlyCalendar
                for (week in days.chunked(7)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        week.forEach { (day, status) ->
                            val bg = when (status) { "present" -> GreenSoft; "late" -> OrangeSoft; "absent" -> RedSoft; "holiday" -> Background; else -> Surface }
                            val fg = when (status) { "present" -> Green; "late" -> Orange; "absent" -> Red; "holiday" -> TextMuted; else -> TextPrimary }
                            Box(Modifier.weight(1f).aspectRatio(1f).padding(2.dp).clip(RoundedCornerShape(12.dp)).background(bg), contentAlignment = Alignment.Center) {
                                Text(if (day > 0) "$day" else "", style = MaterialTheme.typography.labelSmall, color = fg)
                            }
                        }
                        repeat(7 - week.size) { Box(Modifier.weight(1f)) }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    LegendDot(GreenSoft, "Hadir"); LegendDot(OrangeSoft, "Terlambat"); LegendDot(RedSoft, "Alpha")
                }
            }
        }

        if (state.message != null) {
            Spacer(Modifier.height(8.dp))
            Text(state.message!!, modifier = Modifier.padding(horizontal = 18.dp), color = Green, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(100.dp))
    }
}

@Composable
fun AttendanceMonitorScreen(user: User, vm: AttendanceViewModel = viewModel()) {
    LaunchedEffect(Unit) { vm.loadAllToday() }
    val state by vm.state.collectAsState()

    Column(Modifier.fillMaxSize().background(Background).statusBarsPadding().verticalScroll(rememberScrollState())) {
        Text("Monitoring Absensi", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(start = 18.dp, end = 64.dp, top = 14.dp, bottom = 10.dp))
        state.todayList.forEach { att ->
            Surface(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 4.dp), shape = RoundedCornerShape(18.dp), color = Surface, shadowElevation = 1.dp) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(40.dp).clip(RoundedCornerShape(14.dp)).background(if (att.attendanceStatus == "late") OrangeSoft else GreenSoft), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Person, null, tint = if (att.attendanceStatus == "late") Orange else Green, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(att.employeeId, style = MaterialTheme.typography.titleSmall)
                        Text("Check-in: ${att.checkIn} · ${att.attendanceStatus}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                }
            }
        }
        Spacer(Modifier.height(100.dp))
    }
}

@Composable
private fun InfoRow(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Surface(shape = RoundedCornerShape(999.dp), color = color.copy(alpha = 0.1f)) {
            Text(value, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.labelMedium, color = color)
        }
    }
}

@Composable
private fun LegendDot(color: androidx.compose.ui.graphics.Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(color))
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}

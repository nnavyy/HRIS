package com.ptniger.hris.ui.manager

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ptniger.hris.data.model.EmployeePresence
import com.ptniger.hris.data.model.PresenceStatus
import com.ptniger.hris.data.model.User
import com.ptniger.hris.data.repository.EmployeeRepository
import com.ptniger.hris.ui.components.PresenceSummaryChip
import com.ptniger.hris.ui.components.presenceIcon
import com.ptniger.hris.ui.components.presenceColors
import com.ptniger.hris.ui.theme.*
import com.ptniger.hris.utils.DateUtils
import com.ptniger.hris.utils.PresenceResolver

/**
 * PRES-06: Screen khusus Manager untuk lihat semua anggota tim
 * dengan status presence real-time, dikelompokkan per status.
 */
@Composable
fun TeamPresenceScreen(user: User, onBack: () -> Unit, onNavigateToEmployee: (String) -> Unit) {
    var presenceList by remember { mutableStateOf<List<EmployeePresence>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedFilter by remember { mutableStateOf("all") }

    LaunchedEffect(Unit) {
        val empRepo = EmployeeRepository()
        val thisUser = empRepo.getByUserId(user.userId)
        val allEmployees = empRepo.getAll()
        val teamMembers = com.ptniger.hris.utils.HierarchyHelper.getSubordinates(user, thisUser, allEmployees)
        presenceList = PresenceResolver.resolveTeam(teamMembers)
        isLoading = false
    }

    // Group by status
    val grouped = remember(presenceList, selectedFilter) {
        val base = when (selectedFilter) {
            "present" -> presenceList.filter { it.presenceStatus == PresenceStatus.PRESENT_ONSITE || it.presenceStatus == PresenceStatus.LATE_ONSITE }
            "wfh"     -> presenceList.filter { it.presenceStatus == PresenceStatus.PRESENT_WFH }
            "not_yet" -> presenceList.filter { it.presenceStatus == PresenceStatus.NOT_YET }
            "leave"   -> presenceList.filter { it.presenceStatus in listOf(PresenceStatus.ON_LEAVE, PresenceStatus.SICK, PresenceStatus.PERMISSION) }
            "absent"  -> presenceList.filter { it.presenceStatus == PresenceStatus.ABSENT }
            else -> presenceList
        }
        base.groupBy { it.presenceStatus }
    }

    // Summary counts
    val countPresent = presenceList.count { it.presenceStatus == PresenceStatus.PRESENT_ONSITE || it.presenceStatus == PresenceStatus.LATE_ONSITE }
    val countWfh     = presenceList.count { it.presenceStatus == PresenceStatus.PRESENT_WFH }
    val countNotYet  = presenceList.count { it.presenceStatus == PresenceStatus.NOT_YET }
    val countLeave   = presenceList.count { it.presenceStatus in listOf(PresenceStatus.ON_LEAVE, PresenceStatus.SICK, PresenceStatus.PERMISSION) }
    val countAbsent  = presenceList.count { it.presenceStatus == PresenceStatus.ABSENT }

    Column(Modifier.fillMaxSize().background(Background).statusBarsPadding()) {
        // Header
        Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = 14.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
            Column {
                Text("Tim Saya Hari Ini", style = MaterialTheme.typography.headlineMedium)
                Text(DateUtils.todayFormatted(),
                    style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }

        // Summary row
        if (!isLoading) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PresenceSummaryChip("Hadir", countPresent, Green, GreenSoft,
                    selected = selectedFilter == "present") { selectedFilter = if (selectedFilter == "present") "all" else "present" }
                PresenceSummaryChip("WFH", countWfh, Purple, PurpleSoft,
                    selected = selectedFilter == "wfh") { selectedFilter = if (selectedFilter == "wfh") "all" else "wfh" }
                PresenceSummaryChip("Belum", countNotYet, Orange, OrangeSoft,
                    selected = selectedFilter == "not_yet") { selectedFilter = if (selectedFilter == "not_yet") "all" else "not_yet" }
                PresenceSummaryChip("Cuti", countLeave, Blue, BlueSoft,
                    selected = selectedFilter == "leave") { selectedFilter = if (selectedFilter == "leave") "all" else "leave" }
                if (countAbsent > 0) {
                    PresenceSummaryChip("Absen", countAbsent, Red, RedSoft,
                        selected = selectedFilter == "absent") { selectedFilter = if (selectedFilter == "absent") "all" else "absent" }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Blue)
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 18.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Render grouped sections
                val sectionOrder = listOf(
                    PresenceStatus.PRESENT_ONSITE, PresenceStatus.LATE_ONSITE,
                    PresenceStatus.PRESENT_WFH,
                    PresenceStatus.NOT_YET,
                    PresenceStatus.ON_LEAVE, PresenceStatus.SICK, PresenceStatus.PERMISSION,
                    PresenceStatus.ABSENT
                )

                sectionOrder.forEach { status ->
                    val members = grouped[status] ?: return@forEach
                    if (members.isEmpty()) return@forEach

                    item {
                        Spacer(Modifier.height(8.dp))
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                val (_, iconColor) = presenceColors(status)
                                Icon(presenceIcon(status), null, tint = iconColor, modifier = Modifier.size(18.dp))
                                Text("${status.label} (${members.size})",
                                    style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                            }
                            // Tombol "Kirim Pengingat" hanya untuk Belum Absen
                            if (status == PresenceStatus.NOT_YET) {
                                TextButton(onClick = { /* kirim notif ke semua yang belum absen */ }) {
                                    Icon(Icons.Default.NotificationsActive, null,
                                        tint = Orange, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Ingatkan", style = MaterialTheme.typography.labelSmall, color = Orange)
                                }
                            }
                        }
                    }

                    items(members) { pres ->
                        Surface(
                            Modifier.fillMaxWidth().clickable { onNavigateToEmployee(pres.employeeId) },
                            shape = RoundedCornerShape(16.dp), color = Surface, shadowElevation = 1.dp
                        ) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                // Avatar
                                Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(BlueSoft),
                                    contentAlignment = Alignment.Center) {
                                    Text(pres.employeeName.take(2).uppercase(),
                                        style = MaterialTheme.typography.labelLarge, color = Blue)
                                }
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(pres.employeeName, style = MaterialTheme.typography.bodyMedium)
                                    Text(pres.employeePosition,
                                        style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                }
                                // Check-in time atau status
                                Column(horizontalAlignment = Alignment.End) {
                                    if (pres.checkInTime.isNotEmpty()) {
                                        Text(pres.checkInTime,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (pres.lateMinutes > 0) Orange else Green)
                                    }
                                    val (_, sColor) = presenceColors(status)
                                    Icon(presenceIcon(status), null, tint = sColor, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

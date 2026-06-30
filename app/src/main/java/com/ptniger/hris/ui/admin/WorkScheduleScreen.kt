package com.ptniger.hris.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ptniger.hris.data.model.User
import com.ptniger.hris.data.model.WorkSchedule
import com.ptniger.hris.data.repository.WorkScheduleRepository
import com.ptniger.hris.ui.theme.*
import com.ptniger.hris.utils.DateUtils
import kotlinx.coroutines.launch

@Composable
fun WorkScheduleScreen(user: User, onBack: () -> Unit) {
    val repo = remember { WorkScheduleRepository() }
    val scope = rememberCoroutineScope()
    var schedules by remember { mutableStateOf<List<WorkSchedule>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showForm by remember { mutableStateOf(false) }
    var editSchedule by remember { mutableStateOf<WorkSchedule?>(null) }
    var message by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        schedules = repo.getAll()
        isLoading = false
    }

    Column(Modifier.fillMaxSize().background(Background).statusBarsPadding().verticalScroll(rememberScrollState())) {
        // Header
        Row(
            Modifier.fillMaxWidth().padding(start = 4.dp, end = 64.dp, top = 14.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = if (showForm) { { showForm = false; editSchedule = null } } else onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
            }
            Column {
                Text(if (showForm) "Form Jadwal Kerja" else "Jadwal Kerja", style = MaterialTheme.typography.headlineMedium)
                Text("Kelola jadwal kerja karyawan", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }

        message?.let {
            Text(it, Modifier.padding(horizontal = 18.dp), style = MaterialTheme.typography.bodySmall,
                color = if (it.startsWith("✅")) Green else Red)
            Spacer(Modifier.height(8.dp))
        }

        if (showForm) {
            ScheduleForm(
                initial = editSchedule,
                userId = user.userId,
                onSave = { schedule ->
                    scope.launch {
                        val result = repo.save(schedule)
                        if (result.isSuccess) {
                            message = "✅ Jadwal '${schedule.name}' tersimpan"
                            schedules = repo.getAll()
                            showForm = false; editSchedule = null
                        } else {
                            message = "❌ Gagal: ${result.exceptionOrNull()?.message}"
                        }
                    }
                }
            )
        } else {
            if (isLoading) {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Blue)
                }
            } else {
                schedules.forEach { s ->
                    Surface(
                        Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(20.dp), color = Surface, shadowElevation = 1.dp
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(s.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                                IconButton(onClick = { editSchedule = s; showForm = true }) {
                                    Icon(Icons.Default.Edit, null, tint = Blue, modifier = Modifier.size(18.dp))
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text("Masuk: ${s.workStartTime} · Toleransi: ${s.lateThreshold}",
                                style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            Text("Pulang: ${s.workEndTime} · Lembur: >${s.overtimeStartsAfter} (max ${s.maxOvertimeHours} jam)",
                                style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            Text("Early leave sebelum: ${s.earlyLeaveBuffer}",
                                style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            val dayNames = s.workDays.map { when(it) { 1->"Min"; 2->"Sen"; 3->"Sel"; 4->"Rab"; 5->"Kam"; 6->"Jum"; 7->"Sab"; else->"?" } }
                            Text("Hari: ${dayNames.joinToString(", ")}",
                                style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { editSchedule = null; showForm = true },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Blue)
                ) {
                    Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Tambah Jadwal Baru")
                }
            }
        }
        Spacer(Modifier.height(100.dp))
    }
}

@Composable
private fun ScheduleForm(initial: WorkSchedule?, userId: String, onSave: (WorkSchedule) -> Unit) {
    var name by remember(initial) { mutableStateOf(initial?.name ?: "") }
    var scheduleId by remember(initial) { mutableStateOf(initial?.scheduleId ?: "") }
    var startTime by remember(initial) { mutableStateOf(initial?.workStartTime ?: "08:00") }
    var lateThreshold by remember(initial) { mutableStateOf(initial?.lateThreshold ?: "08:15") }
    var earlyLeave by remember(initial) { mutableStateOf(initial?.earlyLeaveBuffer ?: "15:45") }
    var endTime by remember(initial) { mutableStateOf(initial?.workEndTime ?: "16:00") }
    var overtimeAfter by remember(initial) { mutableStateOf(initial?.overtimeStartsAfter ?: "16:10") }
    var maxOT by remember(initial) { mutableStateOf(initial?.maxOvertimeHours?.toString() ?: "4.0") }
    var workDays by remember(initial) { mutableStateOf(initial?.workDays ?: listOf(2,3,4,5,6)) }

    val dayLabels = listOf(2 to "Sen", 3 to "Sel", 4 to "Rab", 5 to "Kam", 6 to "Jum", 7 to "Sab", 1 to "Min")

    Column(Modifier.padding(horizontal = 18.dp)) {
        if (initial == null) {
            OutlinedTextField(value = scheduleId, onValueChange = { scheduleId = it.lowercase().replace(" ", "_") },
                label = { Text("ID Jadwal (unik, lowercase)") }, modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp), singleLine = true)
            Spacer(Modifier.height(8.dp))
        }
        OutlinedTextField(value = name, onValueChange = { name = it },
            label = { Text("Nama Jadwal") }, modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp), singleLine = true)
        Spacer(Modifier.height(12.dp))

        Text("Hari Kerja", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        Spacer(Modifier.height(4.dp))
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), 
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            dayLabels.forEach { (day, label) ->
                FilterChip(
                    selected = day in workDays,
                    onClick = {
                        workDays = if (day in workDays) workDays - day else workDays + day
                    },
                    label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }
        Spacer(Modifier.height(12.dp))

        // Time fields
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = startTime, onValueChange = { startTime = it },
                label = { Text("Jam Masuk") }, modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp), singleLine = true)
            OutlinedTextField(value = lateThreshold, onValueChange = { lateThreshold = it },
                label = { Text("Toleransi") }, modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp), singleLine = true)
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = endTime, onValueChange = { endTime = it },
                label = { Text("Jam Pulang") }, modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp), singleLine = true)
            OutlinedTextField(value = earlyLeave, onValueChange = { earlyLeave = it },
                label = { Text("Min. Pulang") }, modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp), singleLine = true)
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = overtimeAfter, onValueChange = { overtimeAfter = it },
                label = { Text("Lembur Mulai") }, modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp), singleLine = true)
            OutlinedTextField(value = maxOT, onValueChange = { maxOT = it },
                label = { Text("Max Lembur (jam)") }, modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp), singleLine = true)
        }

        Spacer(Modifier.height(16.dp))

        // Preview card
        Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = BlueSoft) {
            Column(Modifier.padding(12.dp)) {
                Text("Preview", style = MaterialTheme.typography.labelSmall, color = Blue)
                Spacer(Modifier.height(4.dp))
                val lateMins = DateUtils.calculateLateMinutes("08:20", startTime, lateThreshold)
                val otHours = DateUtils.calculateOvertimeHours("17:30", overtimeAfter, maxOT.toDoubleOrNull() ?: 4.0)
                Text("Masuk 08:20 → ${if (lateMins > 0) "TERLAMBAT $lateMins menit" else "On-time"}",
                    style = MaterialTheme.typography.bodySmall, color = if (lateMins > 0) Orange else Green)
                Text("Pulang 17:30 → ${if (otHours > 0) "LEMBUR $otHours jam" else "Tidak ada lembur"}",
                    style = MaterialTheme.typography.bodySmall, color = if (otHours > 0) Purple else Green)
            }
        }

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                val id = if (initial != null) initial.scheduleId else scheduleId
                if (id.isNotBlank() && name.isNotBlank()) {
                    onSave(WorkSchedule(
                        scheduleId = id, name = name, workDays = workDays.sorted(),
                        workStartTime = startTime, lateThreshold = lateThreshold,
                        earlyLeaveBuffer = earlyLeave, workEndTime = endTime,
                        overtimeStartsAfter = overtimeAfter,
                        maxOvertimeHours = maxOT.toDoubleOrNull() ?: 4.0,
                        createdBy = userId
                    ))
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Blue),
            enabled = name.isNotBlank() && (initial != null || scheduleId.isNotBlank())
        ) {
            Text(if (initial != null) "Simpan Perubahan" else "Buat Jadwal")
        }
    }
}

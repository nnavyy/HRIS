package com.ptniger.hris.ui.leave

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ptniger.hris.data.model.LeaveRequest
import com.ptniger.hris.data.model.User
import com.ptniger.hris.data.repository.EmployeeRepository
import com.ptniger.hris.ui.theme.*
import kotlinx.coroutines.launch
import java.util.Calendar

@Composable
fun LeaveRequestScreen(user: User, vm: LeaveViewModel = viewModel()) {
    var type by remember { mutableStateOf("Cuti Tahunan") }
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }
    var leaveQuota by remember { mutableIntStateOf(12) }
    var employeeDocId by remember { mutableStateOf("") } // Actual Firestore document ID
    val leaves by vm.leaves.collectAsState()
    val message by vm.message.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Resolve the employee ID robustly:
    // Try employeeId first, then fall back to userId for querying leave history
    val effectiveEmpId = user.employeeId.ifEmpty { user.userId }

    LaunchedEffect(effectiveEmpId) {
        // Load leave history — try both employeeId and userId as query keys
        vm.loadByEmployeeWithFallback(effectiveEmpId, user.userId)
        // Load actual quota from Firestore employee document
        try {
            val empRepo = EmployeeRepository()
            val employee = empRepo.getByUserId(user.userId)
                ?: empRepo.getById(user.employeeId)
            leaveQuota = employee?.leaveQuota ?: 12
            employeeDocId = employee?.employeeId ?: ""
        } catch (_: Exception) {}
    }

    // Refresh history when message changes (after submit)
    LaunchedEffect(message) {
        if (message?.contains("berhasil") == true) {
            vm.loadByEmployeeWithFallback(effectiveEmpId, user.userId)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        // Title — right-padded for avatar
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 72.dp, top = 14.dp, bottom = 10.dp)
        ) {
            Text("Pengajuan Cuti", style = MaterialTheme.typography.headlineMedium)
        }

        // Form card
        Surface(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp),
            shape = RoundedCornerShape(24.dp),
            color = Surface,
            shadowElevation = 2.dp
        ) {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header with live quota badge
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Ajukan Cuti", style = MaterialTheme.typography.titleMedium)
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = when {
                            leaveQuota > 5 -> BlueSoft
                            leaveQuota > 0 -> OrangeSoft
                            else -> RedSoft
                        }
                    ) {
                        Text(
                            "Sisa $leaveQuota hari",
                            Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = when {
                                leaveQuota > 5 -> Blue
                                leaveQuota > 0 -> Orange
                                else -> Red
                            }
                        )
                    }
                }

                // Leave type dropdown
                var typeExpanded by remember { mutableStateOf(false) }
                val leaveTypes = listOf("Cuti Tahunan", "Cuti Sakit", "Cuti Melahirkan", "Izin", "Darurat")
                val emergencyTypes = listOf("Cuti Sakit", "Cuti Melahirkan", "Darurat")
                
                Column {
                    Text("Jenis Cuti", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        onClick = { typeExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = Surface,
                        border = ButtonDefaults.outlinedButtonBorder
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(type, style = MaterialTheme.typography.bodyMedium)
                                if (emergencyTypes.contains(type)) {
                                    Surface(shape = RoundedCornerShape(4.dp), color = RedSoft) {
                                        Text("Darurat", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = Red)
                                    }
                                }
                            }
                            Icon(Icons.Default.ArrowDropDown, null, tint = TextSecondary)
                        }
                    }
                    DropdownMenu(
                        expanded = typeExpanded,
                        onDismissRequest = { typeExpanded = false }
                    ) {
                        leaveTypes.forEach { t ->
                            DropdownMenuItem(
                                text = { 
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(t) 
                                        if (emergencyTypes.contains(t)) {
                                            Surface(shape = RoundedCornerShape(4.dp), color = RedSoft) {
                                                Text("Darurat", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = Red)
                                            }
                                        }
                                    }
                                },
                                onClick = { type = t; typeExpanded = false }
                            )
                        }
                    }
                }

                // Start Date
                DatePickerField(
                    label = "Tanggal Mulai",
                    value = startDate,
                    context = context,
                    minDate = null,
                    onDateSelected = { startDate = it; validationError = null }
                )

                // End Date
                DatePickerField(
                    label = "Tanggal Selesai",
                    value = endDate,
                    context = context,
                    minDate = startDate,
                    onDateSelected = { endDate = it; validationError = null }
                )

                // Duration preview
                if (startDate.isNotEmpty() && endDate.isNotEmpty() && endDate >= startDate) {
                    val dur = calcDuration(startDate, endDate)
                    Surface(shape = RoundedCornerShape(10.dp), color = if (dur <= leaveQuota) BlueSoft else RedSoft) {
                        Text(
                            "$dur hari ${if (dur > leaveQuota) "⚠ Melebihi kuota!" else ""}",
                            Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (dur <= leaveQuota) Blue else Red
                        )
                    }
                }

                LabeledField("Alasan", reason) { reason = it; validationError = null }

                // Policy Warning
                Column {
                    Text("* Berdasarkan kebijakan, pengajuan cuti mungkin ditolak otomatis oleh sistem jika tidak diajukan tepat waktu (misal: H-3).", 
                        style = MaterialTheme.typography.labelSmall, color = Orange)
                    if (emergencyTypes.contains(type)) {
                        Text("* Cuti tipe Darurat tidak memotong kuota dan mengabaikan aturan batas H-3.", 
                            style = MaterialTheme.typography.labelSmall, color = Red, modifier = Modifier.padding(top = 4.dp))
                    }
                }

                // Error / success messages
                if (validationError != null) {
                    Surface(shape = RoundedCornerShape(10.dp), color = RedSoft) {
                        Text(
                            validationError!!,
                            Modifier.padding(10.dp),
                            color = Red,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                if (message != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (message!!.contains("Error") || message!!.contains("Kuota") || message!!.contains("Melebihi")) RedSoft else GreenSoft
                    ) {
                        Text(
                            message!!,
                            Modifier.padding(10.dp),
                            color = if (message!!.contains("Error") || message!!.contains("Kuota") || message!!.contains("Melebihi")) Red else Green,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                // Submit button
                Button(
                    onClick = {
                        validationError = null
                        val dur = if (startDate.isNotEmpty() && endDate.isNotEmpty()) calcDuration(startDate, endDate) else 0

                        when {
                            startDate.isBlank() -> { validationError = "Pilih tanggal mulai"; return@Button }
                            endDate.isBlank() -> { validationError = "Pilih tanggal selesai"; return@Button }
                            reason.isBlank() -> { validationError = "Alasan wajib diisi"; return@Button }
                            endDate < startDate -> { validationError = "Tanggal selesai harus setelah tanggal mulai"; return@Button }
                            dur > leaveQuota -> { validationError = "Melebihi sisa kuota ($leaveQuota hari). Durasi pengajuan: $dur hari."; return@Button }
                            leaveQuota <= 0 -> { validationError = "Kuota cuti sudah habis!"; return@Button }
                        }

                        val empId = user.employeeId.ifEmpty { user.userId }
                        vm.submitWithQuotaUpdate(
                            leave = LeaveRequest(
                                employeeId = empId,
                                employeeName = user.fullName.ifEmpty { user.name },
                                departmentId = user.departmentId,
                                type = type,
                                startDate = startDate,
                                endDate = endDate,
                                duration = dur,
                                reason = reason
                            ),
                            userId = user.userId,
                            employeeDocId = employeeDocId,
                            duration = dur,
                            currentQuota = leaveQuota
                        )

                        // Optimistic UI update
                        leaveQuota = (leaveQuota - dur).coerceAtLeast(0)
                        startDate = ""; endDate = ""; reason = ""
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Blue),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(Modifier.size(18.dp), color = Surface, strokeWidth = 2.dp)
                    } else {
                        Text("Kirim Pengajuan")
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Leave history card
        Surface(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp),
            shape = RoundedCornerShape(24.dp),
            color = Surface,
            shadowElevation = 1.dp
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Riwayat Cuti", style = MaterialTheme.typography.titleMedium)
                    if (isLoading) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                }
                Spacer(Modifier.height(12.dp))
                if (leaves.isEmpty()) {
                    Box(Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                        Text(
                            "Belum ada riwayat cuti",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                } else {
                    leaves.forEachIndexed { idx, leave ->
                        val displayDate = if (leave.startDate.isNotEmpty() && leave.endDate.isNotEmpty()) {
                            "${com.ptniger.hris.utils.DateUtils.formatDate(leave.startDate)} → ${com.ptniger.hris.utils.DateUtils.formatDate(leave.endDate)} (${leave.duration} hari)"
                        } else if (leave.startDate.isNotEmpty()) {
                            "${com.ptniger.hris.utils.DateUtils.formatDate(leave.startDate)} (${leave.duration} hari)"
                        } else {
                            "Tanggal tidak tersedia (${leave.duration} hari)"
                        }
                        LeaveRow(
                            title = leave.type,
                            date = displayDate,
                            status = leave.status,
                            rejectionReason = leave.rejectionReason,
                            autoRejected = leave.autoRejected
                        )
                        if (idx < leaves.lastIndex) HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    }
                }
            }
        }
        Spacer(Modifier.height(100.dp))
    }
}

@Composable
fun LeaveApprovalScreen(user: User, onBack: () -> Unit = {}, vm: LeaveViewModel = viewModel()) {
    val pending by vm.pending.collectAsState()
    val message by vm.message.collectAsState()
    LaunchedEffect(Unit) {
        val role = user.primaryRole.ifEmpty { user.role }
        val employeeRepo = com.ptniger.hris.data.repository.EmployeeRepository()
        val employee = employeeRepo.getByUserId(user.userId)
        val empId = employee?.employeeId ?: user.employeeId

        vm.loadPendingForApprover(empId, role)
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 72.dp, top = 14.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text("Approval Cuti", style = MaterialTheme.typography.headlineMedium)
            }
            if (pending.isNotEmpty()) {
                Surface(shape = RoundedCornerShape(999.dp), color = OrangeSoft) {
                    Text("${pending.size} Pending", Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = Orange)
                }
            }
        }

        if (message != null) {
            Surface(
                Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                color = GreenSoft
            ) {
                Text(message!!, Modifier.padding(10.dp), color = Green, style = MaterialTheme.typography.bodySmall)
            }
        }

        pending.forEach { leave ->
            Surface(
                Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 4.dp),
                shape = RoundedCornerShape(22.dp),
                color = Surface,
                shadowElevation = 1.dp
            ) {
                Column(Modifier.padding(14.dp)) {
                    val role = user.primaryRole.ifEmpty { user.role }
                    val isSuperAdmin = role == com.ptniger.hris.utils.Constants.Role.SUPER_ADMIN

                    if (leave.requesterRole == com.ptniger.hris.utils.Constants.Role.MANAGER) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = PurpleSoft,
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Text(
                                "Diajukan oleh Manager → HR yang approve",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = Purple
                            )
                        }
                    }

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text(leave.employeeName, style = MaterialTheme.typography.titleSmall)
                            Text("${leave.type} · ${leave.duration} hari", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            val displayDate = if (leave.startDate.isNotEmpty() && leave.endDate.isNotEmpty()) {
                                "${com.ptniger.hris.utils.DateUtils.formatDate(leave.startDate)} → ${com.ptniger.hris.utils.DateUtils.formatDate(leave.endDate)}"
                            } else {
                                "Tanggal tidak tersedia"
                            }
                            Text(displayDate, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            Text("Alasan: ${leave.reason}", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    
                    if (!isSuperAdmin) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { vm.approve(leave.leaveId, user.userId) },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Green),
                                modifier = Modifier.weight(1f)
                            ) { Text("Setuju") }
                            OutlinedButton(
                                onClick = { vm.reject(leave.leaveId, user.userId) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) { Text("Tolak", color = Red) }
                        }
                    } else {
                        Surface(shape = RoundedCornerShape(8.dp), color = CardBorder.copy(alpha = 0.3f)) {
                            Text(
                                "👁 Mode Audit — Super Admin tidak bisa approve cuti",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }
        if (pending.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("Tidak ada pengajuan cuti", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            }
        }
        Spacer(Modifier.height(100.dp))
    }
}

// ============== Helpers ==============

private fun calcDuration(startDate: String, endDate: String): Int {
    return try {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val start = sdf.parse(startDate)
        val end = sdf.parse(endDate)
        if (start != null && end != null) ((end.time - start.time) / (1000 * 60 * 60 * 24)).toInt() + 1 else 1
    } catch (_: Exception) { 1 }
}

@Composable
private fun DatePickerField(
    label: String,
    value: String,
    context: android.content.Context,
    minDate: String?,
    onDateSelected: (String) -> Unit
) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        Spacer(Modifier.height(4.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Surface,
            border = ButtonDefaults.outlinedButtonBorder,
            onClick = {
                val cal = Calendar.getInstance()
                // If minDate is set (for end date), start picker from that date
                if (!minDate.isNullOrEmpty()) {
                    try {
                        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                        val min = sdf.parse(minDate)
                        if (min != null) cal.time = min
                    } catch (_: Exception) {}
                }
                val dialog = DatePickerDialog(
                    context,
                    { _, year, month, day ->
                        val dateStr = String.format("%04d-%02d-%02d", year, month + 1, day)
                        onDateSelected(dateStr)
                    },
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
                )
                // Restrict past dates for start date, restrict before startDate for end date
                if (!minDate.isNullOrEmpty()) {
                    try {
                        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                        val min = sdf.parse(minDate)
                        if (min != null) dialog.datePicker.minDate = min.time
                    } catch (_: Exception) {}
                } else {
                    dialog.datePicker.minDate = System.currentTimeMillis() - 1000
                }
                dialog.show()
            }
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (value.isNotEmpty()) value else "Pilih tanggal",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (value.isNotEmpty()) TextPrimary else TextMuted
                )
                Icon(Icons.Default.CalendarMonth, null, tint = Blue, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun LabeledField(label: String, value: String, onChange: (String) -> Unit) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = value, onValueChange = onChange,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            minLines = 1
        )
    }
}

@Composable
private fun LeaveRow(title: String, date: String, status: String, rejectionReason: String? = null, autoRejected: Boolean = false) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(date, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            Spacer(Modifier.width(8.dp))
            val (bg, fg) = when (status) {
                "approved" -> GreenSoft to Green
                "rejected" -> RedSoft to Red
                else -> OrangeSoft to Orange
            }
            Surface(shape = RoundedCornerShape(999.dp), color = bg) {
                Text(
                    when (status) { "approved" -> "Disetujui"; "rejected" -> if (autoRejected) "Auto Reject" else "Ditolak"; else -> "Menunggu" },
                    Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = fg
                )
            }
        }
        if (status == "rejected" && !rejectionReason.isNullOrBlank()) {
            Spacer(Modifier.height(4.dp))
            Surface(shape = RoundedCornerShape(8.dp), color = RedSoft) {
                Text(
                    "Alasan: $rejectionReason",
                    Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = Red
                )
            }
        }
    }
}

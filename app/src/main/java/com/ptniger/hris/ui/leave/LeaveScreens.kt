package com.ptniger.hris.ui.leave

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ptniger.hris.data.model.LeaveRequest
import com.ptniger.hris.data.model.User
import com.ptniger.hris.ui.theme.*

@Composable
fun LeaveRequestScreen(user: User, vm: LeaveViewModel = viewModel()) {
    var type by remember { mutableStateOf("Cuti Tahunan") }
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    val leaves by vm.leaves.collectAsState()
    val message by vm.message.collectAsState()

    LaunchedEffect(Unit) { vm.loadByEmployee(user.employeeId) }

    Column(Modifier.fillMaxSize().background(Background).verticalScroll(rememberScrollState())) {
        Text("Pengajuan Cuti", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(18.dp))

        Surface(Modifier.fillMaxWidth().padding(horizontal = 18.dp), shape = RoundedCornerShape(24.dp), color = Surface, shadowElevation = 2.dp) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Ajukan Cuti", style = MaterialTheme.typography.titleMedium)
                    Surface(shape = RoundedCornerShape(999.dp), color = BlueSoft) {
                        Text("Sisa 12 hari", Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = Blue)
                    }
                }
                LabeledField("Jenis Cuti", type) { type = it }
                LabeledField("Tanggal Mulai (YYYY-MM-DD)", startDate) { startDate = it }
                LabeledField("Tanggal Selesai (YYYY-MM-DD)", endDate) { endDate = it }
                LabeledField("Alasan", reason) { reason = it }

                if (message != null) Text(message!!, color = if (message!!.contains("Error")) Red else Green, style = MaterialTheme.typography.bodySmall)

                Button(onClick = {
                    vm.submit(LeaveRequest(employeeId = user.employeeId, employeeName = user.name, type = type, startDate = startDate, endDate = endDate, duration = 1, reason = reason))
                }, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = Blue)) {
                    Text("Kirim Pengajuan")
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Surface(Modifier.fillMaxWidth().padding(horizontal = 18.dp), shape = RoundedCornerShape(24.dp), color = Surface, shadowElevation = 1.dp) {
            Column(Modifier.padding(16.dp)) {
                Text("Riwayat Cuti", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                leaves.forEach { leave ->
                    LeaveRow(leave.type, "${leave.startDate} - ${leave.endDate}", leave.status)
                    if (leave != leaves.last()) HorizontalDivider(Modifier.padding(vertical = 8.dp))
                }
                if (leaves.isEmpty()) Text("Belum ada riwayat", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }
        Spacer(Modifier.height(100.dp))
    }
}

@Composable
fun LeaveApprovalScreen(user: User, vm: LeaveViewModel = viewModel()) {
    val pending by vm.pending.collectAsState()
    val message by vm.message.collectAsState()
    LaunchedEffect(Unit) { vm.loadPending() }

    Column(Modifier.fillMaxSize().background(Background).verticalScroll(rememberScrollState())) {
        Row(Modifier.fillMaxWidth().padding(18.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Approval Cuti", style = MaterialTheme.typography.headlineMedium)
            Surface(shape = RoundedCornerShape(999.dp), color = OrangeSoft) {
                Text("${pending.size} Pending", Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = Orange)
            }
        }

        if (message != null) Text(message!!, Modifier.padding(horizontal = 18.dp), color = Green, style = MaterialTheme.typography.bodySmall)

        pending.forEach { leave ->
            Surface(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 4.dp), shape = RoundedCornerShape(22.dp), color = Surface, shadowElevation = 1.dp) {
                Column(Modifier.padding(14.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(leave.employeeName, style = MaterialTheme.typography.titleSmall)
                            Text("${leave.type} · ${leave.startDate} - ${leave.endDate}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            Text("Alasan: ${leave.reason}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { vm.approve(leave.leaveId, user.userId) }, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Green), modifier = Modifier.weight(1f)) { Text("Setuju") }
                        OutlinedButton(onClick = { vm.reject(leave.leaveId, user.userId) }, shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f)) { Text("Tolak", color = Red) }
                    }
                }
            }
        }
        if (pending.isEmpty()) Text("Tidak ada pengajuan cuti", Modifier.padding(18.dp), style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Spacer(Modifier.height(100.dp))
    }
}

@Composable
private fun LabeledField(label: String, value: String, onChange: (String) -> Unit) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(value = value, onValueChange = onChange, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), singleLine = true)
    }
}

@Composable
private fun LeaveRow(title: String, date: String, status: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(date, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        val (bg, fg) = when (status) { "approved" -> GreenSoft to Green; "rejected" -> RedSoft to Red; else -> OrangeSoft to Orange }
        Surface(shape = RoundedCornerShape(999.dp), color = bg) {
            Text(when (status) { "approved" -> "Disetujui"; "rejected" -> "Ditolak"; else -> "Menunggu" },
                Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = fg)
        }
    }
}

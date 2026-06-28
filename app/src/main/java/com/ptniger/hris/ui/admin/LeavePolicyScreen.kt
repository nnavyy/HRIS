package com.ptniger.hris.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ptniger.hris.data.model.LeavePolicy
import com.ptniger.hris.data.model.User
import com.ptniger.hris.data.repository.LeavePolicyRepository
import com.ptniger.hris.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun LeavePolicyScreen(user: User, onBack: () -> Unit) {
    val repo = remember { LeavePolicyRepository() }
    val scope = rememberCoroutineScope()
    var policy by remember { mutableStateOf<LeavePolicy?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var message by remember { mutableStateOf<String?>(null) }

    var minDays by remember { mutableStateOf("3") }
    var maxDays by remember { mutableStateOf("12") }
    var allowPast by remember { mutableStateOf(false) }
    var autoReject by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val p = repo.getActivePolicy()
        policy = p
        minDays = p.minAdvanceDays.toString()
        maxDays = p.maxDaysPerRequest.toString()
        allowPast = p.allowPastDateSubmission
        autoReject = p.autoRejectOnExpiry
        isLoading = false
    }

    Column(Modifier.fillMaxSize().background(Background).statusBarsPadding().verticalScroll(rememberScrollState())) {
        Row(
            Modifier.fillMaxWidth().padding(start = 4.dp, end = 64.dp, top = 14.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
            }
            Column {
                Text("Kebijakan Cuti", style = MaterialTheme.typography.headlineMedium)
                Text("Atur aturan pengajuan cuti", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }

        message?.let {
            Text(it, Modifier.padding(horizontal = 18.dp), style = MaterialTheme.typography.bodySmall,
                color = if (it.startsWith("✅")) Green else Red)
            Spacer(Modifier.height(8.dp))
        }

        if (isLoading) {
            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Blue)
            }
        } else {
            Column(Modifier.padding(horizontal = 18.dp)) {
                // Min advance days
                OutlinedTextField(
                    value = minDays,
                    onValueChange = { if (it.isEmpty() || it.all { c -> c.isDigit() }) minDays = it },
                    label = { Text("Minimal hari sebelum tanggal mulai") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    supportingText = { Text("Karyawan harus mengajukan H-${minDays.ifEmpty { "0" }} minimal") }
                )
                Spacer(Modifier.height(8.dp))

                // Max days per request
                OutlinedTextField(
                    value = maxDays,
                    onValueChange = { if (it.isEmpty() || it.all { c -> c.isDigit() }) maxDays = it },
                    label = { Text("Maksimal hari per pengajuan") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    supportingText = { Text("Satu pengajuan tidak bisa lebih dari ${maxDays.ifEmpty { "0" }} hari") }
                )
                Spacer(Modifier.height(16.dp))

                // Toggle switches
                Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = Surface) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Izinkan tanggal lampau", style = MaterialTheme.typography.bodyMedium)
                                Text("Karyawan bisa ajukan cuti untuk tanggal yang sudah lewat",
                                    style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            }
                            Switch(checked = allowPast, onCheckedChange = { allowPast = it })
                        }
                        HorizontalDivider(Modifier.padding(vertical = 12.dp), color = CardBorder)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Auto-tolak jika melebihi batas", style = MaterialTheme.typography.bodyMedium)
                                Text("Sistem otomatis menolak pengajuan yang tidak memenuhi syarat",
                                    style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            }
                            Switch(checked = autoReject, onCheckedChange = { autoReject = it })
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Preview card
                val sdf = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
                val futureDate = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_MONTH, (minDays.toIntOrNull() ?: 0))
                }
                Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = BlueSoft) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Preview Kebijakan", style = MaterialTheme.typography.labelSmall, color = Blue)
                        Spacer(Modifier.height(4.dp))
                        Text("Karyawan yang mengajukan hari ini tidak dapat memilih tanggal mulai sebelum ${sdf.format(futureDate.time)}",
                            style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                        Text("Maks ${maxDays.ifEmpty { "0" }} hari per pengajuan${if (autoReject) " · Auto-reject aktif" else ""}",
                            style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                }

                Spacer(Modifier.height(20.dp))

                Button(
                    onClick = {
                        scope.launch {
                            val updated = LeavePolicy(
                                policyId = "default",
                                companyId = "default",
                                minAdvanceDays = minDays.toIntOrNull() ?: 3,
                                maxDaysPerRequest = maxDays.toIntOrNull() ?: 12,
                                allowPastDateSubmission = allowPast,
                                autoRejectOnExpiry = autoReject,
                                updatedBy = user.userId,
                                updatedAt = System.currentTimeMillis()
                            )
                            val result = repo.update(updated)
                            message = if (result.isSuccess) "✅ Kebijakan cuti tersimpan"
                                      else "❌ Gagal: ${result.exceptionOrNull()?.message}"
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Blue)
                ) {
                    Icon(Icons.Default.Save, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Simpan Kebijakan")
                }
            }
        }
        Spacer(Modifier.height(100.dp))
    }
}

package com.ptniger.hris.ui.kpi

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ptniger.hris.data.model.Employee
import com.ptniger.hris.data.model.KpiDimension
import com.ptniger.hris.data.model.PeerReview
import com.ptniger.hris.data.model.User
import com.ptniger.hris.data.repository.EmployeeRepository
import com.ptniger.hris.data.repository.PeerReviewRepository
import com.ptniger.hris.ui.theme.*
import com.ptniger.hris.utils.DateUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeerReviewScreen(
    user: User,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var employees by remember { mutableStateOf<List<Employee>>(emptyList()) }
    var selectedEmployee by remember { mutableStateOf<Employee?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var alreadyReviewed by remember { mutableStateOf(false) }
    
    // Period = current month YYYY-MM
    val currentPeriod = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
    
    // Score sliders per dimension
    val dimensionLabels = mapOf(
        KpiDimension.PERFORMANCE       to "Performa Kerja",
        KpiDimension.OUTPUT_QUALITY    to "Kualitas Output",
        KpiDimension.TEAM_CONTRIBUTION to "Kontribusi Tim",
        KpiDimension.DISCIPLINE        to "Disiplin & Etika"
    )
    val scores = remember { mutableStateMapOf<String, Float>().apply {
        dimensionLabels.keys.forEach { put(it, 50f) }
    }}
    var comments by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val empRepo = EmployeeRepository()
        employees = empRepo.getAll().filter { it.userId != user.uid && it.userId.isNotEmpty() }
        isLoading = false
    }

    LaunchedEffect(selectedEmployee) {
        val emp = selectedEmployee ?: return@LaunchedEffect
        val repo = PeerReviewRepository()
        val reviewerUid = user.uid
        alreadyReviewed = repo.hasPeerReviewed(reviewerUid, emp.userId, currentPeriod)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        // Top bar
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
            Column(Modifier.padding(start = 4.dp)) {
                Text("Peer Review", style = MaterialTheme.typography.titleLarge)
                Text("Periode: $currentPeriod", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }

        Column(Modifier.padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            when {
                isLoading -> Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }

                employees.isEmpty() -> Text("Tidak ada karyawan lain untuk direview.", color = TextSecondary)

                else -> {
                    // Employee Selector
                    Text("Pilih Karyawan yang Dinilai", style = MaterialTheme.typography.titleSmall)
                    var expandedEmp by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = expandedEmp, onExpandedChange = { expandedEmp = it }) {
                        OutlinedTextField(
                            value = selectedEmployee?.name ?: "Pilih karyawan...",
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            shape = RoundedCornerShape(16.dp),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedEmp) }
                        )
                        ExposedDropdownMenu(expanded = expandedEmp, onDismissRequest = { expandedEmp = false }) {
                            employees.forEach { emp ->
                                DropdownMenuItem(
                                    text = { Text("${emp.name} · ${emp.position}") },
                                    onClick = {
                                        selectedEmployee = emp
                                        expandedEmp = false
                                        message = null
                                    }
                                )
                            }
                        }
                    }

                    if (selectedEmployee != null) {
                        if (alreadyReviewed) {
                            // Already reviewed — show read-only badge
                            Surface(shape = RoundedCornerShape(16.dp), color = GreenSoft, modifier = Modifier.fillMaxWidth()) {
                                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, null, tint = Green, modifier = Modifier.size(24.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text("Sudah Direview", style = MaterialTheme.typography.titleSmall, color = Green)
                                        Text("Kamu sudah memberikan penilaian untuk ${selectedEmployee!!.name} periode ini.", style = MaterialTheme.typography.bodySmall, color = Green)
                                    }
                                }
                            }
                        } else {
                            // Score sliders per dimension
                            Text("Penilaian per Dimensi", style = MaterialTheme.typography.titleSmall)
                            dimensionLabels.forEach { (dim, label) ->
                                Surface(shape = RoundedCornerShape(16.dp), color = Surface, shadowElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
                                    Column(Modifier.padding(14.dp)) {
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(label, style = MaterialTheme.typography.bodyMedium)
                                            Text("${scores[dim]?.toInt() ?: 50}/100", style = MaterialTheme.typography.titleSmall, color = Blue)
                                        }
                                        Slider(
                                            value = scores[dim] ?: 50f,
                                            onValueChange = { scores[dim] = it },
                                            valueRange = 0f..100f,
                                            steps = 99,
                                            colors = SliderDefaults.colors(thumbColor = Blue, activeTrackColor = Blue)
                                        )
                                    }
                                }
                            }

                            // Comments
                            Text("Komentar (Opsional)", style = MaterialTheme.typography.titleSmall)
                            OutlinedTextField(
                                value = comments,
                                onValueChange = { comments = it },
                                modifier = Modifier.fillMaxWidth().height(100.dp),
                                shape = RoundedCornerShape(16.dp),
                                placeholder = { Text("Berikan komentar konstruktif...") },
                                maxLines = 4
                            )

                            // Message
                            message?.let {
                                Surface(shape = RoundedCornerShape(12.dp), color = if (it.contains("berhasil")) GreenSoft else RedSoft) {
                                    Text(it, Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
                                }
                            }

                            // Submit button
                            Button(
                                onClick = {
                                    val target = selectedEmployee ?: return@Button
                                    scope.launch {
                                        isSaving = true
                                        message = null
                                        val repo = PeerReviewRepository()
                                        // Average score across all dimensions for the single PeerReview record
                                        val avgScore = scores.values.map { it.toInt() }.average().toInt()
                                        // Submit one record per dimension
                                        var success = true
                                        scores.forEach { (dim, score) ->
                                            val review = PeerReview(
                                                targetEmployeeId = target.userId,
                                                reviewerEmployeeId = user.uid,
                                                period = currentPeriod,
                                                score = score.toInt(),
                                                comments = if (dim == scores.keys.last()) comments else "",
                                                dimension = dim
                                            )
                                            repo.submit(review).onFailure { success = false }
                                        }
                                        if (success) {
                                            message = "Peer review berhasil dikirim! ✅"
                                            alreadyReviewed = true
                                        } else {
                                            message = "Ada error saat mengirim review. Coba lagi."
                                        }
                                        isSaving = false
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                shape = RoundedCornerShape(16.dp),
                                enabled = !isSaving,
                                colors = ButtonDefaults.buttonColors(containerColor = Purple)
                            ) {
                                if (isSaving) {
                                    CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.Star, null, Modifier.size(20.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Kirim Peer Review")
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

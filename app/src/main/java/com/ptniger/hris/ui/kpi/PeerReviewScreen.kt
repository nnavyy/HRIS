package com.ptniger.hris.ui.kpi

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
    val scores = remember { mutableStateMapOf<String, Int>().apply {
        dimensionLabels.keys.forEach { put(it, 0) }
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
                            Text("Penilaian per Dimensi (Bintang 1-5)", style = MaterialTheme.typography.titleSmall)
                            dimensionLabels.forEach { (dim, label) ->
                                val currentRating = scores[dim] ?: 0
                                Surface(shape = RoundedCornerShape(16.dp), color = Surface, shadowElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
                                    Column(Modifier.padding(14.dp)) {
                                        Text(label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.align(Alignment.CenterHorizontally))
                                        Spacer(Modifier.height(12.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceEvenly
                                        ) {
                                            for (i in 1..5) {
                                                val isSelected = i <= currentRating
                                                Icon(
                                                    imageVector = Icons.Default.Star,
                                                    contentDescription = null,
                                                    tint = if (isSelected) Color(0xFFFFC107) else Color.LightGray.copy(alpha = 0.5f),
                                                    modifier = Modifier
                                                        .size(48.dp)
                                                        .clickable(
                                                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                                            indication = null
                                                        ) { scores[dim] = i }
                                                )
                                            }
                                        }
                                        Spacer(Modifier.height(8.dp))
                                        val desc = when (currentRating) {
                                            1 -> "Sangat Kurang"
                                            2 -> "Kurang"
                                            3 -> "Cukup Baik"
                                            4 -> "Baik"
                                            5 -> "Luar Biasa"
                                            else -> "Belum Dinilai"
                                        }
                                        Text(
                                            text = desc,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = if (currentRating > 0) Blue else TextSecondary,
                                            modifier = Modifier.align(Alignment.CenterHorizontally)
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
                                        val allRated = scores.values.all { it > 0 }
                                        if (!allRated) {
                                            message = "Harap nilai semua dimensi (bintang 1-5)!"
                                            isSaving = false
                                            return@launch
                                        }
                                        // Submit one record per dimension
                                        var success = true
                                        scores.forEach { (dim, starScore) ->
                                            val review = PeerReview(
                                                targetEmployeeId = target.userId,
                                                reviewerEmployeeId = user.uid,
                                                period = currentPeriod,
                                                score = starScore * 20, // Konversi 5 Bintang -> Skala 100
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

            Spacer(Modifier.height(120.dp))
        }
    }
}

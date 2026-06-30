package com.ptniger.hris.ui.employee

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ptniger.hris.data.model.Attendance
import com.ptniger.hris.data.model.Employee
import com.ptniger.hris.data.model.User
import com.ptniger.hris.data.repository.AttendanceRepository
import com.ptniger.hris.data.repository.EmployeeRepository
import com.ptniger.hris.data.repository.KpiRepository
import com.ptniger.hris.ui.theme.*
import com.ptniger.hris.utils.Constants
import com.ptniger.hris.utils.DateUtils
import java.util.Calendar
import kotlinx.coroutines.launch

@Composable
fun EmployeeDetailScreen(
    employeeId: String,
    user: User,
    onBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit = {},
    onNavigateToFaceRegistration: (String, String) -> Unit = { _, _ -> }
) {
    var employee by remember { mutableStateOf<Employee?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var todayAttendance by remember { mutableStateOf<Attendance?>(null) }
    var monthlyStats by remember { mutableStateOf<MonthlyAttStats?>(null) }
    var kpiScore by remember { mutableStateOf(0.0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(employeeId) {
        val empRepo = EmployeeRepository()
        val attRepo = AttendanceRepository()
        val kpiRepo = KpiRepository()
        val cal = Calendar.getInstance()

        employee = empRepo.getById(employeeId)
        todayAttendance = attRepo.getTodayAttendance(employeeId)

        // Statistik absensi bulan ini
        val monthlyAtt = attRepo.getMonthlyAttendance(
            employeeId, cal.get(Calendar.MONTH) + 1, cal.get(Calendar.YEAR)
        )
        val presentDays  = monthlyAtt.count { it.attendanceStatus == Constants.AttendanceStatus.PRESENT || it.attendanceStatus == Constants.AttendanceStatus.LATE }
        val lateDays     = monthlyAtt.count { it.attendanceStatus == Constants.AttendanceStatus.LATE }
        val absentDays   = monthlyAtt.count { it.attendanceStatus == Constants.AttendanceStatus.ABSENT }
        val totalOvertime = monthlyAtt.sumOf { it.overtimeHours }

        monthlyStats = MonthlyAttStats(presentDays, lateDays, absentDays, totalOvertime)
        kpiScore = kpiRepo.getTotalWeightedScore(employeeId, DateUtils.currentPeriod())
        isLoading = false
    }

    val isHrOrAdmin = user.primaryRole in listOf(
        Constants.Role.HR, Constants.Role.SUPER_ADMIN
    )

    Column(
        Modifier.fillMaxSize().background(Background).statusBarsPadding()
    ) {
        // Header dengan tombol back dan edit (HR/Admin only)
        Row(
            Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = 14.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
            }
            Text("Profil Karyawan", style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f))
            if (isHrOrAdmin && employee != null) {
                IconButton(onClick = { onNavigateToEdit(employeeId) }) {
                    Icon(Icons.Default.Edit, null, tint = Blue)
                }
            }
        }

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Blue)
            }
            return@Column
        }

        val emp = employee ?: run {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Karyawan tidak ditemukan", color = TextSecondary)
            }
            return@Column
        }

        Column(Modifier.verticalScroll(rememberScrollState())) {

            // ── Avatar + Nama ─────────────────────────────────────────
            Surface(
                Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                shape = RoundedCornerShape(24.dp), color = Surface, shadowElevation = 2.dp
            ) {
                Column(
                    Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        Modifier.size(80.dp).clip(RoundedCornerShape(28.dp)).background(BlueSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            emp.name.take(2).uppercase(),
                            style = MaterialTheme.typography.headlineMedium,
                            color = Blue
                        )
                    }
                    Text(emp.name, style = MaterialTheme.typography.titleLarge)
                    Text(
                        "${emp.position} · ${emp.department}",
                        style = MaterialTheme.typography.bodyMedium, color = TextSecondary
                    )
                    // Status badge
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = if (emp.employmentStatus == "active") GreenSoft else RedSoft
                    ) {
                        Text(
                            if (emp.employmentStatus == "active") "Aktif" else "Nonaktif",
                            Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (emp.employmentStatus == "active") Green else Red
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Info Dasar ────────────────────────────────────────────
            Surface(
                Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                shape = RoundedCornerShape(24.dp), color = Surface, shadowElevation = 1.dp
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Informasi Karyawan", style = MaterialTheme.typography.titleSmall)
                    InfoRow("NIK", emp.nik)
                    InfoRow("Email", emp.email)
                    InfoRow("Telepon", emp.phone.ifEmpty { "-" })
                    InfoRow("Cabang", emp.branch.ifEmpty { "-" })
                    InfoRow("Tanggal Bergabung", emp.joinDate.ifEmpty { "-" })
                    InfoRow("Kuota Cuti Sisa", "${emp.leaveQuota} hari")
                    // Gaji hanya tampil untuk HR/Admin
                    if (isHrOrAdmin) {
                        InfoRow("Gaji Pokok", "Rp ${"%,.0f".format(emp.baseSalary)}")
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Absensi Bulan Ini ─────────────────────────────────────
            monthlyStats?.let { stats ->
                Surface(
                    Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                    shape = RoundedCornerShape(24.dp), color = Surface, shadowElevation = 1.dp
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Absensi Bulan Ini", style = MaterialTheme.typography.titleSmall)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            AttStatChip("Hadir", "${stats.presentDays}", Green, GreenSoft)
                            AttStatChip("Terlambat", "${stats.lateDays}", Orange, OrangeSoft)
                            AttStatChip("Absen", "${stats.absentDays}", Red, RedSoft)
                            AttStatChip("Lembur", "${stats.totalOvertime}j", Purple, PurpleSoft)
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── KPI Score ─────────────────────────────────────────────
            Surface(
                Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                shape = RoundedCornerShape(24.dp), color = Surface, shadowElevation = 1.dp
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Skor KPI", style = MaterialTheme.typography.titleSmall)
                        Text(DateUtils.currentPeriod(), style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                    Text(
                        "%.1f".format(kpiScore),
                        style = MaterialTheme.typography.headlineMedium,
                        color = when {
                            kpiScore >= 80 -> Green
                            kpiScore >= 60 -> Orange
                            else -> Red
                        }
                    )
                }
            }
            
            Spacer(Modifier.height(12.dp))
            
            // ── Face Recognition (HR/Admin Only) ──────────────────────
            if (isHrOrAdmin) {
                Surface(
                    Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                    shape = RoundedCornerShape(24.dp), color = Surface, shadowElevation = 1.dp
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Face Recognition", style = MaterialTheme.typography.titleSmall)
            
                        if (emp.isFaceRegistered) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(shape = RoundedCornerShape(8.dp), color = GreenSoft) {
                                    Text("✓ Wajah Terdaftar",
                                        Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall, color = Green)
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Didaftarkan ${DateUtils.formatTimestamp(emp.faceRegisteredAt)}",
                                    style = MaterialTheme.typography.labelSmall, color = TextSecondary
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { onNavigateToFaceRegistration(emp.employeeId, emp.name) },
                                    modifier = Modifier.weight(1f)
                                ) { Text("Update Wajah") }
                                OutlinedButton(
                                    onClick = { 
                                        scope.launch {
                                            com.ptniger.hris.data.repository.EmployeeRepository().clearFaceEmbedding(emp.employeeId)
                                            employee = employee?.copy(isFaceRegistered = false, faceEmbedding = emptyList())
                                        }
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Red),
                                    modifier = Modifier.weight(1f)
                                ) { Text("Hapus Wajah", color = Red) }
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(shape = RoundedCornerShape(8.dp), color = RedSoft) {
                                    Text("✗ Belum Terdaftar",
                                        Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall, color = Red)
                                }
                            }
                            Button(
                                onClick = { onNavigateToFaceRegistration(emp.employeeId, emp.name) },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Blue)
                            ) {
                                Text("Daftarkan Wajah Karyawan")
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(80.dp))
        }
    }
}

// Helper data class
data class MonthlyAttStats(
    val presentDays: Int,
    val lateDays: Int,
    val absentDays: Int,
    val totalOvertime: Double
)

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        Text(value, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun AttStatChip(label: String, value: String, textColor: androidx.compose.ui.graphics.Color, bgColor: androidx.compose.ui.graphics.Color) {
    Surface(shape = RoundedCornerShape(12.dp), color = bgColor) {
        Column(
            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.titleMedium, color = textColor)
            Text(label, style = MaterialTheme.typography.labelSmall, color = textColor)
        }
    }
}

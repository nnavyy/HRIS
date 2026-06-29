package com.ptniger.hris.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ptniger.hris.data.model.User
import com.ptniger.hris.data.repository.ContractRepository
import com.ptniger.hris.ui.theme.*
import com.ptniger.hris.utils.SeedDataManager
import kotlinx.coroutines.launch
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await

@Composable
fun HrDashboardScreen(user: User, onNavigate: (String) -> Unit, vm: DashboardViewModel = viewModel()) {
    LaunchedEffect(Unit) { vm.loadHrDashboard() }
    val s by vm.state.collectAsState()

    DashboardLayout(title = "Dashboard HR", subtitle = "HRIS Portal · ${user.fullName.ifEmpty { user.name }}", user = user) {
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricCard(Modifier.weight(1f), "Total Karyawan", "${s.totalEmployees}", "Semua karyawan", Icons.Default.People, BlueSoft, Blue)
            MetricCard(Modifier.weight(1f), "Approval Pending", "${s.pendingApprovals}", "Cuti & izin", Icons.Default.Inbox, OrangeSoft, Orange)
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricCard(Modifier.weight(1f), "Hadir Hari Ini", "${s.presentToday}", "Kehadiran", Icons.Default.CheckCircle, GreenSoft, Green)
            MetricCard(Modifier.weight(1f), "KPI Config", "Aktif", "Kelola KPI", Icons.Default.Star, PurpleSoft, Purple)
        }
        Spacer(Modifier.height(16.dp))
        Text("Aksi Cepat", style = MaterialTheme.typography.titleSmall, color = TextSecondary)
        Spacer(Modifier.height(8.dp))
        QuickActionButton("Data Karyawan", Icons.Default.People, BlueSoft, Blue) { onNavigate("employees") }
        Spacer(Modifier.height(8.dp))
        QuickActionButton("Approval Cuti", Icons.Default.CalendarMonth, OrangeSoft, Orange) { onNavigate("leave_approval") }
        Spacer(Modifier.height(8.dp))
        QuickActionButton("Konfigurasi KPI", Icons.Default.Star, PurpleSoft, Purple) { onNavigate("kpi_config") }
        Spacer(Modifier.height(8.dp))
        QuickActionButton("AI Review Kinerja", Icons.Default.Psychology, PurpleSoft, Purple) { onNavigate("ai_review") }
        Spacer(Modifier.height(8.dp))
        QuickActionButton("Buat Kontrak", Icons.Default.Description, TealSoft, Teal) { onNavigate("contract_form") }
        Spacer(Modifier.height(8.dp))
        QuickActionButton("Jadwal Kerja", Icons.Default.Schedule, GreenSoft, Green) { onNavigate("work_schedule_config") }
        Spacer(Modifier.height(8.dp))
        QuickActionButton("Kebijakan Cuti", Icons.Default.Policy, RedSoft, Red) { onNavigate("leave_policy") }
        Spacer(Modifier.height(8.dp))
        QuickActionButton("Lokasi Kantor", Icons.Default.LocationCity, OrangeSoft, Orange) { onNavigate("office_locations") }
        Spacer(Modifier.height(8.dp))
        QuickActionButton("Lihat Audit Log", Icons.Default.Shield, TealSoft, Teal) { onNavigate("audit_log") }
        Spacer(Modifier.height(100.dp))
    }
}

@Composable
fun FinanceDashboardScreen(user: User, onNavigate: (String) -> Unit, vm: DashboardViewModel = viewModel()) {
    LaunchedEffect(Unit) { vm.loadFinanceDashboard() }
    val s by vm.state.collectAsState()

    DashboardLayout(title = "Dashboard Finance", subtitle = "HRIS Portal · ${user.fullName.ifEmpty { user.name }}", user = user) {
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricCard(Modifier.weight(1f), "Total Karyawan", "${s.totalEmployees}", "Data payroll", Icons.Default.People, OrangeSoft, Orange)
            MetricCard(Modifier.weight(1f), "Slip Gaji", "Proses", "Generate slip", Icons.Default.Description, BlueSoft, Blue)
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricCard(Modifier.weight(1f), "Potongan", "BPJS+PPh", "Kalkulasi", Icons.Default.Remove, RedSoft, Red)
            MetricCard(Modifier.weight(1f), "KPI Bonus", "Aktif", "Terhitung otomatis", Icons.AutoMirrored.Filled.TrendingUp, GreenSoft, Green)
        }
        Spacer(Modifier.height(16.dp))
        Text("Aksi Cepat", style = MaterialTheme.typography.titleSmall, color = TextSecondary)
        Spacer(Modifier.height(8.dp))
        QuickActionButton("Kelola Payroll", Icons.Default.Payments, OrangeSoft, Orange) { onNavigate("payroll") }
        Spacer(Modifier.height(8.dp))
        QuickActionButton("Approval Payroll", Icons.Default.CheckCircle, GreenSoft, Green) { onNavigate("payroll_approval") }
        Spacer(Modifier.height(8.dp))
        QuickActionButton("Download Laporan", Icons.Default.Description, BlueSoft, Blue) { onNavigate("report") }
        Spacer(Modifier.height(8.dp))
        QuickActionButton("Lihat Audit Log", Icons.Default.Shield, PurpleSoft, Purple) { onNavigate("audit_log") }
        Spacer(Modifier.height(100.dp))
    }
}

@Composable
fun ManagerDashboardScreen(user: User, onNavigate: (String) -> Unit, vm: DashboardViewModel = viewModel()) {
    LaunchedEffect(Unit) { vm.loadManagerDashboard(user.userId, user.departmentId) }
    val s by vm.state.collectAsState()

    DashboardLayout(title = "Dashboard Manager", subtitle = "HRIS Portal · ${user.fullName.ifEmpty { user.name }}", user = user) {
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricCard(Modifier.weight(1f), "Anggota Tim", "${s.totalEmployees}", "Di departemen Anda", Icons.Default.People, TealSoft, Teal)
            MetricCard(Modifier.weight(1f), "Cuti Pending", "${s.pendingApprovals}", "Perlu review", Icons.Default.CalendarMonth, OrangeSoft, Orange)
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricCard(Modifier.weight(1f), "Hadir Hari Ini", "${s.presentToday}", "Absensi tim", Icons.Default.CheckCircle, GreenSoft, Green)
            MetricCard(Modifier.weight(1f), "Approval Payroll", "Pending", "Perlu review", Icons.Default.Payments, BlueSoft, Blue)
        }
        Spacer(Modifier.height(16.dp))
        Text("Menu Saya", style = MaterialTheme.typography.titleSmall, color = TextSecondary)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(
                onClick = { onNavigate("attendance") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp), color = Surface, shadowElevation = 1.dp
            ) {
                Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(GreenSoft), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.AccessTime, null, tint = Green, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("AbsenKu", style = MaterialTheme.typography.labelSmall)
                }
            }
            Surface(
                onClick = { onNavigate("leave_request") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp), color = Surface, shadowElevation = 1.dp
            ) {
                Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(BlueSoft), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.CalendarMonth, null, tint = Blue, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("CutiKu", style = MaterialTheme.typography.labelSmall)
                }
            }
            Surface(
                onClick = { onNavigate("salary_slip") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp), color = Surface, shadowElevation = 1.dp
            ) {
                Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(OrangeSoft), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Payments, null, tint = Orange, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("GajiKu", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("Aksi Cepat", style = MaterialTheme.typography.titleSmall, color = TextSecondary)
        Spacer(Modifier.height(8.dp))
        QuickActionButton("Approval Cuti Bawahan", Icons.Default.CalendarMonth, OrangeSoft, Orange) { onNavigate("leave_approval") }
        Spacer(Modifier.height(8.dp))
        QuickActionButton("Approval Payroll", Icons.Default.Payments, BlueSoft, Blue) { onNavigate("payroll_approval") }
        Spacer(Modifier.height(8.dp))
        QuickActionButton("Penilaian KPI", Icons.Default.Star, PurpleSoft, Purple) { onNavigate("kpi_scoring") }
        Spacer(Modifier.height(8.dp))
        QuickActionButton("AI Review Kinerja", Icons.Default.Psychology, PurpleSoft, Purple) { onNavigate("ai_review") }
        Spacer(Modifier.height(8.dp))
        QuickActionButton("Peer Review", Icons.Default.RateReview, TealSoft, Teal) { onNavigate("peer_review") }
        Spacer(Modifier.height(8.dp))
        QuickActionButton("Monitor Absensi Tim", Icons.Default.AccessTime, TealSoft, Teal) { onNavigate("attendance_monitor") }
        Spacer(Modifier.height(8.dp))
        QuickActionButton("Daftar Tim Saya", Icons.Default.People, GreenSoft, Green) { onNavigate("employees") }
        Spacer(Modifier.height(100.dp))
    }
}

@Composable
fun AdminDashboardScreen(user: User, onNavigate: (String) -> Unit, vm: DashboardViewModel = viewModel()) {
    LaunchedEffect(Unit) { vm.loadAdminDashboard() }
    val s by vm.state.collectAsState()

    DashboardLayout(title = "Dashboard Super Admin", subtitle = "HRIS Portal · Super Admin", user = user) {
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricCard(Modifier.weight(1f), "Akun Aktif", "${s.totalUsers}", "Karyawan + admin", Icons.Default.Shield, PurpleSoft, Purple)
            MetricCard(Modifier.weight(1f), "Role Aktif", "5", "HR,FN,MG,SA,KY", Icons.Default.Person, BlueSoft, Blue)
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricCard(Modifier.weight(1f), "Automation", "${s.automationRules}", "Rules aktif", Icons.Default.Settings, TealSoft, Teal)
            MetricCard(Modifier.weight(1f), "Audit Events", "${s.auditEvents}", "Bulan ini", Icons.Default.FindInPage, RedSoft, Red)
        }
        Spacer(Modifier.height(20.dp))
        // Quick Actions
        Button(
            onClick = { onNavigate("manage_accounts") },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Blue)
        ) {
            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Tambah Akun Baru", style = MaterialTheme.typography.titleSmall)
        }
        Spacer(Modifier.height(8.dp))
        QuickActionButton("Lokasi Kantor (GPS Absensi)", Icons.Default.LocationCity, OrangeSoft, Orange) { onNavigate("office_locations") }
        Spacer(Modifier.height(8.dp))
        QuickActionButton("Pengaturan Sistem (API Key)", Icons.Default.Settings, TealSoft, Teal) { onNavigate("app_config") }
        Spacer(Modifier.height(8.dp))
        QuickActionButton("Lihat Audit Log", Icons.Default.Shield, PurpleSoft, Purple) { onNavigate("audit_log") }
        Spacer(Modifier.height(8.dp))
        QuickActionButton("Approval Cuti", Icons.Default.CalendarMonth, OrangeSoft, Orange) { onNavigate("leave_approval") }
        Spacer(Modifier.height(8.dp))

        // Demo Data Section
        var seedMessage by remember { mutableStateOf<String?>(null) }
        var isSeedLoading by remember { mutableStateOf(false) }
        val seedScope = rememberCoroutineScope()
        HorizontalDivider(color = CardBorder)
        Spacer(Modifier.height(8.dp))
        Text("Developer Tools", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        Spacer(Modifier.height(4.dp))

        // Button 1: SeedDataManager (lengkap untuk AI Review)
        var isSeedAiLoading by remember { mutableStateOf(false) }
        var seedAiResult by remember { mutableStateOf<String?>(null) }
        OutlinedButton(
            onClick = {
                if (!isSeedAiLoading) {
                    isSeedAiLoading = true
                    seedAiResult = null
                    seedScope.launch {
                        val result = SeedDataManager.seedAll()
                        seedAiResult = if (result.errors.isEmpty()) {
                            "✅ Berhasil insert ${result.inserted} dokumen (attendance Q2, KPI scores, peer reviews)"
                        } else {
                            "⚠️ ${result.inserted} berhasil, ${result.errors.size} error: ${result.errors.joinToString()}"
                        }
                        isSeedAiLoading = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            enabled = !isSeedAiLoading,
            border = androidx.compose.foundation.BorderStroke(1.dp, Purple)
        ) {
            if (isSeedAiLoading) {
                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = Purple)
                Spacer(Modifier.width(8.dp))
                Text("Seeding data AI Review...", color = Purple)
            } else {
                Icon(Icons.Default.Psychology, null, tint = Purple, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Seed Data Lengkap (AI Review + KPI)", color = Purple)
            }
        }
        seedAiResult?.let {
            Spacer(Modifier.height(4.dp))
            Text(it, style = MaterialTheme.typography.bodySmall,
                color = if (it.startsWith("✅")) Green else Orange)
        }

        Spacer(Modifier.height(8.dp))

        // Button 2: DummyDataSeeder (demo presentasi ringan)
        OutlinedButton(
            onClick = {
                if (!isSeedLoading) {
                    isSeedLoading = true
                    seedMessage = null
                    seedScope.launch {
                        seedMessage = com.ptniger.hris.utils.DummyDataSeeder.seedAll()
                        isSeedLoading = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.PlayArrow, null, Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(if (isSeedLoading) "Memuat data demo..." else "Muat Data Demo Ringan (Presentasi)")
        }
        if (seedMessage != null) {
            Spacer(Modifier.height(4.dp))
            Text(seedMessage!!, style = MaterialTheme.typography.bodySmall, color = if (seedMessage!!.contains("Gagal")) Red else Green)
        }
        
        Spacer(Modifier.height(16.dp))
        
        // Hapus Collection (Dev Mode)
        var collectionToDelete by remember { mutableStateOf("") }
        var deleteMessage by remember { mutableStateOf<String?>(null) }
        val coroutineScope = rememberCoroutineScope()
        
        Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = Surface, shadowElevation = 1.dp) {
            Column(Modifier.padding(16.dp)) {
                Text("Hapus Collection (Dev Mode)", style = MaterialTheme.typography.titleSmall, color = Red)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = collectionToDelete, onValueChange = { collectionToDelete = it; deleteMessage = null },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    label = { Text("Nama Collection") }
                )
                Spacer(Modifier.height(8.dp))
                if (deleteMessage != null) {
                    Text(deleteMessage!!, color = if (deleteMessage!!.contains("berhasil", ignoreCase = true)) Green else Red, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                }
                Button(
                    onClick = {
                        val colName = collectionToDelete.trim()
                        if (colName.isNotEmpty()) {
                            coroutineScope.launch(Dispatchers.IO) {
                                val db = FirebaseFirestore.getInstance()
                                val colRef = db.collection(colName)
                                try {
                                    val snapshot = colRef.get().await()
                                    if (snapshot.isEmpty) {
                                        deleteMessage = "Tidak menemukan \"$colName\" atau sudah kosong."
                                    } else {
                                        for (doc in snapshot.documents) {
                                            doc.reference.delete().await()
                                        }
                                        deleteMessage = "Data collection \"$colName\" berhasil dihapus."
                                    }
                                } catch (e: Exception) {
                                    deleteMessage = "Gagal menghapus: ${e.message}"
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Red)
                ) {
                    Text("Hapus Seluruh Data")
                }
            }
        }
        Spacer(Modifier.height(100.dp))
    }
}

@Composable
fun EmployeeDashboardScreen(user: User, onNavigate: (String) -> Unit, vm: DashboardViewModel = viewModel()) {
    LaunchedEffect(Unit) { vm.loadEmployeeDashboard(user.userId) }
    val s by vm.state.collectAsState()

    // Check pending contract
    var hasPendingContract by remember { mutableStateOf(false) }
    LaunchedEffect(user.userId) {
        if (user.userId.isNotEmpty()) {
            val contracts = ContractRepository().getUnsignedContracts(user.userId)
            hasPendingContract = contracts.isNotEmpty()
        }
    }

    DashboardLayout(title = "Halo, ${user.fullName.ifEmpty { user.name }}", subtitle = "HRIS Portal · Employee Self Service", user = user) {
        // Pending contract banner
        if (hasPendingContract) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = OrangeSoft,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigate("contract_sign") }
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Description, null, tint = Orange, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Ada kontrak yang perlu ditandatangani",
                        style = MaterialTheme.typography.bodySmall, color = Orange)
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Default.ChevronRight, null, tint = Orange, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricCard(Modifier.weight(1f), "Status Hari Ini", if (s.checkInTime.isNotEmpty()) "Hadir" else "Belum", s.checkInTime.ifEmpty { "Check-in" }, Icons.Default.AccessTime, GreenSoft, Green)
            MetricCard(Modifier.weight(1f), "Sisa Cuti", "${s.leaveQuota}", "dari 12 hari", Icons.Default.CalendarMonth, BlueSoft, Blue)
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricCard(Modifier.weight(1f), "Skor KPI", String.format("%.1f", s.kpiScore), com.ptniger.hris.utils.KpiCalculator.getBonusLabel(s.kpiScore), Icons.Default.Star, PurpleSoft, Purple)
            MetricCard(Modifier.weight(1f), "Notifikasi", "${s.unreadNotifications}", "Belum dibaca", Icons.Default.Notifications, OrangeSoft, Orange)
        }
        Spacer(Modifier.height(16.dp))
        Text("Akses Cepat", style = MaterialTheme.typography.titleSmall, color = TextSecondary)
        Spacer(Modifier.height(8.dp))
        QuickActionButton("Absensi Harian", Icons.Default.AccessTime, GreenSoft, Green) { onNavigate("attendance") }
        Spacer(Modifier.height(8.dp))
        QuickActionButton("Pengajuan Cuti", Icons.Default.CalendarMonth, BlueSoft, Blue) { onNavigate("leave_request") }
        Spacer(Modifier.height(8.dp))
        QuickActionButton("Slip Gaji", Icons.Default.Payments, OrangeSoft, Orange) { onNavigate("salary_slip") }
        Spacer(Modifier.height(8.dp))
        QuickActionButton("Hasil KPI Saya", Icons.Default.Star, PurpleSoft, Purple) { onNavigate("kpi_result") }
        Spacer(Modifier.height(100.dp))
    }
}

// ============== Shared Composables ==============

@Composable
fun DashboardLayout(title: String, subtitle: String, user: User, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().background(Background).statusBarsPadding()) {
        // Top bar — avatar is now in MainScaffold overlay, so just show title here
        Row(
            Modifier.fillMaxWidth().padding(start = 18.dp, end = 64.dp, top = 12.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Spacer(Modifier.height(2.dp))
                Text(title, style = MaterialTheme.typography.headlineMedium)
            }
        }

        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp),
            content = content
        )
    }
}

@Composable
fun HeroCard(title: String, desc: String, badge: String) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = Color(0xFFEFF6FF),
        shadowElevation = 2.dp
    ) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(6.dp))
                Text(desc, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            Spacer(Modifier.width(12.dp))
            Surface(shape = RoundedCornerShape(999.dp), color = BlueSoft) {
                Text(badge, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium, color = Blue)
            }
        }
    }
}

@Composable
fun MetricCard(modifier: Modifier = Modifier, label: String, value: String, note: String, icon: ImageVector, bgColor: Color, fgColor: Color) {
    Surface(modifier = modifier, shape = RoundedCornerShape(22.dp), color = Surface, shadowElevation = 1.dp) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column {
                    Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Spacer(Modifier.height(4.dp))
                    Text(value, style = MaterialTheme.typography.headlineMedium)
                }
                Box(Modifier.size(34.dp).clip(RoundedCornerShape(13.dp)).background(bgColor), contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = fgColor, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(note, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        }
    }
}

/**
 * Quick Action button — used on dashboard to access features removed from navbar.
 */
@Composable
fun QuickActionButton(label: String, icon: ImageVector, bgColor: Color, fgColor: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Surface,
        shadowElevation = 1.dp
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(14.dp)).background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = fgColor, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text(label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, null, tint = TextSecondary, modifier = Modifier.size(20.dp))
        }
    }
}

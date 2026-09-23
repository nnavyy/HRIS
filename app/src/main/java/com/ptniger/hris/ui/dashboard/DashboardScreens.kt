package com.ptniger.hris.ui.dashboard

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ptniger.hris.data.model.User
import com.ptniger.hris.data.model.EmployeePresence
import com.ptniger.hris.data.repository.ContractRepository
import com.ptniger.hris.data.repository.EmployeeRepository
import com.ptniger.hris.ui.components.PresenceBadge
import com.ptniger.hris.ui.components.PresenceBadgeSize
import com.ptniger.hris.ui.components.WeeklyScheduleRow
import com.ptniger.hris.ui.components.WeatherGreetingWidget
import com.ptniger.hris.ui.components.InfoChip
import com.ptniger.hris.ui.theme.*
import com.ptniger.hris.utils.DateUtils
import com.ptniger.hris.utils.PresenceResolver
import kotlinx.coroutines.launch
import com.ptniger.hris.utils.AppUpdateChecker
import com.ptniger.hris.utils.AppUpdateInfo
import com.ptniger.hris.BuildConfig
import androidx.compose.ui.platform.LocalContext
@Composable
fun HrDashboardScreen(user: User, onNavigate: (String) -> Unit, vm: DashboardViewModel = viewModel()) {
    LaunchedEffect(Unit) { vm.loadHrDashboard() }
    val s by vm.state.collectAsState()

    DashboardLayout(title = "Dashboard HR", subtitle = "HRIS Portal · ${user.fullName.ifEmpty { user.name }}", user = user) {
        if (s.isLoading) {
            com.ptniger.hris.ui.components.DashboardSkeleton()
        } else {
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
            MyMenuSection(onNavigate)
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
}

@Composable
fun FinanceDashboardScreen(user: User, onNavigate: (String) -> Unit, vm: DashboardViewModel = viewModel()) {
    LaunchedEffect(Unit) { vm.loadFinanceDashboard() }
    val s by vm.state.collectAsState()

    DashboardLayout(title = "Dashboard Finance", subtitle = "HRIS Portal · ${user.fullName.ifEmpty { user.name }}", user = user) {
        if (s.isLoading) {
            com.ptniger.hris.ui.components.DashboardSkeleton()
        } else {
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
            MyMenuSection(onNavigate)
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
}

@Composable
fun ManagerDashboardScreen(user: User, onNavigate: (String) -> Unit, vm: DashboardViewModel = viewModel()) {
    LaunchedEffect(Unit) { vm.loadManagerDashboard(user) }
    val s by vm.state.collectAsState()

    DashboardLayout(title = "Dashboard Manager", subtitle = "HRIS Portal · ${user.fullName.ifEmpty { user.name }}", user = user) {
        if (s.isLoading) {
            com.ptniger.hris.ui.components.DashboardSkeleton()
        } else {
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard(
                    modifier = Modifier.weight(1f),
                    label = "Anggota Tim",
                    value = "${s.totalEmployees}",
                    note = "Bawahan langsung & tim",
                    icon = Icons.Default.People,
                    bgColor = TealSoft,
                    fgColor = Teal,
                    onClick = { onNavigate("team_presence") }
                )
                MetricCard(
                    modifier = Modifier.weight(1f),
                    label = "Cuti Pending",
                    value = "${s.pendingApprovals}",
                    note = if (s.pendingApprovals > 0) "Perlu review" else "Semua bersih",
                    icon = Icons.Default.CalendarMonth,
                    bgColor = OrangeSoft,
                    fgColor = Orange,
                    onClick = { onNavigate("leave_approval") }
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // PRES-07: Metric card Hadir clickable → buka team_presence
                Surface(
                    Modifier.weight(1f).clickable { onNavigate("team_presence") },
                    shape = RoundedCornerShape(20.dp),
                    color = GreenSoft
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.People, null, tint = Green, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Tim Hari Ini", style = MaterialTheme.typography.labelSmall, color = Green)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text("${s.presentToday}/${s.totalEmployees}",
                            style = MaterialTheme.typography.headlineMedium, color = Green)
                        Text("Hadir  •  Tap untuk detail",
                            style = MaterialTheme.typography.labelSmall, color = Green.copy(alpha = 0.7f))
                    }
                }
                MetricCard(
                    modifier = Modifier.weight(1f),
                    label = "Approval Payroll",
                    value = "${s.pendingPayrolls}",
                    note = if (s.pendingPayrolls > 0) "Perlu review manajer" else "Tidak ada pending",
                    icon = Icons.Default.Payments,
                    bgColor = BlueSoft,
                    fgColor = Blue,
                    onClick = { onNavigate("payroll_approval") }
                )
            }
            Spacer(Modifier.height(16.dp))
            MyMenuSection(onNavigate)
            Spacer(Modifier.height(16.dp))
            Text("Aksi Cepat", style = MaterialTheme.typography.titleSmall, color = TextSecondary)
            Spacer(Modifier.height(8.dp))
            QuickActionButton("Kehadiran Tim Hari Ini", Icons.Default.HowToReg, GreenSoft, Green) { onNavigate("team_presence") }
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
}

@OptIn(ExperimentalMaterial3Api::class)
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
        Spacer(Modifier.height(16.dp))
        MyMenuSection(onNavigate)
        Spacer(Modifier.height(16.dp))
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
        QuickActionButton("App Config (Cuaca & AI)", Icons.Default.SettingsApplications, TealSoft, Teal) { onNavigate("app_config") }
        Spacer(Modifier.height(8.dp))
        QuickActionButton("Lihat Audit Log", Icons.Default.Shield, PurpleSoft, Purple) { onNavigate("audit_log") }
        Spacer(Modifier.height(8.dp))
        QuickActionButton("Approval Cuti", Icons.Default.CalendarMonth, OrangeSoft, Orange) { onNavigate("leave_approval") }
        Spacer(Modifier.height(8.dp))

        // ── DEV TOOLS ──
        HorizontalDivider(color = CardBorder)
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = { onNavigate("dev_tools") },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Orange.copy(alpha = 0.5f))
        ) {
            Icon(Icons.Default.Settings, null, tint = Orange, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Dev Tools", color = Orange)
            Spacer(Modifier.width(4.dp))
            Surface(shape = RoundedCornerShape(4.dp), color = OrangeSoft) {
                Text("DEV", Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall, color = Orange,
                    fontWeight = FontWeight.Bold)
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

    // PRES-08: Fetch presence status diri sendiri
    var myPresence by remember { mutableStateOf<EmployeePresence?>(null) }
    LaunchedEffect(Unit) {
        val emp = EmployeeRepository().getByUserId(user.userId)
        emp?.let { myPresence = PresenceResolver.resolveToday(it) }
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

        // PRES-08: Status Saya Hari Ini card
        myPresence?.let { pres ->
            Surface(
                shape = RoundedCornerShape(20.dp), color = Surface, shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("Status Saya Hari Ini", style = MaterialTheme.typography.titleSmall)
                        Text(DateUtils.todayShort(),
                            style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }

                    PresenceBadge(status = pres.presenceStatus, size = PresenceBadgeSize.FULL,
                        modifier = Modifier.fillMaxWidth())

                    if (pres.checkInTime.isNotEmpty()) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            InfoChip("Masuk", pres.checkInTime)
                            if (pres.checkOutTime.isNotEmpty()) InfoChip("Keluar", pres.checkOutTime)
                            else Text("Belum checkout", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        }
                    }

                    // Jadwal mingguan ringkas
                    WeeklyScheduleRow(schedule = null) // akan di-populate saat schedule di-fetch
                }
            }
            Spacer(Modifier.height(12.dp))
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
        MyMenuSection(onNavigate)
        Spacer(Modifier.height(100.dp))
    }
}

@Composable
fun MyMenuSection(onNavigate: (String) -> Unit) {
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
}

// ============== Shared Composables ==============

@Composable
fun DashboardLayout(
    title: String,
    subtitle: String,
    user: User,
    content: @Composable ColumnScope.() -> Unit
) {
    // Fetch cuaca inline — tidak blocking UI
    var weatherLine by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(user.userId) {
        try {
            val employee = com.ptniger.hris.data.repository.EmployeeRepository().getByUserId(user.userId)
            val officeId = employee?.officeId ?: "office_main"
            val office = com.ptniger.hris.data.repository.OfficeLocationRepository().getById(officeId)
            
            // Default to Jakarta if office not found so weather still shows
            val lat = office?.latitude ?: -6.2088
            val lon = office?.longitude ?: 106.8456

            val w = com.ptniger.hris.data.repository.WeatherRepository().fetchCurrentWeather(
                lat = lat,
                lon = lon
            )
            if (w != null) {
                weatherLine = "${w.getWeatherLabel()} ${w.tempCelsius.toInt()}°C"
            }
        } catch (_: Exception) {}
    }

    val hour = remember { java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY) }
    val greetWord = when {
        hour < 11 -> "Selamat pagi"
        hour < 15 -> "Selamat siang"
        hour < 18 -> "Selamat sore"
        else      -> "Selamat malam"
    }
    val firstName = remember(user) {
        (user.fullName.ifEmpty { user.name }).split(" ").firstOrNull() ?: user.name
    }

    Column(Modifier.fillMaxSize().background(Background).statusBarsPadding()) {

        // ── HEADER — putih dengan border bawah supaya beda dari Background ──
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Surface,
            shadowElevation = 2.dp,
            tonalElevation = 0.dp
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 12.dp)
            ) {
                // Baris 1: Greeting + cuaca inline — kecil, abu-abu
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "$greetWord, $firstName",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = androidx.compose.ui.unit.TextUnit(11f, androidx.compose.ui.unit.TextUnitType.Sp)
                        ),
                        color = TextSecondary
                    )
                    weatherLine?.let { weather ->
                        Text(
                            text = "\u00B7",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = androidx.compose.ui.unit.TextUnit(11f, androidx.compose.ui.unit.TextUnitType.Sp)
                            ),
                            color = TextMuted
                        )
                        Text(
                            text = weather,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = androidx.compose.ui.unit.TextUnit(11f, androidx.compose.ui.unit.TextUnitType.Sp)
                            ),
                            color = TextMuted
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                // Baris 2: Judul halaman — paling menonjol
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium
                )
            }
        }
        // ── END HEADER ────────────────────────────────────────────────────────

        val isHighTraffic by com.ptniger.hris.utils.TrafficLatencyGuard.isHighTraffic.collectAsState()
        val trafficMsg by com.ptniger.hris.utils.TrafficLatencyGuard.trafficMessage.collectAsState()
        com.ptniger.hris.utils.HighTrafficBanner(visible = isHighTraffic, message = trafficMsg)

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 18.dp),
            content = content
        )

        // Auto-Update Checker Logic
        val context = LocalContext.current
        var updateInfo by remember { mutableStateOf<AppUpdateInfo?>(null) }
        var showUpdateDialog by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            val checker = AppUpdateChecker()
            val info = checker.checkForUpdate()
            if (info != null && info.latestVersionCode > BuildConfig.VERSION_CODE) {
                updateInfo = info
                showUpdateDialog = true
            }
        }

        if (showUpdateDialog && updateInfo != null) {
            AlertDialog(
                onDismissRequest = {
                    if (updateInfo?.isForceUpdate != true) {
                        showUpdateDialog = false
                    }
                },
                title = { Text("Update Tersedia") },
                text = {
                    Column {
                        Text("Versi baru (${updateInfo?.latestVersionName}) telah tersedia.")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(updateInfo?.releaseNotes ?: "Silakan perbarui aplikasi untuk fitur terbaru dan perbaikan bug.")
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        updateInfo?.downloadUrl?.let { url ->
                            AppUpdateChecker.openDownloadLink(context, url)
                        }
                    }) {
                        Text("Update Sekarang")
                    }
                },
                dismissButton = {
                    if (updateInfo?.isForceUpdate != true) {
                        TextButton(onClick = { showUpdateDialog = false }) {
                            Text("Nanti Saja")
                        }
                    }
                }
            )
        }
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
fun MetricCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    note: String,
    icon: ImageVector,
    bgColor: Color,
    fgColor: Color,
    onClick: (() -> Unit)? = null
) {
    val cardModifier = if (onClick != null) modifier.clickable { onClick() } else modifier
    Surface(modifier = cardModifier, shape = RoundedCornerShape(22.dp), color = Surface, shadowElevation = 1.dp) {
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

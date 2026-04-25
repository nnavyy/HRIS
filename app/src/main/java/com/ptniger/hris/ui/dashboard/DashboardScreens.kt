package com.ptniger.hris.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import com.ptniger.hris.ui.theme.*

@Composable
fun HrDashboardScreen(user: User, onNavigate: (String) -> Unit, vm: DashboardViewModel = viewModel()) {
    LaunchedEffect(Unit) { vm.loadHrDashboard() }
    val s by vm.state.collectAsState()

    DashboardLayout(title = "Dashboard HR", subtitle = "HRIS Portal · HR / Admin", user = user) {
        HeroCard("Dashboard HR / Admin", "Kelola master data, approval cuti, payroll, laporan, dan automation.", "HR")
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricCard(Modifier.weight(1f), "Total Karyawan", "${s.totalEmployees}", "+4 bulan ini", Icons.Default.People, BlueSoft, Blue)
            MetricCard(Modifier.weight(1f), "Approval Pending", "${s.pendingApprovals}", "Cuti & izin", Icons.Default.Inbox, OrangeSoft, Orange)
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricCard(Modifier.weight(1f), "Hadir Hari Ini", "${s.presentToday}", "Kehadiran", Icons.Default.CheckCircle, GreenSoft, Green)
            MetricCard(Modifier.weight(1f), "KPI Config", "Aktif", "Kelola KPI", Icons.Default.Star, PurpleSoft, Purple)
        }
        Spacer(Modifier.height(100.dp))
    }
}

@Composable
fun FinanceDashboardScreen(user: User, onNavigate: (String) -> Unit, vm: DashboardViewModel = viewModel()) {
    LaunchedEffect(Unit) { vm.loadFinanceDashboard() }
    val s by vm.state.collectAsState()

    DashboardLayout(title = "Dashboard Finance", subtitle = "HRIS Portal · Finance", user = user) {
        HeroCard("Dashboard Finance", "Fokus pada payroll, tunjangan, potongan, slip gaji, dan laporan penggajian.", "FN")
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricCard(Modifier.weight(1f), "Total Karyawan", "${s.totalEmployees}", "Data payroll", Icons.Default.Payments, OrangeSoft, Orange)
            MetricCard(Modifier.weight(1f), "Slip Gaji", "Proses", "Generate slip", Icons.Default.Description, BlueSoft, Blue)
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricCard(Modifier.weight(1f), "Potongan", "BPJS+PPh", "Kalkulasi", Icons.Default.Remove, RedSoft, Red)
            MetricCard(Modifier.weight(1f), "KPI Bonus", "Aktif", "Terhitung otomatis", Icons.Default.TrendingUp, GreenSoft, Green)
        }
        Spacer(Modifier.height(100.dp))
    }
}

@Composable
fun ManagerDashboardScreen(user: User, onNavigate: (String) -> Unit, vm: DashboardViewModel = viewModel()) {
    LaunchedEffect(Unit) { vm.loadManagerDashboard(user.departmentId) }
    val s by vm.state.collectAsState()

    DashboardLayout(title = "Dashboard Manager", subtitle = "HRIS Portal · Manager", user = user) {
        HeroCard("Dashboard Manager", "Pantau anggota tim, absensi, dan approval pengajuan cuti bawahan.", "MG")
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricCard(Modifier.weight(1f), "Anggota Tim", "${s.totalEmployees}", "Divisi Anda", Icons.Default.People, TealSoft, Teal)
            MetricCard(Modifier.weight(1f), "Cuti Pending", "${s.pendingApprovals}", "Perlu review", Icons.Default.CalendarMonth, OrangeSoft, Orange)
        }
        Spacer(Modifier.height(100.dp))
    }
}

@Composable
fun AdminDashboardScreen(user: User, onNavigate: (String) -> Unit, vm: DashboardViewModel = viewModel()) {
    LaunchedEffect(Unit) { vm.loadAdminDashboard() }
    val s by vm.state.collectAsState()

    DashboardLayout(title = "Dashboard Admin", subtitle = "HRIS Portal · Super Admin", user = user) {
        HeroCard("Dashboard Super Admin", "Kelola akun, role, pengaturan sistem, automation rule, dan audit log.", "SA")
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricCard(Modifier.weight(1f), "Akun Aktif", "${s.totalEmployees + 5}", "Karyawan + admin", Icons.Default.Shield, PurpleSoft, Purple)
            MetricCard(Modifier.weight(1f), "Role Aktif", "5", "HR,FN,MG,SA,KY", Icons.Default.Person, BlueSoft, Blue)
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricCard(Modifier.weight(1f), "Automation", "${s.automationRules}", "Rules aktif", Icons.Default.Settings, TealSoft, Teal)
            MetricCard(Modifier.weight(1f), "Audit Events", "${s.auditEvents}", "Bulan ini", Icons.Default.FindInPage, RedSoft, Red)
        }
        Spacer(Modifier.height(100.dp))
    }
}

@Composable
fun EmployeeDashboardScreen(user: User, onNavigate: (String) -> Unit, vm: DashboardViewModel = viewModel()) {
    LaunchedEffect(Unit) { vm.loadEmployeeDashboard(user.userId) }
    val s by vm.state.collectAsState()

    DashboardLayout(title = "Dashboard", subtitle = "HRIS Portal · Karyawan", user = user) {
        HeroCard("Halo, ${user.name}", "Akses absensi, pengajuan cuti, slip gaji, dan profil lewat Employee Self Service.", "ESS")
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricCard(Modifier.weight(1f), "Status Hari Ini", if (s.checkInTime.isNotEmpty()) "Hadir" else "Belum", s.checkInTime.ifEmpty { "Check-in" }, Icons.Default.AccessTime, GreenSoft, Green)
            MetricCard(Modifier.weight(1f), "Sisa Cuti", "${s.leaveQuota}", "dari 12 hari", Icons.Default.CalendarMonth, BlueSoft, Blue)
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricCard(Modifier.weight(1f), "Skor KPI", String.format("%.1f", s.kpiScore), com.ptniger.hris.utils.KpiCalculator.getBonusLabel(s.kpiScore), Icons.Default.Star, PurpleSoft, Purple)
            MetricCard(Modifier.weight(1f), "Notifikasi", "${s.unreadNotifications}", "Belum dibaca", Icons.Default.Notifications, OrangeSoft, Orange)
        }
        Spacer(Modifier.height(100.dp))
    }
}

// ============== Shared Composables ==============

@Composable
fun DashboardLayout(title: String, subtitle: String, user: User, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().background(Background)) {
        // Top bar
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Spacer(Modifier.height(2.dp))
                Text(title, style = MaterialTheme.typography.headlineMedium)
            }
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(50))
                    .background(Brush.linearGradient(listOf(Blue, Color(0xFF60A5FA)))),
                contentAlignment = Alignment.Center
            ) {
                Text(com.ptniger.hris.utils.RoleManager.getRoleShort(user.role), color = Color.White, style = MaterialTheme.typography.labelMedium)
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
        border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.linearGradient(listOf(Color(0xFFDBEAFE), Color(0xFFDBEAFE)))),
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

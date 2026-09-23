package com.ptniger.hris.ui.superadmin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ptniger.hris.ui.theme.*
import com.ptniger.hris.utils.SeedDataManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Dev Tools Screen — dedicated halaman untuk development operations.
 * Hanya bisa diakses oleh Super Admin.
 *
 * Features:
 * 1. Reset Semua Data (Danger Zone)
 * 2. Generate Semua Data (Seed Lengkap)
 * 3. Reset + Generate (One-Click)
 * 4. Quick Actions per Module
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevToolsScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var showResetDialog by remember { mutableStateOf(false) }
    var includeUsersInReset by remember { mutableStateOf(false) }

    // ── State: Reset ──
    var isResetting by remember { mutableStateOf(false) }
    var resetLogs by remember { mutableStateOf(listOf<String>()) }
    var resetResult by remember { mutableStateOf<String?>(null) }

    // ── State: Generate ──
    var isGenerating by remember { mutableStateOf(false) }
    var generateLogs by remember { mutableStateOf(listOf<String>()) }
    var generateResult by remember { mutableStateOf<String?>(null) }

    // ── State: Reset+Generate ──
    var isResetAndSeed by remember { mutableStateOf(false) }
    var resetAndSeedResult by remember { mutableStateOf<String?>(null) }

    // ── State: Quick Actions ──
    var quickActionResult by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var quickActionLoading by remember { mutableStateOf<Set<String>>(emptySet()) }

    Column(
        Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        // ── TOP BAR ──
        Row(
            Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
            }
            Column(Modifier.padding(start = 4.dp)) {
                Text("Dev Tools", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("DEVELOPMENT MODE", style = MaterialTheme.typography.labelSmall, color = Orange,
                    fontWeight = FontWeight.Bold)
            }
        }

        // ── WARNING BANNER ──
        Surface(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp),
            shape = RoundedCornerShape(14.dp),
            color = OrangeSoft,
            border = BorderStroke(1.dp, Orange.copy(alpha = 0.3f))
        ) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Warning, null, tint = Orange, modifier = Modifier.size(20.dp))
                Text("Halaman ini khusus untuk development & testing. Operasi di sini akan mengubah data di Firestore secara langsung.",
                    style = MaterialTheme.typography.bodySmall, color = Orange)
            }
        }

        Spacer(Modifier.height(16.dp))

        // ══════════════════════════════════════════════════════════════════
        // SECTION A: RESET + GENERATE (ONE CLICK)
        // ══════════════════════════════════════════════════════════════════
        DevSection(
            title = "Reset + Generate",
            subtitle = "Hapus semua data lalu generate ulang dalam satu langkah",
            icon = Icons.Default.Refresh,
            iconColor = Blue
        ) {
            Button(
                onClick = {
                    if (!isResetAndSeed) {
                        isResetAndSeed = true
                        resetAndSeedResult = null
                        scope.launch(Dispatchers.IO) {
                            val result = SeedDataManager.resetAndSeedAll(
                                includeUsers = false,
                                onProgress = { step -> resetAndSeedResult = step }
                            )
                            isResetAndSeed = false
                            resetAndSeedResult = if (result.errors.isEmpty()) {
                                "Berhasil! ${result.inserted} dokumen di-generate."
                            } else {
                                "Selesai dengan ${result.errors.size} error: ${result.errors.joinToString("; ")}"
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Blue),
                enabled = !isResetAndSeed && !isResetting && !isGenerating
            ) {
                if (isResetAndSeed) {
                    CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(resetAndSeedResult ?: "Memproses...", color = Color.White)
                } else {
                    Icon(Icons.Default.Refresh, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Reset + Generate Semua Data")
                }
            }
            resetAndSeedResult?.takeIf { !isResetAndSeed }?.let {
                Spacer(Modifier.height(6.dp))
                Text(it, style = MaterialTheme.typography.bodySmall,
                    color = if (it.startsWith("Berhasil")) Green else Orange)
            }
        }

        // ══════════════════════════════════════════════════════════════════
        // SECTION B: GENERATE DATA (SEED)
        // ══════════════════════════════════════════════════════════════════
        DevSection(
            title = "Generate Semua Data",
            subtitle = "Seed data lengkap: employees, attendance, payroll, KPI, peer reviews, contracts, dll",
            icon = Icons.Default.PlayArrow,
            iconColor = Green
        ) {
            OutlinedButton(
                onClick = {
                    if (!isGenerating) {
                        isGenerating = true
                        generateLogs = emptyList()
                        generateResult = null
                        scope.launch(Dispatchers.IO) {
                            val result = SeedDataManager.seedAllWithProgress { name, count ->
                                generateLogs = generateLogs + "$name → $count docs"
                            }
                            isGenerating = false
                            generateResult = if (result.errors.isEmpty()) {
                                "Berhasil! Total ${result.inserted} dokumen di-insert."
                            } else {
                                "${result.inserted} berhasil, ${result.errors.size} error"
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Green),
                enabled = !isGenerating && !isResetAndSeed
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(Modifier.size(16.dp), color = Green, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Generating...", color = Green)
                } else {
                    Icon(Icons.Default.PlayArrow, null, tint = Green, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Generate Semua Data", color = Green)
                }
            }

            // Progress logs
            AnimatedVisibility(generateLogs.isNotEmpty()) {
                Surface(
                    Modifier.fillMaxWidth().padding(top = 8.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = CardBorder.copy(alpha = 0.2f)
                ) {
                    Column(Modifier.padding(10.dp)) {
                        generateLogs.forEach { log ->
                            Text("• $log", style = MaterialTheme.typography.labelSmall,
                                color = if (log.startsWith("❌")) Red else TextSecondary)
                        }
                    }
                }
            }
            generateResult?.let {
                Spacer(Modifier.height(4.dp))
                Text(it, style = MaterialTheme.typography.bodySmall,
                    color = if (it.startsWith("Berhasil")) Green else Orange)
            }
        }

        // ══════════════════════════════════════════════════════════════════
        // SECTION C: QUICK ACTIONS PER MODULE
        // ══════════════════════════════════════════════════════════════════
        DevSection(
            title = "Quick Actions (Per Module)",
            subtitle = "Regenerate data untuk modul tertentu tanpa reset semua",
            icon = Icons.Default.Settings,
            iconColor = Teal
        ) {
            val modules = listOf(
                Triple("attendance", "Attendance (Absensi)", Icons.Default.AccessTime),
                Triple("payroll", "Payroll (Slip Gaji)", Icons.Default.Payments),
                Triple("kpi", "KPI Config + Scores", Icons.Default.Star),
                Triple("peer_review", "Peer Reviews", Icons.Default.RateReview),
                Triple("leave", "Leave Requests (Cuti)", Icons.Default.CalendarMonth),
                Triple("contracts", "Employee Contracts", Icons.Default.Description),
            )

            modules.forEach { (key, label, icon) ->
                val isLoading = key in quickActionLoading
                val result = quickActionResult[key]

                OutlinedButton(
                    onClick = {
                        if (!isLoading) {
                            quickActionLoading = quickActionLoading + key
                            quickActionResult = quickActionResult - key
                            scope.launch(Dispatchers.IO) {
                                val res = when (key) {
                                    "attendance" -> SeedDataManager.seedModuleAttendance()
                                    "payroll" -> SeedDataManager.seedModulePayroll()
                                    "kpi" -> SeedDataManager.seedModuleKpi()
                                    "peer_review" -> SeedDataManager.seedModulePeerReview()
                                    "leave" -> SeedDataManager.seedModuleLeave()
                                    "contracts" -> SeedDataManager.seedModuleContracts()
                                    else -> SeedDataManager.SeedResult(0, listOf("Unknown module"))
                                }
                                quickActionLoading = quickActionLoading - key
                                quickActionResult = quickActionResult + (key to
                                    if (res.errors.isEmpty()) "${res.inserted} docs"
                                    else "Error: ${res.errors.first()}")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp, color = Teal)
                    } else {
                        Icon(icon, null, tint = Teal, modifier = Modifier.size(16.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(label, Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                    result?.let {
                        Text(it, style = MaterialTheme.typography.labelSmall,
                            color = if (it.startsWith("Error")) Red else Green)
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
        }

        // ══════════════════════════════════════════════════════════════════
        // SECTION D: DANGER ZONE — RESET
        // ══════════════════════════════════════════════════════════════════
        DevSection(
            title = "Danger Zone — Reset",
            subtitle = "Hapus seluruh data dari Firestore (kecuali akun inti)",
            icon = Icons.Default.Delete,
            iconColor = Red,
            borderColor = Red.copy(alpha = 0.3f)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = includeUsersInReset,
                    onCheckedChange = { includeUsersInReset = it },
                    colors = CheckboxDefaults.colors(checkedColor = Red)
                )
                Text("Termasuk hapus Users (kecuali akun inti)",
                    style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }

            Button(
                onClick = { showResetDialog = true },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Red),
                enabled = !isResetting && !isResetAndSeed
            ) {
                if (isResetting) {
                    CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Menghapus...", color = Color.White)
                } else {
                    Icon(Icons.Default.Delete, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Reset Semua Data")
                }
            }

            // Reset progress logs
            AnimatedVisibility(resetLogs.isNotEmpty()) {
                Surface(
                    Modifier.fillMaxWidth().padding(top = 8.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = RedSoft.copy(alpha = 0.3f)
                ) {
                    Column(Modifier.padding(10.dp)) {
                        resetLogs.forEach { log ->
                            Text("• $log", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        }
                    }
                }
            }
            resetResult?.let {
                Spacer(Modifier.height(4.dp))
                Text(it, style = MaterialTheme.typography.bodySmall,
                    color = if (it.startsWith("Berhasil")) Green else Red)
            }
        }

        Spacer(Modifier.height(100.dp))
    }

    // ── CONFIRM DIALOG ──
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            icon = { Icon(Icons.Default.Warning, null, tint = Red, modifier = Modifier.size(32.dp)) },
            title = { Text("Reset Semua Data?", textAlign = TextAlign.Center) },
            text = {
                Column {
                    Text("Tindakan ini akan menghapus SEMUA data di Firestore:", fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(6.dp))
                    Text("• Employees, Attendance, Payroll\n• KPI, Peer Reviews, AI Reviews\n• Leave, Contracts, Schedules\n• Notifications, Audit Logs, dll",
                        style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    if (includeUsersInReset) {
                        Spacer(Modifier.height(6.dp))
                        Text("• TERMASUK Users (kecuali 4 akun inti)",
                            style = MaterialTheme.typography.bodySmall, color = Red, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Akun inti yang di-preserve:\nakungweh54, nandazhafran, driveperson69420, akunsayananda0",
                        style = MaterialTheme.typography.labelSmall, color = TextMuted)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetDialog = false
                        isResetting = true
                        resetLogs = emptyList()
                        resetResult = null
                        scope.launch(Dispatchers.IO) {
                            val result = SeedDataManager.resetAll(
                                includeUsers = includeUsersInReset,
                                onProgress = { col, count ->
                                    resetLogs = resetLogs + "$col → $count dihapus"
                                }
                            )
                            isResetting = false
                            resetResult = if (result.errors.isEmpty()) {
                                "Berhasil! ${result.inserted} dokumen dihapus."
                            } else {
                                "${result.inserted} dihapus, ${result.errors.size} error"
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Red)
                ) { Text("Ya, Reset Semua") }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("Batal") }
            }
        )
    }
}

// ── SECTION CARD COMPOSABLE ──────────────────────────────────────────────
@Composable
private fun DevSection(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    borderColor: Color = CardBorder.copy(alpha = 0.2f),
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        color = Surface,
        shadowElevation = 1.dp,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(icon, null, tint = iconColor, modifier = Modifier.size(22.dp))
                Column {
                    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(subtitle, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

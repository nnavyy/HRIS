package com.ptniger.hris.ui.kpi

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ptniger.hris.data.model.KpiConfig
import com.ptniger.hris.data.model.KpiScore
import com.ptniger.hris.data.model.User
import com.ptniger.hris.ui.theme.*
import com.ptniger.hris.utils.DateUtils
import com.ptniger.hris.utils.KpiCalculator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KpiConfigScreen(user: User, onBack: () -> Unit = {}, vm: KpiViewModel = viewModel()) {
    var dept by remember { mutableStateOf("") }
    var deptExpanded by remember { mutableStateOf(false) }
    var kpiName by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    val configs by vm.configs.collectAsState()
    val message by vm.message.collectAsState()
    LaunchedEffect(Unit) { vm.loadConfigs() }

    val depts = listOf(
        "Engineering", "Marketing", "Finance", "HR",
        "Operations", "Sales", "IT", "Legal", "Procurement"
    )

    Column(Modifier.fillMaxSize().background(Background).statusBarsPadding().verticalScroll(rememberScrollState())) {
        Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 64.dp, top = 14.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            Text("KPI Configuration", style = MaterialTheme.typography.headlineMedium)
        }
        Surface(Modifier.fillMaxWidth().padding(horizontal = 18.dp), shape = RoundedCornerShape(24.dp), color = Surface, shadowElevation = 2.dp) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Tambah KPI Baru", style = MaterialTheme.typography.titleMedium)
                ExposedDropdownMenuBox(expanded = deptExpanded, onExpandedChange = { deptExpanded = !deptExpanded }) {
                    OutlinedTextField(
                        value = dept.ifEmpty { "Pilih Departemen" },
                        onValueChange = {}, readOnly = true,
                        label = { Text("Departemen") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = deptExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    )
                    ExposedDropdownMenu(expanded = deptExpanded, onDismissRequest = { deptExpanded = false }) {
                        depts.forEach { d ->
                            DropdownMenuItem(text = { Text(d) }, onClick = { dept = d; deptExpanded = false })
                        }
                    }
                }
                OutlinedTextField(kpiName, { kpiName = it }, Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), label = { Text("Nama KPI") }, singleLine = true)
                OutlinedTextField(weight, { weight = it }, Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), label = { Text("Bobot (0.0-1.0)") }, singleLine = true)
                OutlinedTextField(desc, { desc = it }, Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), label = { Text("Deskripsi") }, singleLine = true)
                if (message != null) Text(message!!, color = Green, style = MaterialTheme.typography.bodySmall)
                Button(onClick = {
                    vm.addConfig(KpiConfig(department = dept, kpiName = kpiName, weight = weight.toDoubleOrNull() ?: 0.0, description = desc))
                    kpiName = ""; weight = ""; desc = ""
                }, Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = Blue)) { Text("Simpan KPI") }
            }
        }
        Spacer(Modifier.height(16.dp))
        configs.forEach { cfg ->
            Surface(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 4.dp), shape = RoundedCornerShape(18.dp), color = Surface, shadowElevation = 1.dp) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(40.dp).clip(RoundedCornerShape(14.dp)).background(PurpleSoft), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Star, null, tint = Purple, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(cfg.kpiName, style = MaterialTheme.typography.titleSmall)
                        Text("${cfg.department} · Bobot: ${(cfg.weight * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                    IconButton(onClick = { vm.deleteConfig(cfg.configId) }) { Icon(Icons.Default.Delete, null, tint = Red, modifier = Modifier.size(18.dp)) }
                }
            }
        }
        Spacer(Modifier.height(100.dp))
    }
}

@Composable
fun KpiScoringScreen(user: User, onBack: () -> Unit = {}, vm: KpiViewModel = viewModel()) {
    var selectedEmpId by remember { mutableStateOf("") }
    val employees by vm.employees.collectAsState()
    val configs by vm.configs.collectAsState()
    val scoreInputs = remember { mutableStateMapOf<String, String>() }
    val message by vm.message.collectAsState()
    LaunchedEffect(Unit) { vm.loadEmployees(); vm.loadConfigs() }

    Column(Modifier.fillMaxSize().background(Background).statusBarsPadding().verticalScroll(rememberScrollState())) {
        Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 64.dp, top = 14.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            Text("Penilaian KPI", style = MaterialTheme.typography.headlineMedium)
        }
        Surface(Modifier.fillMaxWidth().padding(horizontal = 18.dp), shape = RoundedCornerShape(24.dp), color = Surface, shadowElevation = 2.dp) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Pilih Karyawan", style = MaterialTheme.typography.titleMedium)
                val filteredEmployees = if (user.departmentId.isNotEmpty()) {
                    employees.filter { it.department.equals(user.departmentId, ignoreCase = true) }
                } else {
                    employees
                }
                
                if (filteredEmployees.isEmpty()) {
                    Text("Belum ada anggota tim", color = TextSecondary)
                } else {
                    filteredEmployees.take(10).forEach { emp ->
                        Surface(onClick = { selectedEmpId = emp.employeeId }, shape = RoundedCornerShape(12.dp), color = if (selectedEmpId == emp.employeeId) BlueSoft else Background) {
                            Text("${emp.name} - ${emp.department}", Modifier.fillMaxWidth().padding(12.dp), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
        if (selectedEmpId.isNotEmpty() && configs.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Surface(Modifier.fillMaxWidth().padding(horizontal = 18.dp), shape = RoundedCornerShape(24.dp), color = Surface, shadowElevation = 2.dp) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Input Skor (0-100)", style = MaterialTheme.typography.titleMedium)
                    val selectedEmp = employees.find { it.employeeId == selectedEmpId }
                    val empConfigs = configs.filter { it.department.equals(selectedEmp?.department, ignoreCase = true) }
                    
                    if (empConfigs.isEmpty()) {
                        Text("Belum ada konfigurasi KPI untuk departemen ${selectedEmp?.department}", color = TextSecondary)
                    } else {
                        empConfigs.forEach { cfg ->
                            OutlinedTextField(scoreInputs[cfg.configId] ?: "", { scoreInputs[cfg.configId] = it }, Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), singleLine = true, label = { Text("${cfg.kpiName} (${(cfg.weight * 100).toInt()}%)") })
                        }
                    }
                    if (message != null) Text(message!!, color = Green, style = MaterialTheme.typography.bodySmall)
                    Button(onClick = {
                        val selectedEmp = employees.find { it.employeeId == selectedEmpId }
                        val empConfigs = configs.filter { it.department.equals(selectedEmp?.department, ignoreCase = true) }
                        empConfigs.forEach { cfg ->
                            val s = scoreInputs[cfg.configId]?.toIntOrNull() ?: 0
                            vm.submitScore(KpiScore(employeeId = selectedEmpId, configId = cfg.configId, kpiName = cfg.kpiName, score = s, weight = cfg.weight, weightedScore = s * cfg.weight, period = DateUtils.currentPeriod(), scoredBy = user.userId))
                        }
                    }, Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = Blue)) { Text("Simpan Skor") }
                }
            }
        }
        Spacer(Modifier.height(100.dp))
    }
}

@Composable
fun KpiResultScreen(user: User, onBack: () -> Unit = {}, vm: KpiViewModel = viewModel()) {
    val scores by vm.scores.collectAsState()
    val totalScore by vm.totalScore.collectAsState()
    LaunchedEffect(Unit) { vm.loadScores(user.employeeId) }

    Column(Modifier.fillMaxSize().background(Background).statusBarsPadding().verticalScroll(rememberScrollState())) {
        Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 64.dp, top = 14.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            Text("Hasil KPI Saya", style = MaterialTheme.typography.headlineMedium)
        }
        Surface(Modifier.fillMaxWidth().padding(horizontal = 18.dp), shape = RoundedCornerShape(24.dp), color = Surface, shadowElevation = 2.dp) {
            Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Total Skor KPI", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text(String.format("%.1f", totalScore), style = MaterialTheme.typography.displayLarge, color = Blue)
                Spacer(Modifier.height(4.dp))
                val label = KpiCalculator.getBonusLabel(totalScore)
                val pct = KpiCalculator.getBonusPercentage(totalScore)
                Surface(shape = RoundedCornerShape(999.dp), color = if (totalScore >= 80) GreenSoft else OrangeSoft) {
                    Text(label, Modifier.padding(horizontal = 12.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium, color = if (totalScore >= 80) Green else Orange)
                }
                Text("Bonus: ${(pct * 100).toInt()}% dari gaji pokok", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }
        Spacer(Modifier.height(16.dp))
        scores.forEach { score ->
            Surface(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 4.dp), shape = RoundedCornerShape(18.dp), color = Surface, shadowElevation = 1.dp) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(score.kpiName, style = MaterialTheme.typography.titleSmall)
                        Text("Bobot: ${(score.weight * 100).toInt()}% · Skor: ${score.score}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                    Text(String.format("%.1f", score.weightedScore), style = MaterialTheme.typography.titleMedium, color = Blue)
                }
            }
        }
        Spacer(Modifier.height(100.dp))
    }
}

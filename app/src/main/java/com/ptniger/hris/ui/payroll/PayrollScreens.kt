package com.ptniger.hris.ui.payroll

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ptniger.hris.data.model.User
import com.ptniger.hris.ui.theme.*
import com.ptniger.hris.utils.Constants
import java.text.NumberFormat
import java.util.Locale

@Composable
fun PayrollApprovalScreen(user: User, onBack: () -> Unit = {}, vm: PayrollViewModel = viewModel()) {
    val payrolls by vm.payrolls.collectAsState()
    val message by vm.message.collectAsState()
    LaunchedEffect(Unit) { vm.getPendingApprovals(user.userId) }

    Column(Modifier.fillMaxSize().background(Background).statusBarsPadding().verticalScroll(rememberScrollState())) {
        Row(
            Modifier.fillMaxWidth().padding(start = 4.dp, end = 64.dp, top = 14.dp, bottom = 10.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Persetujuan Payroll", style = MaterialTheme.typography.headlineMedium)
        }
        if (message != null) Text(message!!, Modifier.padding(horizontal = 18.dp), color = Green, style = MaterialTheme.typography.bodySmall)
        
        if (payrolls.isEmpty()) {
            Text("Tidak ada payroll menunggu persetujuan", Modifier.padding(18.dp), color = TextSecondary)
        } else {
            payrolls.forEach { p ->
                Surface(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp), shape = RoundedCornerShape(24.dp), color = Surface, shadowElevation = 1.dp) {
                    Column(Modifier.padding(16.dp)) {
                        Text("${p.employeeName} · ${com.ptniger.hris.utils.DateUtils.formatMonthYear(p.month, p.year)}", style = MaterialTheme.typography.titleMedium)
                        Text("Net: Rp ${p.netSalary}", style = MaterialTheme.typography.bodyMedium, color = Green)
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { vm.processApproval(p.payrollId, user.userId, true, "OK") }, colors = ButtonDefaults.buttonColors(containerColor = Green), modifier = Modifier.weight(1f)) {
                                Text("Setujui")
                            }
                            Button(onClick = { vm.processApproval(p.payrollId, user.userId, false, "Revisi") }, colors = ButtonDefaults.buttonColors(containerColor = Red), modifier = Modifier.weight(1f)) {
                                Text("Tolak")
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(100.dp))
    }
}

@Composable
fun PayrollScreen(user: User, onBack: () -> Unit = {}, vm: PayrollViewModel = viewModel()) {
    var empId by remember { mutableStateOf("") }
    var empName by remember { mutableStateOf("") }
    var baseSalary by remember { mutableStateOf("") }
    var allowance by remember { mutableStateOf("") }
    var overtime by remember { mutableStateOf("") }
    var deductions by remember { mutableStateOf("") }
    val payrolls by vm.payrolls.collectAsState()
    val message by vm.message.collectAsState()
    LaunchedEffect(Unit) { vm.loadAll() }

    Column(Modifier.fillMaxSize().background(Background).statusBarsPadding().verticalScroll(rememberScrollState())) {
        Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 64.dp, top = 14.dp, bottom = 10.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            Text("Payroll", style = MaterialTheme.typography.headlineMedium)
        }
        Surface(Modifier.fillMaxWidth().padding(horizontal = 18.dp), shape = RoundedCornerShape(24.dp), color = Surface, shadowElevation = 2.dp) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Generate Payroll", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(empId, { empId = it }, Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), label = { Text("Employee ID") }, singleLine = true)
                OutlinedTextField(empName, { empName = it }, Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), label = { Text("Nama") }, singleLine = true)
                OutlinedTextField(baseSalary, { baseSalary = it }, Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), label = { Text("Gaji Pokok") }, singleLine = true)
                OutlinedTextField(allowance, { allowance = it }, Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), label = { Text("Tunjangan") }, singleLine = true)
                OutlinedTextField(overtime, { overtime = it }, Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), label = { Text("Jam Lembur") }, singleLine = true)
                OutlinedTextField(deductions, { deductions = it }, Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), label = { Text("Potongan Lainnya") }, singleLine = true)
                Text("* Bonus KPI dan Potongan BPJS (Kes, JHT, JP) dihitung otomatis", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                if (message != null) Text(message!!, color = Green, style = MaterialTheme.typography.bodySmall)
                Button(onClick = {
                    vm.generate(empId, empName, baseSalary.toDoubleOrNull() ?: 0.0, allowance.toDoubleOrNull() ?: 0.0, overtime.toDoubleOrNull() ?: 0.0, deductions.toDoubleOrNull() ?: 0.0)
                }, Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = Blue)) { Text("Generate Payroll") }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("Riwayat Payroll", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 18.dp))
        Spacer(Modifier.height(8.dp))
        payrolls.forEach { p ->
            SlipCard(p, user)
            
            // Finance Action Buttons
            if (p.status == Constants.PayrollStatus.DRAFT) {
                Button(onClick = { vm.requestApproval(p.payrollId, user.userId) }, Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 4.dp)) { Text("Ajukan Persetujuan") }
            } else if (p.status == Constants.PayrollStatus.APPROVED) {
                Button(onClick = { vm.finalizePayroll(p.payrollId, user.userId) }, Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 4.dp)) { Text("Finalisasi") }
            } else if (p.status == Constants.PayrollStatus.FINALIZED) {
                Button(onClick = { vm.markPayrollAsPaid(p.payrollId, user.userId) }, Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 4.dp), colors = ButtonDefaults.buttonColors(containerColor = Green)) { Text("Tandai Sudah Dibayar") }
            }
            
            Spacer(Modifier.height(8.dp))
        }
        Spacer(Modifier.height(100.dp))
    }
}

@Composable
fun SalarySlipScreen(user: User, onBack: () -> Unit = {}, vm: PayrollViewModel = viewModel()) {
    val payrolls by vm.payrolls.collectAsState()
    val empId = user.employeeId.ifEmpty { user.userId }
    LaunchedEffect(Unit) { vm.loadByEmployee(empId) }

    Column(Modifier.fillMaxSize().background(Background).statusBarsPadding().verticalScroll(rememberScrollState())) {
        Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 64.dp, top = 14.dp, bottom = 10.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            Text("Slip Gaji", style = MaterialTheme.typography.headlineMedium)
        }
        if (payrolls.isEmpty()) Text("Belum ada slip gaji", Modifier.padding(18.dp), style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        payrolls.forEach { p ->
            SlipCard(p, user)
            Spacer(Modifier.height(8.dp))
        }
        Spacer(Modifier.height(100.dp))
    }
}

@Composable
private fun SlipCard(p: com.ptniger.hris.data.model.Payroll, user: User) {
    val fmt = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    val name = p.employeeName.ifEmpty { user.name }
    Surface(Modifier.fillMaxWidth().padding(horizontal = 18.dp), shape = RoundedCornerShape(24.dp), color = Surface, shadowElevation = 1.dp) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Slip Gaji · ${com.ptniger.hris.utils.DateUtils.formatMonthYear(p.month, p.year)}", style = MaterialTheme.typography.titleSmall)
                Surface(shape = RoundedCornerShape(999.dp), color = if (p.status == "paid") GreenSoft else BlueSoft) {
                    Text(p.status.uppercase(), Modifier.padding(horizontal = 8.dp, vertical = 3.dp), style = MaterialTheme.typography.labelSmall, color = if (p.status == "paid") Green else Blue)
                }
            }
            Text(name, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            SlipRow("Gaji Pokok", fmt.format(p.baseSalary), TextPrimary)
            SlipRow("Tunjangan", fmt.format(p.allowance), TextPrimary)
            SlipRow("Lembur (${p.overtimeHours} jam)", fmt.format(p.overtimePay), TextPrimary)
            SlipRow("Bonus KPI (Skor: ${String.format("%.1f", p.kpiScore)})", fmt.format(p.kpiBonus), Purple)
            
            val totalBpjs = p.bpjsKesehatan + p.bpjsJht + p.bpjsJp
            SlipRow("Potongan BPJS (Kes/JHT/JP)", "-${fmt.format(totalBpjs)}", Red)
            SlipRow("Potongan Lainnya", "-${fmt.format(p.deductions)}", Red)
            
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total Bersih", style = MaterialTheme.typography.titleSmall)
                Text(fmt.format(p.netSalary), style = MaterialTheme.typography.titleSmall, color = Green)
            }
        }
    }
}

@Composable
private fun SlipRow(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        Text(value, style = MaterialTheme.typography.bodySmall, color = color)
    }
}

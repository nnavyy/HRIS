package com.ptniger.hris.ui.payroll

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ptniger.hris.data.model.User
import com.ptniger.hris.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

@Composable
fun PayrollScreen(user: User, vm: PayrollViewModel = viewModel()) {
    var empId by remember { mutableStateOf("") }
    var empName by remember { mutableStateOf("") }
    var baseSalary by remember { mutableStateOf("") }
    var allowance by remember { mutableStateOf("") }
    var overtime by remember { mutableStateOf("") }
    var deductions by remember { mutableStateOf("") }
    val payrolls by vm.payrolls.collectAsState()
    val message by vm.message.collectAsState()
    LaunchedEffect(Unit) { vm.loadAll() }

    Column(Modifier.fillMaxSize().background(Background).verticalScroll(rememberScrollState())) {
        Text("Payroll", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(18.dp))
        Surface(Modifier.fillMaxWidth().padding(horizontal = 18.dp), shape = RoundedCornerShape(24.dp), color = Surface, shadowElevation = 2.dp) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Generate Payroll", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(empId, { empId = it }, Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), label = { Text("Employee ID") }, singleLine = true)
                OutlinedTextField(empName, { empName = it }, Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), label = { Text("Nama") }, singleLine = true)
                OutlinedTextField(baseSalary, { baseSalary = it }, Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), label = { Text("Gaji Pokok") }, singleLine = true)
                OutlinedTextField(allowance, { allowance = it }, Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), label = { Text("Tunjangan") }, singleLine = true)
                OutlinedTextField(overtime, { overtime = it }, Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), label = { Text("Lembur") }, singleLine = true)
                OutlinedTextField(deductions, { deductions = it }, Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), label = { Text("Potongan") }, singleLine = true)
                Text("* Bonus KPI dihitung otomatis dari skor KPI karyawan", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
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
            SlipCard(p.employeeName, p.month, p.year, p.baseSalary, p.allowance, p.overtimePay, p.kpiBonus, p.kpiScore, p.deductions, p.netSalary, p.status)
            Spacer(Modifier.height(8.dp))
        }
        Spacer(Modifier.height(100.dp))
    }
}

@Composable
fun SalarySlipScreen(user: User, vm: PayrollViewModel = viewModel()) {
    val payrolls by vm.payrolls.collectAsState()
    LaunchedEffect(Unit) { vm.loadByEmployee(user.employeeId) }

    Column(Modifier.fillMaxSize().background(Background).verticalScroll(rememberScrollState())) {
        Text("Slip Gaji", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(18.dp))
        if (payrolls.isEmpty()) Text("Belum ada slip gaji", Modifier.padding(18.dp), style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        payrolls.forEach { p ->
            SlipCard(p.employeeName.ifEmpty { user.name }, p.month, p.year, p.baseSalary, p.allowance, p.overtimePay, p.kpiBonus, p.kpiScore, p.deductions, p.netSalary, p.status)
            Spacer(Modifier.height(8.dp))
        }
        Spacer(Modifier.height(100.dp))
    }
}

@Composable
private fun SlipCard(name: String, month: Int, year: Int, base: Double, allow: Double, ot: Double, kpiBonus: Double, kpiScore: Double, ded: Double, net: Double, status: String) {
    val fmt = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    Surface(Modifier.fillMaxWidth().padding(horizontal = 18.dp), shape = RoundedCornerShape(24.dp), color = Surface, shadowElevation = 1.dp) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Slip Gaji · ${com.ptniger.hris.utils.DateUtils.formatMonthYear(month, year)}", style = MaterialTheme.typography.titleSmall)
                Surface(shape = RoundedCornerShape(999.dp), color = if (status == "paid") GreenSoft else BlueSoft) {
                    Text(status.uppercase(), Modifier.padding(horizontal = 8.dp, vertical = 3.dp), style = MaterialTheme.typography.labelSmall, color = if (status == "paid") Green else Blue)
                }
            }
            Text(name, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            SlipRow("Gaji Pokok", fmt.format(base), TextPrimary)
            SlipRow("Tunjangan", fmt.format(allow), TextPrimary)
            SlipRow("Lembur", fmt.format(ot), TextPrimary)
            SlipRow("Bonus KPI (Skor: ${String.format("%.1f", kpiScore)})", fmt.format(kpiBonus), Purple)
            SlipRow("Potongan", fmt.format(ded), Red)
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total Bersih", style = MaterialTheme.typography.titleSmall)
                Text(fmt.format(net), style = MaterialTheme.typography.titleSmall, color = Green)
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

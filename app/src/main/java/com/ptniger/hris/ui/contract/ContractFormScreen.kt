package com.ptniger.hris.ui.contract

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ptniger.hris.data.model.Employee
import com.ptniger.hris.data.model.EmployeeContract
import com.ptniger.hris.data.model.User
import com.ptniger.hris.data.repository.ContractRepository
import com.ptniger.hris.data.repository.NotificationRepository
import com.ptniger.hris.ui.theme.*
import com.ptniger.hris.utils.DateUtils
import com.ptniger.hris.utils.PayrollCalculator
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContractFormScreen(
    user: User,
    employee: Employee,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    // Form fields
    var effectiveDate by remember { mutableStateOf(DateUtils.today()) }
    var baseSalary by remember { mutableStateOf(employee.baseSalary.toLong().toString()) }
    var allowanceMeal by remember { mutableStateOf("0") }
    var allowanceTransport by remember { mutableStateOf("0") }
    var allowancePosition by remember { mutableStateOf("0") }
    var ptkpStatus by remember { mutableStateOf("TK/0") }
    var overrideMultiplier by remember { mutableStateOf(false) }
    var overtime1x by remember { mutableStateOf("1.5") }
    var overtimeNx by remember { mutableStateOf("2.0") }
    var expandedPtkp by remember { mutableStateOf(false) }

    val ptkpOptions = listOf("TK/0", "TK/1", "TK/2", "TK/3", "K/0", "K/1", "K/2", "K/3")

    // Live preview calculation
    val base = baseSalary.toDoubleOrNull() ?: 0.0
    val meal = allowanceMeal.toDoubleOrNull() ?: 0.0
    val transport = allowanceTransport.toDoubleOrNull() ?: 0.0
    val position = allowancePosition.toDoubleOrNull() ?: 0.0
    val totalAllowance = PayrollCalculator.calculateTotalAllowance(meal, transport, position)
    val gross = base + totalAllowance
    val bpjsKes = PayrollCalculator.calculateBpjsKesehatan(base, totalAllowance)
    val bpjsJht = PayrollCalculator.calculateBpjsJht(base, totalAllowance)
    val bpjsJp = PayrollCalculator.calculateBpjsJp(base, totalAllowance)
    val pph21 = PayrollCalculator.calculatePph21Ter(gross, ptkpStatus)
    val net = gross - bpjsKes - bpjsJht - bpjsJp - pph21
    val jkk = PayrollCalculator.calculateBpjsJkk(base)
    val jkm = PayrollCalculator.calculateBpjsJkm(base)

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
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = null)
            }
            Column(Modifier.weight(1f).padding(start = 4.dp)) {
                Text("Buat Kontrak Kerja", style = MaterialTheme.typography.titleLarge)
                Text(employee.name, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }

        Column(Modifier.padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

            // -- Effective Date --
            Text("Tanggal Berlaku", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            OutlinedTextField(
                value = effectiveDate,
                onValueChange = { effectiveDate = it },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                placeholder = { Text("YYYY-MM-DD") },
                singleLine = true
            )

            HorizontalDivider(color = CardBorder)

            // -- Salary section --
            Text("Komponen Gaji", style = MaterialTheme.typography.titleSmall)

            NumberField("Gaji Pokok (Rp)", baseSalary) { baseSalary = it }
            NumberField("Tunjangan Makan (Rp)", allowanceMeal) { allowanceMeal = it }
            NumberField("Tunjangan Transport (Rp)", allowanceTransport) { allowanceTransport = it }
            NumberField("Tunjangan Jabatan (Rp)", allowancePosition) { allowancePosition = it }

            HorizontalDivider(color = CardBorder)

            // -- PTKP Status --
            Text("Status PTKP (PPh 21)", style = MaterialTheme.typography.titleSmall)
            ExposedDropdownMenuBox(
                expanded = expandedPtkp,
                onExpandedChange = { expandedPtkp = it }
            ) {
                OutlinedTextField(
                    value = ptkpStatus,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    shape = RoundedCornerShape(16.dp),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPtkp) }
                )
                ExposedDropdownMenu(
                    expanded = expandedPtkp,
                    onDismissRequest = { expandedPtkp = false }
                ) {
                    ptkpOptions.forEach { opt ->
                        DropdownMenuItem(text = { Text(opt) }, onClick = {
                            ptkpStatus = opt
                            expandedPtkp = false
                        })
                    }
                }
            }

            HorizontalDivider(color = CardBorder)

            // -- Overtime multiplier override --
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = overrideMultiplier, onCheckedChange = { overrideMultiplier = it })
                Spacer(Modifier.width(8.dp))
                Column {
                    Text("Override Multiplier Lembur", style = MaterialTheme.typography.bodyMedium)
                    Text("Default: 1.5x & 2x (UU Cipta Kerja)", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }
            }
            if (overrideMultiplier) {
                NumberField("Jam ke-1 (default 1.5)", overtime1x) { overtime1x = it }
                NumberField("Jam ke-2+ (default 2.0)", overtimeNx) { overtimeNx = it }
            }

            HorizontalDivider(color = CardBorder)

            // -- Live Preview --
            Surface(shape = RoundedCornerShape(20.dp), color = BlueSoft, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Prakiraan Payroll Bulanan", style = MaterialTheme.typography.titleSmall, color = Blue)
                    PreviewRow("Gaji Pokok", base)
                    PreviewRow("Total Tunjangan", totalAllowance)
                    PreviewRow("Gross", gross)
                    HorizontalDivider(color = Blue.copy(0.2f))
                    PreviewRow("BPJS Kesehatan (−)", bpjsKes)
                    PreviewRow("BPJS JHT (−)", bpjsJht)
                    PreviewRow("BPJS JP (−)", bpjsJp)
                    PreviewRow("PPh 21 TER (−)", pph21)
                    HorizontalDivider(color = Blue.copy(0.2f))
                    PreviewRow("Estimasi Gaji Bersih", net, highlight = true)
                    HorizontalDivider(color = Blue.copy(0.2f))
                    Text("Tanggungan Perusahaan (tidak dipotong dari gaji)", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    PreviewRow("BPJS JKK", jkk)
                    PreviewRow("BPJS JKM", jkm)
                }
            }

            // -- Message --
            message?.let {
                Surface(shape = RoundedCornerShape(12.dp), color = if (it.contains("berhasil")) GreenSoft else RedSoft) {
                    Text(it, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
                }
            }

            // -- Save button --
            Button(
                onClick = {
                    scope.launch {
                        isLoading = true
                        message = null
                        val contract = EmployeeContract(
                            employeeId = employee.employeeId.ifEmpty { employee.userId },
                            effectiveDate = effectiveDate,
                            baseSalary = base,
                            allowanceMeal = meal,
                            allowanceTransport = transport,
                            allowancePosition = position,
                            ptkpStatus = ptkpStatus,
                            overrideOvertimeMultiplier = overrideMultiplier,
                            overtime1xMultiplier = overtime1x.toDoubleOrNull() ?: 1.5,
                            overtimeNxMultiplier = overtimeNx.toDoubleOrNull() ?: 2.0,
                            createdBy = user.uid
                        )
                        ContractRepository().create(contract).fold(
                            onSuccess = { contractId ->
                                // Send notification to employee
                                NotificationRepository().sendContractNotification(
                                    employeeId = contract.employeeId,
                                    contractId = contractId,
                                    employeeName = employee.name
                                )
                                message = "Kontrak berhasil dibuat dan notifikasi dikirim ke ${employee.name}"
                            },
                            onFailure = { message = "Gagal: ${it.message}" }
                        )
                        isLoading = false
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = Blue)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Save, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Simpan & Kirim ke Karyawan untuk TTD")
                }
            }

            Spacer(Modifier.height(100.dp))
        }
    }
}

@Composable
private fun NumberField(label: String, value: String, onChange: (String) -> Unit) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = value, onValueChange = onChange,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
    }
}

@Composable
private fun PreviewRow(label: String, amount: Double, highlight: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            label,
            style = if (highlight) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodySmall,
            color = if (highlight) Blue else TextSecondary
        )
        Text(
            "Rp ${"%,.0f".format(amount)}",
            style = if (highlight) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodySmall,
            color = if (highlight) Blue else TextPrimary
        )
    }
}

package com.ptniger.hris.ui.employee

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ptniger.hris.data.model.Employee
import com.ptniger.hris.data.model.User
import com.ptniger.hris.data.repository.EmployeeRepository
import com.ptniger.hris.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun EmployeeFormScreen(employeeId: String?, user: User, onBack: () -> Unit, vm: EmployeeViewModel = viewModel()) {
    val isNew = employeeId == null
    var nik by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var position by remember { mutableStateOf("") }
    var department by remember { mutableStateOf("") }
    var branch by remember { mutableStateOf("") }
    var baseSalary by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val message by vm.message.collectAsState()

    LaunchedEffect(employeeId) {
        if (!isNew && employeeId != null) {
            val emp = EmployeeRepository().getById(employeeId)
            emp?.let {
                nik = it.nik; name = it.name; email = it.email; phone = it.phone
                position = it.position; department = it.department; branch = it.branch
                baseSalary = it.baseSalary.toLong().toString()
            }
        }
    }

    LaunchedEffect(message) { if (message != null) { kotlinx.coroutines.delay(1500); vm.clearMessage(); if (message?.contains("berhasil") == true) onBack() } }

    Column(Modifier.fillMaxSize().background(Background).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(start = 18.dp, end = 64.dp, top = 14.dp, bottom = 10.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            Text(if (isNew) "Tambah Karyawan" else "Edit Karyawan", style = MaterialTheme.typography.headlineMedium)
        }

        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            FormField("NIK", nik) { nik = it }
            FormField("Nama Lengkap", name) { name = it }
            FormField("Email", email) { email = it }
            FormField("No. Telepon", phone) { phone = it }
            FormField("Jabatan", position) { position = it }
            FormField("Departemen", department) { department = it }
            FormField("Cabang", branch) { branch = it }
            FormField("Gaji Pokok (Rp)", baseSalary) { baseSalary = it }

            if (message != null) {
                Text(message!!, color = if (message!!.contains("Error")) Red else Green, style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick = {
                    val emp = Employee(
                        employeeId = employeeId ?: "", nik = nik, name = name, email = email,
                        phone = phone, position = position, department = department, branch = branch,
                        baseSalary = baseSalary.toDoubleOrNull() ?: 0.0
                    )
                    vm.save(emp, isNew)
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = Blue)
            ) { Text(if (isNew) "Simpan Karyawan" else "Update Data") }

            Spacer(Modifier.height(100.dp))
        }
    }
}

@Composable
private fun FormField(label: String, value: String, onChange: (String) -> Unit) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(value = value, onValueChange = onChange, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), singleLine = true)
    }
}

package com.ptniger.hris.ui.employee

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ptniger.hris.data.model.Employee
import com.ptniger.hris.data.model.User
import com.ptniger.hris.data.repository.EmployeeRepository
import com.ptniger.hris.ui.theme.*
import com.ptniger.hris.utils.Constants

val DEPARTMENTS = listOf(
    "Engineering", "Marketing", "Finance", "HR",
    "Operations", "Sales", "IT", "Legal", "Procurement"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeFormScreen(employeeId: String?, user: User, onBack: () -> Unit, vm: EmployeeViewModel = viewModel()) {
    LaunchedEffect(Unit) { vm.loadAll() }
    val officeLocations by vm.officeLocations.collectAsState()
    val usersList by vm.users.collectAsState()
    val isNew = employeeId == null
    var linkedUserId by remember { mutableStateOf("") }
    var userExpanded by remember { mutableStateOf(false) }
    var nik by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var position by remember { mutableStateOf("") }
    var department by remember { mutableStateOf("") }
    var departmentExpanded by remember { mutableStateOf(false) }
    var branch by remember { mutableStateOf("") }
    var officeId by remember { mutableStateOf("") }
    var officeExpanded by remember { mutableStateOf(false) }
    var managerId by remember { mutableStateOf("") }
    var managerExpanded by remember { mutableStateOf(false) }
    var baseSalary by remember { mutableStateOf("") }
    val message by vm.message.collectAsState()

    val managerUsers = usersList.filter {
        it.primaryRole == Constants.Role.MANAGER || it.role == Constants.Role.MANAGER ||
            it.roles.contains(Constants.Role.MANAGER)
    }

    LaunchedEffect(employeeId) {
        if (!isNew && employeeId != null) {
            val emp = EmployeeRepository().getById(employeeId)
            emp?.let {
                nik = it.nik; name = it.name; email = it.email; phone = it.phone
                position = it.position; department = it.department; branch = it.branch
                officeId = it.officeId; managerId = it.managerId
                baseSalary = it.baseSalary.toLong().toString()
                linkedUserId = it.userId
            }
        }
    }

    LaunchedEffect(message) {
        if (message != null) {
            kotlinx.coroutines.delay(1500)
            vm.clearMessage()
            if (message?.contains("berhasil") == true) onBack()
        }
    }

    Column(Modifier.fillMaxSize().background(Background).statusBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 4.dp, end = 64.dp, top = 14.dp, bottom = 10.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            Text(if (isNew) "Tambah Karyawan" else "Edit Karyawan", style = MaterialTheme.typography.headlineMedium)
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column {
                    Text("Integrasi Akun Sistem (Opsional)", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    Spacer(Modifier.height(4.dp))
                    ExposedDropdownMenuBox(expanded = userExpanded, onExpandedChange = { userExpanded = !userExpanded }) {
                        OutlinedTextField(
                            value = usersList.find { it.userId == linkedUserId }?.let { "${it.fullName.ifEmpty { it.name }} - ${it.role}" } ?: "Pilih Akun yang Sudah Terdaftar",
                            onValueChange = {}, readOnly = true,
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            shape = RoundedCornerShape(16.dp),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = userExpanded) }
                        )
                        ExposedDropdownMenu(expanded = userExpanded, onDismissRequest = { userExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text("— Tidak Terhubung —") },
                                onClick = { linkedUserId = ""; userExpanded = false }
                            )
                            usersList.forEach { usr ->
                                DropdownMenuItem(
                                    text = { Text("${usr.fullName.ifEmpty { usr.name }} (${usr.email})") },
                                    onClick = { linkedUserId = usr.userId; if(name.isEmpty()) name = usr.fullName.ifEmpty { usr.name }; if(email.isEmpty()) email = usr.email; userExpanded = false }
                                )
                            }
                        }
                    }
                }
            }

            item { FormField("NIK", nik) { nik = it } }
            item { FormField("Nama Lengkap", name) { name = it } }
            item { FormField("Email", email, KeyboardType.Email) { email = it } }
            item { FormField("No. Telepon", phone, KeyboardType.Phone) { phone = it } }
            item { FormField("Jabatan", position) { position = it } }

            item {
                Column {
                    Text("Departemen", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    Spacer(Modifier.height(4.dp))
                    ExposedDropdownMenuBox(expanded = departmentExpanded, onExpandedChange = { departmentExpanded = !departmentExpanded }) {
                        OutlinedTextField(
                            value = department.ifEmpty { "Pilih Departemen" }, onValueChange = {}, readOnly = true,
                            modifier = Modifier.fillMaxWidth().menuAnchor(), shape = RoundedCornerShape(16.dp),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = departmentExpanded) }
                        )
                        ExposedDropdownMenu(expanded = departmentExpanded, onDismissRequest = { departmentExpanded = false }) {
                            DEPARTMENTS.forEach { dept ->
                                DropdownMenuItem(text = { Text(dept) }, onClick = { department = dept; departmentExpanded = false })
                            }
                        }
                    }
                }
            }

            item {
                Column {
                    Text("Manager Langsung (Opsional)", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    Spacer(Modifier.height(4.dp))
                    ExposedDropdownMenuBox(expanded = managerExpanded, onExpandedChange = { managerExpanded = !managerExpanded }) {
                        OutlinedTextField(
                            value = managerUsers.find { it.userId == managerId }?.let { it.fullName.ifEmpty { it.name } } ?: if (managerId.isEmpty()) "Pilih Manager" else managerId,
                            onValueChange = {}, readOnly = true,
                            modifier = Modifier.fillMaxWidth().menuAnchor(), shape = RoundedCornerShape(16.dp),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = managerExpanded) }
                        )
                        ExposedDropdownMenu(expanded = managerExpanded, onDismissRequest = { managerExpanded = false }) {
                            DropdownMenuItem(text = { Text("— Tidak ada —") }, onClick = { managerId = ""; managerExpanded = false })
                            managerUsers.forEach { mgr ->
                                DropdownMenuItem(
                                    text = { Text("${mgr.fullName.ifEmpty { mgr.name }} (${mgr.email})") },
                                    onClick = { managerId = mgr.userId; managerExpanded = false }
                                )
                            }
                        }
                    }
                }
            }

            item {
                Column {
                    Text("Lokasi Kantor", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    Spacer(Modifier.height(4.dp))
                    ExposedDropdownMenuBox(expanded = officeExpanded, onExpandedChange = { officeExpanded = !officeExpanded }) {
                        val selectedOffice = officeLocations.find { it.id == officeId }
                        val displayValue = if (selectedOffice != null) {
                            selectedOffice.name.ifEmpty { "Lokasi Tanpa Nama" }
                        } else {
                            if (officeId.isEmpty()) "Pilih Kantor" else "Lokasi Tidak Valid"
                        }
                        
                        OutlinedTextField(
                            value = displayValue,
                            onValueChange = {}, readOnly = true,
                            modifier = Modifier.fillMaxWidth().menuAnchor(), shape = RoundedCornerShape(16.dp),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = officeExpanded) }
                        )
                        ExposedDropdownMenu(expanded = officeExpanded, onDismissRequest = { officeExpanded = false }) {
                            if (officeLocations.isEmpty()) {
                                DropdownMenuItem(text = { Text("Belum ada lokasi - tambah di menu Admin") }, onClick = { officeExpanded = false })
                            } else {
                                DropdownMenuItem(text = { Text("— Pilih Kantor —") }, onClick = { officeId = ""; branch = ""; officeExpanded = false })
                                officeLocations.forEach { loc ->
                                    DropdownMenuItem(text = { Text(loc.name.ifEmpty { "Lokasi Tanpa Nama" }) }, onClick = { officeId = loc.id; branch = loc.name; officeExpanded = false })
                                }
                            }
                        }
                    }
                }
            }

            item {
                Column {
                    Text("Cabang", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(
                        value = branch.ifEmpty { "Otomatis dari lokasi kantor" }, onValueChange = {},
                        readOnly = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(disabledTextColor = TextSecondary, disabledBorderColor = TextMuted, disabledLabelColor = TextMuted),
                        enabled = false
                    )
                }
            }

            item { FormField("Gaji Pokok (Rp)", baseSalary, KeyboardType.Number) { baseSalary = it } }

            if (message != null) {
                item { Text(message!!, color = if (message!!.contains("Error")) Red else Green, style = MaterialTheme.typography.bodySmall) }
            }

            item {
                Button(
                    onClick = {
                        val emp = Employee(
                            employeeId = employeeId ?: "", nik = nik, name = name, email = email,
                            phone = phone, position = position, department = department, branch = branch,
                            officeId = officeId, managerId = managerId,
                            baseSalary = baseSalary.toDoubleOrNull() ?: 0.0, userId = linkedUserId
                        )
                        vm.save(emp, isNew)
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Blue)
                ) { Text(if (isNew) "Simpan Karyawan" else "Update Data") }
            }

            item { Spacer(Modifier.height(100.dp)) }
        }
    }
}

@Composable
private fun FormField(label: String, value: String, keyboardType: KeyboardType = KeyboardType.Text, onChange: (String) -> Unit) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = value, onValueChange = onChange, modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp), singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType)
        )
    }
}

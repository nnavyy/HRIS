package com.ptniger.hris.ui.admin

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
import com.ptniger.hris.data.model.AutomationRule
import com.ptniger.hris.data.model.User
import com.ptniger.hris.data.repository.AuditLogRepository
import com.ptniger.hris.ui.theme.*
import com.ptniger.hris.utils.Constants
import com.ptniger.hris.utils.RoleManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import androidx.compose.ui.tooling.preview.Preview
import com.ptniger.hris.ui.theme.HRISTheme

@Composable
fun RoleManagementScreen(user: User, onBack: () -> Unit = {}) {
    val roles = listOf(Constants.Role.HR, Constants.Role.FINANCE, Constants.Role.MANAGER, Constants.Role.EMPLOYEE)
    var expandedRole by remember { mutableStateOf<String?>(null) }
    var usersByRole by remember { mutableStateOf<Map<String, List<User>>>(emptyMap()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                val allUsers = db.collection(Constants.Collections.USERS).get().await()
                    .documents.mapNotNull { doc ->
                        doc.toObject(User::class.java)?.copy(userId = doc.id)
                    }
                val grouped = mutableMapOf<String, List<User>>()
                roles.forEach { role ->
                    grouped[role] = allUsers.filter { u ->
                        u.roles.contains(role) || u.primaryRole == role || u.role == role
                    }
                }
                usersByRole = grouped
            } catch (_: Exception) {}
        }
    }

    Column(Modifier.fillMaxSize().background(Background).statusBarsPadding().verticalScroll(rememberScrollState())) {
        Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 64.dp, top = 14.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            Text("Role & Access", style = MaterialTheme.typography.headlineMedium)
        }
        roles.forEach { role ->
            val color = when (role) { Constants.Role.HR -> Blue; Constants.Role.FINANCE -> Orange; Constants.Role.MANAGER -> Teal; else -> Pink }
            val bg = when (role) { Constants.Role.HR -> BlueSoft; Constants.Role.FINANCE -> OrangeSoft; Constants.Role.MANAGER -> TealSoft; else -> PinkSoft }
            val isExpanded = expandedRole == role
            val usersInRole = usersByRole[role] ?: emptyList()

            Surface(
                onClick = { expandedRole = if (isExpanded) null else role },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 4.dp),
                shape = RoundedCornerShape(22.dp), color = Surface, shadowElevation = 1.dp
            ) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(44.dp).clip(RoundedCornerShape(16.dp)).background(bg), contentAlignment = Alignment.Center) {
                            Text(RoleManager.getRoleShort(role), style = MaterialTheme.typography.labelMedium, color = color)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(RoleManager.getRoleDisplayName(role), style = MaterialTheme.typography.titleSmall)
                            Text("${usersInRole.size} anggota", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                        Icon(
                            if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    if (isExpanded) {
                        Spacer(Modifier.height(10.dp))
                        HorizontalDivider(color = CardBorder)
                        Spacer(Modifier.height(8.dp))
                        
                        if (usersInRole.isEmpty()) {
                            Text("Belum ada user dengan role ini", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        } else {
                            usersInRole.forEach { u ->
                                Row(
                                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(bg),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            (u.fullName.ifEmpty { u.name }).take(2).uppercase(),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = color
                                        )
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(u.fullName.ifEmpty { u.name }, style = MaterialTheme.typography.bodyMedium)
                                        Text(u.email, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                    }
                                    if (u.isActive) {
                                        Surface(shape = RoundedCornerShape(999.dp), color = GreenSoft) {
                                            Text("Aktif", Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = Green)
                                        }
                                    } else {
                                        Surface(shape = RoundedCornerShape(999.dp), color = RedSoft) {
                                            Text("Nonaktif", Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = Red)
                                        }
                                    }
                                }
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
fun AutomationScreen(user: User, onBack: () -> Unit = {}) {
    val repo = remember { AuditLogRepository() }
    var rules by remember { mutableStateOf<List<AutomationRule>>(emptyList()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        repo.seedDefaultRules()
        rules = repo.getRules()
        // Sync into global engine so all modules respect current state
        com.ptniger.hris.utils.AutomationEngine.setRules(rules)
    }

    Column(Modifier.fillMaxSize().background(Background).statusBarsPadding().verticalScroll(rememberScrollState())) {
        Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 64.dp, top = 14.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            Text("Automation Rules", style = MaterialTheme.typography.headlineMedium)
        }
        
        // Info banner
        Surface(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 4.dp),
            shape = RoundedCornerShape(16.dp),
            color = com.ptniger.hris.ui.theme.BlueSoft
        ) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, null, tint = Blue, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "Menonaktifkan rule akan menonaktifkan fitur terkait di semua role.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Blue
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        
        rules.forEach { rule ->
            val colors = mapOf("attendance" to (TealSoft to Teal), "leave" to (BlueSoft to Blue), "payroll" to (OrangeSoft to Orange),
                "notification" to (PurpleSoft to Purple), "account" to (RedSoft to Red), "audit" to (GreenSoft to Green))
            val (bg, fg) = colors[rule.type] ?: (BlueSoft to Blue)

            val statusText = if (rule.isActive) "Aktif" else "Nonaktif"
            val statusColor = if (rule.isActive) Green else Red
            val statusBg = if (rule.isActive) GreenSoft else RedSoft

            Surface(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 4.dp), shape = RoundedCornerShape(22.dp), color = Surface, shadowElevation = 1.dp) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(44.dp).clip(RoundedCornerShape(16.dp)).background(bg), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Settings, null, tint = fg, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(rule.name, style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.width(6.dp))
                            Surface(shape = RoundedCornerShape(999.dp), color = statusBg) {
                                Text(statusText, Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = statusColor)
                            }
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(rule.description, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                    Switch(
                        checked = rule.isActive,
                        onCheckedChange = { checked ->
                            // Optimistic update
                            rules = rules.map { if (it.ruleId == rule.ruleId) it.copy(isActive = checked) else it }
                            // Immediately sync to AutomationEngine
                            com.ptniger.hris.utils.AutomationEngine.setRules(rules)
                            scope.launch {
                                val result = repo.toggleRule(rule.ruleId, checked)
                                if (result.isFailure) {
                                    // Revert on failure
                                    rules = repo.getRules()
                                    com.ptniger.hris.utils.AutomationEngine.setRules(rules)
                                }
                            }
                        },
                        colors = SwitchDefaults.colors(checkedTrackColor = Green)
                    )
                }
            }
        }
        Spacer(Modifier.height(100.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountManagementScreen(user: User, onBack: () -> Unit = {}) {
    val repo = remember { com.ptniger.hris.data.repository.AuthRepository() }
    val officeRepo = remember { com.ptniger.hris.data.repository.OfficeLocationRepository() }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var nik by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(Constants.Role.EMPLOYEE) }
    var roleExpanded by remember { mutableStateOf(false) }
    var selectedDept by remember { mutableStateOf("") }
    var deptExpanded by remember { mutableStateOf(false) }
    var selectedOfficeId by remember { mutableStateOf("") }
    var officeExpanded by remember { mutableStateOf(false) }
    var officeLocations by remember { mutableStateOf<List<com.ptniger.hris.data.model.OfficeLocation>>(emptyList()) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        officeLocations = officeRepo.getActiveLocations()
    }

    val roles = listOf(
        Constants.Role.EMPLOYEE,
        Constants.Role.HR,
        Constants.Role.FINANCE,
        Constants.Role.MANAGER,
        Constants.Role.SUPER_ADMIN
    )
    val depts = listOf(
        "Engineering", "Marketing", "Finance", "HR",
        "Operations", "Sales", "IT", "Legal", "Procurement"
    )
    // Show department dropdown if role is Manager, HR, or Employee
    val needsDept = selectedRole in listOf(Constants.Role.MANAGER, Constants.Role.EMPLOYEE, Constants.Role.HR)

    Column(Modifier.fillMaxSize().background(Background).statusBarsPadding().verticalScroll(rememberScrollState())) {
        Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 64.dp, top = 14.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            Text("Buat Akun Baru", style = MaterialTheme.typography.headlineMedium)
        }

        if (errorMessage != null) {
            Text(errorMessage!!, color = Red, modifier = Modifier.padding(horizontal = 18.dp))
            Spacer(Modifier.height(8.dp))
        }
        if (successMessage != null) {
            Text(successMessage!!, color = Green, modifier = Modifier.padding(horizontal = 18.dp))
            Spacer(Modifier.height(8.dp))
        }

        Surface(Modifier.fillMaxWidth().padding(horizontal = 18.dp), shape = RoundedCornerShape(22.dp), color = Surface) {
            Column(Modifier.padding(18.dp)) {
                OutlinedTextField(
                    value = fullName, onValueChange = { fullName = it },
                    label = { Text("Nama Lengkap") }, modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = nik, onValueChange = { nik = it },
                    label = { Text("NIK / ID Karyawan") }, modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = email, onValueChange = { email = it },
                    label = { Text("Email Login") }, modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = password, onValueChange = { password = it },
                    label = { Text("Password Sementara") }, modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))

                // Role Dropdown
                ExposedDropdownMenuBox(expanded = roleExpanded, onExpandedChange = { roleExpanded = !roleExpanded }) {
                    OutlinedTextField(
                        value = RoleManager.getRoleDisplayName(selectedRole),
                        onValueChange = {}, readOnly = true,
                        label = { Text("Pilih Role Akses") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = roleExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = roleExpanded, onDismissRequest = { roleExpanded = false }) {
                        roles.forEach { role ->
                            DropdownMenuItem(
                                text = { Text(RoleManager.getRoleDisplayName(role)) },
                                onClick = { selectedRole = role; roleExpanded = false }
                            )
                        }
                    }
                }

                // Departemen (tampil jika role Manager/Employee/HR)
                if (needsDept) {
                    Spacer(Modifier.height(12.dp))
                    ExposedDropdownMenuBox(expanded = deptExpanded, onExpandedChange = { deptExpanded = !deptExpanded }) {
                        OutlinedTextField(
                            value = selectedDept.ifEmpty { "Pilih Departemen" },
                            onValueChange = {}, readOnly = true,
                            label = { Text(if (selectedRole == Constants.Role.MANAGER) "Departemen yang Dikelola" else "Departemen") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = deptExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = deptExpanded, onDismissRequest = { deptExpanded = false }) {
                            depts.forEach { dept ->
                                DropdownMenuItem(text = { Text(dept) }, onClick = { selectedDept = dept; deptExpanded = false })
                            }
                        }
                    }
                }

                // Lokasi Kantor
                Spacer(Modifier.height(12.dp))
                ExposedDropdownMenuBox(expanded = officeExpanded, onExpandedChange = { officeExpanded = !officeExpanded }) {
                    OutlinedTextField(
                        value = officeLocations.find { it.id == selectedOfficeId }?.name ?: "Pilih Lokasi Kantor (Opsional)",
                        onValueChange = {}, readOnly = true,
                        label = { Text("Lokasi Kantor") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = officeExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = officeExpanded, onDismissRequest = { officeExpanded = false }) {
                        DropdownMenuItem(text = { Text("— Tidak ada —") }, onClick = { selectedOfficeId = ""; officeExpanded = false })
                        officeLocations.forEach { loc ->
                            DropdownMenuItem(text = { Text(loc.name) }, onClick = { selectedOfficeId = loc.id; officeExpanded = false })
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = {
                        if (fullName.isEmpty() || email.isEmpty() || password.isEmpty() || nik.isEmpty()) {
                            errorMessage = "Harap isi semua field"; return@Button
                        }
                        if (needsDept && selectedDept.isEmpty()) {
                            errorMessage = "Harap pilih departemen untuk role ini"; return@Button
                        }
                        isLoading = true; errorMessage = null; successMessage = null

                        val newUser = User(
                            name = fullName.split(" ").firstOrNull() ?: fullName,
                            fullName = fullName, email = email, nik = nik,
                            role = selectedRole,
                            roles = listOf("employee", selectedRole).distinct(),
                            primaryRole = selectedRole,
                            departmentId = selectedDept,
                            officeId = selectedOfficeId,
                            isActive = true
                        )

                        scope.launch {
                            val res = repo.createUserByAdmin(context, email, password, newUser)
                            isLoading = false
                            if (res.isSuccess) {
                                successMessage = "Akun berhasil dibuat!"
                                fullName = ""; email = ""; password = ""; nik = ""
                                selectedDept = ""; selectedOfficeId = ""
                            } else {
                                errorMessage = res.exceptionOrNull()?.message ?: "Gagal membuat akun"
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoading
                ) { Text(if (isLoading) "Memproses..." else "Buat Akun") }
            }
        }
        Spacer(Modifier.height(100.dp))
    }
}

// ============== Previews ==============

@Preview(showBackground = true, name = "Role Management")
@Composable
fun RoleManagementPreview() {
    HRISTheme {
        RoleManagementScreen(user = com.ptniger.hris.data.model.User(name = "Admin", role = com.ptniger.hris.utils.Constants.Role.SUPER_ADMIN))
    }
}

@Preview(showBackground = true, name = "Automation Rules")
@Composable
fun AutomationPreview() {
    HRISTheme {
        AutomationScreen(user = com.ptniger.hris.data.model.User(name = "Admin", role = com.ptniger.hris.utils.Constants.Role.SUPER_ADMIN))
    }
}

@Preview(showBackground = true, name = "Account Creation")
@Composable
fun AccountManagementPreview() {
    HRISTheme {
        AccountManagementScreen(user = com.ptniger.hris.data.model.User(name = "Admin", role = com.ptniger.hris.utils.Constants.Role.SUPER_ADMIN))
    }
}




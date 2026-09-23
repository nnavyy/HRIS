package com.ptniger.hris.ui.employee

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ptniger.hris.data.model.Employee
import com.ptniger.hris.data.model.EmployeePresence
import com.ptniger.hris.data.model.User
import com.ptniger.hris.ui.components.PresenceBadge
import com.ptniger.hris.ui.components.PresenceBadgeSize
import com.ptniger.hris.ui.theme.*
import com.ptniger.hris.utils.PresenceResolver

@Composable
fun EmployeeListScreen(
    user: User, 
    onNavigateToForm: (String) -> Unit, 
    onNavigateToDetail: (String) -> Unit = {},
    vm: EmployeeViewModel = viewModel()
) {
    var search by remember { mutableStateOf("") }
    var showAddMemberDialog by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    
    val csvLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let { vm.importFromCsv(context, it) }
    }
    
    var showCsvInstructions by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) { vm.loadAll() }
    val employees by vm.employees.collectAsState()
    
    val isManager = user.primaryRole == com.ptniger.hris.utils.Constants.Role.MANAGER ||
        user.role == com.ptniger.hris.utils.Constants.Role.MANAGER
    
    // Manager hanya melihat tim di departemennya sendiri atau bawahannya langsung
    val baseList = if (isManager) {
        employees.filter { it.managerId == user.userId || (user.departmentId.isNotEmpty() && it.department.equals(user.departmentId, ignoreCase = true)) }
    } else {
        employees
    }
    val filtered = baseList.filter { it.name.contains(search, true) || it.nik.contains(search, true) || it.department.contains(search, true) }

    Box(Modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize().background(Background).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Start) {
            Text(
                if (isManager) "Tim Saya" else "Data Karyawan",
                style = MaterialTheme.typography.headlineMedium
            )
        }

        OutlinedTextField(
            value = search, onValueChange = { search = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
            shape = RoundedCornerShape(18.dp), singleLine = true,
            placeholder = { Text("Cari nama, NIK, departemen") },
            leadingIcon = { Icon(Icons.Default.Search, null) }
        )

        // CSV Import Button (Only for HR or Super Admin)
        if (user.primaryRole == com.ptniger.hris.utils.Constants.Role.SUPER_ADMIN || user.primaryRole == com.ptniger.hris.utils.Constants.Role.HR ||
            user.role == com.ptniger.hris.utils.Constants.Role.SUPER_ADMIN || user.role == com.ptniger.hris.utils.Constants.Role.HR) {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { showCsvInstructions = true },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Blue),
                border = androidx.compose.foundation.BorderStroke(1.dp, Blue)
            ) {
                Icon(Icons.Default.UploadFile, contentDescription = "Import CSV", modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Import CSV", style = MaterialTheme.typography.labelMedium)
            }
        }

        Spacer(Modifier.height(12.dp))

        LazyColumn(contentPadding = PaddingValues(horizontal = 18.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(filtered) { emp ->
                EmployeeCardWithPresence(
                    employee = emp,
                    isManager = isManager,
                    onNavigateToDetail = onNavigateToDetail,
                    onNavigateToForm = onNavigateToForm
                )
            }
            if (filtered.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                        Text(
                            if (isManager) "Belum ada karyawan di departemen Anda" else "Tidak ada karyawan ditemukan",
                            style = MaterialTheme.typography.bodyMedium, color = TextSecondary
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(100.dp)) }
        }
    }
    // FAB — HR add new employee, Manager assign employee
    Box(Modifier.fillMaxSize().padding(end = 18.dp, bottom = 90.dp), contentAlignment = Alignment.BottomEnd) {
        FloatingActionButton(
            onClick = { 
                if (isManager) showAddMemberDialog = true 
                else onNavigateToForm("new") 
            }, 
            containerColor = Blue, 
            contentColor = Surface, 
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(if (isManager) Icons.Default.PersonAdd else Icons.Default.Add, contentDescription = "Tambah", modifier = Modifier.size(24.dp))
        }
    }

    if (showAddMemberDialog) {
        val availableMembers = employees.filter { it.managerId.isEmpty() && it.employeeId != user.employeeId }
        AlertDialog(
            onDismissRequest = { showAddMemberDialog = false },
            title = { Text("Pilih Anggota Tim") },
            text = {
                Column {
                    Text("Pilih karyawan yang belum memiliki manajer untuk ditambahkan ke tim Anda.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(modifier = Modifier.fillMaxHeight(0.6f)) {
                        items(availableMembers) { emp ->
                            Surface(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable {
                                    vm.save(emp.copy(managerId = user.userId), false)
                                    showAddMemberDialog = false
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column {
                                        Text(emp.name, style = MaterialTheme.typography.titleSmall)
                                        Text("${emp.department} · ${emp.position}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                    }
                                }
                            }
                        }
                        if (availableMembers.isEmpty()) {
                            item { 
                                Text("Semua karyawan sudah memiliki manajer atau tidak ada karyawan lain.", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodySmall) 
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddMemberDialog = false }) { Text("Tutup") }
            }
        )
    }

    if (showCsvInstructions) {
        AlertDialog(
            onDismissRequest = { showCsvInstructions = false },
            title = { Text("Instruksi Import CSV") },
            text = {
                Column {
                    Text("Pastikan file CSV Anda memiliki format kolom berikut (pisahkan dengan koma):", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Spacer(Modifier.height(8.dp))
                    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "nik, name, email, phone, position, department, branch, joinDate, baseSalary, leaveQuota",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(12.dp),
                            color = TextPrimary
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Baris pertama akan diabaikan (dianggap sebagai Header).", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            },
            confirmButton = {
                Button(onClick = { 
                    showCsvInstructions = false
                    csvLauncher.launch("text/comma-separated-values") 
                }, colors = ButtonDefaults.buttonColors(containerColor = Blue)) {
                    Text("Pilih File CSV")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCsvInstructions = false }) { Text("Batal") }
            }
        )
    }

    } // Box
} // EmployeeListScreen

/**
 * PRES-05: Employee card dengan presence badge (lazy loaded).
 * Badge di-fetch saat card di-render (scroll ke viewport).
 */
@Composable
fun EmployeeCardWithPresence(
    employee: Employee,
    isManager: Boolean,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToForm: (String) -> Unit
) {
    var presence by remember { mutableStateOf<EmployeePresence?>(null) }

    // Hanya fetch presence saat composable ini benar-benar ditampilkan di layar
    LaunchedEffect(employee.employeeId) {
        kotlinx.coroutines.delay(100)  // small delay untuk hindari fetch semua sekaligus
        presence = PresenceResolver.resolveToday(employee)
    }

    Surface(
        shape = RoundedCornerShape(22.dp), color = Surface, shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth().clickable {
            if (isManager) onNavigateToDetail(employee.employeeId)
            else onNavigateToForm(employee.employeeId)
        }
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            // Avatar
            Box(Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(BlueSoft),
                contentAlignment = Alignment.Center) {
                Text(employee.name.take(2).uppercase(),
                    style = MaterialTheme.typography.titleSmall, color = Blue)
            }
            Spacer(Modifier.width(12.dp))

            // Info utama
            Column(Modifier.weight(1f)) {
                Text(employee.name, style = MaterialTheme.typography.titleSmall)
                Text("${employee.position} · ${employee.department}",
                    style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                // Presence badge dalam satu baris
                Row(
                    Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Presence badge (compact)
                    presence?.let {
                        PresenceBadge(status = it.presenceStatus, size = PresenceBadgeSize.COMPACT)
                    }
                }
            }

            Icon(Icons.Default.ChevronRight, null, tint = TextMuted)
        }
    }
}

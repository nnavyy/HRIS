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
import com.ptniger.hris.data.model.User
import com.ptniger.hris.ui.theme.*

@Composable
fun EmployeeListScreen(user: User, onNavigateToForm: (String) -> Unit, vm: EmployeeViewModel = viewModel()) {
    var search by remember { mutableStateOf("") }
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
        Row(Modifier.fillMaxWidth().padding(start = 18.dp, end = 72.dp, top = 14.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
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

        Spacer(Modifier.height(12.dp))

        LazyColumn(contentPadding = PaddingValues(horizontal = 18.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(filtered) { emp ->
                Surface(shape = RoundedCornerShape(22.dp), color = Surface, shadowElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth().clickable { if (!isManager) onNavigateToForm(emp.employeeId) }) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(44.dp).clip(RoundedCornerShape(16.dp)).background(BlueSoft), contentAlignment = Alignment.Center) {
                            Text(emp.name.take(2).uppercase(), style = MaterialTheme.typography.labelMedium, color = Blue)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(emp.name, style = MaterialTheme.typography.titleSmall)
                            Text("${emp.nik} · ${emp.department} · ${emp.branch}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                        Surface(shape = RoundedCornerShape(999.dp), color = if (emp.employmentStatus == "active") GreenSoft else OrangeSoft) {
                            Text(
                                if (emp.employmentStatus == "active") "Aktif" else "Probasi",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (emp.employmentStatus == "active") Green else Orange
                            )
                        }
                    }
                }
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
    // FAB — hanya tampil untuk HR, bukan Manager
    if (!isManager) {
        Box(Modifier.fillMaxSize().padding(end = 18.dp, bottom = 90.dp), contentAlignment = Alignment.BottomEnd) {
            FloatingActionButton(onClick = { onNavigateToForm("new") }, containerColor = Blue, contentColor = Surface, shape = RoundedCornerShape(16.dp)) {
                Icon(Icons.Default.Add, contentDescription = "Tambah", modifier = Modifier.size(24.dp))
            }
        }
    }
    } // Box
} // EmployeeListScreen


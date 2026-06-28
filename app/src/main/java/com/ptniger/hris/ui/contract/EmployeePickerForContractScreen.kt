package com.ptniger.hris.ui.contract

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.ptniger.hris.data.model.Employee
import com.ptniger.hris.data.model.User
import com.ptniger.hris.data.repository.EmployeeRepository
import com.ptniger.hris.ui.theme.*

@Composable
fun EmployeePickerForContractScreen(
    user: User,
    onEmployeeSelected: (Employee) -> Unit,
    onBack: () -> Unit
) {
    var employees by remember { mutableStateOf<List<Employee>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var search by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        employees = EmployeeRepository().getAll()
        isLoading = false
    }

    val filtered = if (search.isBlank()) employees
    else employees.filter {
        it.name.contains(search, ignoreCase = true) ||
        it.nik.contains(search, ignoreCase = true) ||
        it.position.contains(search, ignoreCase = true)
    }

    Column(Modifier.fillMaxSize().background(Background).statusBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 4.dp, end = 64.dp, top = 14.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Column {
                Text("Buat Kontrak", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Pilih karyawan terlebih dahulu",
                    style = MaterialTheme.typography.bodySmall, color = TextSecondary
                )
            }
        }
        OutlinedTextField(
            value = search, onValueChange = { search = it },
            placeholder = { Text("Cari nama, NIK, atau jabatan...") },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
            shape = RoundedCornerShape(16.dp),
            leadingIcon = { Icon(Icons.Default.Search, null, tint = Blue) },
            singleLine = true
        )
        Spacer(Modifier.height(12.dp))
        if (isLoading) {
            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Blue)
            }
        } else if (filtered.isEmpty()) {
            Text(
                "Tidak ada karyawan ditemukan",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 18.dp)
            )
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filtered) { emp ->
                    Surface(
                        modifier = Modifier.fillMaxWidth().clickable { onEmployeeSelected(emp) },
                        shape = RoundedCornerShape(18.dp), color = Surface, shadowElevation = 1.dp
                    ) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(BlueSoft),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    emp.name.take(2).uppercase(),
                                    style = MaterialTheme.typography.labelLarge, color = Blue
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(emp.name, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "${emp.position} • ${emp.department}",
                                    style = MaterialTheme.typography.bodySmall, color = TextSecondary
                                )
                                Text(
                                    "NIK: ${emp.nik}",
                                    style = MaterialTheme.typography.labelSmall, color = TextMuted
                                )
                            }
                            Icon(Icons.Default.ChevronRight, null, tint = TextMuted)
                        }
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

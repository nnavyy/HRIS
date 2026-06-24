package com.ptniger.hris.ui.audit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.ptniger.hris.data.model.AuditLog
import com.ptniger.hris.data.model.User
import com.ptniger.hris.data.repository.AuditLogRepository
import com.ptniger.hris.ui.theme.*

@Composable
fun AuditLogScreen(user: User, onBack: () -> Unit = {}) {
    val repo = remember { AuditLogRepository() }
    var logs by remember { mutableStateOf<List<AuditLog>>(emptyList()) }
    LaunchedEffect(Unit) { logs = repo.getAll() }

    Column(Modifier.fillMaxSize().background(Background).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(start = 8.dp, end = 72.dp, top = 14.dp, bottom = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Text("Audit Log", style = MaterialTheme.typography.headlineMedium)
            }
            Surface(shape = RoundedCornerShape(999.dp), color = RedSoft) {
                Text("Secure", Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = Red)
            }
        }
        LazyColumn(contentPadding = PaddingValues(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(logs) { log ->
                Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = Surface, shadowElevation = 1.dp) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(40.dp).clip(RoundedCornerShape(14.dp)).background(RedSoft), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Shield, null, tint = Red, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(log.action, style = MaterialTheme.typography.titleSmall)
                            Text("${log.userName} · ${log.targetCollection}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            if (log.details.isNotEmpty()) Text(log.details, style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        }
                    }
                }
            }
            if (logs.isEmpty()) {
                item { Text("Belum ada audit log", Modifier.padding(18.dp), style = MaterialTheme.typography.bodyMedium, color = TextSecondary) }
            }
            item { Spacer(Modifier.height(100.dp)) }
        }
    }
}

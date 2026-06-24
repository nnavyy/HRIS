package com.ptniger.hris.ui.notification

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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ptniger.hris.data.model.Notification
import com.ptniger.hris.data.model.User
import com.ptniger.hris.data.repository.NotificationRepository
import com.ptniger.hris.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun NotificationScreen(user: User, onBack: () -> Unit = {}) {
    val repo = remember { NotificationRepository() }
    var notifications by remember { mutableStateOf<List<Notification>>(emptyList()) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { notifications = repo.getByUser(user.userId) }

    Column(Modifier.fillMaxSize().background(Background).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 64.dp, top = 14.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            Text("Notifikasi", style = MaterialTheme.typography.headlineMedium)
        }
        LazyColumn(contentPadding = PaddingValues(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(notifications) { notif ->
                Surface(
                    Modifier.fillMaxWidth().clickable {
                        scope.launch { repo.markAsRead(notif.notificationId); notifications = repo.getByUser(user.userId) }
                    },
                    shape = RoundedCornerShape(18.dp), color = if (notif.isRead) Surface else BlueSoft, shadowElevation = 1.dp
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(40.dp).clip(RoundedCornerShape(14.dp)).background(if (notif.isRead) Background else Blue.copy(0.1f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Notifications, null, tint = if (notif.isRead) TextSecondary else Blue, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(notif.title, style = MaterialTheme.typography.titleSmall)
                            Text(notif.message, style = MaterialTheme.typography.bodySmall, color = TextSecondary, maxLines = 2)
                        }
                    }
                }
            }
            if (notifications.isEmpty()) {
                item { Text("Tidak ada notifikasi", Modifier.padding(18.dp), style = MaterialTheme.typography.bodyMedium, color = TextSecondary) }
            }
            item { Spacer(Modifier.height(100.dp)) }
        }
    }
}

package com.ptniger.hris.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.ptniger.hris.data.model.User
import com.ptniger.hris.data.repository.AuthRepository
import com.ptniger.hris.ui.theme.*
import com.ptniger.hris.utils.RoleManager
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import coil.compose.AsyncImage
import coil.request.ImageRequest

@Composable
fun ProfileScreen(user: User, onLogout: () -> Unit, vm: ProfileViewModel = viewModel()) {
    val roleName = RoleManager.getRoleDisplayName(user.role)
    val roleShort = RoleManager.getRoleShort(user.role)
    val color = when (user.role) { "hr" -> Blue; "finance" -> Orange; "manager" -> Teal; "super_admin" -> Purple; else -> Pink }
    val bg = when (user.role) { "hr" -> BlueSoft; "finance" -> OrangeSoft; "manager" -> TealSoft; "super_admin" -> PurpleSoft; else -> PinkSoft }

    val isLoading by vm.isLoading.collectAsState()
    val message by vm.message.collectAsState()
    val photoUrl by vm.photoUrl.collectAsState()

    val context = LocalContext.current

    // Auto-dismiss message after 3 seconds
    LaunchedEffect(message) {
        if (message != null) {
            kotlinx.coroutines.delay(3000)
            vm.clearMessage()
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            vm.uploadProfilePicture(user.userId, uri, context)
        }
    }

    Column(Modifier.fillMaxSize().background(Background).statusBarsPadding().verticalScroll(rememberScrollState())) {
        Text("Profil", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(start = 18.dp, end = 64.dp, top = 14.dp, bottom = 10.dp))
        
        if (message != null) {
            Surface(
                Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                shape = RoundedCornerShape(12.dp),
                color = if (message!!.contains("Gagal")) RedSoft else GreenSoft
            ) {
                Text(
                    message!!,
                    Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    color = if (message!!.contains("Gagal")) Red else Green,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(Modifier.height(8.dp))
        }

        Surface(Modifier.fillMaxWidth().padding(horizontal = 18.dp), shape = RoundedCornerShape(24.dp), color = Surface, shadowElevation = 2.dp) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(64.dp).clip(RoundedCornerShape(22.dp)).clickable { galleryLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    val displayPhotoUrl = photoUrl.ifEmpty { user.photoUrl }
                    if (displayPhotoUrl.isNotEmpty()) {
                        // Show actual profile photo from Firebase Storage via Coil
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(displayPhotoUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Foto Profil",
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(22.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        // Fallback to initials avatar
                        Box(
                            Modifier.fillMaxSize().background(bg),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(Modifier.size(24.dp), color = color)
                            } else {
                                Text(roleShort, style = MaterialTheme.typography.titleLarge, color = color)
                            }
                        }
                    }
                    // Upload loading overlay
                    if (isLoading) {
                        Box(
                            Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                        }
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(user.name, style = MaterialTheme.typography.titleMedium)
                    Text(user.email, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Spacer(Modifier.height(6.dp))
                    Surface(shape = RoundedCornerShape(999.dp), color = bg) {
                        Text(roleName, Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = color)
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Text("Tap foto untuk mengubah gambar profil", style = MaterialTheme.typography.labelSmall, color = TextMuted, modifier = Modifier.padding(horizontal = 18.dp))

        Spacer(Modifier.height(16.dp))

        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            InfoBox(Modifier.weight(1f), "Role", roleName)
            InfoBox(Modifier.weight(1f), "Status", if (user.status == "active") "Aktif" else "Nonaktif")
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            InfoBox(Modifier.weight(1f), "Branch", user.branch.ifEmpty { "-" })
            InfoBox(Modifier.weight(1f), "Department", user.departmentId.ifEmpty { "-" })
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = { AuthRepository().logout(); onLogout() },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp).height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Red)
        ) { Text("Logout", color = Color.White) }

        Spacer(Modifier.height(100.dp))
    }
}

@Composable
private fun InfoBox(modifier: Modifier, label: String, value: String) {
    Surface(modifier, shape = RoundedCornerShape(16.dp), color = Background, border = ButtonDefaults.outlinedButtonBorder) {
        Column(Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleSmall)
        }
    }
}



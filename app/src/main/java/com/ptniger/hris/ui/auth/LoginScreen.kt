package com.ptniger.hris.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ptniger.hris.data.model.User
import com.ptniger.hris.ui.theme.*
import com.ptniger.hris.utils.Constants
import com.ptniger.hris.utils.RoleManager
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: LoginViewModel = viewModel(),
    onLoginSuccess: (User) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var selectedRole by remember { mutableStateOf(Constants.Role.HR) }
    var showForgotPassword by remember { mutableStateOf(false) }
    
    // Login history state
    val emailHistory = remember { mutableStateOf(com.ptniger.hris.utils.LoginHistoryManager.getHistory(context)) }
    var expandedHistory by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.loggedInUser) {
        uiState.loggedInUser?.let { 
            com.ptniger.hris.utils.LoginHistoryManager.saveHistory(context, email)
            onLoginSuccess(it) 
        }
    }

    if (showForgotPassword) {
        ForgotPasswordDialog(onDismiss = { showForgotPassword = false })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState())
            .padding(18.dp)
    ) {
        Spacer(Modifier.height(48.dp))

        // Hero
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(32.dp))
                .background(Brush.linearGradient(listOf(BlueDark, Blue, Color(0xFF60A5FA))))
                .padding(20.dp)
        ) {
            Column {
                Text("Mobile HRIS", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(0.85f))
                Spacer(Modifier.height(6.dp))
                Text("Login sesuai\nrole organisasi", style = MaterialTheme.typography.headlineLarge, color = Color.White)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Satu aplikasi HRIS dengan dashboard berbeda untuk HR, Finance, Manager, Super Admin, dan Karyawan.",
                    style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.88f)
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Role selector
        val roles = listOf(
            Constants.Role.HR to "HR / Admin",
            Constants.Role.FINANCE to "Finance",
            Constants.Role.MANAGER to "Manager",
            Constants.Role.SUPER_ADMIN to "Super Admin",
            Constants.Role.EMPLOYEE to "Karyawan"
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            for (row in roles.chunked(2)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    row.forEach { (roleKey, roleName) ->
                        val isSelected = selectedRole == roleKey
                        Surface(
                            modifier = Modifier.weight(1f).clickable { selectedRole = roleKey },
                            shape = RoundedCornerShape(18.dp),
                            color = if (isSelected) BlueSoft else Surface,
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = Brush.linearGradient(
                                    if (isSelected) listOf(Blue, Blue) else listOf(CardBorder, CardBorder)
                                )
                            )
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text(roleName, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    RoleManager.getNavItems(roleKey).joinToString(", ") { it.label },
                                    style = MaterialTheme.typography.labelSmall, color = TextSecondary, maxLines = 1
                                )
                            }
                        }
                    }
                    if (row.size < 2) Spacer(Modifier.weight(1f))
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Login form
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Surface,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text("Login ${RoleManager.getRoleDisplayName(selectedRole)}", style = MaterialTheme.typography.titleMedium)
                    }
                    Surface(shape = RoundedCornerShape(999.dp), color = BlueSoft) {
                        Text(
                            RoleManager.getRoleShort(selectedRole),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium, color = Blue
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                Text("Email / NIK", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                Spacer(Modifier.height(6.dp))
                ExposedDropdownMenuBox(
                    expanded = expandedHistory && email.isEmpty(),
                    onExpandedChange = { if (email.isEmpty()) expandedHistory = it else expandedHistory = false }
                ) {
                    OutlinedTextField(
                        value = email, onValueChange = { 
                            email = it.trim()
                            if (email.isNotEmpty()) expandedHistory = false
                        },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        placeholder = { Text("email@company.co.id") }
                    )
                    
                    if (emailHistory.value.isNotEmpty()) {
                        ExposedDropdownMenu(
                            expanded = expandedHistory,
                            onDismissRequest = { expandedHistory = false }
                        ) {
                            emailHistory.value.forEach { historyEmail ->
                                DropdownMenuItem(
                                    text = { Text(historyEmail) },
                                    onClick = {
                                        email = historyEmail
                                        expandedHistory = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                Text("Password", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = password, onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null
                            )
                        }
                    }
                )

                if (uiState.error != null) {
                    Spacer(Modifier.height(8.dp))
                    val errorMsg = when {
                        uiState.error!!.contains("credential", ignoreCase = true) ||
                        uiState.error!!.contains("malformed", ignoreCase = true) ||
                        uiState.error!!.contains("invalid", ignoreCase = true) ->
                            "Email atau password salah. Pastikan email dan password yang dimasukkan benar."
                        uiState.error!!.contains("network", ignoreCase = true) ||
                        uiState.error!!.contains("connection", ignoreCase = true) ->
                            "Tidak ada koneksi internet. Periksa jaringan kamu."
                        uiState.error!!.contains("too-many-requests", ignoreCase = true) ||
                        uiState.error!!.contains("blocked", ignoreCase = true) ->
                            "Terlalu banyak percobaan login. Coba lagi beberapa menit."
                        else -> uiState.error!!
                    }
                    Text(errorMsg, color = Red, style = MaterialTheme.typography.bodySmall)
                }

                // Forgot password link
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = { showForgotPassword = true },
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            "Lupa Password?",
                            style = MaterialTheme.typography.bodySmall,
                            color = Blue
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = { viewModel.login(email.trim(), password, selectedRole) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Blue),
                    enabled = !uiState.isLoading && email.isNotBlank() && password.isNotBlank()
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Masuk sebagai ${RoleManager.getRoleDisplayName(selectedRole)}", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(Modifier.height(80.dp))
    }
}

package com.ptniger.hris.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.ptniger.hris.ui.theme.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun ForgotPasswordDialog(onDismiss: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var successMsg by remember { mutableStateOf<String?>(null) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        containerColor = Surface,
        shape = RoundedCornerShape(24.dp),
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .padding(bottom = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(shape = RoundedCornerShape(16.dp), color = BlueSoft, modifier = Modifier.size(52.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Email, contentDescription = null, tint = Blue, modifier = Modifier.size(26.dp))
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("Lupa Password", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Masukkan email kamu. Kami akan kirimkan link reset password ke email tersebut.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (successMsg != null) {
                    Surface(shape = RoundedCornerShape(12.dp), color = GreenSoft, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            successMsg!!,
                            Modifier.padding(12.dp),
                            color = Green,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                } else {
                    Text("Email", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it.trim(); errorMsg = null },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        placeholder = { Text("email@company.co.id") },
                        enabled = !isLoading
                    )
                    if (errorMsg != null) {
                        Text(errorMsg!!, color = Red, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            if (successMsg != null) {
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Blue),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Tutup") }
            } else {
                Button(
                    onClick = {
                        if (email.isBlank()) {
                            errorMsg = "Masukkan email terlebih dahulu"
                            return@Button
                        }
                        isLoading = true
                        scope.launch {
                            try {
                                FirebaseAuth.getInstance()
                                    .sendPasswordResetEmail(email.trim())
                                    .await()
                                successMsg = "✅ Link reset password telah dikirim ke $email.\nSilakan cek inbox atau folder spam kamu."
                                errorMsg = null
                            } catch (e: Exception) {
                                errorMsg = when {
                                    e.message?.contains("user-not-found") == true ||
                                    e.message?.contains("no user") == true ->
                                        "Email tidak terdaftar. Periksa kembali alamat email kamu."
                                    e.message?.contains("invalid-email") == true ||
                                    e.message?.contains("malformed") == true ->
                                        "Format email tidak valid."
                                    e.message?.contains("network") == true ->
                                        "Tidak ada koneksi internet."
                                    else -> "Gagal mengirim email reset. Coba lagi nanti."
                                }
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    enabled = !isLoading && email.isNotBlank(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Blue),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Kirim Link Reset")
                    }
                }
            }
        },
        dismissButton = {
            if (successMsg == null) {
                TextButton(onClick = { if (!isLoading) onDismiss() }) {
                    Text("Batal", color = TextSecondary)
                }
            }
        }
    )
}

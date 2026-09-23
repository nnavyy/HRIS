package com.ptniger.hris.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore
import com.ptniger.hris.data.model.User
import com.ptniger.hris.ui.theme.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Onboarding Wizard — ditampilkan saat user pertama kali login
 * dan isProfileComplete == false.
 *
 * 3 Step:
 * 1. Verifikasi Profil (nama lengkap, nomor HP)
 * 2. Data Rekening (opsional)
 * 3. Selesai / Registrasi Wajah (opsional)
 */
@Composable
fun OnboardingScreen(
    user: User,
    onComplete: () -> Unit,
    onNavigateToFaceRegistration: () -> Unit = {}
) {
    var currentStep by remember { mutableStateOf(0) }
    var fullName by remember { mutableStateOf(user.fullName.ifEmpty { user.name }) }
    var phone by remember { mutableStateOf(user.phone ?: "") }
    var bankName by remember { mutableStateOf("") }
    var bankAccount by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        Modifier.fillMaxSize().background(Background).statusBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(24.dp))

        // Header
        Column(Modifier.padding(horizontal = 24.dp)) {
            Text("Selamat Datang! 👋", style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text("Lengkapi profil Anda untuk mulai menggunakan HRIS",
                style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }

        Spacer(Modifier.height(20.dp))

        // Step indicator
        Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (i in 0..2) {
                Box(
                    Modifier.weight(1f).height(4.dp)
                        .background(
                            if (i <= currentStep) Blue else CardBorder,
                            RoundedCornerShape(999.dp)
                        )
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // Step content
        Surface(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp),
            shape = RoundedCornerShape(24.dp), color = Surface, shadowElevation = 1.dp
        ) {
            Column(Modifier.padding(20.dp)) {
                when (currentStep) {
                    0 -> StepProfileVerification(
                        fullName = fullName, onFullNameChange = { fullName = it },
                        phone = phone, onPhoneChange = { phone = it }
                    )
                    1 -> StepBankAccount(
                        bankName = bankName, onBankNameChange = { bankName = it },
                        bankAccount = bankAccount, onBankAccountChange = { bankAccount = it }
                    )
                    2 -> StepComplete()
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Navigation buttons
        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (currentStep > 0) {
                OutlinedButton(
                    onClick = { currentStep-- },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp)
                ) { Text("Kembali") }
            }

            Button(
                onClick = {
                    when (currentStep) {
                        0 -> {
                            if (fullName.isNotBlank() && phone.isNotBlank()) {
                                currentStep = 1
                            }
                        }
                        1 -> currentStep = 2
                        2 -> {
                            isSubmitting = true
                            scope.launch {
                                saveOnboarding(user.userId, fullName, phone, bankName, bankAccount)
                                isSubmitting = false
                                onComplete()
                            }
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Blue),
                enabled = !isSubmitting && when (currentStep) {
                    0 -> fullName.isNotBlank() && phone.isNotBlank()
                    else -> true
                }
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(Modifier.size(18.dp), color = androidx.compose.ui.graphics.Color.White, strokeWidth = 2.dp)
                } else {
                    Text(if (currentStep == 2) "Selesai & Masuk" else "Lanjut")
                }
            }
        }

        // Skip face registration link di step terakhir
        if (currentStep == 2) {
            Spacer(Modifier.height(12.dp))
            TextButton(
                onClick = {
                    scope.launch {
                        saveOnboarding(user.userId, fullName, phone, bankName, bankAccount)
                        onNavigateToFaceRegistration()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Face, null, tint = Teal, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Registrasi Wajah Sekarang", color = Teal)
            }
        }

        Spacer(Modifier.height(100.dp))
    }
}

@Composable
private fun StepProfileVerification(
    fullName: String, onFullNameChange: (String) -> Unit,
    phone: String, onPhoneChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.Person, null, tint = Blue, modifier = Modifier.size(24.dp))
            Text("Langkah 1: Verifikasi Profil", style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold)
        }
        Text("Pastikan data berikut sudah benar sebelum melanjutkan.",
            style = MaterialTheme.typography.bodySmall, color = TextSecondary)

        OutlinedTextField(
            value = fullName, onValueChange = onFullNameChange,
            label = { Text("Nama Lengkap") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        )
        OutlinedTextField(
            value = phone, onValueChange = onPhoneChange,
            label = { Text("Nomor HP (WhatsApp)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone
            )
        )
    }
}

@Composable
private fun StepBankAccount(
    bankName: String, onBankNameChange: (String) -> Unit,
    bankAccount: String, onBankAccountChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.Payments, null, tint = Blue, modifier = Modifier.size(24.dp))
            Text("Langkah 2: Data Rekening", style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold)
        }
        Text("Opsional — untuk pembayaran gaji. Bisa dilengkapi nanti di profil.",
            style = MaterialTheme.typography.bodySmall, color = TextSecondary)

        OutlinedTextField(
            value = bankName, onValueChange = onBankNameChange,
            label = { Text("Nama Bank") },
            placeholder = { Text("BCA, BRI, Mandiri...") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        )
        OutlinedTextField(
            value = bankAccount, onValueChange = onBankAccountChange,
            label = { Text("Nomor Rekening") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
            )
        )
    }
}

@Composable
private fun StepComplete() {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(Icons.Default.CheckCircle, null, tint = Green, modifier = Modifier.size(56.dp))
        Text("Profil Siap!", style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold)
        Text("Data profil Anda sudah lengkap. Anda bisa mulai menggunakan aplikasi HRIS.\n\nAnda juga bisa melakukan registrasi wajah untuk fitur absensi biometrik.",
            style = MaterialTheme.typography.bodySmall, color = TextSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

private suspend fun saveOnboarding(
    userId: String, fullName: String, phone: String,
    bankName: String, bankAccount: String
) {
    try {
        val db = FirebaseFirestore.getInstance()
        val updates = mutableMapOf<String, Any>(
            "fullName" to fullName,
            "phone" to phone,
            "isProfileComplete" to true
        )
        if (bankName.isNotBlank()) updates["bankName"] = bankName
        if (bankAccount.isNotBlank()) updates["bankAccount"] = bankAccount

        db.collection("users").document(userId).update(updates).await()
    } catch (_: Exception) {}
}
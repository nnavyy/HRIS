package com.ptniger.hris.ui.superadmin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ptniger.hris.data.model.AppConfig
import com.ptniger.hris.data.repository.AppConfigRepository
import com.ptniger.hris.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AppConfigViewModel : ViewModel() {
    private val repo = AppConfigRepository()
    private val _configs = MutableStateFlow<List<AppConfig>>(emptyList())
    val configs: StateFlow<List<AppConfig>> = _configs
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    fun loadConfigs() {
        viewModelScope.launch { _configs.value = repo.getAllConfigs() }
    }

    fun updateConfig(config: AppConfig, newValue: String) {
        viewModelScope.launch {
            val updated = config.copy(value = newValue)
            repo.saveConfig(updated).fold(
                onSuccess = { _message.value = "Konfigurasi berhasil disimpan"; loadConfigs() },
                onFailure = { _message.value = "Gagal menyimpan: ${it.message}" }
            )
        }
    }
}

@Composable
fun AppConfigScreen(onBack: () -> Unit, vm: AppConfigViewModel = viewModel()) {
    val configs by vm.configs.collectAsState()
    val message by vm.message.collectAsState()
    LaunchedEffect(Unit) { vm.loadConfigs() }

    Column(Modifier.fillMaxSize().background(Background).statusBarsPadding().verticalScroll(rememberScrollState())) {
        Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 64.dp, top = 14.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            Text("Pengaturan Sistem", style = MaterialTheme.typography.headlineMedium)
        }

        if (message != null) {
            Surface(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp), shape = RoundedCornerShape(12.dp), color = GreenSoft) {
                Text(message!!, Modifier.padding(12.dp), color = Green, style = MaterialTheme.typography.bodyMedium)
            }
        }

        if (configs.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(32.dp).height(400.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(64.dp), tint = TextSecondary.copy(alpha = 0.5f))
                    Spacer(Modifier.height(16.dp))
                    Text("Konfigurasi Kosong", style = MaterialTheme.typography.titleMedium, color = TextSecondary)
                    Spacer(Modifier.height(8.dp))
                    Text("Belum ada konfigurasi sistem yang diatur. Tekan tombol Seed Data dari halaman Home untuk memuat pengaturan standar, atau tambahkan konfigurasi manual (saat ini hanya mendukung seed).", 
                        style = MaterialTheme.typography.bodySmall, color = TextSecondary, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
        } else {
            configs.forEach { config ->
                AppConfigItem(config = config, onSave = { newValue -> vm.updateConfig(config, newValue) })
            }
        }
        Spacer(Modifier.height(100.dp))
    }
}

@Composable
fun AppConfigItem(config: AppConfig, onSave: (String) -> Unit) {
    var value by remember(config.value) { mutableStateOf(config.value) }
    var passwordVisible by remember { mutableStateOf(!config.isSecret) }

    Surface(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp), shape = RoundedCornerShape(24.dp), color = Surface, shadowElevation = 2.dp) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(config.key, style = MaterialTheme.typography.titleMedium)
            Text(config.description, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    if (config.isSecret) {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null)
                        }
                    }
                }
            )
            
            Button(
                onClick = { onSave(value) },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Blue)
            ) {
                Text("Simpan Perubahan")
            }
        }
    }
}

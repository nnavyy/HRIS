package com.ptniger.hris.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ptniger.hris.data.model.OfficeLocation
import com.ptniger.hris.data.model.User
import com.ptniger.hris.data.repository.OfficeLocationRepository
import com.ptniger.hris.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class OfficeLocationViewModel : ViewModel() {
    private val repo = OfficeLocationRepository()
    private val _locations = MutableStateFlow<List<OfficeLocation>>(emptyList())
    val locations: StateFlow<List<OfficeLocation>> = _locations
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    fun loadAll() {
        viewModelScope.launch {
            _locations.value = repo.getAll()
        }
    }

    fun addLocation(name: String, lat: Double, lng: Double, radius: Double) {
        viewModelScope.launch {
            val loc = OfficeLocation(
                name = name,
                latitude = lat,
                longitude = lng,
                allowedRadiusMeters = radius
            )
            repo.add(loc).fold(
                onSuccess = {
                    _message.value = "Lokasi kantor berhasil ditambahkan"
                    loadAll()
                },
                onFailure = {
                    _message.value = "Gagal: ${it.message}"
                }
            )
        }
    }

    fun clearMessage() { _message.value = null }
}

@Composable
fun OfficeLocationScreen(user: User, onBack: () -> Unit, vm: OfficeLocationViewModel = viewModel()) {
    LaunchedEffect(Unit) { vm.loadAll() }
    val locations by vm.locations.collectAsState()
    val message by vm.message.collectAsState()

    var showDialog by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var latitude by remember { mutableStateOf("") }
    var longitude by remember { mutableStateOf("") }
    var radius by remember { mutableStateOf("100.0") } // Default 100 meters

    Column(Modifier.fillMaxSize().background(Background).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(start = 18.dp, end = 64.dp, top = 14.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Master Lokasi Kantor", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
            Button(
                onClick = { showDialog = true },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Blue)
            ) {
                Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Tambah")
            }
        }
        
        if (message != null) {
            Text(message!!, modifier = Modifier.padding(horizontal = 18.dp), color = Green, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
        }

        LazyColumn(contentPadding = PaddingValues(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(locations) { loc ->
                Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = Surface, shadowElevation = 1.dp) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(if (loc.isActive) GreenSoft else RedSoft), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.LocationCity, null, tint = if (loc.isActive) Green else Red, modifier = Modifier.size(24.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(loc.name, style = MaterialTheme.typography.titleMedium)
                            Text("Lat: ${loc.latitude}, Lng: ${loc.longitude}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            Text("Radius: ${loc.allowedRadiusMeters}m", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                        Surface(shape = RoundedCornerShape(999.dp), color = if (loc.isActive) GreenSoft else RedSoft) {
                            Text(if (loc.isActive) "AKTIF" else "NONAKTIF", Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = if (loc.isActive) Green else Red)
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(100.dp)) }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Tambah Lokasi Kantor") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nama Kantor") }, singleLine = true)
                    Text("Pilih Lokasi di Peta (Tap & Tahan)", style = MaterialTheme.typography.labelMedium)
                    com.ptniger.hris.ui.components.OsmMapView(
                        modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(8.dp)),
                        initialLat = latitude.toDoubleOrNull() ?: -6.2088, // Default Jakarta
                        initialLng = longitude.toDoubleOrNull() ?: 106.8456,
                        onLocationSelected = { lat, lng ->
                            latitude = lat.toString()
                            longitude = lng.toString()
                        }
                    )
                    Text("Lat: ${latitude.take(8)}, Lng: ${longitude.take(9)}", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(value = radius, onValueChange = { radius = it }, label = { Text("Radius (meter)") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(onClick = {
                    vm.addLocation(name, latitude.toDoubleOrNull() ?: 0.0, longitude.toDoubleOrNull() ?: 0.0, radius.toDoubleOrNull() ?: 100.0)
                    showDialog = false
                    name = ""
                    latitude = ""
                    longitude = ""
                    radius = "100.0"
                }) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Batal") }
            }
        )
    }
}

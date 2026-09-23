package com.ptniger.hris.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import android.location.Geocoder
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

    fun addLocation(name: String, lat: Double, lng: Double, radius: Double,
                    companyName: String = "", companyAddress: String = "",
                    companyPhone: String = "", companyEmail: String = "", companyNpwp: String = "",
                    timeZone: String = "Asia/Jakarta", operationalHours: String = "08:00 - 17:00") {
        viewModelScope.launch {
            val loc = OfficeLocation(
                name = name,
                latitude = lat,
                longitude = lng,
                allowedRadiusMeters = radius,
                companyName = companyName,
                companyAddress = companyAddress,
                companyPhone = companyPhone,
                companyEmail = companyEmail,
                companyNpwp = companyNpwp,
                timeZone = timeZone,
                operationalHours = operationalHours
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

    fun updateLocation(id: String, name: String, lat: Double, lng: Double, radius: Double, isActive: Boolean,
                       companyName: String = "", companyAddress: String = "",
                       companyPhone: String = "", companyEmail: String = "", companyNpwp: String = "",
                       timeZone: String = "Asia/Jakarta", operationalHours: String = "08:00 - 17:00") {
        viewModelScope.launch {
            val loc = OfficeLocation(
                id = id,
                name = name,
                latitude = lat,
                longitude = lng,
                allowedRadiusMeters = radius,
                isActive = isActive,
                companyName = companyName,
                companyAddress = companyAddress,
                companyPhone = companyPhone,
                companyEmail = companyEmail,
                companyNpwp = companyNpwp,
                timeZone = timeZone,
                operationalHours = operationalHours
            )
            repo.update(id, loc).fold(
                onSuccess = {
                    _message.value = "Lokasi kantor berhasil diperbarui"
                    loadAll()
                },
                onFailure = {
                    _message.value = "Gagal: ${it.message}"
                }
            )
        }
    }

    fun deleteLocation(id: String) {
        viewModelScope.launch {
            repo.delete(id).fold(
                onSuccess = {
                    _message.value = "Lokasi kantor berhasil dihapus"
                    loadAll()
                },
                onFailure = {
                    _message.value = "Gagal: ${it.message}"
                }
            )
        }
    }

    fun toggleStatus(id: String, isActive: Boolean) {
        viewModelScope.launch {
            repo.toggleStatus(id, isActive).fold(
                onSuccess = { loadAll() },
                onFailure = { _message.value = "Gagal: ${it.message}" }
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
    var editingLocation by remember { mutableStateOf<OfficeLocation?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<String?>(null) }

    // Form fields
    var name by remember { mutableStateOf("") }
    var latitude by remember { mutableStateOf("") }
    var longitude by remember { mutableStateOf("") }
    var radius by remember { mutableStateOf("100.0") }
    var searchQuery by remember { mutableStateOf("") }
    var searchError by remember { mutableStateOf<String?>(null) }
    var companyName by remember { mutableStateOf("") }
    var companyAddress by remember { mutableStateOf("") }
    var companyPhone by remember { mutableStateOf("") }
    var companyEmail by remember { mutableStateOf("") }
    var companyNpwp by remember { mutableStateOf("") }
    var selectedTimeZone by remember { mutableStateOf("Asia/Jakarta") }
    var operationalHours by remember { mutableStateOf("08:00 - 17:00") }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    Box(Modifier.fillMaxSize().background(Background).statusBarsPadding()) {
        Column(Modifier.fillMaxSize()) {
            // Header with Back Button only
            Row(
                Modifier.fillMaxWidth().padding(start = 4.dp, end = 64.dp, top = 14.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text("Master Lokasi Kantor", style = MaterialTheme.typography.headlineMedium)
            }
            
            if (message != null) {
                Text(message!!, modifier = Modifier.padding(horizontal = 18.dp), color = Green, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
            }

            LazyColumn(contentPadding = PaddingValues(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(locations) { loc ->
                    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = Surface, shadowElevation = 1.dp) {
                        Column(Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(if (loc.isActive) GreenSoft else RedSoft), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.LocationCity, null, tint = if (loc.isActive) Green else Red, modifier = Modifier.size(24.dp))
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(loc.name, style = MaterialTheme.typography.titleMedium)
                                    if (loc.companyName.isNotEmpty()) {
                                        Text(loc.companyName, style = MaterialTheme.typography.bodySmall, color = Blue)
                                    }
                                    val tzName = when (loc.timeZone) {
                                        "Asia/Makassar", "WITA" -> "WITA (UTC+8)"
                                        "Asia/Jayapura", "WIT" -> "WIT (UTC+9)"
                                        else -> "WIB (UTC+7)"
                                    }
                                    Text("Zona Waktu: $tzName • Jam: ${loc.operationalHours.ifEmpty { "08:00 - 17:00" }}", style = MaterialTheme.typography.bodySmall, color = Blue)
                                    Text("Lat: ${loc.latitude}, Lng: ${loc.longitude} • Radius: ${loc.allowedRadiusMeters}m", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                }
                                Surface(shape = RoundedCornerShape(999.dp), color = if (loc.isActive) GreenSoft else RedSoft) {
                                    Text(if (loc.isActive) "AKTIF" else "NONAKTIF", Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = if (loc.isActive) Green else Red)
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            // Action buttons
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        editingLocation = loc
                                        name = loc.name
                                        latitude = loc.latitude.toString()
                                        longitude = loc.longitude.toString()
                                        radius = loc.allowedRadiusMeters.toString()
                                        companyName = loc.companyName
                                        companyAddress = loc.companyAddress
                                        companyPhone = loc.companyPhone
                                        companyEmail = loc.companyEmail
                                        companyNpwp = loc.companyNpwp
                                        selectedTimeZone = loc.timeZone.ifEmpty { "Asia/Jakarta" }
                                        operationalHours = loc.operationalHours.ifEmpty { "08:00 - 17:00" }
                                        searchQuery = ""; searchError = null
                                        showDialog = true
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Edit, null, Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Edit")
                                }
                                OutlinedButton(
                                    onClick = { vm.toggleStatus(loc.id, !loc.isActive) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(if (loc.isActive) "Nonaktifkan" else "Aktifkan")
                                }
                                OutlinedButton(
                                    onClick = { showDeleteConfirm = loc.id },
                                    modifier = Modifier.weight(0.6f),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Red)
                                ) {
                                    Icon(Icons.Default.Delete, null, Modifier.size(16.dp), tint = Red)
                                }
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(100.dp)) }
            }
        }

        // FAB at bottom-right
        FloatingActionButton(
            onClick = {
                editingLocation = null
                name = ""; latitude = ""; longitude = ""; radius = "100.0"; searchQuery = ""; searchError = null
                companyName = ""; companyAddress = ""; companyPhone = ""; companyEmail = ""; companyNpwp = ""
                showDialog = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = 90.dp),
            containerColor = Blue,
            contentColor = Surface,
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Tambah Lokasi", modifier = Modifier.size(28.dp))
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirm != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("Hapus Lokasi Kantor?") },
            text = { Text("Lokasi kantor ini akan dihapus secara permanen. Lanjutkan?") },
            confirmButton = {
                Button(
                    onClick = {
                        vm.deleteLocation(showDeleteConfirm!!)
                        showDeleteConfirm = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Red)
                ) { Text("Hapus") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) { Text("Batal") }
            }
        )
    }

    // Add/Edit Dialog
    if (showDialog) {
        val isEditing = editingLocation != null
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if (isEditing) "Edit Lokasi Kantor" else "Tambah Lokasi Kantor") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text("Profil Perusahaan", style = MaterialTheme.typography.labelMedium, color = Blue)
                    OutlinedTextField(value = companyName, onValueChange = { companyName = it }, label = { Text("Nama Perusahaan (PT)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = companyAddress, onValueChange = { companyAddress = it }, label = { Text("Alamat Perusahaan") }, singleLine = false, maxLines = 2, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = companyPhone, onValueChange = { companyPhone = it }, label = { Text("Telepon") }, singleLine = true, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                        OutlinedTextField(value = companyEmail, onValueChange = { companyEmail = it }, label = { Text("Email") }, singleLine = true, modifier = Modifier.weight(1f))
                    }
                    OutlinedTextField(value = companyNpwp, onValueChange = { companyNpwp = it }, label = { Text("NPWP Perusahaan") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                    HorizontalDivider(color = CardBorder)
                    Text("Pengaturan Waktu & Jam Kerja Kantor", style = MaterialTheme.typography.labelMedium, color = Blue)
                    
                    Text("Zona Waktu Resmi Kantor", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val tzList = listOf("Asia/Jakarta" to "WIB (UTC+7)", "Asia/Makassar" to "WITA (UTC+8)", "Asia/Jayapura" to "WIT (UTC+9)")
                        tzList.forEach { (tzKey, tzLabel) ->
                            val isSel = selectedTimeZone == tzKey
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSel) BlueSoft else CardBorder.copy(alpha = 0.2f),
                                border = if (isSel) ButtonDefaults.outlinedButtonBorder else null,
                                modifier = Modifier.weight(1f),
                                onClick = { selectedTimeZone = tzKey }
                            ) {
                                Text(
                                    tzLabel,
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSel) Blue else TextSecondary,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                    
                    OutlinedTextField(
                        value = operationalHours,
                        onValueChange = { operationalHours = it },
                        label = { Text("Jam Operasional (Cth: 08:00 - 17:00)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    HorizontalDivider(color = CardBorder)
                    Text("Informasi Lokasi & Geofence", style = MaterialTheme.typography.labelMedium, color = Blue)
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nama Kantor / Cabang") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Text("Cari Lokasi / Pilih di Peta (Tap & Tahan)", style = MaterialTheme.typography.labelMedium)
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            label = { Text("Cari (Cth: Jakarta)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = {
                                if (searchQuery.isNotBlank()) {
                                    coroutineScope.launch {
                                        try {
                                            val geocoder = Geocoder(context)
                                            val results = withContext(Dispatchers.IO) { geocoder.getFromLocationName(searchQuery, 1) }
                                            if (results != null && results.isNotEmpty()) {
                                                val loc = results[0]
                                                latitude = loc.latitude.toString()
                                                longitude = loc.longitude.toString()
                                                searchError = null
                                                loc.getAddressLine(0)?.let { companyAddress = it }
                                            } else {
                                                searchError = "Lokasi tidak ditemukan"
                                            }
                                        } catch (e: Exception) {
                                            searchError = "Gagal mencari lokasi"
                                        }
                                    }
                                }
                            })
                        )
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = {
                            if (searchQuery.isNotBlank()) {
                                coroutineScope.launch {
                                    try {
                                        val geocoder = Geocoder(context)
                                        val results = withContext(Dispatchers.IO) { geocoder.getFromLocationName(searchQuery, 1) }
                                        if (results != null && results.isNotEmpty()) {
                                            val loc = results[0]
                                            latitude = loc.latitude.toString()
                                            longitude = loc.longitude.toString()
                                            searchError = null
                                            loc.getAddressLine(0)?.let { companyAddress = it }
                                        } else {
                                            searchError = "Tidak ditemukan"
                                        }
                                    } catch (e: Exception) {
                                        searchError = "Error"
                                    }
                                }
                            }
                        }) {
                            Text("Cari")
                        }
                    }
                    if (searchError != null) {
                        Text(searchError!!, color = Red, style = MaterialTheme.typography.bodySmall)
                    }

                    com.ptniger.hris.ui.components.OsmMapView(
                        modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(8.dp)),
                        targetLat = latitude.toDoubleOrNull() ?: -6.2088,
                        targetLng = longitude.toDoubleOrNull() ?: 106.8456,
                        onLocationSelected = { lat, lng ->
                            latitude = lat.toString()
                            longitude = lng.toString()
                            coroutineScope.launch {
                                try {
                                    val geocoder = Geocoder(context)
                                    val results = withContext(Dispatchers.IO) { geocoder.getFromLocation(lat, lng, 1) }
                                    if (!results.isNullOrEmpty()) {
                                        results[0].getAddressLine(0)?.let { companyAddress = it }
                                    }
                                } catch (e: Exception) {
                                    // Silently fail if reverse geocoding is unavailable
                                }
                            }
                        }
                    )
                    Text("Lat: ${latitude.take(12)}, Lng: ${longitude.take(13)}", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(value = radius, onValueChange = { radius = it }, label = { Text("Radius (meter)") }, singleLine = true, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                }
            },
            confirmButton = {
                Button(onClick = {
                    val lat = latitude.toDoubleOrNull() ?: 0.0
                    val lng = longitude.toDoubleOrNull() ?: 0.0
                    val rad = radius.toDoubleOrNull() ?: 100.0
                    if (isEditing) {
                        vm.updateLocation(editingLocation!!.id, name, lat, lng, rad, editingLocation!!.isActive,
                            companyName, companyAddress, companyPhone, companyEmail, companyNpwp,
                            selectedTimeZone, operationalHours)
                    } else {
                        vm.addLocation(name, lat, lng, rad, companyName, companyAddress, companyPhone, companyEmail, companyNpwp,
                            selectedTimeZone, operationalHours)
                    }
                    showDialog = false
                    name = ""; latitude = ""; longitude = ""; radius = "100.0"
                    companyName = ""; companyAddress = ""; companyPhone = ""; companyEmail = ""; companyNpwp = ""
                    selectedTimeZone = "Asia/Jakarta"; operationalHours = "08:00 - 17:00"
                    editingLocation = null
                }) {
                    Text(if (isEditing) "Perbarui" else "Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false; editingLocation = null }) { Text("Batal") }
            }
        )
    }
}

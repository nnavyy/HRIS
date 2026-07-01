package com.ptniger.hris.ui.attendance

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ptniger.hris.data.model.User
import com.ptniger.hris.ui.theme.*
import com.ptniger.hris.utils.DateUtils
import com.ptniger.hris.utils.Constants
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun AttendanceScreen(user: User, vm: AttendanceViewModel = viewModel()) {
    val empId = user.employeeId.ifEmpty { user.userId }
    LaunchedEffect(Unit) { vm.loadTodayAttendance(empId, user.email) }
    val state by vm.state.collectAsState()

    // Camera & Location Setup
    val context = LocalContext.current
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var locationCoords by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var currentClockType by remember { mutableStateOf("") }
    var isMockDetected by remember { mutableStateOf(false) }
    var showFaceAttendance by remember { mutableStateOf(false) }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    
    // Use resolvedEmployeeId from state if available (resolved via email/userId fallback)
    val resolvedEmpId = if (state.resolvedEmployeeId.isNotEmpty()) state.resolvedEmployeeId else empId

    val coroutineScope = rememberCoroutineScope()
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && imageUri != null && locationCoords != null) {
            coroutineScope.launch {
                try {
                    val bitmap = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                        val source = android.graphics.ImageDecoder.createSource(context.contentResolver, imageUri!!)
                        android.graphics.ImageDecoder.decodeBitmap(source)
                    } else {
                        @Suppress("DEPRECATION")
                        android.provider.MediaStore.Images.Media.getBitmap(context.contentResolver, imageUri)
                    }
                    val softwareBitmap = bitmap.copy(android.graphics.Bitmap.Config.ARGB_8888, true)
                    
                    com.ptniger.hris.utils.FaceRecognitionManager.detectFacesSync(softwareBitmap) { faces ->
                        if (faces.isNotEmpty()) {
                            vm.submitAttendance(resolvedEmpId, imageUri!!, locationCoords!!.first, locationCoords!!.second, currentClockType, context, isMockDetected, user.email)
                        } else {
                            android.widget.Toast.makeText(context, "Wajah tidak terdeteksi pada foto. Absensi dibatalkan.", android.widget.Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    android.widget.Toast.makeText(context, "Gagal memproses foto: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                }
            }
        } else {
            vm.clearMessage()
        }
    }

    // Helper: buat file foto di selfies/ agar sesuai file_paths.xml FileProvider config
    fun createPhotoFile(ctx: Context): File {
        val selfiesDir = File(ctx.cacheDir, "selfies").also { it.mkdirs() }
        return File(selfiesDir, "attendance_${System.currentTimeMillis()}.jpg")
    }

    // Helper: launch kamera setelah lokasi didapat
    fun launchCameraWithLocation() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
            .setMaxUpdates(1)
            .build()
        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    val loc = locationResult.lastLocation
                    if (loc != null) {
                        // Anti Fake GPS Detection
                        isMockDetected = com.ptniger.hris.utils.LocationUtils.isMockLocation(loc)
                        
                        locationCoords = Pair(loc.latitude, loc.longitude)
                        val photoFile = createPhotoFile(context)
                        val uri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            photoFile
                        )
                        imageUri = uri
                        cameraLauncher.launch(uri)
                    }
                }
            }, Looper.getMainLooper())
        } catch (e: SecurityException) {
            vm.clearMessage()
        }
    }

    // Permission launcher: minta lokasi + kamera sekaligus
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocation = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocation = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        val camera = permissions[Manifest.permission.CAMERA] ?: false

        if ((fineLocation || coarseLocation) && camera) {
            launchCameraWithLocation()
        }
    }

    fun launchAttendanceSequence(clockType: String) {
        currentClockType = clockType

        // Cek apakah semua permission sudah di-grant (tanpa perlu request ulang)
        val hasFineLocation = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarseLocation = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCamera = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if ((hasFineLocation || hasCoarseLocation) && hasCamera) {
            // Check if user has registered face embedding
            if (state.isFaceRegistered) {
                showFaceAttendance = true
            } else {
                launchCameraWithLocation()
            }
        } else {
            // Request permissions yang belum di-grant
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.CAMERA
                )
            )
        }
    }

    Column(Modifier.fillMaxSize().background(Background).statusBarsPadding().verticalScroll(rememberScrollState())) {
        // Header with current date
        Column(Modifier.fillMaxWidth().padding(start = 18.dp, end = 64.dp, top = 14.dp, bottom = 10.dp)) {
            Text("Absensi", style = MaterialTheme.typography.headlineMedium)
            Text(
                "${DateUtils.formatDate(DateUtils.today())} • ${DateUtils.nowTime()}",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }

        // Today status card
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
            shape = RoundedCornerShape(28.dp),
            color = if (state.hasCheckedIn) GreenSoft else Surface, shadowElevation = 2.dp
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Absensi Hari Ini", style = MaterialTheme.typography.titleMedium)
                    Surface(shape = RoundedCornerShape(999.dp), color = if (state.hasCheckedIn) GreenSoft else OrangeSoft) {
                        Text(
                            if (state.hasCheckedIn) if (state.isLate) "Terlambat" else "Tepat Waktu" else "Belum Check-in",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (state.hasCheckedIn && !state.isLate) Green else if (state.isLate) Red else Orange
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                if (state.hasCheckedIn) {
                    InfoRow("Check-in", state.checkInTime, Green)
                    if (state.checkOutTime.isNotEmpty()) InfoRow("Check-out", state.checkOutTime, Blue)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Check-in / Check-out buttons
        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = { launchAttendanceSequence(Constants.AttendanceType.CLOCK_IN) },
                modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Green),
                enabled = !state.hasCheckedIn && !state.isLoading
            ) {
                Icon(Icons.Default.Login, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Check-in")
            }
            Button(
                onClick = { launchAttendanceSequence(Constants.AttendanceType.CLOCK_OUT) },
                modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Blue),
                enabled = state.hasCheckedIn && state.checkOutTime.isEmpty() && !state.isLoading
            ) {
                Icon(Icons.Default.Logout, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Check-out")
            }
        }

        Spacer(Modifier.height(16.dp))

        // Attendance calendar
        Surface(Modifier.fillMaxWidth().padding(horizontal = 18.dp), shape = RoundedCornerShape(24.dp), color = Surface, shadowElevation = 1.dp) {
            Column(Modifier.padding(16.dp)) {
                Text("Kalender Kehadiran", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    listOf("S","S","R","K","J","S","M").forEach {
                        Text(it, style = MaterialTheme.typography.labelSmall, color = TextMuted, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                }
                Spacer(Modifier.height(8.dp))
                val days = state.monthlyCalendar
                for (week in days.chunked(7)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        week.forEach { (day, status) ->
                            val bg = when (status) { "present" -> GreenSoft; "late" -> OrangeSoft; "absent" -> RedSoft; "holiday" -> Background; else -> Surface }
                            val fg = when (status) { "present" -> Green; "late" -> Orange; "absent" -> Red; "holiday" -> TextMuted; else -> TextPrimary }
                            Box(Modifier.weight(1f).aspectRatio(1f).padding(2.dp).clip(RoundedCornerShape(12.dp)).background(bg), contentAlignment = Alignment.Center) {
                                Text(if (day > 0) "$day" else "", style = MaterialTheme.typography.labelSmall, color = fg)
                            }
                        }
                        repeat(7 - week.size) { Box(Modifier.weight(1f)) }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    LegendDot(GreenSoft, "Hadir"); LegendDot(OrangeSoft, "Terlambat"); LegendDot(RedSoft, "Alpha")
                }
            }
        }

        if (state.message != null) {
            Spacer(Modifier.height(8.dp))
            Text(state.message!!, modifier = Modifier.padding(horizontal = 18.dp), color = Green, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(100.dp))
    }
    
    if (showFaceAttendance) {
        FaceAttendanceScreen(
            user = user,
            onSuccess = { uri, similarity ->
                showFaceAttendance = false
                val loc = locationCoords
                if (loc != null) {
                    vm.submitAttendance(
                        employeeId = resolvedEmpId,
                        imageUri = uri,
                        latitude = loc.first,
                        longitude = loc.second,
                        clockType = currentClockType,
                        context = context,
                        isMockDetected = isMockDetected,
                        userEmail = user.email,
                        checkInMode = "face_recognition",
                        faceRecognitionSimilarity = similarity,
                        livenessVerified = true
                    )
                }
            },
            onFallback = {
                showFaceAttendance = false
                vm.clearMessage()
                android.widget.Toast.makeText(context, "Autentikasi wajah gagal 3 kali. Absen dibatalkan.", android.widget.Toast.LENGTH_LONG).show()
            },
            onBack = {
                showFaceAttendance = false
            }
        )
    }
}

@Composable
fun AttendanceMonitorScreen(user: User, onBack: () -> Unit = {}, vm: AttendanceViewModel = viewModel()) {
    LaunchedEffect(Unit) {
        val isManager = user.primaryRole == com.ptniger.hris.utils.Constants.Role.MANAGER || user.role == com.ptniger.hris.utils.Constants.Role.MANAGER
        if (isManager) {
            vm.loadAllToday(user.userId, user.departmentId)
        } else {
            vm.loadAllToday()
        }
    }
    val state by vm.state.collectAsState()

    Column(Modifier.fillMaxSize().background(Background).statusBarsPadding().verticalScroll(rememberScrollState())) {
        Row(
            Modifier.fillMaxWidth().padding(start = 4.dp, end = 64.dp, top = 14.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Text("Monitoring Absensi", style = MaterialTheme.typography.headlineMedium)
            }
            if (state.todayList.isNotEmpty()) {
                Surface(shape = RoundedCornerShape(999.dp), color = com.ptniger.hris.ui.theme.GreenSoft) {
                    Text(
                        "${state.todayList.size} Hadir",
                        Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = com.ptniger.hris.ui.theme.Green
                    )
                }
            }
        }

        if (state.isLoading) {
            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = com.ptniger.hris.ui.theme.Blue)
            }
        } else if (state.todayList.isEmpty()) {
            // Empty state
            Box(
                Modifier.fillMaxWidth().padding(vertical = 48.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.AccessTime,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = com.ptniger.hris.ui.theme.TextMuted
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Belum ada absensi hari ini",
                        style = MaterialTheme.typography.titleSmall,
                        color = com.ptniger.hris.ui.theme.TextSecondary
                    )
                    Text(
                        "Daftar akan muncul setelah karyawan melakukan check-in",
                        style = MaterialTheme.typography.bodySmall,
                        color = com.ptniger.hris.ui.theme.TextMuted
                    )
                }
            }
        } else {
            state.todayList.forEach { att ->
                val isLate = att.attendanceStatus == "late"
                Surface(
                    Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(18.dp),
                    color = com.ptniger.hris.ui.theme.Surface,
                    shadowElevation = 1.dp
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(42.dp).clip(RoundedCornerShape(14.dp))
                                .background(if (isLate) com.ptniger.hris.ui.theme.OrangeSoft else com.ptniger.hris.ui.theme.GreenSoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                att.employeeId.take(2).uppercase(),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isLate) com.ptniger.hris.ui.theme.Orange else com.ptniger.hris.ui.theme.Green
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "ID: ${att.employeeId}",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                "Check-in: ${att.checkIn}",
                                style = MaterialTheme.typography.bodySmall,
                                color = com.ptniger.hris.ui.theme.TextSecondary
                            )
                            if (att.checkOut.isNotEmpty()) {
                                Text(
                                    "Check-out: ${att.checkOut}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = com.ptniger.hris.ui.theme.TextSecondary
                                )
                            }
                        }
                        Surface(shape = RoundedCornerShape(999.dp), color = if (isLate) com.ptniger.hris.ui.theme.OrangeSoft else com.ptniger.hris.ui.theme.GreenSoft) {
                            Text(
                                if (isLate) "Terlambat" else "Tepat Waktu",
                                Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isLate) com.ptniger.hris.ui.theme.Orange else com.ptniger.hris.ui.theme.Green
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(100.dp))
    }
}

@Composable
private fun InfoRow(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Surface(shape = RoundedCornerShape(999.dp), color = color.copy(alpha = 0.1f)) {
            Text(value, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.labelMedium, color = color)
        }
    }
}

@Composable
private fun LegendDot(color: androidx.compose.ui.graphics.Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(color))
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}


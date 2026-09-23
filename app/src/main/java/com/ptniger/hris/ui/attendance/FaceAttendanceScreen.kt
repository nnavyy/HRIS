package com.ptniger.hris.ui.attendance

import android.graphics.Bitmap
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.ptniger.hris.data.model.User
import com.ptniger.hris.data.repository.AttendanceRepository
import com.ptniger.hris.data.repository.EmployeeRepository
import com.ptniger.hris.ui.theme.Blue
import com.ptniger.hris.ui.theme.Green
import com.ptniger.hris.ui.theme.Red
import com.ptniger.hris.utils.FaceNetModel
import com.ptniger.hris.utils.FaceRecognitionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.ptniger.hris.utils.toCorrectBitmap
import java.util.concurrent.Executors

@Composable
fun FaceAttendanceScreen(
    user: User,
    onSuccess: (android.net.Uri, Float) -> Unit,
    onFallback: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    
    fun saveBitmapToCache(bmp: Bitmap): android.net.Uri {
        val file = java.io.File(context.cacheDir, "face_attendance_${System.currentTimeMillis()}.jpg")
        file.outputStream().use { out ->
            bmp.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        return androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }
    var detectionState by remember { mutableStateOf(FaceDetectionState.SEARCHING) }
    var activeChallenge by remember { mutableStateOf(FaceRecognitionManager.getRandomChallenge()) }
    var challengeCompleted by remember { mutableStateOf(false) }
    var isVerifying by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var failureCount by remember { mutableStateOf(0) }
    
    val faceNetModel = remember { FaceNetModel(context) }
    val employeeRepo = remember { EmployeeRepository() }
    val attendanceRepo = remember { AttendanceRepository() }
    val coroutineScope = rememberCoroutineScope()
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    
    var allEmployees by remember { mutableStateOf<List<com.ptniger.hris.data.model.Employee>>(emptyList()) }

    DisposableEffect(Unit) {
        onDispose {
            faceNetModel.close()
            cameraExecutor.shutdown()
        }
    }
    
    LaunchedEffect(Unit) {
        allEmployees = employeeRepo.getAll().filter { it.isFaceRegistered }
    }

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    
    // Auto-verify when active challenge is passed & face is oriented forward
    var latestBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var latestFace by remember { mutableStateOf<com.google.mlkit.vision.face.Face?>(null) }

    LaunchedEffect(challengeCompleted) {
        if (challengeCompleted && !isVerifying) {
            val bmp = latestBitmap
            val face = latestFace
            if (bmp != null && face != null) {
                isVerifying = true
                message = "Tantangan lolos! Memverifikasi biometrik wajah..."
                
                try {
                    val croppedFace = FaceRecognitionManager.cropFaceBitmap(bmp, face)
                    val result = FaceRecognitionManager.verifyFace(croppedFace, allEmployees, faceNetModel)
                    
                    when (result) {
                        is FaceRecognitionManager.RecognitionResult.Match -> {
                            // Cek apakah match dengan user yang login
                            if (result.employee.userId == user.userId || result.employee.employeeId == user.employeeId) {
                                message = "Wajah Terverifikasi Asli ✓\nSelamat datang, ${result.employee.name}!"
                                detectionState = FaceDetectionState.REGISTERED // Re-use this state for success
                                
                                delay(1200)
                                val uri = saveBitmapToCache(croppedFace)
                                onSuccess(uri, result.similarity)
                            } else {
                                message = "Wajah terdeteksi sebagai ${result.employee.name}, bukan akun Anda."
                                delay(2200)
                                activeChallenge = FaceRecognitionManager.getRandomChallenge()
                                challengeCompleted = false
                                isVerifying = false
                                detectionState = FaceDetectionState.SEARCHING
                            }
                        }
                        is FaceRecognitionManager.RecognitionResult.NoMatch -> {
                            failureCount++
                            message = "Wajah tidak cocok dengan data terdaftar (Sim: ${"%.2f".format(result.similarity)})"
                            delay(2000)
                            if (failureCount >= 3) {
                                onFallback()
                            } else {
                                activeChallenge = FaceRecognitionManager.getRandomChallenge()
                                challengeCompleted = false
                                isVerifying = false
                                detectionState = FaceDetectionState.SEARCHING
                            }
                        }
                        else -> {
                            message = "Gagal memverifikasi biometrik"
                            delay(2000)
                            activeChallenge = FaceRecognitionManager.getRandomChallenge()
                            challengeCompleted = false
                            isVerifying = false
                            detectionState = FaceDetectionState.SEARCHING
                        }
                    }
                } catch (e: Exception) {
                    message = "Error: ${e.message}"
                    delay(2000)
                    activeChallenge = FaceRecognitionManager.getRandomChallenge()
                    challengeCompleted = false
                    isVerifying = false
                }
            }
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val imageAnalyzer = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { analysis ->
                            var lastAnalysisTime = 0L
                            var noFaceFrameCount = 0

                            analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                val currentTime = System.currentTimeMillis()
                                if (currentTime - lastAnalysisTime < 300) {
                                    imageProxy.close()
                                    return@setAnalyzer
                                }
                                lastAnalysisTime = currentTime

                                val bitmap = try { 
                                    imageProxy.toCorrectBitmap() 
                                } catch (e: Exception) { 
                                    null 
                                }
                                
                                if (bitmap != null && !isVerifying) {
                                    FaceRecognitionManager.detectFacesSync(bitmap) { faces ->
                                        if (faces.isNotEmpty()) {
                                            noFaceFrameCount = 0
                                            val face = faces.first()
                                            
                                            coroutineScope.launch {
                                                latestBitmap = bitmap
                                                latestFace = face
                                                if (detectionState != FaceDetectionState.DETECTED && detectionState != FaceDetectionState.REGISTERED) {
                                                    detectionState = FaceDetectionState.DETECTED
                                                }
                                                // Cek apakah aksi tantangan liveness terpenuhi
                                                if (!challengeCompleted && FaceRecognitionManager.verifyChallenge(face, activeChallenge)) {
                                                    challengeCompleted = true
                                                }
                                            }
                                        } else {
                                            noFaceFrameCount++
                                            if (noFaceFrameCount > 3) {
                                                coroutineScope.launch {
                                                    detectionState = FaceDetectionState.SEARCHING
                                                }
                                            }
                                        }
                                        imageProxy.close()
                                    }
                                } else {
                                    imageProxy.close()
                                }
                            }
                        }

                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        ctx as LifecycleOwner,
                        CameraSelector.DEFAULT_FRONT_CAMERA,
                        preview,
                        imageAnalyzer
                    )
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        Canvas(Modifier.fillMaxSize()) {
            val ellipseRect = androidx.compose.ui.geometry.Rect(
                center = Offset(size.width / 2f, size.height * 0.42f),
                radius = size.width * 0.36f
            )
            drawRect(color = Color.Black.copy(alpha = 0.55f), size = size)
            drawOval(
                color = Color.Transparent,
                topLeft = Offset(ellipseRect.left, ellipseRect.top - ellipseRect.height * 0.25f),
                size = androidx.compose.ui.geometry.Size(ellipseRect.width, ellipseRect.height * 1.3f),
                blendMode = BlendMode.Clear
            )
            drawOval(
                color = when {
                    detectionState == FaceDetectionState.REGISTERED -> Green
                    challengeCompleted -> Green
                    detectionState == FaceDetectionState.DETECTED -> Color(0xFFF59E0B) // Amber saat mengerjakan tantangan
                    else -> Color.White
                },
                topLeft = Offset(ellipseRect.left, ellipseRect.top - ellipseRect.height * 0.25f),
                size = androidx.compose.ui.geometry.Size(ellipseRect.width, ellipseRect.height * 1.3f),
                style = Stroke(width = 3.dp.toPx())
            )
        }

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.75f))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Anti-spoofing challenge badge
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = if (challengeCompleted) Green.copy(alpha = 0.2f) else Color(0xFFF59E0B).copy(alpha = 0.2f)
            ) {
                Text(
                    text = if (challengeCompleted) "✓ Liveness Terkonfirmasi Asli" else "🛡️ Verifikasi Anti-Foto",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (challengeCompleted) Green else Color(0xFFF59E0B)
                )
            }

            Text(
                when {
                    message.isNotEmpty() -> message
                    challengeCompleted -> "Tantangan selesai! Menghadap lurus ke kamera..."
                    detectionState == FaceDetectionState.DETECTED -> "👉 ${activeChallenge.instruction}\n(${activeChallenge.hint})"
                    else -> "Arahkan wajah Anda ke dalam oval"
                },
                color = if (challengeCompleted || detectionState == FaceDetectionState.REGISTERED) Green else Color.White,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )

            if (failureCount > 0) {
                Text(
                    "Percobaan gagal: $failureCount/3",
                    color = Red,
                    style = MaterialTheme.typography.labelSmall
                )
            }



            TextButton(onClick = onBack) {
                Text("Batal", color = Color.White.copy(alpha = 0.7f))
            }
        }
    }
}

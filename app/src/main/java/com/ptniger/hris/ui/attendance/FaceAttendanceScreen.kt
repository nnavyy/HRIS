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
    var blinkDetected by remember { mutableStateOf(false) }
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
    
    // Auto-verify when blink is detected
    var latestBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var latestFace by remember { mutableStateOf<com.google.mlkit.vision.face.Face?>(null) }

    LaunchedEffect(blinkDetected) {
        if (blinkDetected && !isVerifying && detectionState == FaceDetectionState.DETECTED) {
            val bmp = latestBitmap
            val face = latestFace
            if (bmp != null && face != null) {
                isVerifying = true
                message = "Memverifikasi wajah..."
                
                try {
                    val croppedFace = FaceRecognitionManager.cropFaceBitmap(bmp, face)
                    val result = FaceRecognitionManager.verifyFace(croppedFace, allEmployees, faceNetModel)
                    
                    when (result) {
                        is FaceRecognitionManager.RecognitionResult.Match -> {
                            // Cek apakah match dengan user yang login
                            if (result.employee.userId == user.userId || result.employee.employeeId == user.employeeId) {
                                message = "Selamat datang, ${result.employee.name}!"
                                detectionState = FaceDetectionState.REGISTERED // Re-use this state for success
                                
                                // In real app, you would pass these back to AttendanceScreen or call checkIn directly.
                                // For simplicity we call checkIn here, but you should pass coordinates.
                                // Since we don't have location here, we better navigate back to AttendanceScreen
                                // and pass the similarity score to be submitted with location.
                                delay(1500)
                                val uri = saveBitmapToCache(croppedFace)
                                onSuccess(uri, result.similarity)
                            } else {
                                message = "Wajah terdeteksi sebagai ${result.employee.name}, bukan akun Anda."
                                delay(2000)
                                blinkDetected = false
                                isVerifying = false
                                detectionState = FaceDetectionState.SEARCHING
                            }
                        }
                        is FaceRecognitionManager.RecognitionResult.NoMatch -> {
                            failureCount++
                            message = "Wajah tidak dikenali (Sim: ${"%.2f".format(result.similarity)})"
                            delay(2000)
                            if (failureCount >= 3) {
                                onFallback()
                            } else {
                                blinkDetected = false
                                isVerifying = false
                                detectionState = FaceDetectionState.SEARCHING
                            }
                        }
                        else -> {
                            message = "Gagal memverifikasi"
                            delay(2000)
                            blinkDetected = false
                            isVerifying = false
                            detectionState = FaceDetectionState.SEARCHING
                        }
                    }
                } catch (e: Exception) {
                    message = "Error: ${e.message}"
                    delay(2000)
                    blinkDetected = false
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
                                                if (detectionState != FaceDetectionState.DETECTED) {
                                                    detectionState = FaceDetectionState.DETECTED
                                                }
                                                if (FaceRecognitionManager.isBlinking(face)) {
                                                    blinkDetected = true
                                                }
                                            }
                                        } else {
                                            noFaceFrameCount++
                                            if (noFaceFrameCount > 3) {
                                                coroutineScope.launch {
                                                    detectionState = FaceDetectionState.SEARCHING
                                                    blinkDetected = false
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
                color = when (detectionState) {
                    FaceDetectionState.REGISTERED -> Green
                    FaceDetectionState.DETECTED -> Color(0xFF34A853)
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
                .background(Color.Black.copy(alpha = 0.7f))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                when {
                    message.isNotEmpty() -> message
                    detectionState == FaceDetectionState.DETECTED -> "Wajah terdeteksi ✓ — Berkedip untuk konfirmasi"
                    else -> "Arahkan wajah Anda ke dalam kotak"
                },
                color = if (detectionState == FaceDetectionState.REGISTERED) Green else Color.White,
                style = MaterialTheme.typography.bodyMedium,
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

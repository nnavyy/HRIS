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
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.ptniger.hris.data.model.User
import com.ptniger.hris.data.repository.EmployeeRepository
import com.ptniger.hris.ui.theme.Blue
import com.ptniger.hris.utils.FaceNetModel
import com.ptniger.hris.utils.FaceRecognitionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.ptniger.hris.utils.toCorrectBitmap
import java.util.concurrent.Executors

enum class FaceDetectionState { SEARCHING, DETECTED, REGISTERED }

@Composable
fun FaceRegistrationScreen(
    employeeId: String,
    employeeName: String,
    user: User,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var detectionState by remember { mutableStateOf(FaceDetectionState.SEARCHING) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var blinkDetected by remember { mutableStateOf(false) }
    var isRegistering by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    
    // Track the latest bitmap from camera stream
    var latestBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var latestFace by remember { mutableStateOf<com.google.mlkit.vision.face.Face?>(null) }
    
    var faceNetModel by remember { mutableStateOf<FaceNetModel?>(null) }
    var modelError by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(Unit) {
        try {
            faceNetModel = FaceNetModel(context)
        } catch (e: Exception) {
            modelError = e.message
            message = "Gagal memuat AI Model: ${e.message}"
        }
    }
    val employeeRepo = remember { EmployeeRepository() }
    val coroutineScope = rememberCoroutineScope()

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            faceNetModel?.close()
            cameraExecutor.shutdown()
        }
    }

    // CameraX setup
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasCameraPermission = granted
        if (!granted) {
            message = "Izin kamera diperlukan untuk mendaftarkan wajah."
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {

        // Camera Preview
        if (hasCameraPermission) {
            AndroidView(
                factory = { ctx ->
                val previewView = PreviewView(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    // Image Analyzer untuk real-time face detection
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

                                if (bitmap != null) {
                                    FaceRecognitionManager.detectFacesSync(bitmap) { faces ->
                                        if (faces.isNotEmpty()) {
                                            noFaceFrameCount = 0
                                            val face = faces.first()
                                            
                                            // Pindah ke main thread untuk update state Compose
                                            coroutineScope.launch {
                                                latestBitmap = bitmap
                                                latestFace = face
                                                detectionState = FaceDetectionState.DETECTED
                                                val currentFaceNet = faceNetModel
                                                if (currentFaceNet != null) {
                                                    try {
                                                        if (FaceRecognitionManager.isBlinking(face)) {
                                                            blinkDetected = true
                                                        }
                                                    } catch (e: Exception) {
                                                        message = "Error liveness: ${e.message}"
                                                    }
                                                }
                                            }
                                        } else {
                                            noFaceFrameCount++
                                            if (noFaceFrameCount > 3) {
                                                coroutineScope.launch {
                                                    detectionState = FaceDetectionState.SEARCHING
                                                    // Jika tidak ada wajah dalam beberapa frame, kita reset liveness
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
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Menunggu Izin Kamera...", color = Color.White)
            }
        }

        // Overlay: ellipse guide
        Canvas(Modifier.fillMaxSize()) {
            // Background gelap dengan lubang elips di tengah
            val ellipseRect = androidx.compose.ui.geometry.Rect(
                center = Offset(size.width / 2f, size.height * 0.42f),
                radius = size.width * 0.36f
            )
            drawRect(color = Color.Black.copy(alpha = 0.55f), size = size)
            // Hapus area elips (lubang panduan)
            drawOval(
                color = Color.Transparent,
                topLeft = Offset(ellipseRect.left, ellipseRect.top - ellipseRect.height * 0.25f),
                size = androidx.compose.ui.geometry.Size(ellipseRect.width, ellipseRect.height * 1.3f),
                blendMode = BlendMode.Clear
            )
            // Border elips
            drawOval(
                color = when (detectionState) {
                    FaceDetectionState.DETECTED -> Color(0xFF34A853)
                    else -> Color.White
                },
                topLeft = Offset(ellipseRect.left, ellipseRect.top - ellipseRect.height * 0.25f),
                size = androidx.compose.ui.geometry.Size(ellipseRect.width, ellipseRect.height * 1.3f),
                style = Stroke(width = 3.dp.toPx())
            )
        }

        // Bottom panel
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
                    detectionState == FaceDetectionState.DETECTED && blinkDetected ->
                        "✓ Wajah terdeteksi & liveness OK — siap daftarkan"
                    detectionState == FaceDetectionState.DETECTED ->
                        "Wajah terdeteksi ✓ — Berkedip sekali untuk liveness"
                    else -> {
                        val nameParts = employeeName.split(" ")
                        val firstName = if (nameParts.isNotEmpty()) nameParts[0] else employeeName
                        "Arahkan wajah $firstName ke dalam kotak"
                    }
                },
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )

            Button(
                onClick = {
                    val bmp = latestBitmap
                    val face = latestFace
                    if (bmp != null && face != null) {
                        isRegistering = true
                        coroutineScope.launch {
                            try {
                                val croppedFace = FaceRecognitionManager.cropFaceBitmap(bmp, face)
                                val embedding = faceNetModel?.getEmbedding(croppedFace)
                                if (embedding == null) {
                                    message = "Model AI tidak siap."
                                    isRegistering = false
                                    return@launch
                                }
                                message = "Menyimpan foto wajah..."
                                val uploadResult = employeeRepo.uploadFaceImage(employeeId, croppedFace)
                                val imageUrl = uploadResult.getOrNull()

                                val result = employeeRepo.saveFaceEmbedding(
                                    employeeId = employeeId,
                                    embedding = embedding,
                                    registeredBy = user.userId,
                                    faceImageUrl = imageUrl
                                )
                                if (result.isSuccess) {
                                    message = "Wajah berhasil didaftarkan! ✓"
                                    detectionState = FaceDetectionState.REGISTERED
                                    withContext(Dispatchers.Main) {
                                        kotlinx.coroutines.delay(2000)
                                        onBack()
                                    }
                                } else {
                                    message = "Gagal mendaftarkan: ${result.exceptionOrNull()?.message}"
                                    isRegistering = false
                                }
                            } catch (e: Exception) {
                                message = "Gagal mendaftarkan wajah: ${e.message}"
                                isRegistering = false
                            }
                        }
                    }
                },
                enabled = detectionState == FaceDetectionState.DETECTED && blinkDetected && !isRegistering,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Blue)
            ) {
                if (isRegistering) {
                    CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("Tangkap & Daftarkan")
                }
            }

            TextButton(onClick = onBack) {
                Text("Batal", color = Color.White.copy(alpha = 0.7f))
            }
        }
    }
}

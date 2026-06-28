package com.ptniger.hris.ui.contract

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.util.Base64
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.ptniger.hris.data.model.EmployeeContract
import com.ptniger.hris.data.model.User
import com.ptniger.hris.data.repository.ContractRepository
import com.ptniger.hris.ui.theme.*
import com.ptniger.hris.utils.PayrollCalculator
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

@Composable
fun ContractSignScreen(
    user: User,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var contracts by remember { mutableStateOf<List<EmployeeContract>>(emptyList()) }
    var selectedContract by remember { mutableStateOf<EmployeeContract?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isSigning by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var signaturePaths by remember { mutableStateOf<List<List<Offset>>>(emptyList()) }
    var currentPath by remember { mutableStateOf<List<Offset>>(emptyList()) }

    // Load unsigned contracts for this user
    LaunchedEffect(Unit) {
        val repo = ContractRepository()
        val employeeId = user.uid
        contracts = repo.getUnsignedContracts(employeeId)
        if (contracts.isNotEmpty()) selectedContract = contracts.first()
        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        // Top bar
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
            Text("Kontrak Kerja", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 4.dp))
        }

        when {
            isLoading -> Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            contracts.isEmpty() -> Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CheckCircle, null, tint = Green, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("Tidak ada kontrak yang perlu ditandatangani", style = MaterialTheme.typography.bodyLarge)
                    Text("Semua kontrak kamu sudah ditandatangani ✅", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }

            else -> {
                val contract = selectedContract ?: return@Column
                Column(Modifier.padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

                    // Contract Details Card
                    Surface(shape = RoundedCornerShape(20.dp), color = Surface, shadowElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Detail Kontrak", style = MaterialTheme.typography.titleMedium)
                            HorizontalDivider(color = CardBorder)
                            ContractDetailRow("Berlaku Sejak", contract.effectiveDate)
                            ContractDetailRow("Gaji Pokok", "Rp ${"%,.0f".format(contract.baseSalary)}")
                            ContractDetailRow("Tunjangan Makan", "Rp ${"%,.0f".format(contract.allowanceMeal)}")
                            ContractDetailRow("Tunjangan Transport", "Rp ${"%,.0f".format(contract.allowanceTransport)}")
                            ContractDetailRow("Tunjangan Jabatan", "Rp ${"%,.0f".format(contract.allowancePosition)}")
                            val totalAllowance = PayrollCalculator.calculateTotalAllowance(
                                contract.allowanceMeal, contract.allowanceTransport, contract.allowancePosition
                            )
                            val gross = contract.baseSalary + totalAllowance
                            val pph21 = PayrollCalculator.calculatePph21Ter(gross, contract.ptkpStatus)
                            ContractDetailRow("Status PTKP", contract.ptkpStatus)
                            ContractDetailRow("PPh 21 TER (est.)", "Rp ${"%,.0f".format(pph21)}")
                            HorizontalDivider(color = CardBorder)
                            val bpjsKes = PayrollCalculator.calculateBpjsKesehatan(contract.baseSalary, totalAllowance)
                            val bpjsJht = PayrollCalculator.calculateBpjsJht(contract.baseSalary, totalAllowance)
                            val bpjsJp  = PayrollCalculator.calculateBpjsJp(contract.baseSalary, totalAllowance)
                            val net = gross - bpjsKes - bpjsJht - bpjsJp - pph21
                            ContractDetailRow("Estimasi Gaji Bersih", "Rp ${"%,.0f".format(net)}", highlight = true)
                        }
                    }

                    // Disclaimer
                    Surface(shape = RoundedCornerShape(12.dp), color = OrangeSoft, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "⚠️ Tanda tangan digital ini bukan merupakan tanda tangan yang memiliki kekuatan hukum. " +
                            "Kontrak ini merupakan kesepakatan internal perusahaan yang disimpan secara digital di sistem HRIS.",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Orange
                        )
                    }

                    // Signature Canvas
                    Text("Tanda Tangan Digital", style = MaterialTheme.typography.titleSmall)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                            .background(Color.White, RoundedCornerShape(16.dp))
                    ) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    detectDragGestures(
                                        onDragStart = { offset -> currentPath = listOf(offset) },
                                        onDrag = { change, _ ->
                                            currentPath = currentPath + change.position
                                        },
                                        onDragEnd = {
                                            if (currentPath.isNotEmpty()) {
                                                signaturePaths = signaturePaths + listOf(currentPath)
                                                currentPath = emptyList()
                                            }
                                        }
                                    )
                                }
                        ) {
                            val paint = androidx.compose.ui.graphics.Color.Black
                            (signaturePaths + (if (currentPath.isNotEmpty()) listOf(currentPath) else emptyList())).forEach { points ->
                                if (points.size > 1) {
                                    val path = Path()
                                    path.moveTo(points.first().x, points.first().y)
                                    for (i in 1 until points.size) {
                                        path.lineTo(points[i].x, points[i].y)
                                    }
                                    drawPath(path, paint, style = Stroke(width = 3f))
                                }
                            }
                            if (signaturePaths.isEmpty() && currentPath.isEmpty()) {
                                drawContext.canvas.nativeCanvas.drawText(
                                    "Tanda tangan di sini",
                                    size.width / 2f,
                                    size.height / 2f,
                                    android.graphics.Paint().apply {
                                        color = android.graphics.Color.LTGRAY
                                        textSize = 40f
                                        textAlign = android.graphics.Paint.Align.CENTER
                                    }
                                )
                            }
                        }
                    }

                    // Clear signature button
                    OutlinedButton(
                        onClick = { signaturePaths = emptyList(); currentPath = emptyList() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Delete, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Hapus Tanda Tangan")
                    }

                    // Message
                    message?.let {
                        Surface(shape = RoundedCornerShape(12.dp), color = if (it.contains("berhasil")) GreenSoft else RedSoft) {
                            Text(it, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    // Sign button
                    Button(
                        onClick = {
                            if (signaturePaths.isEmpty()) {
                                message = "Tanda tangan belum dibuat. Silakan tanda tangan terlebih dahulu."
                                return@Button
                            }
                            scope.launch {
                                isSigning = true
                                message = null
                                // Convert signature canvas to base64
                                val signatureBase64 = renderSignatureToBase64(signaturePaths)
                                ContractRepository().signContract(contract.contractId, signatureBase64).fold(
                                    onSuccess = {
                                        message = "Kontrak berhasil ditandatangani! ✅"
                                        contracts = ContractRepository().getUnsignedContracts(user.uid)
                                        selectedContract = contracts.firstOrNull()
                                    },
                                    onFailure = { message = "Gagal: ${it.message}" }
                                )
                                isSigning = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        enabled = !isSigning && signaturePaths.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = Green)
                    ) {
                        if (isSigning) {
                            CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.CheckCircle, null, Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Setuju & Tanda Tangan")
                        }
                    }

                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun ContractDetailRow(label: String, value: String, highlight: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        Text(
            value,
            style = if (highlight) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodySmall,
            color = if (highlight) Green else TextPrimary
        )
    }
}

private fun renderSignatureToBase64(paths: List<List<Offset>>): String {
    val width = 600
    val height = 200
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(android.graphics.Color.WHITE)
    val paint = Paint().apply {
        color = android.graphics.Color.BLACK
        strokeWidth = 5f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }
    paths.forEach { points ->
        if (points.size > 1) {
            val path = android.graphics.Path()
            path.moveTo(points.first().x, points.first().y)
            for (i in 1 until points.size) {
                path.lineTo(points[i].x, points[i].y)
            }
            canvas.drawPath(path, paint)
        }
    }
    val baos = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.PNG, 90, baos)
    return Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT)
}

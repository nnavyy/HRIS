package com.ptniger.hris.ui.payroll

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ptniger.hris.data.model.User
import com.ptniger.hris.data.model.Payroll
import com.ptniger.hris.ui.theme.*
import com.ptniger.hris.utils.Constants
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import java.text.NumberFormat
import java.util.Locale

@Composable
fun PayrollApprovalScreen(user: User, onBack: () -> Unit = {}, vm: PayrollViewModel = viewModel()) {
    val role = user.primaryRole.ifEmpty { user.role }

    // ROLE GUARD: hanya Manager yang bisa approve
    if (role != Constants.Role.MANAGER && role != Constants.Role.SUPER_ADMIN) {
        Box(Modifier.fillMaxSize().background(Background), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Info, null, tint = Red, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(8.dp))
                Text("Akses Ditolak", style = MaterialTheme.typography.titleMedium)
                Text("Hanya Manager yang dapat menyetujui payroll.",
                    style = MaterialTheme.typography.bodySmall, color = TextSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp))
                Spacer(Modifier.height(16.dp))
                OutlinedButton(onClick = onBack) { Text("Kembali") }
            }
        }
        return
    }

    val payrolls by vm.payrolls.collectAsState()
    val message by vm.message.collectAsState()
    val fmt = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    LaunchedEffect(Unit) { vm.getTeamPayrolls(user) }

    // State untuk membuka dialog detail inspeksi & approval slip gaji
    var selectedPayrollForDetail by remember { mutableStateOf<Payroll?>(null) }

    Column(Modifier.fillMaxSize().background(Background).statusBarsPadding().verticalScroll(rememberScrollState())) {
        Row(
            Modifier.fillMaxWidth().padding(start = 4.dp, end = 64.dp, top = 14.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Persetujuan Payroll", style = MaterialTheme.typography.headlineMedium)
        }
        if (message != null) Text(message!!, Modifier.padding(horizontal = 18.dp), color = Green, style = MaterialTheme.typography.bodySmall)
        
        if (payrolls.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CheckCircle, null, tint = Green, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("Tidak ada payroll tim Anda", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
            }
        } else {
            // Group by status: pending first, then others
            val pending = payrolls.filter { it.status == Constants.PayrollStatus.PENDING_APPROVAL }
            val others = payrolls.filter { it.status != Constants.PayrollStatus.PENDING_APPROVAL }

            if (pending.isNotEmpty()) {
                Text("Menunggu Persetujuan (${pending.size})",
                    Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.titleSmall, color = Orange)
            }
            pending.forEach { p ->
                PayrollApprovalCard(
                    payroll = p,
                    fmt = fmt,
                    onOpenDetail = { selectedPayrollForDetail = p }
                )
            }

            if (others.isNotEmpty()) {
                Text("Riwayat Persetujuan",
                    Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.titleSmall, color = TextSecondary)
            }
            others.forEach { p ->
                PayrollApprovalCard(
                    payroll = p,
                    fmt = fmt,
                    onOpenDetail = { selectedPayrollForDetail = p }
                )
            }
        }
        Spacer(Modifier.height(100.dp))
    }

    // Modal Dialog Detail Lengkap & Keputusan Slip Gaji
    selectedPayrollForDetail?.let { payrollToReview ->
        PayrollApprovalDetailDialog(
            payroll = payrollToReview,
            user = user,
            fmt = fmt,
            onDismiss = { selectedPayrollForDetail = null },
            onApprove = { notes ->
                vm.processApproval(payrollToReview.payrollId, user, true, notes)
                selectedPayrollForDetail = null
            },
            onReject = { reason ->
                vm.processApproval(payrollToReview.payrollId, user, false, reason)
                selectedPayrollForDetail = null
            }
        )
    }
}

@Composable
private fun PayrollApprovalCard(
    payroll: Payroll,
    fmt: NumberFormat,
    onOpenDetail: () -> Unit
) {
    Surface(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 6.dp)
            .clickable { onOpenDetail() },
        shape = RoundedCornerShape(20.dp),
        color = Surface,
        shadowElevation = 1.dp
    ) {
        Column(Modifier.padding(16.dp)) {
            // Header row
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(payroll.employeeName, style = MaterialTheme.typography.titleSmall)
                    Text(
                        "${payroll.departmentId.ifEmpty { "Departemen" }} • ${com.ptniger.hris.utils.DateUtils.formatMonthYear(payroll.month, payroll.year)}",
                        style = MaterialTheme.typography.bodySmall, color = TextSecondary
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(fmt.format(payroll.netSalary),
                        style = MaterialTheme.typography.titleSmall, color = Green)
                    val statusColor = when(payroll.status) {
                        Constants.PayrollStatus.PENDING_APPROVAL -> Orange
                        Constants.PayrollStatus.APPROVED -> Green
                        Constants.PayrollStatus.REJECTED -> Red
                        Constants.PayrollStatus.FINALIZED -> Blue
                        Constants.PayrollStatus.PAID -> Purple
                        else -> TextSecondary
                    }
                    Surface(shape = RoundedCornerShape(999.dp), color = statusColor.copy(alpha = 0.12f)) {
                        Text(payroll.status.replaceFirstChar { it.uppercase() },
                            Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall, color = statusColor)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = CardBorder)
            Spacer(Modifier.height(8.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Lembur: ${payroll.overtimeHours}j", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    if (payroll.kpiBonus > 0) {
                        Spacer(Modifier.width(8.dp))
                        Text("• Bonus KPI: ${fmt.format(payroll.kpiBonus)}", style = MaterialTheme.typography.bodySmall, color = Purple)
                    }
                }
                TextButton(onClick = onOpenDetail, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                    Icon(Icons.Default.Visibility, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Periksa Rincian", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
fun PayrollApprovalDetailDialog(
    payroll: Payroll,
    user: User,
    fmt: NumberFormat,
    onDismiss: () -> Unit,
    onApprove: (notes: String) -> Unit,
    onReject: (reason: String) -> Unit
) {
    val context = LocalContext.current
    var employee by remember { mutableStateOf<com.ptniger.hris.data.model.Employee?>(null) }
    var monthlyAttendance by remember { mutableStateOf<List<com.ptniger.hris.data.model.Attendance>>(emptyList()) }
    var isLoadingBasics by remember { mutableStateOf(true) }

    // Dialog states
    var showApproveConfirm by remember { mutableStateOf(false) }
    var showRejectConfirm by remember { mutableStateOf(false) }
    var approveNotes by remember { mutableStateOf("") }
    var rejectReason by remember { mutableStateOf("") }
    var rejectError by remember { mutableStateOf<String?>(null) }

    // Load attendance & employee profile
    LaunchedEffect(payroll.employeeId) {
        val empRepo = com.ptniger.hris.data.repository.EmployeeRepository()
        val attRepo = com.ptniger.hris.data.repository.AttendanceRepository()

        employee = empRepo.getById(payroll.employeeId) ?: empRepo.getByUserId(payroll.employeeId)
        monthlyAttendance = attRepo.getMonthlyAttendance(payroll.employeeId, payroll.month, payroll.year)
        isLoadingBasics = false
    }

    val presentDays = monthlyAttendance.count {
        it.attendanceStatus == Constants.AttendanceStatus.PRESENT || it.attendanceStatus == Constants.AttendanceStatus.LATE
    }
    val lateDays = monthlyAttendance.count { it.attendanceStatus == Constants.AttendanceStatus.LATE }
    val totalOtFromAtt = monthlyAttendance.sumOf { it.overtimeHours }
    val isPending = payroll.status == Constants.PayrollStatus.PENDING_APPROVAL

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(24.dp),
            color = Surface,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header Dialog with Close
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Verifikasi Slip Gaji", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Periode: ${com.ptniger.hris.utils.DateUtils.formatMonthYear(payroll.month, payroll.year)}",
                            style = MaterialTheme.typography.bodySmall, color = TextSecondary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup")
                    }
                }

                HorizontalDivider(Modifier.padding(vertical = 10.dp))

                // Scrollable content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. Employee Card
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Background,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(46.dp).clip(RoundedCornerShape(16.dp)).background(BlueSoft),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    payroll.employeeName.take(2).uppercase(),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Blue
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(payroll.employeeName, style = MaterialTheme.typography.titleMedium)
                                val pos = employee?.position?.ifEmpty { "Karyawan" } ?: "Karyawan"
                                val dept = employee?.department?.ifEmpty { payroll.departmentId } ?: payroll.departmentId
                                Text(
                                    "$pos • $dept",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                                Text(
                                    "NIK: ${employee?.nik ?: "-"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )
                            }
                            val statusColor = when (payroll.status) {
                                Constants.PayrollStatus.PENDING_APPROVAL -> Orange
                                Constants.PayrollStatus.APPROVED -> Green
                                Constants.PayrollStatus.REJECTED -> Red
                                Constants.PayrollStatus.FINALIZED -> Blue
                                Constants.PayrollStatus.PAID -> Purple
                                else -> TextSecondary
                            }
                            Surface(shape = RoundedCornerShape(999.dp), color = statusColor.copy(alpha = 0.12f)) {
                                Text(
                                    payroll.status.uppercase(),
                                    Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = statusColor
                                )
                            }
                        }
                    }

                    // 2. Dasar Verifikasi Kehadiran & Lembur (Riil Database)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Background,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, null, tint = Green, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Verifikasi Data Absensi & Kinerja", style = MaterialTheme.typography.titleSmall)
                            }
                            Spacer(Modifier.height(10.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                StatVerificationItem("Hadir", "$presentDays hari", Green)
                                StatVerificationItem("Terlambat", "$lateDays hari", Orange)
                                StatVerificationItem("Lembur Log", "${String.format("%.1f", totalOtFromAtt)} jam", Teal)
                                StatVerificationItem("Skor KPI", "${String.format("%.1f", payroll.kpiScore)}", Purple)
                            }
                            if (totalOtFromAtt != payroll.overtimeHours) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "* Lembur disetujui di payroll: ${payroll.overtimeHours} jam (Log absensi: ${String.format("%.1f", totalOtFromAtt)} jam)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Orange
                                )
                            }
                        }
                    }

                    // 3. Rincian Pendapatan
                    Surface(shape = RoundedCornerShape(16.dp), color = Background, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp)) {
                            Text("Rincian Pendapatan", style = MaterialTheme.typography.titleSmall, color = Blue)
                            Spacer(Modifier.height(8.dp))
                            PayrollDetailRow("Gaji Pokok", fmt.format(payroll.baseSalary), TextPrimary)
                            if (payroll.allowanceMeal > 0) PayrollDetailRow("Tunjangan Makan", fmt.format(payroll.allowanceMeal), TextPrimary)
                            if (payroll.allowanceTransport > 0) PayrollDetailRow("Tunjangan Transport", fmt.format(payroll.allowanceTransport), TextPrimary)
                            if (payroll.allowancePosition > 0) PayrollDetailRow("Tunjangan Jabatan", fmt.format(payroll.allowancePosition), TextPrimary)
                            if (payroll.allowance > 0 && payroll.allowanceMeal == 0.0) PayrollDetailRow("Tunjangan Umum", fmt.format(payroll.allowance), TextPrimary)
                            if (payroll.overtimePay > 0) PayrollDetailRow("Upah Lembur (${payroll.overtimeHours} jam)", fmt.format(payroll.overtimePay), TextPrimary)
                            if (payroll.kpiBonus > 0) PayrollDetailRow("Bonus KPI (Skor ${String.format("%.1f", payroll.kpiScore)})", fmt.format(payroll.kpiBonus), Purple)

                            val gross = payroll.baseSalary + payroll.allowance + payroll.allowanceMeal + payroll.allowanceTransport + payroll.allowancePosition + payroll.overtimePay + payroll.kpiBonus
                            HorizontalDivider(Modifier.padding(vertical = 6.dp))
                            PayrollDetailRow("Total Penghasilan Kotor", fmt.format(gross), TextPrimary, isBold = true)
                        }
                    }

                    // 4. Rincian Potongan
                    Surface(shape = RoundedCornerShape(16.dp), color = Background, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp)) {
                            Text("Rincian Potongan", style = MaterialTheme.typography.titleSmall, color = Red)
                            Spacer(Modifier.height(8.dp))
                            if (payroll.bpjsKesehatan > 0) PayrollDetailRow("BPJS Kesehatan (1%)", "-${fmt.format(payroll.bpjsKesehatan)}", Red)
                            if (payroll.bpjsJht > 0) PayrollDetailRow("BPJS JHT (2%)", "-${fmt.format(payroll.bpjsJht)}", Red)
                            if (payroll.bpjsJp > 0) PayrollDetailRow("BPJS JP (1%)", "-${fmt.format(payroll.bpjsJp)}", Red)
                            if (payroll.pph21 > 0) PayrollDetailRow("PPh 21 TER (${payroll.ptkpStatus})", "-${fmt.format(payroll.pph21)}", Red)
                            if (payroll.deductions > 0) PayrollDetailRow("Potongan Lainnya / Sanksi", "-${fmt.format(payroll.deductions)}", Red)

                            val totalDeductions = payroll.bpjsKesehatan + payroll.bpjsJht + payroll.bpjsJp + payroll.pph21 + payroll.deductions
                            HorizontalDivider(Modifier.padding(vertical = 6.dp))
                            PayrollDetailRow("Total Potongan", "-${fmt.format(totalDeductions)}", Red, isBold = true)
                        }
                    }

                    // 5. Total Take Home Pay
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = GreenSoft,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Gaji Bersih Diterima", style = MaterialTheme.typography.titleSmall, color = Green)
                                Text("Take-Home Pay Karyawan", style = MaterialTheme.typography.labelSmall, color = Green.copy(alpha = 0.8f))
                            }
                            Text(
                                fmt.format(payroll.netSalary),
                                style = MaterialTheme.typography.headlineSmall,
                                color = Green
                            )
                        }
                    }

                    // 6. Tanggungan Perusahaan (Info Saja)
                    if (payroll.bpjsJkk > 0 || payroll.bpjsJkm > 0) {
                        Surface(shape = RoundedCornerShape(16.dp), color = Background, modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp)) {
                                Text("Tanggungan Perusahaan (Non-Potongan)", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                                Spacer(Modifier.height(4.dp))
                                PayrollDetailRow("BPJS JKK (Kecelakaan Kerja)", fmt.format(payroll.bpjsJkk), TextMuted)
                                PayrollDetailRow("BPJS JKM (Kematian)", fmt.format(payroll.bpjsJkm), TextMuted)
                            }
                        }
                    }

                    // Catatan Penolakan / Persetujuan Sebelumnya
                    if (payroll.rejectionReason.isNotEmpty()) {
                        Surface(shape = RoundedCornerShape(12.dp), color = RedSoft, modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp)) {
                                Text("Alasan Penolakan:", style = MaterialTheme.typography.labelSmall, color = Red)
                                Text(payroll.rejectionReason, style = MaterialTheme.typography.bodySmall, color = Red)
                            }
                        }
                    }
                    if (payroll.approvalNotes.isNotEmpty() && payroll.status == Constants.PayrollStatus.APPROVED) {
                        Surface(shape = RoundedCornerShape(12.dp), color = GreenSoft, modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp)) {
                                Text("Catatan Persetujuan:", style = MaterialTheme.typography.labelSmall, color = Green)
                                Text(payroll.approvalNotes, style = MaterialTheme.typography.bodySmall, color = Green)
                            }
                        }
                    }

                    // 7. Tombol Unduh / Preview PDF
                    OutlinedButton(
                        onClick = {
                            com.ptniger.hris.utils.PdfSlipGenerator.generateAndOpen(
                                context = context,
                                payroll = payroll,
                                employeeName = payroll.employeeName,
                                nik = employee?.nik ?: "",
                                position = employee?.position ?: "",
                                department = employee?.department?.ifEmpty { payroll.departmentId } ?: payroll.departmentId,
                                signatures = com.ptniger.hris.utils.PdfSlipGenerator.SignatureInfo(
                                    employeeName = payroll.employeeName,
                                    managerName = user.fullName.ifEmpty { user.name },
                                    hrName = "HRD"
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Blue),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Blue)
                    ) {
                        Icon(Icons.Default.Download, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Preview / Unduh PDF Slip Gaji", style = MaterialTheme.typography.labelMedium)
                    }
                }

                // 8. Footer Action Buttons
                HorizontalDivider(Modifier.padding(vertical = 10.dp))
                if (isPending) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { showRejectConfirm = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Red),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Text("Tolak & Revisi")
                        }
                        Button(
                            onClick = { showApproveConfirm = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Green),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Text("Setujui Payroll")
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Tutup")
                    }
                }
            }
        }
    }

    // Dialog Konfirmasi Setujui
    if (showApproveConfirm) {
        AlertDialog(
            onDismissRequest = { showApproveConfirm = false },
            title = { Text("Setujui Slip Gaji") },
            text = {
                Column {
                    Text(
                        "Konfirmasi persetujuan slip gaji untuk ${payroll.employeeName} sebesar ${fmt.format(payroll.netSalary)}.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = approveNotes,
                        onValueChange = { approveNotes = it },
                        label = { Text("Catatan Persetujuan (Opsional)") },
                        placeholder = { Text("Contoh: Disetujui, lembur sesuai.") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showApproveConfirm = false
                        onApprove(approveNotes.ifEmpty { "Disetujui oleh Manajer" })
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Green)
                ) {
                    Text("Ya, Setujui")
                }
            },
            dismissButton = {
                TextButton(onClick = { showApproveConfirm = false }) { Text("Batal") }
            }
        )
    }

    // Dialog Penolakan & Catatan Revisi
    if (showRejectConfirm) {
        AlertDialog(
            onDismissRequest = { showRejectConfirm = false; rejectError = null },
            title = { Text("Tolak & Minta Revisi") },
            text = {
                Column {
                    Text(
                        "Masukkan alasan penolakan atau instruksi koreksi untuk tim Finance:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = rejectReason,
                        onValueChange = { rejectReason = it; rejectError = null },
                        label = { Text("Alasan Penolakan (Wajib)") },
                        placeholder = { Text("Contoh: Lembur tgl 15 belum ada persetujuan SPK.") },
                        isError = rejectError != null,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    if (rejectError != null) {
                        Text(rejectError!!, color = Red, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (rejectReason.isBlank()) {
                            rejectError = "Alasan penolakan wajib diisi"
                            return@Button
                        }
                        showRejectConfirm = false
                        onReject(rejectReason.trim())
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Red)
                ) {
                    Text("Kirim Penolakan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRejectConfirm = false; rejectError = null }) { Text("Batal") }
            }
        )
    }
}

@Composable
private fun StatVerificationItem(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}

@Composable
private fun PayrollDetailRow(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color,
    isBold: Boolean = false
) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = if (isBold) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodySmall, color = if (isBold) TextPrimary else TextSecondary)
        Text(value, style = if (isBold) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodySmall, color = color)
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayrollScreen(user: User, onBack: () -> Unit = {}, vm: PayrollViewModel = viewModel()) {
    var empId by remember { mutableStateOf("") }
    var empName by remember { mutableStateOf("") }
    var baseSalary by remember { mutableStateOf("") }
    var allowance by remember { mutableStateOf("") }
    var overtime by remember { mutableStateOf("") }
    var deductions by remember { mutableStateOf("") }
    var empExpanded by remember { mutableStateOf(false) }
    var empSearch by remember { mutableStateOf("") }
    val payrolls by vm.payrolls.collectAsState()
    val employees by vm.employees.collectAsState()
    val message by vm.message.collectAsState()
    LaunchedEffect(Unit) { vm.loadAll() }

    val filteredEmployees = if (empSearch.isBlank()) employees else {
        employees.filter {
            it.name.contains(empSearch, ignoreCase = true) ||
            it.nik.contains(empSearch, ignoreCase = true) ||
            it.email.contains(empSearch, ignoreCase = true)
        }
    }

    Column(Modifier.fillMaxSize().background(Background).statusBarsPadding().verticalScroll(rememberScrollState())) {
        Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 64.dp, top = 14.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            Text("Payroll", style = MaterialTheme.typography.headlineMedium)
        }
        Surface(Modifier.fillMaxWidth().padding(horizontal = 18.dp), shape = RoundedCornerShape(24.dp), color = Surface, shadowElevation = 2.dp) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Generate Payroll", style = MaterialTheme.typography.titleMedium)
                
                // Employee Picker (AlertDialog instead of ExposedDropdownMenu)
                val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                OutlinedTextField(
                    value = if (empName.isNotEmpty()) "$empName (${empId.take(8)})" else "Pilih Karyawan",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Karyawan") },
                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, "Dropdown") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    interactionSource = interactionSource.also { interaction ->
                        LaunchedEffect(interaction) {
                            interaction.interactions.collect {
                                if (it is androidx.compose.foundation.interaction.PressInteraction.Release) {
                                    empExpanded = true
                                }
                            }
                        }
                    }
                )

                if (empExpanded) {
                    AlertDialog(
                        onDismissRequest = { empExpanded = false; empSearch = "" },
                        title = { Text("Pilih Karyawan") },
                        text = {
                            Column {
                                OutlinedTextField(
                                    value = empSearch,
                                    onValueChange = { empSearch = it },
                                    placeholder = { Text("Cari nama/NIK...") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                Spacer(Modifier.height(8.dp))
                                androidx.compose.foundation.lazy.LazyColumn(
                                    modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)
                                ) {
                                    if (filteredEmployees.isEmpty()) {
                                        item { Text("Tidak ada karyawan", Modifier.padding(16.dp)) }
                                    }
                                    items(filteredEmployees) { emp ->
                                        Column(Modifier.fillMaxWidth().clickable {
                                            empId = emp.employeeId
                                            empName = emp.name
                                            baseSalary = emp.baseSalary.toLong().toString()
                                            
                                            // Auto-fill overtime
                                            vm.getAutoOvertimeHours(emp.employeeId) { hours ->
                                                overtime = if (hours > 0) hours.toString() else "0.0"
                                            }
                                            
                                            empExpanded = false
                                            empSearch = ""
                                        }.padding(16.dp)) {
                                            Text(emp.name, style = MaterialTheme.typography.bodyMedium)
                                            Text("NIK: ${emp.nik} • ${emp.position}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                        }
                                        HorizontalDivider()
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { empExpanded = false; empSearch = "" }) {
                                Text("Tutup")
                            }
                        }
                    )
                }

                OutlinedTextField(baseSalary, { if (it.isEmpty() || it.all { char -> char.isDigit() || char == '.' }) baseSalary = it }, Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), label = { Text("Gaji Pokok") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(allowance, { if (it.isEmpty() || it.all { char -> char.isDigit() || char == '.' }) allowance = it }, Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), label = { Text("Tunjangan") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(overtime, { if (it.isEmpty() || it.all { char -> char.isDigit() || char == '.' }) overtime = it }, Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), label = { Text("Jam Lembur") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(deductions, { if (it.isEmpty() || it.all { char -> char.isDigit() || char == '.' }) deductions = it }, Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), label = { Text("Potongan Lainnya") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                Text("* Bonus KPI dan Potongan BPJS (Kes, JHT, JP) dihitung otomatis", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                if (message != null) Text(message!!, color = Green, style = MaterialTheme.typography.bodySmall)
                Button(onClick = {
                    vm.generate(empId, empName, baseSalary.toDoubleOrNull() ?: 0.0, allowance.toDoubleOrNull() ?: 0.0, overtime.toDoubleOrNull() ?: 0.0, deductions.toDoubleOrNull() ?: 0.0)
                }, Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = Blue), enabled = empId.isNotEmpty()) { Text("Generate Payroll") }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("Riwayat Payroll", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 18.dp))
        Spacer(Modifier.height(8.dp))
        payrolls.forEach { p ->
            SlipCard(p, user)
            
            // Finance Action Buttons
            if (p.status == Constants.PayrollStatus.DRAFT) {
                Button(onClick = { vm.requestApproval(p.payrollId, user.userId) }, Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 4.dp)) { Text("Ajukan ke Manajer") }
            } else if (p.status == Constants.PayrollStatus.REJECTED) {
                Surface(
                    Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = RedSoft
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Ditolak oleh Manajer:", style = MaterialTheme.typography.labelSmall, color = Red)
                        Text(p.rejectionReason.ifEmpty { "Perlu perbaikan data lembur / potongan." }, style = MaterialTheme.typography.bodySmall, color = Red)
                    }
                }
                Button(
                    onClick = { vm.requestApproval(p.payrollId, user.userId) },
                    Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Orange)
                ) {
                    Text("Ajukan Ulang ke Manajer")
                }
            } else if (p.status == Constants.PayrollStatus.APPROVED) {
                Button(onClick = { vm.finalizePayroll(p.payrollId, user.userId) }, Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 4.dp)) { Text("Finalisasi") }
            } else if (p.status == Constants.PayrollStatus.FINALIZED) {
                Button(onClick = { vm.markPayrollAsPaid(p.payrollId, user.userId) }, Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 4.dp), colors = ButtonDefaults.buttonColors(containerColor = Green)) { Text("Tandai Sudah Dibayar") }
            }
            
            Spacer(Modifier.height(8.dp))
        }
        Spacer(Modifier.height(100.dp))
    }
}

@Composable
fun SalarySlipScreen(user: User, onBack: () -> Unit = {}, vm: PayrollViewModel = viewModel()) {
    val payrolls by vm.payrolls.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // Load company info from first active office location
    var companyInfo by remember { mutableStateOf(com.ptniger.hris.utils.PdfSlipGenerator.CompanyInfo()) }
    LaunchedEffect(Unit) {
        vm.loadSlipForUser(user.employeeId, user.userId)
        try {
            val offices = com.ptniger.hris.data.repository.OfficeLocationRepository().getActiveLocations()
            val office = offices.firstOrNull()
            if (office != null && office.companyName.isNotEmpty()) {
                companyInfo = com.ptniger.hris.utils.PdfSlipGenerator.CompanyInfo(
                    name = office.companyName,
                    address = office.companyAddress,
                    phone = office.companyPhone,
                    email = office.companyEmail,
                    npwp = office.companyNpwp
                )
            }
        } catch (_: Exception) {}
    }

    Column(Modifier.fillMaxSize().background(Background).statusBarsPadding().verticalScroll(rememberScrollState())) {
        Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 64.dp, top = 14.dp, bottom = 10.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            Text("Slip Gaji", style = MaterialTheme.typography.headlineMedium)
        }
        if (payrolls.isEmpty()) Text("Belum ada slip gaji", Modifier.padding(18.dp), style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        payrolls.forEach { p ->
            SlipCard(p, user)
            // Download PDF Button
            if (p.status == Constants.PayrollStatus.PAID || p.status == Constants.PayrollStatus.FINALIZED || p.status == Constants.PayrollStatus.APPROVED) {
                Button(
                    onClick = {
                        com.ptniger.hris.utils.PdfSlipGenerator.generateAndOpen(
                            context = context,
                            payroll = p,
                            employeeName = p.employeeName.ifEmpty { user.fullName.ifEmpty { user.name } },
                            nik = user.nik,
                            position = "",
                            department = user.departmentId,
                            company = companyInfo,
                            signatures = com.ptniger.hris.utils.PdfSlipGenerator.SignatureInfo(
                                employeeName = p.employeeName.ifEmpty { user.name },
                                managerName = "Manager",
                                hrName = "HRD"
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp).height(44.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Blue)
                ) {
                    Icon(Icons.Default.Download, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Unduh Slip Gaji (PDF)")
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        Spacer(Modifier.height(100.dp))
    }
}

@Composable
private fun SlipCard(p: com.ptniger.hris.data.model.Payroll, user: User) {
    val fmt = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    val name = p.employeeName.ifEmpty { user.name }
    Surface(Modifier.fillMaxWidth().padding(horizontal = 18.dp), shape = RoundedCornerShape(24.dp), color = Surface, shadowElevation = 1.dp) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Slip Gaji · ${com.ptniger.hris.utils.DateUtils.formatMonthYear(p.month, p.year)}", style = MaterialTheme.typography.titleSmall)
                Surface(shape = RoundedCornerShape(999.dp), color = if (p.status == "paid") GreenSoft else BlueSoft) {
                    Text(p.status.uppercase(), Modifier.padding(horizontal = 8.dp, vertical = 3.dp), style = MaterialTheme.typography.labelSmall, color = if (p.status == "paid") Green else Blue)
                }
            }
            Text(name, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            SlipRow("Gaji Pokok", fmt.format(p.baseSalary), TextPrimary)
            SlipRow("Tunjangan", fmt.format(p.allowance), TextPrimary)
            SlipRow("Lembur (${p.overtimeHours} jam)", fmt.format(p.overtimePay), TextPrimary)
            SlipRow("Bonus KPI (Skor: ${String.format("%.1f", p.kpiScore)})", fmt.format(p.kpiBonus), Purple)
            
            val totalBpjs = p.bpjsKesehatan + p.bpjsJht + p.bpjsJp
            SlipRow("Potongan BPJS (Kes/JHT/JP)", "-${fmt.format(totalBpjs)}", Red)
            SlipRow("Potongan Lainnya", "-${fmt.format(p.deductions)}", Red)
            
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total Bersih", style = MaterialTheme.typography.titleSmall)
                Text(fmt.format(p.netSalary), style = MaterialTheme.typography.titleSmall, color = Green)
            }
        }
    }
}

@Composable
private fun SlipRow(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        Text(value, style = MaterialTheme.typography.bodySmall, color = color)
    }
}


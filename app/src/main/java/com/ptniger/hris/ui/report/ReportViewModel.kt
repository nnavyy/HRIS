package com.ptniger.hris.ui.report

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ptniger.hris.data.repository.AttendanceRepository
import com.ptniger.hris.data.repository.LeaveRepository
import com.ptniger.hris.data.repository.PayrollRepository
import com.ptniger.hris.data.repository.KpiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import java.io.OutputStream

class ReportViewModel : ViewModel() {
    private val attendanceRepo = AttendanceRepository()
    private val payrollRepo = PayrollRepository()
    private val leaveRepo = LeaveRepository()
    private val kpiRepo = KpiRepository()
    
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // ──────────────────────────────────────────
    // Helper: write CSV content to Downloads folder
    // Handles Android 10+ (MediaStore) and older (FileWriter)
    // ──────────────────────────────────────────
    private fun writeCsvToDownloads(context: Context, fileName: String, content: String): String {
        val file = File(context.cacheDir, fileName)
        file.writeText(content)
        val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(android.content.Intent.EXTRA_SUBJECT, fileName)
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = android.content.Intent.createChooser(intent, "Bagikan Laporan")
        chooser.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
        return file.name
    }

    fun downloadAttendanceReport(context: Context, month: Int, year: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val data = attendanceRepo.getAllToday()
                val sb = StringBuilder()
                sb.appendLine("EmployeeID,Date,CheckIn,CheckOut,Status,LateMinutes,ValidationStatus")
                for (att in data) {
                    sb.appendLine("${att.employeeId},${att.date},${att.checkIn},${att.checkOut},${att.attendanceStatus},${att.lateMinutes},${att.validationStatus}")
                }
                val fileName = "Laporan_Absensi_${System.currentTimeMillis()}.csv"
                writeCsvToDownloads(context, fileName, sb.toString())
                _message.value = "✓ Laporan siap dibagikan: $fileName"
            } catch (e: Exception) {
                _message.value = "Gagal: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun downloadPayrollReport(context: Context, month: Int, year: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val data = payrollRepo.getByPeriod(month, year)
                val sb = StringBuilder()
                sb.appendLine("PayrollID,EmployeeName,Month,Year,BaseSalary,NetSalary,Status")
                for (p in data) {
                    sb.appendLine("${p.payrollId},${p.employeeName},${p.month},${p.year},${p.baseSalary},${p.netSalary},${p.status}")
                }
                val fileName = "Laporan_Payroll_${month}_${year}.csv"
                writeCsvToDownloads(context, fileName, sb.toString())
                _message.value = "✓ Laporan siap dibagikan: $fileName"
            } catch (e: Exception) {
                _message.value = "Gagal: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun downloadLeaveReport(context: Context, month: Int, year: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Use getAll() to fetch all leave requests (not just pending)
                val data = leaveRepo.getAll()
                val sb = StringBuilder()
                sb.appendLine("LeaveID,EmployeeName,Type,StartDate,EndDate,Duration,Status")
                for (l in data) {
                    sb.appendLine("${l.leaveId},${l.employeeName},${l.type},${l.startDate},${l.endDate},${l.duration},${l.status}")
                }
                val fileName = "Laporan_Cuti_${month}_${year}.csv"
                writeCsvToDownloads(context, fileName, sb.toString())
                _message.value = "✓ Berhasil disimpan ke Downloads: $fileName"
            } catch (e: Exception) {
                _message.value = "Gagal: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun downloadKpiReport(context: Context, month: Int, year: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val period = "${year}-${String.format("%02d", month)}"
                val data = kpiRepo.getScoresByPeriod(period)
                val sb = StringBuilder()
                sb.appendLine("ScoreID,EmployeeID,KPIName,Score,Weight,WeightedScore,Period")
                for (k in data) {
                    sb.appendLine("${k.scoreId},${k.employeeId},${k.kpiName},${k.score},${k.weight},${k.weightedScore},${k.period}")
                }
                val fileName = "Laporan_KPI_${month}_${year}.csv"
                writeCsvToDownloads(context, fileName, sb.toString())
                _message.value = "✓ Berhasil disimpan ke Downloads: $fileName"
            } catch (e: Exception) {
                _message.value = "Gagal: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun clearMessage() { _message.value = null }
}

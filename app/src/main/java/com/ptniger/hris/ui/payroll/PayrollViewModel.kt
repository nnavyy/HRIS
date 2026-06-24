package com.ptniger.hris.ui.payroll

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ptniger.hris.data.model.Payroll
import com.ptniger.hris.data.repository.PayrollRepository
import com.ptniger.hris.utils.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PayrollViewModel : ViewModel() {
    private val repo = PayrollRepository()
    private val _payrolls = MutableStateFlow<List<Payroll>>(emptyList())
    val payrolls: StateFlow<List<Payroll>> = _payrolls
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    fun loadAll() { viewModelScope.launch { _payrolls.value = repo.getAll() } }
    fun loadByEmployee(empId: String) { viewModelScope.launch { _payrolls.value = repo.getByEmployee(empId) } }

    fun generate(empId: String, empName: String, base: Double, allow: Double, otHours: Double, ded: Double) {
        viewModelScope.launch {
            val kpiScore = com.ptniger.hris.data.repository.KpiRepository().getTotalWeightedScore(empId, DateUtils.currentPeriod())
            val kpiBonus = com.ptniger.hris.utils.KpiCalculator.calculateKpiBonus(base, kpiScore)
            
            val otPay = com.ptniger.hris.utils.PayrollCalculator.calculateOvertime(base, allow, otHours)
            val bpjsKes = com.ptniger.hris.utils.PayrollCalculator.calculateBpjsKesehatan(base, allow)
            val bpjsJht = com.ptniger.hris.utils.PayrollCalculator.calculateBpjsJht(base, allow)
            val bpjsJp = com.ptniger.hris.utils.PayrollCalculator.calculateBpjsJp(base, allow)
            
            val net = com.ptniger.hris.utils.PayrollCalculator.calculateNetSalary(base, allow, otPay, kpiBonus, bpjsKes, bpjsJht, bpjsJp, ded)
            
            val payroll = Payroll(
                employeeId = empId, employeeName = empName, month = DateUtils.currentMonth(), year = DateUtils.currentYear(),
                baseSalary = base, allowance = allow, overtimeHours = otHours, overtimePay = otPay, kpiScore = kpiScore, kpiBonus = kpiBonus,
                bpjsKesehatan = bpjsKes, bpjsJht = bpjsJht, bpjsJp = bpjsJp, deductions = ded, netSalary = net
            )
            
            repo.generateRaw(payroll).fold(
                onSuccess = { _message.value = "Payroll berhasil di-generate (BPJS & Lembur otomatis)"; loadAll() },
                onFailure = { _message.value = "Error: ${it.message}" }
            )
        }
    }

    fun requestApproval(payrollId: String, financeId: String) {
        viewModelScope.launch {
            repo.requestApproval(payrollId, financeId).fold(
                onSuccess = { _message.value = "Persetujuan diajukan"; loadAll() },
                onFailure = { _message.value = "Error: ${it.message}" }
            )
        }
    }

    fun getPendingApprovals(managerId: String) {
        viewModelScope.launch {
            _payrolls.value = repo.getPendingApprovals(managerId)
        }
    }

    fun processApproval(payrollId: String, managerId: String, isApproved: Boolean, notes: String) {
        viewModelScope.launch {
            repo.processApproval(payrollId, managerId, isApproved, notes).fold(
                onSuccess = { _message.value = if(isApproved) "Disetujui" else "Ditolak"; getPendingApprovals(managerId) },
                onFailure = { _message.value = "Error: ${it.message}" }
            )
        }
    }

    fun finalizePayroll(payrollId: String, financeId: String) {
        viewModelScope.launch {
            repo.finalizePayroll(payrollId, financeId).fold(
                onSuccess = { _message.value = "Payroll difinalisasi"; loadAll() },
                onFailure = { _message.value = "Error: ${it.message}" }
            )
        }
    }

    fun markPayrollAsPaid(payrollId: String, financeId: String) {
        viewModelScope.launch {
            repo.markPayrollAsPaid(payrollId, financeId).fold(
                onSuccess = { _message.value = "Payroll ditandai dibayar"; loadAll() },
                onFailure = { _message.value = "Error: ${it.message}" }
            )
        }
    }

    fun clearMessage() { _message.value = null }
}

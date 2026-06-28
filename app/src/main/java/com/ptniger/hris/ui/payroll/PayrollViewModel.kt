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
    private val empRepo = com.ptniger.hris.data.repository.EmployeeRepository()
    private val _payrolls = MutableStateFlow<List<Payroll>>(emptyList())
    val payrolls: StateFlow<List<Payroll>> = _payrolls
    private val _employees = MutableStateFlow<List<com.ptniger.hris.data.model.Employee>>(emptyList())
    val employees: StateFlow<List<com.ptniger.hris.data.model.Employee>> = _employees
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    fun loadAll() { viewModelScope.launch { _payrolls.value = repo.getAll(); _employees.value = empRepo.getAll() } }
    fun loadByEmployee(empId: String) { viewModelScope.launch { _payrolls.value = repo.getByEmployee(empId) } }

    fun getAutoOvertimeHours(empId: String, onResult: (Double) -> Unit) {
        viewModelScope.launch {
            val attendances = com.ptniger.hris.data.repository.AttendanceRepository().getMonthlyAttendance(empId, DateUtils.currentMonth(), DateUtils.currentYear())
            onResult(attendances.sumOf { it.overtimeHours })
        }
    }

    fun loadSlipForUser(employeeId: String, authUid: String) {
        viewModelScope.launch {
            // Try with employeeId first
            if (employeeId.isNotEmpty()) {
                val results = repo.getByEmployee(employeeId)
                if (results.isNotEmpty()) {
                    _payrolls.value = results
                    return@launch
                }
            }
            // Fallback: find employee document by userId field, then query payroll by that doc ID
            val allEmployees = empRepo.getAll()
            val employee = allEmployees.find { it.userId == authUid }
            if (employee != null) {
                val results = repo.getByEmployee(employee.employeeId)
                if (results.isNotEmpty()) {
                    _payrolls.value = results
                    return@launch
                }
            }
            // Last fallback: try authUid directly
            _payrolls.value = repo.getByEmployee(authUid)
        }
    }

    fun generate(empId: String, empName: String, base: Double, allow: Double, manualOtHours: Double, ded: Double) {
        viewModelScope.launch {
            val employeeRepo = com.ptniger.hris.data.repository.EmployeeRepository()
            val attendanceRepo = com.ptniger.hris.data.repository.AttendanceRepository()
            val allEmployees = employeeRepo.getAll()
            val employee = allEmployees.find { it.employeeId == empId || it.nik == empId }
            val realEmpId = employee?.employeeId ?: empId
            val managerId = employee?.managerId ?: ""

            // Calculate auto overtime if not manually specified
            var finalOtHours = manualOtHours
            if (finalOtHours <= 0.0) {
                val attendances = attendanceRepo.getMonthlyAttendance(realEmpId, DateUtils.currentMonth(), DateUtils.currentYear())
                finalOtHours = attendances.sumOf { it.overtimeHours }
            }

            val kpiScore = com.ptniger.hris.data.repository.KpiRepository().getTotalWeightedScore(realEmpId, DateUtils.currentPeriod())
            val kpiBonus = com.ptniger.hris.utils.KpiCalculator.calculateKpiBonus(base, kpiScore)
            
            val otPay = com.ptniger.hris.utils.PayrollCalculator.calculateOvertime(base, allow, finalOtHours)
            val bpjsKes = com.ptniger.hris.utils.PayrollCalculator.calculateBpjsKesehatan(base, allow)
            val bpjsJht = com.ptniger.hris.utils.PayrollCalculator.calculateBpjsJht(base, allow)
            val bpjsJp = com.ptniger.hris.utils.PayrollCalculator.calculateBpjsJp(base, allow)
            
            val net = com.ptniger.hris.utils.PayrollCalculator.calculateNetSalary(base, allow, otPay, kpiBonus, bpjsKes, bpjsJht, bpjsJp, ded)
            
            val payroll = Payroll(
                employeeId = realEmpId, employeeName = empName, month = DateUtils.currentMonth(), year = DateUtils.currentYear(),
                baseSalary = base, allowance = allow, overtimeHours = finalOtHours, overtimePay = otPay, kpiScore = kpiScore, kpiBonus = kpiBonus,
                bpjsKesehatan = bpjsKes, bpjsJht = bpjsJht, bpjsJp = bpjsJp, deductions = ded, netSalary = net,
                managerId = managerId
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

    fun getTeamPayrolls(managerId: String) {
        viewModelScope.launch {
            _payrolls.value = repo.getTeamPayrolls(managerId)
        }
    }

    fun processApproval(payrollId: String, managerId: String, isApproved: Boolean, notes: String) {
        viewModelScope.launch {
            repo.processApproval(payrollId, managerId, isApproved, notes).fold(
                onSuccess = { _message.value = if(isApproved) "Disetujui" else "Ditolak"; getTeamPayrolls(managerId) },
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

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

    fun generate(empId: String, empName: String, base: Double, allow: Double, ot: Double, ded: Double) {
        viewModelScope.launch {
            repo.generate(empId, empName, DateUtils.currentMonth(), DateUtils.currentYear(), base, allow, ot, ded).fold(
                onSuccess = { _message.value = "Payroll berhasil di-generate (KPI bonus otomatis)"; loadAll() },
                onFailure = { _message.value = "Error: ${it.message}" }
            )
        }
    }
}

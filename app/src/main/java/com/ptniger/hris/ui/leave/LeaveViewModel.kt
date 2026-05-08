package com.ptniger.hris.ui.leave

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ptniger.hris.data.model.LeaveRequest
import com.ptniger.hris.data.repository.LeaveRepository
import com.ptniger.hris.data.repository.NotificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LeaveViewModel : ViewModel() {
    private val repo = LeaveRepository()
    private val notifRepo = NotificationRepository()
    private val _leaves = MutableStateFlow<List<LeaveRequest>>(emptyList())
    val leaves: StateFlow<List<LeaveRequest>> = _leaves
    private val _pending = MutableStateFlow<List<LeaveRequest>>(emptyList())
    val pending: StateFlow<List<LeaveRequest>> = _pending
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun loadByEmployee(empId: String) { viewModelScope.launch { _leaves.value = repo.getByEmployee(empId) } }
    
    fun loadByEmployeeWithFallback(empId: String, userId: String) {
        viewModelScope.launch {
            var list = repo.getByEmployee(empId)
            if (list.isEmpty() && empId != userId) {
                list = repo.getByEmployee(userId)
            }
            _leaves.value = list
        }
    }

    fun loadPending() { viewModelScope.launch { _pending.value = repo.getPending() } }

    fun submitWithQuotaUpdate(leave: LeaveRequest, userId: String, employeeDocId: String, duration: Int, currentQuota: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            repo.submit(leave).fold(
                onSuccess = { 
                    _message.value = "Pengajuan cuti berhasil dikirim"
                    loadByEmployeeWithFallback(leave.employeeId, userId)
                    
                    if (employeeDocId.isNotEmpty()) {
                        val empRepo = com.ptniger.hris.data.repository.EmployeeRepository()
                        val newQuota = (currentQuota - duration).coerceAtLeast(0)
                        empRepo.updateLeaveQuota(employeeDocId, newQuota)
                    }
                },
                onFailure = { _message.value = "Error: ${it.message}" }
            )
            _isLoading.value = false
        }
    }

    fun submit(leave: LeaveRequest) {
        viewModelScope.launch {
            _isLoading.value = true
            repo.submit(leave).fold(
                onSuccess = { _message.value = "Pengajuan cuti berhasil dikirim"; loadByEmployee(leave.employeeId) },
                onFailure = { _message.value = "Error: ${it.message}" }
            )
            _isLoading.value = false
        }
    }

    fun approve(leaveId: String, approver: String) {
        viewModelScope.launch {
            repo.approve(leaveId, approver).fold(
                onSuccess = { _message.value = "Cuti disetujui"; loadPending() },
                onFailure = { _message.value = "Error: ${it.message}" }
            )
        }
    }

    fun reject(leaveId: String, approver: String) {
        viewModelScope.launch {
            repo.reject(leaveId, approver).fold(
                onSuccess = { _message.value = "Cuti ditolak"; loadPending() },
                onFailure = { _message.value = "Error: ${it.message}" }
            )
        }
    }

    fun clearMessage() { _message.value = null }
}

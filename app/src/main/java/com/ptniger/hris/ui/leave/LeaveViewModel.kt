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

    fun loadPending(departmentId: String = "") { viewModelScope.launch { _pending.value = repo.getPending(departmentId) } }

    fun loadPendingForApprover(user: com.ptniger.hris.data.model.User) {
        viewModelScope.launch {
            val role = user.primaryRole.ifEmpty { user.role }
            val empRepo = com.ptniger.hris.data.repository.EmployeeRepository()
            val allEmployees = empRepo.getAll()
            val managerEmp = allEmployees.find { it.userId == user.userId || it.employeeId == user.employeeId }
            val empId = managerEmp?.employeeId ?: user.employeeId
            val subordinateIds = com.ptniger.hris.utils.HierarchyHelper.getSubordinateEmployeeIds(user, managerEmp, allEmployees)
            val dept = managerEmp?.department?.ifEmpty { user.departmentId } ?: user.departmentId

            _pending.value = repo.getPendingForApprover(
                currentUserEmployeeId = empId,
                currentRole = role,
                managerUserId = user.userId,
                departmentId = dept,
                subordinateIds = subordinateIds
            )
        }
    }

    fun loadPendingForApprover(employeeId: String, role: String) {
        viewModelScope.launch {
            _pending.value = repo.getPendingForApprover(employeeId, role)
        }
    }

    fun submitWithQuotaUpdate(leave: LeaveRequest, userId: String, employeeDocId: String, duration: Int, currentQuota: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            repo.submit(leave).fold(
                onSuccess = { 
                    _message.value = "Pengajuan cuti berhasil dikirim"
                    loadByEmployeeWithFallback(leave.employeeId, userId)
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

    fun approve(leaveId: String, approverUser: com.ptniger.hris.data.model.User) {
        viewModelScope.launch {
            repo.approve(leaveId, approverUser.userId).fold(
                onSuccess = { _message.value = "Pengajuan cuti disetujui"; loadPendingForApprover(approverUser) },
                onFailure = { _message.value = "Error: ${it.message}" }
            )
        }
    }

    fun reject(leaveId: String, approverUser: com.ptniger.hris.data.model.User, reason: String = "") {
        viewModelScope.launch {
            repo.reject(leaveId, approverUser.userId, reason).fold(
                onSuccess = { _message.value = "Pengajuan cuti ditolak"; loadPendingForApprover(approverUser) },
                onFailure = { _message.value = "Error: ${it.message}" }
            )
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

    fun reject(leaveId: String, approver: String, reason: String = "") {
        viewModelScope.launch {
            repo.reject(leaveId, approver, reason).fold(
                onSuccess = { _message.value = "Cuti ditolak"; loadPending() },
                onFailure = { _message.value = "Error: ${it.message}" }
            )
        }
    }

    fun clearMessage() { _message.value = null }
}

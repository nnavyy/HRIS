package com.ptniger.hris.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ptniger.hris.data.repository.*
import com.ptniger.hris.utils.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DashboardViewModel : ViewModel() {
    private val employeeRepo = EmployeeRepository()
    private val attendanceRepo = AttendanceRepository()
    private val leaveRepo = LeaveRepository()
    private val kpiRepo = KpiRepository()
    private val auditRepo = AuditLogRepository()
    private val notifRepo = NotificationRepository()

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state

    fun loadHrDashboard() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val empCount = employeeRepo.getCount()
            val pendingLeave = leaveRepo.getPendingCount()
            val presentToday = attendanceRepo.getTodayPresentCount()
            _state.value = DashboardState(
                totalEmployees = empCount, pendingApprovals = pendingLeave,
                presentToday = presentToday, isLoading = false
            )
        }
    }

    fun loadFinanceDashboard() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val empCount = employeeRepo.getCount()
            _state.value = DashboardState(totalEmployees = empCount, isLoading = false)
        }
    }

    fun loadManagerDashboard(userId: String, department: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val allEmployees = employeeRepo.getAll()
            
            // Team members: bawahan langsung (managerId == userId) ATAU se-departemen
            val teamMembers = allEmployees.filter { 
                it.managerId == userId || (department.isNotEmpty() && it.department.equals(department, ignoreCase = true)) 
            }
            
            // Ambil employeeId manager ini dari Firestore
            val managerEmployee = employeeRepo.getByUserId(userId)
            val managerEmpId = managerEmployee?.employeeId ?: ""

            val pendingLeave = if (managerEmpId.isNotEmpty()) {
                leaveRepo.getPendingCountByManagerId(managerEmpId)
            } else {
                leaveRepo.getPendingCount(department)
            }
            
            val allPresentToday = attendanceRepo.getAllToday()
            val teamMemberIds = teamMembers.map { it.employeeId }.toSet()
            val presentToday = allPresentToday.count { it.employeeId in teamMemberIds }
            _state.value = DashboardState(
                totalEmployees = teamMembers.size, pendingApprovals = pendingLeave,
                presentToday = presentToday, isLoading = false
            )
        }
    }

    fun loadAdminDashboard() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val authRepo = AuthRepository()
            val userCount = authRepo.getAllUsers().size
            val empCount = employeeRepo.getCount()
            val auditCount = auditRepo.getCount()
            val rules = auditRepo.getRules()
            _state.value = DashboardState(
                totalEmployees = empCount, totalUsers = userCount, auditEvents = auditCount,
                automationRules = rules.size, isLoading = false
            )
        }
    }

    fun loadEmployeeDashboard(userId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val employee = employeeRepo.getByUserId(userId)
            val todayAtt = employee?.let { attendanceRepo.getTodayAttendance(it.employeeId) }
            val unread = notifRepo.getUnreadCount(userId)
            val kpiScore = employee?.let {
                kpiRepo.getTotalWeightedScore(it.employeeId, DateUtils.currentPeriod())
            } ?: 0.0
            _state.value = DashboardState(
                leaveQuota = employee?.leaveQuota ?: 0,
                presentToday = if (todayAtt != null) 1 else 0,
                checkInTime = todayAtt?.checkIn ?: "",
                unreadNotifications = unread,
                kpiScore = kpiScore,
                isLoading = false
            )
        }
    }
}

data class DashboardState(
    val isLoading: Boolean = false,
    val totalEmployees: Int = 0,
    val totalUsers: Int = 0,
    val pendingApprovals: Int = 0,
    val presentToday: Int = 0,
    val leaveQuota: Int = 0,
    val checkInTime: String = "",
    val unreadNotifications: Int = 0,
    val auditEvents: Int = 0,
    val automationRules: Int = 0,
    val kpiScore: Double = 0.0
)

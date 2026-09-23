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
    private val payrollRepo = PayrollRepository()

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

    fun loadManagerDashboard(user: com.ptniger.hris.data.model.User) {
        loadManagerDashboard(user.userId, user.departmentId, user)
    }

    fun loadManagerDashboard(userId: String, department: String, userObj: com.ptniger.hris.data.model.User? = null) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val allEmployees = employeeRepo.getAll()
            val managerEmployee = employeeRepo.getByUserId(userId)
            val managerEmpId = managerEmployee?.employeeId ?: ""
            val managerUser = userObj ?: com.ptniger.hris.data.model.User(userId = userId, departmentId = department)

            // Menggunakan HierarchyHelper terpusat untuk mendeteksi seluruh bawahan tim
            val teamMembers = com.ptniger.hris.utils.HierarchyHelper.getSubordinates(managerUser, managerEmployee, allEmployees)
            val subordinateEmpIds = teamMembers.mapNotNull { it.employeeId.takeIf { id -> id.isNotEmpty() } }.toSet()
            val subordinateAllIds = com.ptniger.hris.utils.HierarchyHelper.getSubordinateIdentifiers(managerUser, managerEmployee, allEmployees)

            // 1. Cuti Pending untuk bawahan
            val pendingLeave = leaveRepo.getPendingCountByManagerId(
                managerEmployeeId = managerEmpId,
                managerUserId = userId,
                departmentId = department,
                subordinateIds = subordinateAllIds
            )

            // 2. Payroll Pending untuk bawahan
            val teamPayrolls = payrollRepo.getTeamPayrolls(
                managerEmployeeId = managerEmpId,
                managerUserId = userId,
                departmentId = department,
                subordinateEmpIds = subordinateEmpIds
            )
            val pendingPayrolls = teamPayrolls.count { it.status == com.ptniger.hris.utils.Constants.PayrollStatus.PENDING_APPROVAL }

            // 3. Kehadiran tim hari ini
            val allPresentToday = attendanceRepo.getAllToday()
            val presentToday = allPresentToday.count { it.employeeId in subordinateEmpIds }

            _state.value = DashboardState(
                totalEmployees = teamMembers.size,
                pendingApprovals = pendingLeave,
                pendingPayrolls = pendingPayrolls,
                presentToday = presentToday,
                isLoading = false
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
    val pendingPayrolls: Int = 0,
    val presentToday: Int = 0,
    val leaveQuota: Int = 0,
    val checkInTime: String = "",
    val unreadNotifications: Int = 0,
    val auditEvents: Int = 0,
    val automationRules: Int = 0,
    val kpiScore: Double = 0.0
)

package com.ptniger.hris.ui.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Dashboard : Screen("dashboard")
    object EmployeeList : Screen("employees")
    object EmployeeForm : Screen("employee_form/{id}") {
        fun createRoute(id: String = "new") = "employee_form/$id"
    }
    object EmployeeDetail : Screen("employee_detail/{id}") {
        fun createRoute(id: String) = "employee_detail/$id"
    }
    object Attendance : Screen("attendance")
    object AttendanceMonitor : Screen("attendance_monitor")
    object LeaveRequest : Screen("leave_request")
    object LeaveApproval : Screen("leave_approval")
    object KpiConfig : Screen("kpi_config")
    object KpiScoring : Screen("kpi_scoring")
    object KpiResult : Screen("kpi_result")
    object Payroll : Screen("payroll")
    object SalarySlip : Screen("salary_slip")
    object Report : Screen("report")
    object Notifications : Screen("notifications")
    object AuditLog : Screen("audit_log")
    object RoleManagement : Screen("role_management")
    object Automation : Screen("automation")
    object ManageAccounts : Screen("manage_accounts")
    object Profile : Screen("profile")
}

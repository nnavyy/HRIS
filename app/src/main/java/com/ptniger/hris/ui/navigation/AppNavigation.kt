package com.ptniger.hris.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.ptniger.hris.data.model.User
import com.ptniger.hris.ui.admin.AccountManagementScreen
import com.ptniger.hris.ui.admin.AutomationScreen
import com.ptniger.hris.ui.admin.RoleManagementScreen
import com.ptniger.hris.ui.admin.OfficeLocationScreen
import com.ptniger.hris.ui.attendance.AttendanceMonitorScreen
import com.ptniger.hris.ui.attendance.AttendanceScreen
import com.ptniger.hris.ui.audit.AuditLogScreen
import com.ptniger.hris.ui.auth.LoginScreen
import com.ptniger.hris.ui.dashboard.*
import com.ptniger.hris.ui.employee.EmployeeFormScreen
import com.ptniger.hris.ui.employee.EmployeeListScreen
import com.ptniger.hris.ui.kpi.KpiConfigScreen
import com.ptniger.hris.ui.kpi.KpiResultScreen
import com.ptniger.hris.ui.kpi.KpiScoringScreen
import com.ptniger.hris.ui.leave.LeaveApprovalScreen
import com.ptniger.hris.ui.leave.LeaveRequestScreen
import com.ptniger.hris.ui.notification.NotificationScreen
import com.ptniger.hris.ui.payroll.PayrollScreen
import com.ptniger.hris.ui.payroll.PayrollApprovalScreen
import com.ptniger.hris.ui.payroll.SalarySlipScreen
import com.ptniger.hris.ui.profile.ProfileScreen
import com.ptniger.hris.ui.report.ReportScreen
import com.ptniger.hris.ui.theme.*
import com.ptniger.hris.utils.Constants

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Login.route
) {
    var currentUser by remember { mutableStateOf<User?>(null) }
    var currentRoute by remember { mutableStateOf("dashboard") }

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = { user ->
                    currentUser = user
                    currentRoute = "dashboard"
                    navController.navigate("main") {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable("main") {
            val user = currentUser ?: return@composable
            MainScaffold(
                user = user,
                currentRoute = currentRoute,
                onNavigate = { route -> currentRoute = route },
                onLogout = {
                    currentUser = null
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToDetail = { route ->
                    currentRoute = route
                }
            )
        }
    }
}

@Composable
fun MainScaffold(
    user: User,
    currentRoute: String,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit,
    onNavigateToDetail: (String) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) {
                when (currentRoute) {
                    "dashboard" -> DashboardRouter(user = user, onNavigate = onNavigateToDetail)
                    "employees" -> EmployeeListScreen(user = user, onNavigateToForm = { id ->
                        onNavigateToDetail("employee_form_$id")
                    })
                    "leave_approval" -> LeaveApprovalScreen(user = user)
                    "leave_request" -> LeaveRequestScreen(user = user)
                    "attendance" -> AttendanceScreen(user = user)
                    "attendance_monitor" -> AttendanceMonitorScreen(user = user)
                    "kpi_config" -> KpiConfigScreen(user = user)
                    "kpi_scoring" -> KpiScoringScreen(user = user)
                    "kpi_result" -> KpiResultScreen(user = user)
                    "payroll" -> PayrollScreen(user = user)
                    "payroll_approval" -> PayrollApprovalScreen(user = user)
                    "salary_slip" -> SalarySlipScreen(user = user)
                    "report" -> ReportScreen(user = user)
                    "notifications" -> NotificationScreen(user = user)
                    "audit_log" -> AuditLogScreen(user = user)
                    "role_management" -> RoleManagementScreen(user = user)
                    "automation" -> AutomationScreen(user = user)
                    "office_locations" -> OfficeLocationScreen(user = user, onBack = { onNavigate("dashboard") })
                    "manage_accounts" -> AccountManagementScreen(user = user, onBack = { onNavigate("dashboard") })
                    "profile" -> ProfileScreen(user = user, onLogout = onLogout)
                    else -> {
                        if (currentRoute.startsWith("employee_form_")) {
                            val id = currentRoute.removePrefix("employee_form_")
                            EmployeeFormScreen(
                                employeeId = if (id == "new") null else id,
                                user = user,
                                onBack = { onNavigate("employees") }
                            )
                        } else {
                            DashboardRouter(user = user, onNavigate = onNavigateToDetail)
                        }
                    }
                }
            }
        }

        // Profile Avatar - Top Right (always visible except on profile/login)
        if (currentRoute != "profile") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(end = 18.dp, top = 12.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                Surface(
                    onClick = { onNavigate("profile") },
                    modifier = Modifier.size(40.dp),
                    shape = RoundedCornerShape(50),
                    shadowElevation = 4.dp,
                    color = Color.Transparent
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.linearGradient(listOf(Blue, Color(0xFF60A5FA)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (user.fullName.ifEmpty { user.name }).take(2).uppercase(),
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }

        // Bottom Nav Bar
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            BottomNavBar(
                roles = user.roles.ifEmpty { listOf(user.role) },
                currentRoute = currentRoute,
                onNavigate = onNavigate
            )
        }
    }
}

@Composable
fun DashboardRouter(user: User, onNavigate: (String) -> Unit) {
    val effectiveRole = user.primaryRole.ifEmpty { user.role }.ifEmpty { 
        user.roles.firstOrNull { it != Constants.Role.EMPLOYEE } ?: Constants.Role.EMPLOYEE 
    }
    when (effectiveRole) {
        Constants.Role.HR -> HrDashboardScreen(user = user, onNavigate = onNavigate)
        Constants.Role.FINANCE -> FinanceDashboardScreen(user = user, onNavigate = onNavigate)
        Constants.Role.MANAGER -> ManagerDashboardScreen(user = user, onNavigate = onNavigate)
        Constants.Role.SUPER_ADMIN -> AdminDashboardScreen(user = user, onNavigate = onNavigate)
        Constants.Role.EMPLOYEE -> EmployeeDashboardScreen(user = user, onNavigate = onNavigate)
        else -> EmployeeDashboardScreen(user = user, onNavigate = onNavigate)
    }
}

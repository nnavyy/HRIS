package com.ptniger.hris.utils

object RoleManager {

    data class NavItem(
        val route: String,
        val label: String,
        val icon: String
    )

    fun getNavItems(role: String): List<NavItem> {
        return when (role) {
            Constants.Role.HR -> listOf(
                NavItem("dashboard", "Home", "home"),
                NavItem("employees", "Karyawan", "people"),
                NavItem("leave_approval", "Cuti", "calendar"),
                NavItem("kpi_config", "KPI", "star"),
                NavItem("profile", "Profil", "person")
            )
            Constants.Role.FINANCE -> listOf(
                NavItem("dashboard", "Home", "home"),
                NavItem("payroll", "Payroll", "payments"),
                NavItem("report", "Laporan", "chart"),
                NavItem("audit_log", "Audit", "shield"),
                NavItem("profile", "Profil", "person")
            )
            Constants.Role.MANAGER -> listOf(
                NavItem("dashboard", "Home", "home"),
                NavItem("employees", "Tim", "people"),
                NavItem("attendance_monitor", "Absensi", "clock"),
                NavItem("leave_approval", "Cuti", "calendar"),
                NavItem("profile", "Profil", "person")
            )
            Constants.Role.SUPER_ADMIN -> listOf(
                NavItem("dashboard", "Home", "home"),
                NavItem("role_management", "Role", "admin"),
                NavItem("automation", "Auto", "settings"),
                NavItem("audit_log", "Audit", "shield"),
                NavItem("profile", "Profil", "person")
            )
            Constants.Role.EMPLOYEE -> listOf(
                NavItem("dashboard", "Home", "home"),
                NavItem("attendance", "Absensi", "clock"),
                NavItem("leave_request", "Cuti", "calendar"),
                NavItem("salary_slip", "Gaji", "payments"),
                NavItem("profile", "Profil", "person")
            )
            else -> emptyList()
        }
    }

    fun getRoleDisplayName(role: String): String {
        return when (role) {
            Constants.Role.HR -> "HR / Admin"
            Constants.Role.FINANCE -> "Finance"
            Constants.Role.MANAGER -> "Manager"
            Constants.Role.SUPER_ADMIN -> "Super Admin"
            Constants.Role.EMPLOYEE -> "Karyawan"
            else -> "Unknown"
        }
    }

    fun getRoleShort(role: String): String {
        return when (role) {
            Constants.Role.HR -> "HR"
            Constants.Role.FINANCE -> "FN"
            Constants.Role.MANAGER -> "MG"
            Constants.Role.SUPER_ADMIN -> "SA"
            Constants.Role.EMPLOYEE -> "KY"
            else -> "??"
        }
    }
}

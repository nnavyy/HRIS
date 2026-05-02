package com.ptniger.hris.utils

object RoleManager {

    data class NavItem(
        val route: String,
        val label: String,
        val icon: String
    )

    fun getNavItems(role: String): List<NavItem> = getNavItems(listOf(role))

    /**
     * Returns nav items for the bottom bar.
     * Profile is NOT included — it's handled as a top-right avatar.
     * Audit, Approval Cuti, Absensi Tim are also NOT in nav (moved to dashboard).
     * Max ~4-5 items per role to keep the navbar clean.
     */
    fun getNavItems(roles: List<String>): List<NavItem> {
        val items = mutableSetOf<NavItem>()
        
        // Home is always first
        items.add(NavItem("dashboard", "Home", "home"))

        roles.forEach { role ->
            when (role) {
                Constants.Role.HR -> {
                    items.add(NavItem("employees", "Karyawan", "people"))
                    items.add(NavItem("kpi_config", "KPI", "star"))
                }
                Constants.Role.FINANCE -> {
                    items.add(NavItem("payroll", "Payroll", "payments"))
                    items.add(NavItem("report", "Laporan", "chart"))
                }
                Constants.Role.MANAGER -> {
                    // "Tim" includes employee list + attendance monitor in one section
                    items.add(NavItem("employees", "Tim", "people"))
                }
                Constants.Role.SUPER_ADMIN -> {
                    items.add(NavItem("role_management", "Role", "admin"))
                    items.add(NavItem("automation", "Auto", "settings"))
                }
                Constants.Role.EMPLOYEE -> {
                    items.add(NavItem("attendance", "Absensi", "clock"))
                    items.add(NavItem("leave_request", "Cuti", "calendar"))
                    items.add(NavItem("salary_slip", "Gaji", "payments"))
                }
            }
        }
        
        // No profile in nav — it's now a top-right avatar
        return items.toList()
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

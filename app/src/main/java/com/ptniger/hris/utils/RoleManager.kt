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
     * MAX 4 ITEMS per role (Home + 3 role-specific).
     * Features not in navbar are accessible via Dashboard QuickAction cards.
     */
    fun getNavItems(roles: List<String>): List<NavItem> {
        val items = mutableListOf<NavItem>()
        items.add(NavItem("dashboard", "Home", "home"))

        val primary = roles.firstOrNull { it != Constants.Role.EMPLOYEE }
            ?: Constants.Role.EMPLOYEE

        when (primary) {
            Constants.Role.HR -> {
                // HR akses harian: lihat karyawan & approval cuti
                items.add(NavItem("employees", "Karyawan", "people"))
                items.add(NavItem("leave_approval", "Approval", "calendar"))
                items.add(NavItem("kpi_config", "KPI", "star"))
                // AI Review → Dashboard QuickAction
            }
            Constants.Role.FINANCE -> {
                items.add(NavItem("payroll", "Payroll", "payments"))
                items.add(NavItem("report", "Laporan", "chart"))
            }
            Constants.Role.MANAGER -> {
                // Manager akses harian: monitor tim & approval
                items.add(NavItem("attendance_monitor", "Tim", "people"))
                items.add(NavItem("leave_approval", "Approval", "calendar"))
                items.add(NavItem("kpi_scoring", "KPI", "star"))
                // AbsenKu, CutiKu, GajiKu (personal) → Dashboard "Menu Saya"
                // Peer Review & AI Review → Dashboard QuickAction
            }
            Constants.Role.SUPER_ADMIN -> {
                items.add(NavItem("role_management", "Role", "admin"))
                items.add(NavItem("automation", "Auto", "settings"))
            }
            Constants.Role.EMPLOYEE -> {
                items.add(NavItem("attendance", "Absensi", "clock"))
                items.add(NavItem("leave_request", "Cuti", "calendar"))
                items.add(NavItem("salary_slip", "Gaji", "payments"))
                // Kontrak → Dashboard notif badge
            }
        }

        return items
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

package com.ptniger.hris.utils

import com.ptniger.hris.data.model.Employee
import com.ptniger.hris.data.model.User

/**
 * Unified helper untuk menyelesaikan struktur organisasi,
 * hierarki jabatan, dan relasi manajer-bawahan di seluruh modul HRIS.
 */
object HierarchyHelper {

    /**
     * Memeriksa apakah seorang karyawan adalah bawahan dari manajer tertentu.
     * Mendukung relasi atasan langsung (managerId) maupun hierarki departemen (department head).
     */
    fun isSubordinateOf(
        employee: Employee,
        managerUser: User,
        managerEmployee: Employee?
    ): Boolean {
        val managerUserId = managerUser.userId.ifEmpty { managerUser.uid }
        val managerEmpId = managerEmployee?.employeeId ?: managerUser.employeeId

        // Karyawan tidak bisa menjadi bawahan dari dirinya sendiri
        if (employee.employeeId.isNotEmpty() && employee.employeeId == managerEmpId) return false
        if (employee.userId.isNotEmpty() && employee.userId == managerUserId) return false

        // 1. Relasi Langsung: managerId di dokumen Employee merujuk ke managerEmployeeId atau managerUserId
        if (employee.managerId.isNotEmpty()) {
            if (managerEmpId.isNotEmpty() && employee.managerId.equals(managerEmpId, ignoreCase = true)) {
                return true
            }
            if (managerUserId.isNotEmpty() && employee.managerId.equals(managerUserId, ignoreCase = true)) {
                return true
            }
        }

        // 2. Relasi Departemen: Jika manajer memiliki departemen, seluruh karyawan non-manajer di departemen tersebut adalah bawahan
        val managerDept = managerEmployee?.department?.ifEmpty { managerUser.departmentId } ?: managerUser.departmentId
        if (managerDept.isNotEmpty() && employee.department.equals(managerDept, ignoreCase = true)) {
            return true
        }

        return false
    }

    /**
     * Mengambil daftar seluruh karyawan yang merupakan bawahan dari manajer ini.
     */
    fun getSubordinates(
        managerUser: User,
        managerEmployee: Employee?,
        allEmployees: List<Employee>
    ): List<Employee> {
        return allEmployees.filter { isSubordinateOf(it, managerUser, managerEmployee) }
    }

    /**
     * Mengambil himpunan seluruh ID karyawan (employeeId) yang merupakan bawahan manajer.
     */
    fun getSubordinateEmployeeIds(
        managerUser: User,
        managerEmployee: Employee?,
        allEmployees: List<Employee>
    ): Set<String> {
        return getSubordinates(managerUser, managerEmployee, allEmployees)
            .mapNotNull { it.employeeId.takeIf { id -> id.isNotEmpty() } }
            .toSet()
    }

    /**
     * Mengambil himpunan seluruh ID pengenal (baik employeeId maupun userId) bawahan,
     * berguna untuk query Firestore yang mungkin menyimpan salah satu dari ID tersebut.
     */
    fun getSubordinateIdentifiers(
        managerUser: User,
        managerEmployee: Employee?,
        allEmployees: List<Employee>
    ): Set<String> {
        val subordinates = getSubordinates(managerUser, managerEmployee, allEmployees)
        val ids = mutableSetOf<String>()
        subordinates.forEach {
            if (it.employeeId.isNotEmpty()) ids.add(it.employeeId)
            if (it.userId.isNotEmpty()) ids.add(it.userId)
            if (it.nik.isNotEmpty()) ids.add(it.nik)
        }
        return ids
    }

    /**
     * Mencari Atasan / Manajer untuk seorang karyawan tertentu.
     * Jika karyawan belum memiliki managerId langsung, cari Manajer Departemen.
     */
    fun resolveManagerForEmployee(
        employee: Employee,
        allEmployees: List<Employee>,
        allUsers: List<User>
    ): Employee? {
        // 1. Coba cari berdasarkan managerId langsung
        if (employee.managerId.isNotEmpty()) {
            val direct = allEmployees.find {
                it.employeeId == employee.managerId || it.userId == employee.managerId
            }
            if (direct != null) return direct

            val userDirect = allUsers.find { it.userId == employee.managerId }
            if (userDirect != null) {
                val empByUser = allEmployees.find { it.userId == userDirect.userId }
                if (empByUser != null) return empByUser
            }
        }

        // 2. Fallback: Cari Manager di Departemen yang sama
        if (employee.department.isNotEmpty()) {
            val deptManager = allEmployees.find { emp ->
                emp.employeeId != employee.employeeId &&
                emp.department.equals(employee.department, ignoreCase = true) &&
                (emp.position.contains("Manager", ignoreCase = true) ||
                 emp.position.contains("Head", ignoreCase = true) ||
                 emp.position.contains("Lead", ignoreCase = true))
            }
            if (deptManager != null) return deptManager

            // Cari user dengan role MANAGER yang se-departemen
            val managerUser = allUsers.find { u ->
                u.userId != employee.userId &&
                (u.primaryRole == Constants.Role.MANAGER || u.role == Constants.Role.MANAGER || u.roles.contains(Constants.Role.MANAGER)) &&
                (u.departmentId.equals(employee.department, ignoreCase = true))
            }
            if (managerUser != null) {
                val emp = allEmployees.find { it.userId == managerUser.userId }
                if (emp != null) return emp
            }
        }

        return null
    }
}

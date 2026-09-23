package com.ptniger.hris.utils

import com.ptniger.hris.data.model.*
import com.ptniger.hris.data.repository.*
import java.util.Calendar

/**
 * Resolve presence status karyawan berdasarkan 3 layer:
 * 1. Status hari ini (dari Attendance)
 * 2. Cuti/izin yang disetujui (dari LeaveRequest)
 * 3. Jadwal kerja (dari WorkSchedule)
 *
 * Hasilnya adalah EmployeePresence — snapshot status kehadiran hari ini.
 */
object PresenceResolver {

    /**
     * Resolve presence status karyawan untuk hari ini.
     * Dipanggil dari:
     * - EmployeeDetailScreen (profil karyawan)
     * - ManagerDashboardScreen (team presence grid)
     * - EmployeeListScreen (badge per card)
     * - EmployeeDashboardScreen (status diri sendiri)
     */
    suspend fun resolveToday(
        employee: Employee,
        attendanceRepo: AttendanceRepository = AttendanceRepository(),
        leaveRepo: LeaveRepository = LeaveRepository(),
        scheduleRepo: WorkScheduleRepository = WorkScheduleRepository(),
        officeRepo: OfficeLocationRepository = OfficeLocationRepository()
    ): EmployeePresence {
        val today = DateUtils.today()  // "YYYY-MM-DD"
        val cal = Calendar.getInstance()
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)  // 1=Minggu, 2=Senin, ...

        // Fetch data
        val schedule = scheduleRepo.getForEmployee(employee.workScheduleId)
        val todayAttendance = attendanceRepo.getTodayAttendance(employee.employeeId)
        val officeName = officeRepo.getById(employee.officeId)?.name ?: "Kantor"

        // 1. Cek apakah hari ini hari kerja
        if (!schedule.workDays.contains(dayOfWeek)) {
            return buildPresence(employee, EmployeePresence(
                presenceStatus = PresenceStatus.OFF_DAY,
                officeName = officeName,
                workMode = "onsite"
            ))
        }

        // 2. Cek apakah sedang cuti/izin yang disetujui
        val approvedLeaveToday = leaveRepo.getApprovedLeaveForDate(employee.employeeId, today)
        if (approvedLeaveToday != null) {
            val status = when (approvedLeaveToday.type) {
                "sick"       -> PresenceStatus.SICK
                "permission" -> PresenceStatus.PERMISSION
                "emergency"  -> PresenceStatus.PERMISSION
                else         -> PresenceStatus.ON_LEAVE
            }
            return buildPresence(employee, EmployeePresence(
                presenceStatus = status,
                officeName = officeName,
                workMode = "leave"
            ))
        }

        // 3. Tentukan work mode hari ini (onsite / wfh)
        val isWfhDay = schedule.wfhDays.contains(dayOfWeek)
        val modeToday = if (isWfhDay) "wfh" else "onsite"

        // 4. Cek absensi hari ini
        if (todayAttendance != null && todayAttendance.checkIn.isNotEmpty()) {
            val status = when {
                isWfhDay && todayAttendance.lateMinutes > 0 -> PresenceStatus.LATE_ONSITE
                isWfhDay                                    -> PresenceStatus.PRESENT_WFH
                todayAttendance.lateMinutes > 0             -> PresenceStatus.LATE_ONSITE
                else                                        -> PresenceStatus.PRESENT_ONSITE
            }
            return buildPresence(employee, EmployeePresence(
                presenceStatus = status,
                workMode = modeToday,
                checkInTime = todayAttendance.checkIn,
                checkOutTime = todayAttendance.checkOut,
                lateMinutes = todayAttendance.lateMinutes,
                officeName = officeName
            ))
        }

        // 5. Belum check-in hari ini
        val nowHour = cal.get(Calendar.HOUR_OF_DAY)
        val workStartHour = schedule.workStartTime.substringBefore(":").toIntOrNull() ?: 8

        return buildPresence(employee, EmployeePresence(
            presenceStatus = if (nowHour >= workStartHour) PresenceStatus.NOT_YET else PresenceStatus.UNKNOWN,
            workMode = modeToday,
            officeName = officeName
        ))
    }

    /**
     * Resolve presence untuk banyak karyawan sekaligus (untuk team view).
     */
    suspend fun resolveTeam(employees: List<Employee>): List<EmployeePresence> {
        return employees.map { resolveToday(it) }
    }

    private fun buildPresence(employee: Employee, base: EmployeePresence) = base.copy(
        employeeId = employee.employeeId,
        employeeName = employee.name,
        employeePosition = employee.position,
        employeeDepartment = employee.department,
        officeId = employee.officeId,
        isFaceRegistered = employee.isFaceRegistered
    )
}

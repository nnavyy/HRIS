package com.ptniger.hris.utils

import com.google.firebase.firestore.FirebaseFirestore
import com.ptniger.hris.data.model.Attendance
import com.ptniger.hris.data.model.Employee
import com.ptniger.hris.data.model.LeaveRequest
import com.ptniger.hris.data.model.Payroll
import com.ptniger.hris.utils.Constants
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DummyDataSeeder {
    private val db = FirebaseFirestore.getInstance()

    private val demoEmployees = listOf(
        Employee(nik = "DEMO001", name = "Budi Santoso", email = "budi.demo@example.com", phone = "081234567890",
            position = "Senior Engineer", department = "Engineering", branch = "Kantor Pusat Jakarta",
            baseSalary = 12_000_000.0, employmentStatus = "active"),
        Employee(nik = "DEMO002", name = "Siti Rahayu", email = "siti.demo@example.com", phone = "081234567891",
            position = "Marketing Manager", department = "Marketing", branch = "Kantor Pusat Jakarta",
            baseSalary = 10_000_000.0, employmentStatus = "active"),
        Employee(nik = "DEMO003", name = "Ahmad Fauzi", email = "ahmad.demo@example.com", phone = "081234567892",
            position = "Finance Analyst", department = "Finance", branch = "Kantor Pusat Jakarta",
            baseSalary = 9_000_000.0, employmentStatus = "active"),
        Employee(nik = "DEMO004", name = "Dewi Permatasari", email = "dewi.demo@example.com", phone = "081234567893",
            position = "HR Specialist", department = "HR", branch = "Kantor Pusat Jakarta",
            baseSalary = 8_000_000.0, employmentStatus = "active"),
        Employee(nik = "DEMO005", name = "Rudi Hartono", email = "rudi.demo@example.com", phone = "081234567894",
            position = "IT Support", department = "IT", branch = "Kantor Cabang Bandung",
            baseSalary = 7_500_000.0, employmentStatus = "active"),
        Employee(nik = "DEMO006", name = "Maya Indah Lestari", email = "maya.demo@example.com", phone = "081234567895",
            position = "Marketing Executive", department = "Marketing", branch = "Kantor Cabang Surabaya",
            baseSalary = 8_500_000.0, employmentStatus = "active"),
        Employee(nik = "DEMO007", name = "Hendra Wijaya", email = "hendra.demo@example.com", phone = "081234567896",
            position = "Marketing Staff", department = "Marketing", branch = "Kantor Pusat Jakarta",
            baseSalary = 6_500_000.0, employmentStatus = "active"),
    )

    suspend fun seedAll(): String {
        return try {
            seedOfficeLocations()
            val empIds = seedEmployees()
            if (empIds.isEmpty()) return "Gagal memuat data karyawan."
            seedAttendance(empIds)
            seedLeaveRequests(empIds)
            seedPayroll(empIds)
            "✅ Data demo berhasil dimuat! ${empIds.size} karyawan, absensi 7 hari, cuti, dan payroll telah ditambahkan."
        } catch (e: Exception) {
            "❌ Gagal: ${e.message}"
        }
    }

    private suspend fun seedOfficeLocations() {
        val col = db.collection(Constants.Collections.OFFICE_LOCATIONS)
        val existing = col.get().await()
        if (!existing.isEmpty) return
        
        col.add(com.ptniger.hris.data.model.OfficeLocation(
            name = "Kantor Pusat Jakarta",
            latitude = -6.2088,
            longitude = 106.8456,
            allowedRadiusMeters = 200.0,
            isActive = true
        )).await()
        col.add(com.ptniger.hris.data.model.OfficeLocation(
            name = "Kantor Cabang Bandung",
            latitude = -6.9175,
            longitude = 107.6191,
            allowedRadiusMeters = 150.0,
            isActive = true
        )).await()
    }

    private suspend fun seedEmployees(): Map<String, String> {
        val empIds = mutableMapOf<String, String>() // nik -> docId
        val col = db.collection(Constants.Collections.EMPLOYEES)

        // Check if already seeded
        val existing = col.get().await()
        if (!existing.isEmpty) {
            existing.documents.forEach { doc ->
                val nik = doc.getString("nik")
                if (nik != null && nik.startsWith("DEMO")) {
                    empIds[nik] = doc.id
                }
            }
            if (empIds.size >= 7) {
                // Set managerId for Marketing staff (Maya & Hendra) to Siti Rahayu (DEMO002) if not already set
                val sitiId = empIds["DEMO002"]
                val mayaId = empIds["DEMO006"]
                val hendraId = empIds["DEMO007"]
                if (sitiId != null && mayaId != null && hendraId != null) {
                    col.document(mayaId).update("managerId", sitiId).await()
                    col.document(hendraId).update("managerId", sitiId).await()
                }
                return empIds // Already fully seeded
            }
        }

        demoEmployees.forEach { emp ->
            val ref = col.add(emp).await()
            empIds[emp.nik] = ref.id
        }

        // Set managerId for Marketing staff (Maya & Hendra) to Siti Rahayu (DEMO002)
        val sitiId = empIds["DEMO002"]
        val mayaId = empIds["DEMO006"]
        val hendraId = empIds["DEMO007"]
        if (sitiId != null && mayaId != null && hendraId != null) {
            col.document(mayaId).update("managerId", sitiId).await()
            col.document(hendraId).update("managerId", sitiId).await()
        }

        return empIds
    }

    private suspend fun seedAttendance(empIds: Map<String, String>) {
        if (empIds.isEmpty()) return
        val col = db.collection(Constants.Collections.ATTENDANCE)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()

        repeat(7) { dayOffset ->
            cal.time = Date()
            cal.add(Calendar.DAY_OF_MONTH, -dayOffset)
            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
            if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) return@repeat

            val dateStr = sdf.format(cal.time)
            empIds.values.forEachIndexed { idx, docId ->
                val isAbsent = (idx == 2 && dayOffset == 3)
                val isLate = (idx == 0 && dayOffset == 1)
                if (isAbsent) return@forEachIndexed

                val checkInHour = if (isLate) 9 else 8
                val checkInMin = if (isLate) 25 else (0..10).random()
                val status = if (isLate) Constants.AttendanceStatus.LATE else Constants.AttendanceStatus.PRESENT
                val checkInTime = String.format("%02d:%02d", checkInHour, checkInMin)
                val checkOutTime = String.format("17:%02d", (0..30).random())

                val att = Attendance(
                    employeeId = docId,
                    date = dateStr,
                    clockType = "clock_in",
                    checkIn = checkInTime,
                    checkOut = checkOutTime,
                    attendanceStatus = status,
                    validationStatus = "valid",
                    lateMinutes = if (isLate) 25 else 0,
                    isWithinOfficeRadius = true,
                    latitude = -6.2088,
                    longitude = 106.8456
                )
                col.add(att).await()
            }
        }
    }

    private suspend fun seedLeaveRequests(empIds: Map<String, String>) {
        if (empIds.isEmpty()) return
        val col = db.collection(Constants.Collections.LEAVE_REQUESTS)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()
        val empDocIds = empIds.values.toList()

        // Request 1: pending dari Budi
        cal.time = Date(); cal.add(Calendar.DAY_OF_MONTH, 3)
        val start1 = sdf.format(cal.time); cal.add(Calendar.DAY_OF_MONTH, 2)
        val end1 = sdf.format(cal.time)
        col.add(LeaveRequest(
            employeeId = empDocIds.getOrElse(0) { "" }, employeeName = "Budi Santoso",
            departmentId = "Engineering", type = "Cuti Tahunan",
            startDate = start1, endDate = end1, duration = 3,
            reason = "Liburan keluarga", status = Constants.LeaveStatus.PENDING
        )).await()

        // Request 2: approved dari Siti
        val today = sdf.format(Date())
        col.add(LeaveRequest(
            employeeId = empDocIds.getOrElse(1) { "" }, employeeName = "Siti Rahayu",
            departmentId = "Marketing", type = "Cuti Sakit",
            startDate = today, endDate = today, duration = 1,
            reason = "Demam tinggi", status = Constants.LeaveStatus.APPROVED, approvedBy = "System Demo"
        )).await()

        // Request 3: pending dari Maya
        cal.time = Date(); cal.add(Calendar.DAY_OF_MONTH, 7)
        val start3 = sdf.format(cal.time); cal.add(Calendar.DAY_OF_MONTH, 4)
        val end3 = sdf.format(cal.time)
        col.add(LeaveRequest(
            employeeId = empDocIds.getOrElse(5) { "" }, employeeName = "Maya Indah Lestari",
            departmentId = "Marketing", type = "Cuti Tahunan",
            startDate = start3, endDate = end3, duration = 5,
            reason = "Pernikahan saudara kandung", status = Constants.LeaveStatus.PENDING
        )).await()

        // Request 4: rejected dari Hendra
        cal.time = Date(); cal.add(Calendar.DAY_OF_MONTH, -5)
        val start4 = sdf.format(cal.time); cal.add(Calendar.DAY_OF_MONTH, 1)
        val end4 = sdf.format(cal.time)
        col.add(LeaveRequest(
            employeeId = empDocIds.getOrElse(6) { "" }, employeeName = "Hendra Wijaya",
            departmentId = "Marketing", type = "Izin",
            startDate = start4, endDate = end4, duration = 2,
            reason = "Keperluan keluarga", status = Constants.LeaveStatus.REJECTED
        )).await()
    }

    private suspend fun seedPayroll(empIds: Map<String, String>) {
        if (empIds.isEmpty()) return
        val col = db.collection("payrolls")
        val cal = Calendar.getInstance()
        val currentMonth = cal.get(Calendar.MONTH) + 1
        val currentYear = cal.get(Calendar.YEAR)

        val existing = col.whereEqualTo("month", currentMonth).whereEqualTo("year", currentYear).get().await()
        if (!existing.isEmpty) return

        val salaries = listOf(12_000_000.0, 10_000_000.0, 9_000_000.0, 8_000_000.0, 7_500_000.0, 8_500_000.0, 6_500_000.0)
        val names = listOf("Budi Santoso", "Siti Rahayu", "Ahmad Fauzi", "Dewi Permatasari", "Rudi Hartono", "Maya Indah Lestari", "Hendra Wijaya")

        empIds.values.forEachIndexed { idx, docId ->
            val base = salaries.getOrElse(idx) { 5_000_000.0 }
            val allowance = base * 0.1
            val bpjsKes = (base.coerceAtMost(12_000_000.0)) * 0.01
            val bpjsJht = base * 0.02
            val bpjsJp = (base.coerceAtMost(10_042_300.0)) * 0.01
            val totalDeductions = bpjsKes + bpjsJht + bpjsJp
            val overtimePay = if (idx == 0) (base / 173 * 1.5 * 4) else 0.0
            val kpiBonus = if (idx == 1) 500_000.0 else 0.0
            val net = base + allowance + overtimePay + kpiBonus - totalDeductions

            val employeeRepo = com.ptniger.hris.data.repository.EmployeeRepository()
            val employee = employeeRepo.getById(docId)
            val managerId = employee?.managerId ?: ""

            val payroll = Payroll(
                employeeId = docId,
                employeeName = names.getOrElse(idx) { "Demo Employee" },
                month = currentMonth, year = currentYear,
                baseSalary = base, allowance = allowance,
                overtimeHours = if (idx == 0) 4.0 else 0.0,
                overtimePay = overtimePay, kpiBonus = kpiBonus,
                bpjsKesehatan = bpjsKes, bpjsJht = bpjsJht, bpjsJp = bpjsJp,
                deductions = totalDeductions, netSalary = net,
                status = Constants.PayrollStatus.PENDING_APPROVAL,
                managerId = managerId
            )
            col.add(payroll).await()
        }
    }
}

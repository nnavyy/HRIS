package com.ptniger.hris.utils

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.*

/**
 * Firestore dummy data seeder untuk demo AI Review.
 * Berbeda dari DummyDataSeeder (data demo presentasi), ini seed data
 * yang lebih lengkap: KPI scores, peer reviews, attendance 3 bulan,
 * sehingga AI Review punya konteks yang kaya.
 */
object SeedDataManager {

    private val db = FirebaseFirestore.getInstance()

    // ── MAIN ENTRY POINT ─────────────────────────────────────────────────
    suspend fun seedAll(): SeedResult {
        val errors = mutableListOf<String>()
        var totalInserted = 0

        try { totalInserted += seedOfficeLocations() } catch (e: Exception) { errors.add("office: ${e.message}") }
        try { totalInserted += seedEmployees() }       catch (e: Exception) { errors.add("employees: ${e.message}") }
        try { totalInserted += seedWorkSchedules() }    catch (e: Exception) { errors.add("work_schedules: ${e.message}") }
        try { totalInserted += seedLeavePolicy() }      catch (e: Exception) { errors.add("leave_policy: ${e.message}") }
        try { totalInserted += seedAttendance() }      catch (e: Exception) { errors.add("attendance: ${e.message}") }
        try { totalInserted += seedLeaveRequests() }   catch (e: Exception) { errors.add("leave: ${e.message}") }
        try { totalInserted += seedKpiConfigs() }      catch (e: Exception) { errors.add("kpi_config: ${e.message}") }
        try { totalInserted += seedKpiScores() }       catch (e: Exception) { errors.add("kpi_scores: ${e.message}") }
        try { totalInserted += seedPeerReviews() }     catch (e: Exception) { errors.add("peer_reviews: ${e.message}") }
        try { totalInserted += seedPayrolls() }        catch (e: Exception) { errors.add("payrolls: ${e.message}") }
        try { totalInserted += seedAppConfigs() }      catch (e: Exception) { errors.add("app_configs: ${e.message}") }
        try { totalInserted += seedAutomationRules() } catch (e: Exception) { errors.add("automation_rules: ${e.message}") }
        try { totalInserted += seedNotifications() }   catch (e: Exception) { errors.add("notifications: ${e.message}") }

        return SeedResult(totalInserted, errors)
    }

    data class SeedResult(val inserted: Int, val errors: List<String>)

    // Employee IDs — konsisten antar collection
    private const val EMP_001 = "emp_budi_santoso"
    private const val EMP_002 = "emp_siti_rahayu"
    private const val EMP_003 = "emp_ahmad_fauzi"
    private const val EMP_004 = "emp_dewi_lestari"
    private const val EMP_005 = "emp_reza_pratama"
    private const val MGR_UID = "manager_uid_placeholder"
    private const val HR_UID = "hr_uid_placeholder"

    // ── OFFICE LOCATIONS ─────────────────────────────────────────────────
    private suspend fun seedOfficeLocations(): Int {
        val col = db.collection(Constants.Collections.OFFICE_LOCATIONS)
        val offices = listOf(
            mapOf("name" to "Kantor Pusat Semarang", "address" to "Jl. Pemuda No. 100, Semarang Tengah",
                "latitude" to -6.9932, "longitude" to 110.4203, "allowedRadiusMeters" to 100.0, "isActive" to true),
            mapOf("name" to "Cabang Ungaran", "address" to "Jl. Diponegoro No. 45, Ungaran",
                "latitude" to -7.0372, "longitude" to 110.4029, "allowedRadiusMeters" to 150.0, "isActive" to true)
        )
        offices.forEach { col.add(it).await() }
        return offices.size
    }

    // ── EMPLOYEES ─────────────────────────────────────────────────────────
    private suspend fun seedEmployees(): Int {
        val col = db.collection(Constants.Collections.EMPLOYEES)
        val employees = listOf(
            mapOf("employeeId" to EMP_001, "nik" to "3374010101900001", "name" to "Budi Santoso",
                "email" to "karyawan@ptniger.com", "phone" to "081234567001",
                "position" to "Software Engineer", "department" to "Engineering",
                "branch" to "Semarang", "officeId" to "office_main", "managerId" to EMP_003,
                "joinDate" to "2022-03-01", "employmentStatus" to "active",
                "baseSalary" to 7500000.0, "leaveQuota" to 9, "userId" to "", "workScheduleId" to "default"),
            mapOf("employeeId" to EMP_002, "nik" to "3374010201910002", "name" to "Siti Rahayu",
                "email" to "hr@ptniger.com", "phone" to "081234567002",
                "position" to "HR Specialist", "department" to "Human Resources",
                "branch" to "Semarang", "officeId" to "office_main", "managerId" to EMP_003,
                "joinDate" to "2021-07-15", "employmentStatus" to "active",
                "baseSalary" to 6500000.0, "leaveQuota" to 12, "userId" to HR_UID, "workScheduleId" to "default"),
            mapOf("employeeId" to EMP_003, "nik" to "3374010301880003", "name" to "Ahmad Fauzi",
                "email" to "manager@ptniger.com", "phone" to "081234567003",
                "position" to "Engineering Manager", "department" to "Engineering",
                "branch" to "Semarang", "officeId" to "office_main", "managerId" to "",
                "joinDate" to "2020-01-10", "employmentStatus" to "active",
                "baseSalary" to 12000000.0, "leaveQuota" to 12, "userId" to MGR_UID, "workScheduleId" to "default"),
            mapOf("employeeId" to EMP_004, "nik" to "3374010401930004", "name" to "Dewi Lestari",
                "email" to "dewi.lestari@ptniger.com", "phone" to "081234567004",
                "position" to "UI/UX Designer", "department" to "Engineering",
                "branch" to "Semarang", "officeId" to "office_main", "managerId" to EMP_003,
                "joinDate" to "2023-02-01", "employmentStatus" to "active",
                "baseSalary" to 6000000.0, "leaveQuota" to 10, "userId" to "", "workScheduleId" to "default"),
            mapOf("employeeId" to EMP_005, "nik" to "3374010501950005", "name" to "Reza Pratama",
                "email" to "finance@ptniger.com", "phone" to "081234567005",
                "position" to "Finance Staff", "department" to "Finance",
                "branch" to "Ungaran", "officeId" to "office_ungaran", "managerId" to EMP_003,
                "joinDate" to "2022-09-01", "employmentStatus" to "active",
                "baseSalary" to 5500000.0, "leaveQuota" to 11, "userId" to "", "workScheduleId" to "shift_pagi")
        )
        employees.forEach { col.document(it["employeeId"] as String).set(it).await() }
        return employees.size
    }

    // ── ATTENDANCE ────────────────────────────────────────────────────────
    // Data absensi Q2 2025 (April, Mei, Juni) — semua hari kerja
    private suspend fun seedAttendance(): Int {
        val col = db.collection(Constants.Collections.ATTENDANCE)
        var count = 0

        data class EmpPattern(val empId: String, val lateChance: Int, val absentChance: Int)
        val patterns = listOf(
            EmpPattern(EMP_001, lateChance = 2, absentChance = 1),
            EmpPattern(EMP_002, lateChance = 1, absentChance = 0),
            EmpPattern(EMP_003, lateChance = 0, absentChance = 1),
            EmpPattern(EMP_004, lateChance = 4, absentChance = 2),
            EmpPattern(EMP_005, lateChance = 3, absentChance = 3)
        )

        // Firestore batch max 500 writes per commit
        for (month in 4..6) {
            val batch = db.batch()
            var batchCount = 0
            val cal = Calendar.getInstance()
            cal.set(2025, month - 1, 1)
            val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

            for (day in 1..daysInMonth) {
                cal.set(2025, month - 1, day)
                val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
                if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) continue

                val dateStr = String.format("2025-%02d-%02d", month, day)

                patterns.forEachIndexed { idx, pattern ->
                    val seed = (day + idx * 7 + month * 31)
                    val isAbsent = (seed % 10) < pattern.absentChance
                    val isLate = !isAbsent && (seed % 10) < pattern.lateChance

                    val (checkIn, lateMin, status) = when {
                        isAbsent -> Triple("", 0, "absent")
                        isLate -> {
                            val lateMins = ((seed % 30) + 5)
                            Triple("08:${String.format("%02d", lateMins.coerceAtMost(59))}", lateMins, "late")
                        }
                        else -> Triple("07:${String.format("%02d", (seed % 30) + 30)}", 0, "present")
                    }

                    val docId = "${pattern.empId}_${dateStr}"
                    val data = mapOf(
                        "attendanceId" to docId, "employeeId" to pattern.empId,
                        "date" to dateStr, "clockType" to "clock_in",
                        "checkIn" to checkIn,
                        "checkOut" to if (checkIn.isNotEmpty()) "17:${String.format("%02d", (seed % 30))}" else "",
                        "attendanceStatus" to status, "validationStatus" to "valid",
                        "lateMinutes" to lateMin,
                        "overtimeHours" to if (status == "present" && (seed % 5 == 0)) 1.5 else 0.0,
                        "latitude" to -6.9932, "longitude" to 110.4203,
                        "isWithinOfficeRadius" to true, "isMockLocation" to false,
                        "isTimeTampered" to false,
                        "serverTimestamp" to System.currentTimeMillis(),
                        "deviceTimestamp" to System.currentTimeMillis(),
                        "createdAt" to System.currentTimeMillis()
                    )
                    batch.set(col.document(docId), data)
                    batchCount++
                    count++
                }
            }
            if (batchCount > 0) batch.commit().await()
        }
        return count
    }

    // ── LEAVE REQUESTS ────────────────────────────────────────────────────
    private suspend fun seedLeaveRequests(): Int {
        val col = db.collection(Constants.Collections.LEAVE_REQUESTS)
        val leaves = listOf(
            // Budi (EMP_001) cuti tahunan — manager = Ahmad (EMP_003)
            mapOf(
                "leaveId" to "leave_001", "employeeId" to EMP_001,
                "employeeName" to "Budi Santoso",
                "managerId" to EMP_003,
                "requesterRole" to "employee",
                "departmentId" to "Engineering",
                "type" to "annual", "startDate" to "2025-04-14", "endDate" to "2025-04-16",
                "duration" to 3, "reason" to "Liburan keluarga ke Yogyakarta",
                "status" to "approved", "approvedBy" to EMP_003,
                "autoRejected" to false, "rejectionReason" to "",
                "createdAt" to System.currentTimeMillis()
            ),
            // Dewi (EMP_004) sakit — manager = Ahmad (EMP_003)
            mapOf(
                "leaveId" to "leave_002", "employeeId" to EMP_004,
                "employeeName" to "Dewi Lestari",
                "managerId" to EMP_003,
                "requesterRole" to "employee",
                "departmentId" to "Engineering",
                "type" to "sick", "startDate" to "2025-05-05", "endDate" to "2025-05-06",
                "duration" to 2, "reason" to "Demam dan flu, disertai surat dokter",
                "status" to "approved", "approvedBy" to EMP_003,
                "autoRejected" to false, "rejectionReason" to "",
                "createdAt" to System.currentTimeMillis()
            ),
            // Reza (EMP_005) izin PENDING — manager = Ahmad (EMP_003)
            mapOf(
                "leaveId" to "leave_003", "employeeId" to EMP_005,
                "employeeName" to "Reza Pratama",
                "managerId" to EMP_003,
                "requesterRole" to "employee",
                "departmentId" to "Finance",
                "type" to "permission", "startDate" to "2026-07-15", "endDate" to "2026-07-15",
                "duration" to 1, "reason" to "Urusan keluarga mendadak",
                "status" to "pending", "approvedBy" to "",
                "autoRejected" to false, "rejectionReason" to "",
                "createdAt" to System.currentTimeMillis()
            ),
            // Siti (EMP_002) cuti — manager = Ahmad (EMP_003)
            mapOf(
                "leaveId" to "leave_004", "employeeId" to EMP_002,
                "employeeName" to "Siti Rahayu",
                "managerId" to EMP_003,
                "requesterRole" to "employee",
                "departmentId" to "Human Resources",
                "type" to "annual", "startDate" to "2025-06-09", "endDate" to "2025-06-10",
                "duration" to 2, "reason" to "Pernikahan saudara",
                "status" to "approved", "approvedBy" to EMP_003,
                "autoRejected" to false, "rejectionReason" to "",
                "createdAt" to System.currentTimeMillis()
            ),
            // Budi (EMP_001) cuti PENDING
            mapOf(
                "leaveId" to "leave_005", "employeeId" to EMP_001,
                "employeeName" to "Budi Santoso",
                "managerId" to EMP_003,
                "requesterRole" to "employee",
                "departmentId" to "Engineering",
                "type" to "annual", "startDate" to "2026-07-20", "endDate" to "2026-07-22",
                "duration" to 3, "reason" to "Liburan akhir tahun",
                "status" to "pending", "approvedBy" to "",
                "autoRejected" to false, "rejectionReason" to "",
                "createdAt" to System.currentTimeMillis()
            ),
            // Dewi (EMP_004) sakit PENDING
            mapOf(
                "leaveId" to "leave_006", "employeeId" to EMP_004,
                "employeeName" to "Dewi Lestari",
                "managerId" to EMP_003,
                "requesterRole" to "employee",
                "departmentId" to "Engineering",
                "type" to "sick", "startDate" to "2026-06-30", "endDate" to "2026-06-30",
                "duration" to 1, "reason" to "Sakit kepala, tidak bisa hadir",
                "status" to "pending", "approvedBy" to "",
                "autoRejected" to false, "rejectionReason" to "",
                "createdAt" to System.currentTimeMillis()
            )
        )
        val batch = db.batch()
        leaves.forEach { batch.set(col.document(it["leaveId"] as String), it) }
        batch.commit().await()
        return leaves.size
    }

    // ── KPI CONFIGS ───────────────────────────────────────────────────────
    private suspend fun seedKpiConfigs(): Int {
        val col = db.collection(Constants.Collections.KPI_CONFIGS)
        val configs = listOf(
            mapOf("configId" to "kpi_quality_eng", "name" to "Kualitas Code & Bugs",
                "dimension" to "output_quality", "weight" to 0.30,
                "description" to "Penilaian bug rate dan best practices",
                "departmentId" to "Engineering",
                "isActive" to true, "period" to "2025-Q2", "createdBy" to HR_UID),
            mapOf("configId" to "kpi_attendance_eng", "name" to "Kehadiran & Ketepatan Waktu",
                "dimension" to "attendance", "weight" to 0.20,
                "description" to "Penilaian absensi dan kedisiplinan waktu masuk kerja",
                "departmentId" to "Engineering",
                "isActive" to true, "period" to "2025-Q2", "createdBy" to HR_UID),
            mapOf("configId" to "kpi_goal_eng", "name" to "Pencapaian Sprint / OKR",
                "dimension" to "goal_achievement", "weight" to 0.25,
                "description" to "Sejauh mana target kuartal tercapai",
                "departmentId" to "Engineering",
                "isActive" to true, "period" to "2025-Q2", "createdBy" to HR_UID),
            mapOf("configId" to "kpi_team_eng", "name" to "Kontribusi Tim & Kolaborasi",
                "dimension" to "team_contribution", "weight" to 0.15,
                "description" to "Penilaian kerjasama dan kontribusi dalam tim",
                "departmentId" to "Engineering",
                "isActive" to true, "period" to "2025-Q2", "createdBy" to HR_UID),
            mapOf("configId" to "kpi_discipline_eng", "name" to "Disiplin & Attitude",
                "dimension" to "discipline", "weight" to 0.10,
                "description" to "Penilaian kedisiplinan, sikap, dan perilaku kerja",
                "departmentId" to "Engineering",
                "isActive" to true, "period" to "2025-Q2", "createdBy" to HR_UID),
                
            // HR Department
            mapOf("configId" to "kpi_hr_recruit", "name" to "SLA Recruitment",
                "dimension" to "output_quality", "weight" to 0.40,
                "description" to "Kecepatan pemenuhan kandidat",
                "departmentId" to "Human Resources",
                "isActive" to true, "period" to "2025-Q2", "createdBy" to HR_UID),
            mapOf("configId" to "kpi_hr_att", "name" to "Kehadiran & Ketepatan Waktu",
                "dimension" to "attendance", "weight" to 0.30,
                "description" to "Penilaian absensi",
                "departmentId" to "Human Resources",
                "isActive" to true, "period" to "2025-Q2", "createdBy" to HR_UID),
            mapOf("configId" to "kpi_hr_team", "name" to "Employee Satisfaction",
                "dimension" to "team_contribution", "weight" to 0.30,
                "description" to "Tingkat kepuasan karyawan terhadap layanan HR",
                "departmentId" to "Human Resources",
                "isActive" to true, "period" to "2025-Q2", "createdBy" to HR_UID)
        )
        configs.forEach { col.document(it["configId"] as String).set(it).await() }
        return configs.size
    }

    // ── KPI SCORES ────────────────────────────────────────────────────────
    private suspend fun seedKpiScores(): Int {
        val col = db.collection(Constants.Collections.KPI_SCORES)
        val batch = db.batch()
        var count = 0

        // (empId, empName, quality, attendance, goal, team, discipline)
        val empScores = listOf(
            listOf(EMP_001, "Budi Santoso",  85, 90, 80, 88, 92),
            listOf(EMP_002, "Siti Rahayu",   92, 95, 88, 95, 97),
            listOf(EMP_003, "Ahmad Fauzi",   78, 85, 75, 90, 88),
            listOf(EMP_004, "Dewi Lestari",  88, 72, 85, 82, 75),
            listOf(EMP_005, "Reza Pratama",  70, 65, 68, 75, 70)
        )

        val configs = listOf(
            Triple("kpi_quality",    "Kualitas Output Pekerjaan",   0.30),
            Triple("kpi_attendance", "Kehadiran & Ketepatan Waktu", 0.20),
            Triple("kpi_goal",       "Pencapaian Target / OKR",     0.25),
            Triple("kpi_team",       "Kontribusi Tim & Kolaborasi", 0.15),
            Triple("kpi_discipline", "Disiplin & Attitude",         0.10)
        )

        empScores.forEach { row ->
            val empId   = row[0] as String
            val empName = row[1] as String
            val scores  = row.subList(2, row.size).map { it as Int }

            scores.forEachIndexed { idx, score ->
                val (configId, kpiName, weight) = configs[idx]
                val docId = "score_${empId}_${configId}"
                val data = mapOf(
                    "scoreId" to docId, "employeeId" to empId, "employeeName" to empName,
                    "configId" to configId, "kpiName" to kpiName, "score" to score,
                    "weight" to weight, "weightedScore" to (score * weight),
                    "period" to "2025-Q2", "scoredBy" to MGR_UID,
                    "createdAt" to System.currentTimeMillis()
                )
                batch.set(col.document(docId), data)
                count++
            }
        }
        batch.commit().await()
        return count
    }

    // ── PEER REVIEWS ──────────────────────────────────────────────────────
    private suspend fun seedPeerReviews(): Int {
        val col = db.collection(Constants.Collections.PEER_REVIEWS)
        val reviews = listOf(
            mapOf("targetEmployeeId" to EMP_001, "reviewerEmployeeId" to EMP_004,
                "period" to "2025-05", "score" to 84,
                "comments" to "Budi sangat membantu dalam code review dan selalu responsif.",
                "dimension" to "team_contribution", "createdAt" to System.currentTimeMillis()),
            mapOf("targetEmployeeId" to EMP_001, "reviewerEmployeeId" to EMP_002,
                "period" to "2025-05", "score" to 88,
                "comments" to "Komunikasi baik, deliverable tepat waktu.",
                "dimension" to "output_quality", "createdAt" to System.currentTimeMillis()),
            mapOf("targetEmployeeId" to EMP_004, "reviewerEmployeeId" to EMP_001,
                "period" to "2025-05", "score" to 80,
                "comments" to "Desain bagus, tapi perlu lebih proaktif dalam update progress.",
                "dimension" to "team_contribution", "createdAt" to System.currentTimeMillis()),
            mapOf("targetEmployeeId" to EMP_005, "reviewerEmployeeId" to EMP_003,
                "period" to "2025-05", "score" to 68,
                "comments" to "Perlu peningkatan dalam ketepatan laporan dan komunikasi deadline.",
                "dimension" to "discipline", "createdAt" to System.currentTimeMillis()),
            mapOf("targetEmployeeId" to EMP_003, "reviewerEmployeeId" to EMP_002,
                "period" to "2025-05", "score" to 91,
                "comments" to "Leadership kuat, supportif terhadap tim.",
                "dimension" to "team_contribution", "createdAt" to System.currentTimeMillis())
        )
        reviews.forEach { col.add(it).await() }
        return reviews.size
    }

    // ── PAYROLLS ──────────────────────────────────────────────────────────
    private suspend fun seedPayrolls(): Int {
        val col = db.collection(Constants.Collections.PAYROLLS)

        data class EmpPay(val id: String, val name: String, val base: Double, val ptkp: String)
        val emps = listOf(
            EmpPay(EMP_001, "Budi Santoso",  7500000.0, "TK/0"),
            EmpPay(EMP_002, "Siti Rahayu",   6500000.0, "K/1"),
            EmpPay(EMP_003, "Ahmad Fauzi",  12000000.0, "K/2"),
            EmpPay(EMP_004, "Dewi Lestari",  6000000.0, "TK/0"),
            EmpPay(EMP_005, "Reza Pratama",  5500000.0, "TK/0")
        )

        val payrolls = emps.mapIndexed { idx, e ->
            val overtime = e.base / 173 * 1.5 * (idx + 1).toDouble()
            val bpjsKes  = e.base * 0.01
            val bpjsJht  = e.base * 0.02
            val bpjsJp   = e.base * 0.01
            val gross    = e.base + 500000 + 300000 + overtime
            val deduct   = bpjsKes + bpjsJht + bpjsJp
            val net      = gross - deduct

            mapOf(
                "payrollId" to "payroll_${e.id}_2025_05",
                "employeeId" to e.id, "employeeName" to e.name,
                "month" to 5, "year" to 2025,
                "baseSalary" to e.base,
                "allowanceMeal" to 500000.0, "allowanceTransport" to 300000.0,
                "allowancePosition" to if (e.id == EMP_003) 2000000.0 else 0.0,
                "overtimeHours" to (idx + 1).toDouble(), "overtimePay" to overtime,
                "kpiScore" to 82.0, "kpiBonus" to 0.0,
                "bpjsKesehatan" to bpjsKes, "bpjsJht" to bpjsJht, "bpjsJp" to bpjsJp,
                "bpjsJkk" to e.base * 0.0024, "bpjsJkm" to e.base * 0.003,
                "pph21" to 0.0, "ptkpStatus" to e.ptkp,
                "deductions" to deduct, "netSalary" to net,
                "status" to "pending_approval", "managerId" to EMP_003,
                "generatedAt" to System.currentTimeMillis()
            )
        }
        payrolls.forEach { col.document(it["payrollId"] as String).set(it).await() }
        return payrolls.size
    }

    // ── WORK SCHEDULES ────────────────────────────────────────────────────
    private suspend fun seedWorkSchedules(): Int {
        val col = db.collection(Constants.Collections.WORK_SCHEDULES)
        val schedules = listOf(
            mapOf(
                "scheduleId" to "default", "name" to "Jadwal Standar (08:00–16:00)",
                "workDays" to listOf(2, 3, 4, 5, 6),
                "workStartTime" to "08:00", "lateThreshold" to "08:15",
                "earlyLeaveBuffer" to "15:45", "workEndTime" to "16:00",
                "overtimeStartsAfter" to "16:10", "maxOvertimeHours" to 4.0,
                "createdBy" to "system", "updatedAt" to System.currentTimeMillis()
            ),
            mapOf(
                "scheduleId" to "shift_pagi", "name" to "Shift Pagi (07:00–15:00)",
                "workDays" to listOf(2, 3, 4, 5, 6, 7),
                "workStartTime" to "07:00", "lateThreshold" to "07:10",
                "earlyLeaveBuffer" to "14:45", "workEndTime" to "15:00",
                "overtimeStartsAfter" to "15:10", "maxOvertimeHours" to 3.0,
                "createdBy" to "system", "updatedAt" to System.currentTimeMillis()
            )
        )
        schedules.forEach { col.document(it["scheduleId"] as String).set(it).await() }
        return schedules.size
    }

    // ── LEAVE POLICY ──────────────────────────────────────────────────────
    private suspend fun seedLeavePolicy(): Int {
        db.collection(Constants.Collections.LEAVE_POLICIES).document("default").set(
            mapOf(
                "policyId" to "default", "companyId" to "default",
                "minAdvanceDays" to 3, "maxDaysPerRequest" to 12,
                "allowPastDateSubmission" to false, "autoRejectOnExpiry" to true,
                "updatedBy" to "system", "updatedAt" to System.currentTimeMillis()
            )
        ).await()
        return 1
    }

    // ── APP CONFIGS ───────────────────────────────────────────────────────
    private suspend fun seedAppConfigs(): Int {
        val col = db.collection(Constants.Collections.APP_CONFIGS)
        val configs = listOf(
            mapOf("configId" to "config_groq_api", "key" to "groq_api_key",
                "value" to "YOUR_API_KEY_HERE",
                "description" to "API Key untuk engine Groq AI (Llama 3)",
                "isSecret" to true, "updatedAt" to System.currentTimeMillis())
        )
        configs.forEach { col.document(it["configId"] as String).set(it).await() }
        return configs.size
    }

    private suspend fun seedAutomationRules(): Int {
        val col = db.collection("automation_rules")
        val rules = listOf(
            mapOf("ruleId" to "leave",        "isActive" to true,  "name" to "Leave Automation"),
            mapOf("ruleId" to "notification", "isActive" to true,  "name" to "Push Notification"),
            mapOf("ruleId" to "audit",        "isActive" to true,  "name" to "Audit Log"),
            mapOf("ruleId" to "payroll",      "isActive" to true,  "name" to "Payroll Automation"),
            mapOf("ruleId" to "kpi",          "isActive" to true,  "name" to "KPI Auto-scoring"),
            mapOf("ruleId" to "ai_review",    "isActive" to true,  "name" to "AI Performance Review"),
            mapOf("ruleId" to "leave_expiry", "isActive" to true,  "name" to "Leave Expiry Checker")
        )
        rules.forEach { col.document(it["ruleId"] as String).set(it).await() }
        return rules.size
    }

    private suspend fun seedNotifications(): Int {
        val col = db.collection(Constants.Collections.NOTIFICATIONS)
        val notifs = listOf(
            mapOf(
                "notificationId" to "notif_001",
                "userId" to HR_UID,
                "title" to "Pengajuan Cuti Baru",
                "message" to "Budi Santoso mengajukan cuti tahunan (3 hari) mulai 20 Jul 2026.",
                "type" to "leave_request",
                "isRead" to false,
                "createdAt" to System.currentTimeMillis()
            ),
            mapOf(
                "notificationId" to "notif_002",
                "userId" to MGR_UID,
                "title" to "Payroll Menunggu Approval",
                "message" to "Payroll bulan Juni 2026 untuk Budi Santoso menunggu persetujuan Anda.",
                "type" to "payroll_approval",
                "isRead" to false,
                "createdAt" to System.currentTimeMillis() - 3600000
            ),
            mapOf(
                "notificationId" to "notif_003",
                "userId" to MGR_UID,
                "title" to "Karyawan Terlambat",
                "message" to "Dewi Lestari terlambat 25 menit hari ini (check-in 08:25).",
                "type" to "attendance_late",
                "isRead" to true,
                "createdAt" to System.currentTimeMillis() - 7200000
            )
        )
        notifs.forEach { col.document(it["notificationId"] as String).set(it).await() }
        return notifs.size
    }

    suspend fun migrateUsersToEmployees(): String {
        val usersCol = db.collection("users")
        val empCol = db.collection("employees")

        val allUsers = usersCol.get().await().documents.mapNotNull { doc ->
            doc.toObject(com.ptniger.hris.data.model.User::class.java)?.copy(userId = doc.id)
        }

        var created = 0
        var skipped = 0

        allUsers.forEach { user ->
            if (user.employeeId.isNotEmpty()) {
                val empExists = try {
                    empCol.document(user.employeeId).get().await().exists()
                } catch (_: Exception) { false }
                if (empExists) { skipped++; return@forEach }
            }

            val employeeId = if (user.employeeId.isNotEmpty()) user.employeeId
                             else "emp_${user.userId.take(8)}_${System.currentTimeMillis()}"

            val role = user.primaryRole.ifEmpty { user.role }
            val employee = mapOf(
                "employeeId"       to employeeId,
                "userId"           to user.userId,
                "name"             to user.name.ifEmpty { user.fullName },
                "email"            to user.email,
                "nik"              to "",
                "phone"            to "",
                "position"         to getRoleDisplayName(role),
                "department"       to getDeptForRole(role),
                "branch"           to user.branch.ifEmpty { "Semarang" },
                "officeId"         to user.officeId.ifEmpty { "office_main" },
                "managerId"        to "",
                "joinDate"         to "",
                "employmentStatus" to "active",
                "baseSalary"       to 0.0,
                "leaveQuota"       to 12,
                "workScheduleId"   to "default",
                "isFaceRegistered" to false,
                "faceEmbedding"    to emptyList<Float>()
            )
            empCol.document(employeeId).set(employee).await()

            if (user.employeeId.isEmpty() || user.employeeId != employeeId) {
                usersCol.document(user.userId).update("employeeId", employeeId).await()
            }
            created++
        }

        return "Migrasi selesai: $created employee record dibuat, $skipped sudah ada."
    }

    private fun getRoleDisplayName(role: String) = when(role) {
        Constants.Role.MANAGER     -> "Manager"
        Constants.Role.HR          -> "HR Specialist"
        Constants.Role.FINANCE     -> "Finance Staff"
        Constants.Role.SUPER_ADMIN -> "Super Administrator"
        else -> "Staff"
    }

    private fun getDeptForRole(role: String) = when(role) {
        Constants.Role.MANAGER     -> "Management"
        Constants.Role.HR          -> "Human Resources"
        Constants.Role.FINANCE     -> "Finance"
        Constants.Role.SUPER_ADMIN -> "IT/Management"
        else -> "General"
    }
}

package com.ptniger.hris.utils

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.*

/**
 * Firestore data seeder untuk demo lengkap.
 * Menghasilkan data Q1 2026 (Jan-Mar) + Q2 2026 (Apr-Jun):
 *   - Employees (real users + dummy)
 *   - Attendance 6 bulan penuh
 *   - KPI configs & scores per kuartal
 *   - Peer reviews per kuartal
 *   - Payroll 6 bulan
 *   - Leave requests
 *   - Employee contracts
 *   - Office locations, work schedules, automation rules, notifications
 */
object SeedDataManager {

    private val db = FirebaseFirestore.getInstance()

    // ── MAIN ENTRY POINT ─────────────────────────────────────────────────
    suspend fun seedAll(): SeedResult {
        val errors = mutableListOf<String>()
        var totalInserted = 0

        // Step 1: Fetch real users & merge with dummy
        val allEmployees: List<EmpInfo>
        try {
            allEmployees = buildEmployeeList()
        } catch (e: Exception) {
            return SeedResult(0, listOf("FATAL: gagal fetch users: ${e.message}"))
        }

        try { totalInserted += seedOfficeLocations() }                 catch (e: Exception) { errors.add("office: ${e.message}") }
        try { totalInserted += seedEmployeeRecords(allEmployees) }     catch (e: Exception) { errors.add("employees: ${e.message}") }
        try { totalInserted += seedWorkSchedules() }                   catch (e: Exception) { errors.add("work_schedules: ${e.message}") }
        try { totalInserted += seedLeavePolicy() }                     catch (e: Exception) { errors.add("leave_policy: ${e.message}") }
        try { totalInserted += seedContracts(allEmployees) }           catch (e: Exception) { errors.add("contracts: ${e.message}") }
        try { totalInserted += seedAttendance(allEmployees) }          catch (e: Exception) { errors.add("attendance: ${e.message}") }
        try { totalInserted += seedLeaveRequests(allEmployees) }       catch (e: Exception) { errors.add("leave: ${e.message}") }
        try { totalInserted += seedKpiConfigs() }                      catch (e: Exception) { errors.add("kpi_config: ${e.message}") }
        try { totalInserted += seedKpiScores(allEmployees) }           catch (e: Exception) { errors.add("kpi_scores: ${e.message}") }
        try { totalInserted += seedPeerReviews(allEmployees) }         catch (e: Exception) { errors.add("peer_reviews: ${e.message}") }
        try { totalInserted += seedPayrolls(allEmployees) }            catch (e: Exception) { errors.add("payrolls: ${e.message}") }
        try { totalInserted += seedAppConfigs() }                      catch (e: Exception) { errors.add("app_configs: ${e.message}") }
        try { totalInserted += seedAutomationRules() }                 catch (e: Exception) { errors.add("automation_rules: ${e.message}") }
        try { totalInserted += seedNotifications(allEmployees) }       catch (e: Exception) { errors.add("notifications: ${e.message}") }

        return SeedResult(totalInserted, errors)
    }

    data class SeedResult(val inserted: Int, val errors: List<String>)

    // ── EMPLOYEE INFO MODEL ──────────────────────────────────────────────
    data class EmpInfo(
        val empId: String,
        val name: String,
        val email: String,
        val position: String,
        val department: String,
        val role: String,
        val baseSalary: Double,
        val ptkp: String,
        val branch: String = "Semarang",
        val userId: String = "",
        val isReal: Boolean = false   // true = from Firebase users collection
    )

    // ── DUMMY EMPLOYEE IDs ───────────────────────────────────────────────
    private const val EMP_001 = "emp_budi_santoso"
    private const val EMP_002 = "emp_siti_rahayu"
    private const val EMP_003 = "emp_ahmad_fauzi"
    private const val EMP_004 = "emp_dewi_lestari"
    private const val EMP_005 = "emp_reza_pratama"

    // ── BUILD EMPLOYEE LIST (REAL + DUMMY) ───────────────────────────────
    private suspend fun buildEmployeeList(): List<EmpInfo> {
        val result = mutableListOf<EmpInfo>()

        // 1) Fetch real users from Firebase
        val usersSnapshot = db.collection(Constants.Collections.USERS).get().await()
        val realUsers = usersSnapshot.documents.mapNotNull { doc ->
            val data = doc.data ?: return@mapNotNull null
            val uid = doc.id
            val name = (data["name"] as? String)?.ifEmpty { data["fullName"] as? String ?: "User" } ?: "User"
            val email = data["email"] as? String ?: ""
            val role = (data["primaryRole"] as? String)?.ifEmpty { data["role"] as? String ?: "employee" } ?: "employee"
            val branch = (data["branch"] as? String)?.ifEmpty { "Semarang" } ?: "Semarang"
            val empId = (data["employeeId"] as? String)?.ifEmpty { uid } ?: uid

            EmpInfo(
                empId = empId,
                name = name,
                email = email,
                position = getRoleDisplayName(role),
                department = getDeptForRole(role),
                role = role,
                baseSalary = getBaseSalaryForRole(role),
                ptkp = "TK/0",
                branch = branch,
                userId = uid,
                isReal = true
            )
        }
        result.addAll(realUsers)

        // 2) Add dummy employees (skip if empId already exists from real users)
        val existingIds = result.map { it.empId }.toSet()
        val dummies = listOf(
            EmpInfo(EMP_001, "Budi Santoso", "budi@ptniger.com", "Software Engineer", "Engineering", "employee", 7500000.0, "TK/0"),
            EmpInfo(EMP_002, "Siti Rahayu", "siti@ptniger.com", "HR Specialist", "Human Resources", "hr", 6500000.0, "K/1"),
            EmpInfo(EMP_003, "Ahmad Fauzi", "ahmad@ptniger.com", "Engineering Manager", "Engineering", "manager", 12000000.0, "K/2"),
            EmpInfo(EMP_004, "Dewi Lestari", "dewi@ptniger.com", "UI/UX Designer", "Engineering", "employee", 6000000.0, "TK/0"),
            EmpInfo(EMP_005, "Reza Pratama", "reza@ptniger.com", "Finance Staff", "Finance", "finance", 5500000.0, "TK/0", branch = "Ungaran")
        )
        dummies.filter { it.empId !in existingIds }.forEach { result.add(it) }

        return result
    }

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

    // ── EMPLOYEE RECORDS ─────────────────────────────────────────────────
    private suspend fun seedEmployeeRecords(emps: List<EmpInfo>): Int {
        val col = db.collection(Constants.Collections.EMPLOYEES)
        var count = 0
        // Find a manager ID for managerId field
        val managerId = emps.firstOrNull { it.role == "manager" }?.empId ?: EMP_003

        for (emp in emps) {
            val data = mapOf(
                "employeeId" to emp.empId,
                "nik" to "337401${String.format("%010d", emp.empId.hashCode().toLong().and(9999999999L))}",
                "name" to emp.name,
                "email" to emp.email,
                "phone" to "0812${String.format("%08d", emp.empId.hashCode().toLong().and(99999999L))}",
                "position" to emp.position,
                "department" to emp.department,
                "branch" to emp.branch,
                "officeId" to "office_main",
                "managerId" to if (emp.role == "manager") "" else managerId,
                "joinDate" to "2024-01-15",
                "employmentStatus" to "active",
                "baseSalary" to emp.baseSalary,
                "leaveQuota" to 12,
                "userId" to emp.userId,
                "workScheduleId" to "default"
            )
            col.document(emp.empId).set(data, com.google.firebase.firestore.SetOptions.merge()).await()
            count++
        }
        return count
    }

    // ── EMPLOYEE CONTRACTS ───────────────────────────────────────────────
    private suspend fun seedContracts(emps: List<EmpInfo>): Int {
        val col = db.collection(Constants.Collections.EMPLOYEE_CONTRACTS)
        var count = 0
        for (emp in emps) {
            val posAllowance = when {
                emp.role == "manager" -> 2000000.0
                emp.role == "hr" || emp.role == "finance" -> 500000.0
                else -> 0.0
            }
            val data = mapOf(
                "contractId" to "contract_${emp.empId}",
                "employeeId" to emp.empId,
                "effectiveDate" to "2026-01-01",
                "baseSalary" to emp.baseSalary,
                "allowanceMeal" to 500000.0,
                "allowanceTransport" to 300000.0,
                "allowancePosition" to posAllowance,
                "bpjsJkkRate" to 0.0024,
                "bpjsJkmRate" to 0.003,
                "ptkpStatus" to emp.ptkp,
                "overrideOvertimeMultiplier" to false,
                "overtime1xMultiplier" to 1.5,
                "overtimeNxMultiplier" to 2.0,
                "signedByEmployee" to true,
                "signedAt" to System.currentTimeMillis(),
                "signatureData" to "",
                "createdBy" to "system",
                "createdAt" to System.currentTimeMillis()
            )
            col.document("contract_${emp.empId}").set(data).await()
            count++
        }
        return count
    }

    // ── ATTENDANCE (Jan-Jun 2026) ────────────────────────────────────────
    private suspend fun seedAttendance(emps: List<EmpInfo>): Int {
        val col = db.collection(Constants.Collections.ATTENDANCE)
        var count = 0

        // Each employee gets a different pattern
        for (monthIdx in 1..6) {
            val batchList = mutableListOf<Map<String, Any>>()
            val cal = Calendar.getInstance()
            cal.set(2026, monthIdx - 1, 1)
            val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

            for (day in 1..daysInMonth) {
                cal.set(2026, monthIdx - 1, day)
                val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
                if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) continue

                val dateStr = String.format("2026-%02d-%02d", monthIdx, day)

                emps.forEachIndexed { idx, emp ->
                    val seed = (day + idx * 7 + monthIdx * 31)
                    val lateChance = when {
                        emp.role == "manager" -> 1
                        emp.role == "hr" -> 1
                        emp.role == "finance" -> 2
                        else -> 3
                    }
                    val absentChance = when {
                        emp.role == "manager" -> 0
                        emp.role == "hr" -> 0
                        else -> 1
                    }
                    val isAbsent = (seed % 15) < absentChance
                    val isLate = !isAbsent && (seed % 12) < lateChance

                    val (checkIn, lateMin, status) = when {
                        isAbsent -> Triple("", 0, "absent")
                        isLate -> {
                            val lateMins = ((seed % 25) + 5)
                            Triple("08:${String.format("%02d", lateMins.coerceAtMost(59))}", lateMins, "late")
                        }
                        else -> Triple("07:${String.format("%02d", (seed % 30) + 25)}", 0, "present")
                    }

                    val overtimeHrs = if (status == "present" && (seed % 6 == 0)) 1.5 else 0.0
                    val checkOut = if (checkIn.isNotEmpty()) {
                        if (overtimeHrs > 0) "17:${String.format("%02d", 30 + (seed % 30))}"
                        else "16:${String.format("%02d", (seed % 50).coerceAtMost(59))}"
                    } else ""

                    val similarity = if (status != "absent") 0.85f + (seed % 15) * 0.01f else 0f

                    val docId = "${emp.empId}_${dateStr}"
                    val data = mapOf<String, Any>(
                        "attendanceId" to docId, "employeeId" to emp.empId,
                        "date" to dateStr, "clockType" to "clock_in",
                        "checkIn" to checkIn, "checkOut" to checkOut,
                        "attendanceStatus" to status, "validationStatus" to "valid",
                        "lateMinutes" to lateMin,
                        "overtimeHours" to overtimeHrs,
                        "latitude" to -6.9932, "longitude" to 110.4203,
                        "isWithinOfficeRadius" to true, "isMockLocation" to false,
                        "isTimeTampered" to false,
                        "checkInMode" to "face_recognition",
                        "faceRecognitionSimilarity" to similarity,
                        "livenessVerified" to (status != "absent"),
                        "serverTimestamp" to cal.timeInMillis,
                        "deviceTimestamp" to cal.timeInMillis,
                        "createdAt" to cal.timeInMillis
                    )
                    batchList.add(data)
                }
            }

            // Write in batches of 400
            batchList.chunked(400).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { data ->
                    batch.set(col.document(data["attendanceId"] as String), data)
                }
                batch.commit().await()
            }
            count += batchList.size
        }
        return count
    }

    // ── LEAVE REQUESTS ───────────────────────────────────────────────────
    private suspend fun seedLeaveRequests(emps: List<EmpInfo>): Int {
        val col = db.collection(Constants.Collections.LEAVE_REQUESTS)
        val managerId = emps.firstOrNull { it.role == "manager" }?.empId ?: EMP_003
        val leaves = mutableListOf<Map<String, Any>>()
        var leaveIdx = 1

        for (emp in emps) {
            if (emp.role == "manager" || emp.role == "super_admin") continue

            // Q1: 1 approved annual leave
            leaves.add(mapOf(
                "leaveId" to "leave_${String.format("%03d", leaveIdx++)}",
                "employeeId" to emp.empId, "employeeName" to emp.name,
                "managerId" to managerId, "requesterRole" to emp.role,
                "departmentId" to emp.department,
                "type" to "annual", "startDate" to "2026-02-${10 + (emp.empId.hashCode() % 10).coerceIn(0, 15)}",
                "endDate" to "2026-02-${12 + (emp.empId.hashCode() % 10).coerceIn(0, 15)}",
                "duration" to 3, "reason" to "Liburan keluarga",
                "status" to "approved", "approvedBy" to managerId,
                "autoRejected" to false, "rejectionReason" to "",
                "createdAt" to System.currentTimeMillis()
            ))

            // Q1: 1 sick leave (approved)
            leaves.add(mapOf(
                "leaveId" to "leave_${String.format("%03d", leaveIdx++)}",
                "employeeId" to emp.empId, "employeeName" to emp.name,
                "managerId" to managerId, "requesterRole" to emp.role,
                "departmentId" to emp.department,
                "type" to "sick", "startDate" to "2026-03-05", "endDate" to "2026-03-06",
                "duration" to 2, "reason" to "Demam dan flu",
                "status" to "approved", "approvedBy" to managerId,
                "autoRejected" to false, "rejectionReason" to "",
                "createdAt" to System.currentTimeMillis()
            ))

            // Q2: 1 annual leave (approved)
            leaves.add(mapOf(
                "leaveId" to "leave_${String.format("%03d", leaveIdx++)}",
                "employeeId" to emp.empId, "employeeName" to emp.name,
                "managerId" to managerId, "requesterRole" to emp.role,
                "departmentId" to emp.department,
                "type" to "annual", "startDate" to "2026-05-18", "endDate" to "2026-05-20",
                "duration" to 3, "reason" to "Acara keluarga",
                "status" to "approved", "approvedBy" to managerId,
                "autoRejected" to false, "rejectionReason" to "",
                "createdAt" to System.currentTimeMillis()
            ))

            // Q2: 1 pending leave
            leaves.add(mapOf(
                "leaveId" to "leave_${String.format("%03d", leaveIdx++)}",
                "employeeId" to emp.empId, "employeeName" to emp.name,
                "managerId" to managerId, "requesterRole" to emp.role,
                "departmentId" to emp.department,
                "type" to "permission", "startDate" to "2026-07-10", "endDate" to "2026-07-10",
                "duration" to 1, "reason" to "Urusan pribadi mendadak",
                "status" to "pending", "approvedBy" to "",
                "autoRejected" to false, "rejectionReason" to "",
                "createdAt" to System.currentTimeMillis()
            ))
        }

        leaves.chunked(400).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { data -> batch.set(col.document(data["leaveId"] as String), data) }
            batch.commit().await()
        }
        return leaves.size
    }

    // ── KPI CONFIGS ──────────────────────────────────────────────────────
    private suspend fun seedKpiConfigs(): Int {
        val col = db.collection(Constants.Collections.KPI_CONFIGS)
        val periods = listOf("2026-Q1", "2026-Q2")
        var count = 0

        for (period in periods) {
            val suffix = period.replace("-", "_").lowercase()
            val configs = listOf(
                mapOf("configId" to "kpi_quality_eng_$suffix", "name" to "Kualitas Output Pekerjaan",
                    "dimension" to "output_quality", "weight" to 0.30,
                    "description" to "Penilaian kualitas deliverable, bug rate, dan best practices",
                    "departmentId" to "Engineering", "department" to "Engineering",
                    "isActive" to true, "period" to period, "createdBy" to "system",
                    "createdAt" to System.currentTimeMillis()),
                mapOf("configId" to "kpi_attendance_eng_$suffix", "name" to "Kehadiran & Ketepatan Waktu",
                    "dimension" to "attendance", "weight" to 0.20,
                    "description" to "Penilaian absensi dan kedisiplinan waktu masuk kerja",
                    "departmentId" to "Engineering", "department" to "Engineering",
                    "isActive" to true, "period" to period, "createdBy" to "system",
                    "createdAt" to System.currentTimeMillis()),
                mapOf("configId" to "kpi_goal_eng_$suffix", "name" to "Pencapaian Sprint / OKR",
                    "dimension" to "goal_achievement", "weight" to 0.25,
                    "description" to "Sejauh mana target kuartal tercapai",
                    "departmentId" to "Engineering", "department" to "Engineering",
                    "isActive" to true, "period" to period, "createdBy" to "system",
                    "createdAt" to System.currentTimeMillis()),
                mapOf("configId" to "kpi_team_eng_$suffix", "name" to "Kontribusi Tim & Kolaborasi",
                    "dimension" to "team_contribution", "weight" to 0.15,
                    "description" to "Penilaian kerjasama dan kontribusi dalam tim",
                    "departmentId" to "Engineering", "department" to "Engineering",
                    "isActive" to true, "period" to period, "createdBy" to "system",
                    "createdAt" to System.currentTimeMillis()),
                mapOf("configId" to "kpi_discipline_eng_$suffix", "name" to "Disiplin & Attitude",
                    "dimension" to "discipline", "weight" to 0.10,
                    "description" to "Penilaian kedisiplinan, sikap, dan perilaku kerja",
                    "departmentId" to "Engineering", "department" to "Engineering",
                    "isActive" to true, "period" to period, "createdBy" to "system",
                    "createdAt" to System.currentTimeMillis()),

                // HR Department
                mapOf("configId" to "kpi_hr_recruit_$suffix", "name" to "SLA Recruitment",
                    "dimension" to "output_quality", "weight" to 0.40,
                    "description" to "Kecepatan dan kualitas pemenuhan kandidat",
                    "departmentId" to "Human Resources", "department" to "Human Resources",
                    "isActive" to true, "period" to period, "createdBy" to "system",
                    "createdAt" to System.currentTimeMillis()),
                mapOf("configId" to "kpi_hr_att_$suffix", "name" to "Kehadiran & Ketepatan Waktu",
                    "dimension" to "attendance", "weight" to 0.30,
                    "description" to "Penilaian absensi HR",
                    "departmentId" to "Human Resources", "department" to "Human Resources",
                    "isActive" to true, "period" to period, "createdBy" to "system",
                    "createdAt" to System.currentTimeMillis()),
                mapOf("configId" to "kpi_hr_team_$suffix", "name" to "Employee Satisfaction",
                    "dimension" to "team_contribution", "weight" to 0.30,
                    "description" to "Tingkat kepuasan karyawan terhadap layanan HR",
                    "departmentId" to "Human Resources", "department" to "Human Resources",
                    "isActive" to true, "period" to period, "createdBy" to "system",
                    "createdAt" to System.currentTimeMillis()),

                // Finance Department
                mapOf("configId" to "kpi_fin_accuracy_$suffix", "name" to "Akurasi Laporan Keuangan",
                    "dimension" to "output_quality", "weight" to 0.40,
                    "description" to "Keakuratan dan ketepatan waktu laporan keuangan",
                    "departmentId" to "Finance", "department" to "Finance",
                    "isActive" to true, "period" to period, "createdBy" to "system",
                    "createdAt" to System.currentTimeMillis()),
                mapOf("configId" to "kpi_fin_att_$suffix", "name" to "Kehadiran & Ketepatan Waktu",
                    "dimension" to "attendance", "weight" to 0.30,
                    "description" to "Penilaian absensi Finance",
                    "departmentId" to "Finance", "department" to "Finance",
                    "isActive" to true, "period" to period, "createdBy" to "system",
                    "createdAt" to System.currentTimeMillis()),
                mapOf("configId" to "kpi_fin_team_$suffix", "name" to "Kolaborasi Antar Departemen",
                    "dimension" to "team_contribution", "weight" to 0.30,
                    "description" to "Kualitas koordinasi dengan departemen lain",
                    "departmentId" to "Finance", "department" to "Finance",
                    "isActive" to true, "period" to period, "createdBy" to "system",
                    "createdAt" to System.currentTimeMillis())
            )
            configs.forEach { col.document(it["configId"] as String).set(it).await() }
            count += configs.size
        }
        return count
    }

    // ── KPI SCORES ───────────────────────────────────────────────────────
    private suspend fun seedKpiScores(emps: List<EmpInfo>): Int {
        val col = db.collection(Constants.Collections.KPI_SCORES)
        var count = 0
        val periods = listOf("2026-Q1", "2026-Q2")
        val managerId = emps.firstOrNull { it.role == "manager" }?.empId ?: EMP_003

        val configs = listOf(
            Triple("kpi_quality",    "Kualitas Output Pekerjaan",    0.30),
            Triple("kpi_attendance", "Kehadiran & Ketepatan Waktu",  0.20),
            Triple("kpi_goal",       "Pencapaian Target / OKR",      0.25),
            Triple("kpi_team",       "Kontribusi Tim & Kolaborasi",  0.15),
            Triple("kpi_discipline", "Disiplin & Attitude",          0.10)
        )

        for (period in periods) {
            val batchDocs = mutableListOf<Pair<String, Map<String, Any>>>()

            emps.forEachIndexed { idx, emp ->
                // Generate realistic scores per employee based on role
                val baseScore = when {
                    emp.role == "manager" -> 82
                    emp.role == "hr" -> 88
                    emp.role == "finance" -> 75
                    else -> 78
                }
                val qBonus = if (period == "2026-Q2") 3 else 0 // slight improvement Q2

                configs.forEachIndexed { cIdx, (configId, kpiName, weight) ->
                    val variation = ((idx * 7 + cIdx * 13) % 15) - 5 // -5 to +9
                    val score = (baseScore + variation + qBonus).coerceIn(50, 100)
                    val docId = "score_${emp.empId}_${configId}_${period.replace("-", "_").lowercase()}"

                    val data = mapOf<String, Any>(
                        "scoreId" to docId, "employeeId" to emp.empId, "employeeName" to emp.name,
                        "configId" to configId, "kpiName" to kpiName, "score" to score,
                        "weight" to weight, "weightedScore" to (score * weight),
                        "period" to period, "scoredBy" to managerId,
                        "source" to "auto", "autoDetails" to "Auto-scored by system seeder",
                        "createdAt" to System.currentTimeMillis(),
                        "updatedAt" to System.currentTimeMillis()
                    )
                    batchDocs.add(docId to data)
                    count++
                }
            }

            batchDocs.chunked(400).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { (docId, data) -> batch.set(col.document(docId), data) }
                batch.commit().await()
            }
        }
        return count
    }

    // ── PEER REVIEWS ─────────────────────────────────────────────────────
    private suspend fun seedPeerReviews(emps: List<EmpInfo>): Int {
        val col = db.collection(Constants.Collections.PEER_REVIEWS)
        val reviews = mutableListOf<Map<String, Any>>()
        val periods = listOf("2026-03", "2026-06") // end of Q1 and Q2

        val commentPool = listOf(
            "Sangat membantu dalam code review dan selalu responsif.",
            "Komunikasi baik, deliverable tepat waktu.",
            "Desain bagus, tapi perlu lebih proaktif dalam update progress.",
            "Leadership kuat, supportif terhadap tim.",
            "Perlu peningkatan dalam ketepatan laporan dan komunikasi deadline.",
            "Kerjasama tim sangat baik, selalu siap membantu.",
            "Konsisten dalam menyelesaikan tugas sesuai deadline.",
            "Inisiatif tinggi dalam menyelesaikan masalah.",
            "Bisa lebih baik dalam dokumentasi kerja.",
            "Attitude profesional, menjadi contoh bagi rekan kerja."
        )

        for (period in periods) {
            for (i in emps.indices) {
                // Each employee reviews 2 others
                val reviewer = emps[i]
                for (j in 1..2) {
                    val targetIdx = (i + j) % emps.size
                    val target = emps[targetIdx]
                    if (target.empId == reviewer.empId) continue

                    val seed = (i * 7 + j * 13 + period.hashCode()) and 0x7FFFFFFF
                    val score = 65 + (seed % 35)
                    val comment = commentPool[seed % commentPool.size]
                    val dimension = listOf("team_contribution", "output_quality", "discipline")[seed % 3]

                    reviews.add(mapOf(
                        "targetEmployeeId" to target.empId,
                        "reviewerEmployeeId" to reviewer.empId,
                        "period" to period,
                        "score" to score,
                        "comments" to comment,
                        "dimension" to dimension,
                        "createdAt" to System.currentTimeMillis()
                    ))
                }
            }
        }

        reviews.chunked(400).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { data -> batch.set(col.document(), data) }
            batch.commit().await()
        }
        return reviews.size
    }

    // ── PAYROLLS (Jan-Jun 2026) ──────────────────────────────────────────
    private suspend fun seedPayrolls(emps: List<EmpInfo>): Int {
        val col = db.collection(Constants.Collections.PAYROLLS)
        var count = 0
        val managerId = emps.firstOrNull { it.role == "manager" }?.empId ?: EMP_003

        for (month in 1..6) {
            val batchDocs = mutableListOf<Pair<String, Map<String, Any>>>()

            emps.forEachIndexed { idx, emp ->
                val posAllowance = when {
                    emp.role == "manager" -> 2000000.0
                    emp.role == "hr" || emp.role == "finance" -> 500000.0
                    else -> 0.0
                }
                val overtimeHrs = ((idx + month) % 5).toDouble()
                val overtimePay = emp.baseSalary / 173.0 * 1.5 * overtimeHrs
                val bpjsKes = emp.baseSalary * 0.01
                val bpjsJht = emp.baseSalary * 0.02
                val bpjsJp  = emp.baseSalary * 0.01
                val bpjsJkk = emp.baseSalary * 0.0024
                val bpjsJkm = emp.baseSalary * 0.003
                val gross = emp.baseSalary + 500000 + 300000 + posAllowance + overtimePay
                val deductions = bpjsKes + bpjsJht + bpjsJp
                val net = gross - deductions

                // Months 1-4 = finalized, month 5 = approved, month 6 = pending_approval
                val status = when {
                    month <= 4 -> "finalized"
                    month == 5 -> "approved"
                    else -> "pending_approval"
                }

                val docId = "payroll_${emp.empId}_2026_${String.format("%02d", month)}"
                val data = mapOf<String, Any>(
                    "payrollId" to docId,
                    "employeeId" to emp.empId, "employeeName" to emp.name,
                    "month" to month, "year" to 2026,
                    "baseSalary" to emp.baseSalary,
                    "allowanceMeal" to 500000.0, "allowanceTransport" to 300000.0,
                    "allowancePosition" to posAllowance, "allowance" to (500000.0 + 300000.0 + posAllowance),
                    "overtimeHours" to overtimeHrs, "overtimePay" to overtimePay,
                    "kpiScore" to (75.0 + (idx * 3) % 25),
                    "kpiBonus" to 0.0,
                    "bpjsKesehatan" to bpjsKes, "bpjsJht" to bpjsJht, "bpjsJp" to bpjsJp,
                    "bpjsJkk" to bpjsJkk, "bpjsJkm" to bpjsJkm,
                    "pph21" to 0.0, "ptkpStatus" to emp.ptkp,
                    "contractId" to "contract_${emp.empId}",
                    "deductions" to deductions, "netSalary" to net,
                    "status" to status,
                    "managerId" to managerId, "departmentId" to emp.department,
                    "generatedAt" to System.currentTimeMillis()
                )
                batchDocs.add(docId to data)
                count++
            }

            batchDocs.chunked(400).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { (docId, data) -> batch.set(col.document(docId), data) }
                batch.commit().await()
            }
        }
        return count
    }

    // ── WORK SCHEDULES ───────────────────────────────────────────────────
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

    // ── LEAVE POLICY ─────────────────────────────────────────────────────
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

    // ── APP CONFIGS ──────────────────────────────────────────────────────
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

    // ── AUTOMATION RULES ─────────────────────────────────────────────────
    private suspend fun seedAutomationRules(): Int {
        val col = db.collection("automation_rules")
        val rules = listOf(
            mapOf("ruleId" to "leave",        "isActive" to true, "name" to "Leave Automation"),
            mapOf("ruleId" to "notification", "isActive" to true, "name" to "Push Notification"),
            mapOf("ruleId" to "audit",        "isActive" to true, "name" to "Audit Log"),
            mapOf("ruleId" to "payroll",      "isActive" to true, "name" to "Payroll Automation"),
            mapOf("ruleId" to "kpi",          "isActive" to true, "name" to "KPI Auto-scoring"),
            mapOf("ruleId" to "ai_review",    "isActive" to true, "name" to "AI Performance Review"),
            mapOf("ruleId" to "leave_expiry", "isActive" to true, "name" to "Leave Expiry Checker")
        )
        rules.forEach { col.document(it["ruleId"] as String).set(it).await() }
        return rules.size
    }

    // ── NOTIFICATIONS ────────────────────────────────────────────────────
    private suspend fun seedNotifications(emps: List<EmpInfo>): Int {
        val col = db.collection(Constants.Collections.NOTIFICATIONS)
        val hrUid = emps.firstOrNull { it.role == "hr" }?.userId ?: ""
        val mgrUid = emps.firstOrNull { it.role == "manager" }?.userId ?: ""

        val notifs = listOf(
            mapOf(
                "notificationId" to "notif_001",
                "userId" to hrUid,
                "title" to "Pengajuan Cuti Baru",
                "message" to "Ada pengajuan cuti baru yang menunggu approval Anda.",
                "type" to "leave_request",
                "isRead" to false,
                "createdAt" to System.currentTimeMillis()
            ),
            mapOf(
                "notificationId" to "notif_002",
                "userId" to mgrUid,
                "title" to "Payroll Menunggu Approval",
                "message" to "Payroll bulan Juni 2026 menunggu persetujuan Anda.",
                "type" to "payroll_approval",
                "isRead" to false,
                "createdAt" to System.currentTimeMillis() - 3600000
            ),
            mapOf(
                "notificationId" to "notif_003",
                "userId" to mgrUid,
                "title" to "KPI Q1 Siap Review",
                "message" to "Skor KPI Q1 2026 sudah tersedia dan siap untuk di-review.",
                "type" to "kpi_review",
                "isRead" to false,
                "createdAt" to System.currentTimeMillis() - 7200000
            )
        )
        notifs.forEach { col.document(it["notificationId"] as String).set(it).await() }
        return notifs.size
    }

    // ── MIGRATE USERS TO EMPLOYEES ───────────────────────────────────────
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
                "baseSalary"       to getBaseSalaryForRole(role),
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

    // ── HELPER FUNCTIONS ─────────────────────────────────────────────────
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

    private fun getBaseSalaryForRole(role: String) = when(role) {
        Constants.Role.MANAGER     -> 12000000.0
        Constants.Role.HR          -> 6500000.0
        Constants.Role.FINANCE     -> 5500000.0
        Constants.Role.SUPER_ADMIN -> 15000000.0
        else -> 6000000.0
    }
}

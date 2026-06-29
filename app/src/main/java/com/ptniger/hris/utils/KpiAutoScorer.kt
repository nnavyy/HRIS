package com.ptniger.hris.utils

import com.ptniger.hris.data.model.Attendance
import com.ptniger.hris.data.repository.KpiRepository
import com.ptniger.hris.data.repository.AttendanceRepository
import java.util.Calendar

object KpiAutoScorer {

    /**
     * Calculates the attendance KPI score for a specific employee and period
     * based on their attendance records.
     * 
     * Formula:
     * - Base score: 100
     * - Absent: -5 points per day
     * - Late: -2 points per day
     * - Tampered/Invalid: -10 points per occurrence
     * - Overtime bonus: +1 point per hour (max +10)
     */
    suspend fun updateAttendanceKpi(employeeId: String, employeeName: String, departmentId: String) {
        val period = "${Calendar.getInstance().get(Calendar.YEAR)}-Q${(Calendar.getInstance().get(Calendar.MONTH) / 3) + 1}"
        val kpiRepo = KpiRepository()
        
        // 1. Get active configs for the department
        val activeConfigs = kpiRepo.getActiveConfigs(departmentId, period)
        val attendanceConfig = activeConfigs.find { it.dimension == "attendance" } ?: return

        // 2. Fetch all attendance records for this period (simplified to fetch all and filter)
        val attRepo = AttendanceRepository()
        val allAttendance = attRepo.getHistory(employeeId)
        val periodRecords = allAttendance.filter { att ->
            // Extract year and month to determine if it falls in the current Q
            try {
                val parts = att.date.split("-")
                val year = parts[0].toInt()
                val month = parts[1].toInt()
                val recordQ = (month - 1) / 3 + 1
                "$year-Q$recordQ" == period
            } catch (e: Exception) { false }
        }

        if (periodRecords.isEmpty()) return

        // 3. Calculate Score
        var score = 100
        var absentCount = 0
        var lateCount = 0
        var tamperedCount = 0
        var overtimeHours = 0.0

        for (att in periodRecords) {
            when (att.attendanceStatus) {
                "absent" -> absentCount++
                "late" -> lateCount++
            }
            if (att.isTimeTampered || att.isMockLocation || att.validationStatus == "invalid") {
                tamperedCount++
            }
            overtimeHours += att.overtimeHours
        }

        score -= (absentCount * 5)
        score -= (lateCount * 2)
        score -= (tamperedCount * 10)
        
        val bonus = (overtimeHours.toInt() * 1).coerceAtMost(10)
        score += bonus
        
        score = score.coerceIn(0, 100)

        val autoDetails = "Hadir: ${periodRecords.size}, Sakit/Izin/Alpha: $absentCount, Telat: $lateCount, Invalid: $tamperedCount, Bonus OT: +$bonus"

        // 4. Save/Update Score
        kpiRepo.updateScore(
            employeeId = employeeId,
            employeeName = employeeName,
            configId = attendanceConfig.configId,
            kpiName = attendanceConfig.kpiName,
            score = score,
            weight = attendanceConfig.weight,
            period = period,
            scoredBy = "system",
            source = "auto",
            autoDetails = autoDetails
        )
    }
}

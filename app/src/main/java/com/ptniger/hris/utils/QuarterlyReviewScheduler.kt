package com.ptniger.hris.utils

import com.ptniger.hris.data.model.Employee
import com.ptniger.hris.data.repository.AiReviewRepository
import com.ptniger.hris.data.repository.AttendanceRepository
import com.ptniger.hris.data.repository.KpiRepository
import com.ptniger.hris.data.repository.LeaveRepository
import com.ptniger.hris.data.repository.PeerReviewRepository
import kotlinx.coroutines.delay
import java.util.Calendar

/**
 * Scheduler untuk generate AI performance review secara otomatis di akhir kuartal.
 * Dipanggil saat app dibuka (di LoginViewModel.checkExistingSession).
 *
 * Rate limit Groq free tier: 30 req/menit → jalankan SEQUENTIAL dengan delay 2 detik.
 */
object QuarterlyReviewScheduler {

    /**
     * Return true jika bulan saat ini adalah akhir kuartal (Maret, Juni, September, Desember).
     */
    fun isEndOfQuarter(): Boolean {
        val month = Calendar.getInstance().get(Calendar.MONTH) + 1 // 1-indexed
        return month in listOf(3, 6, 9, 12)
    }

    /**
     * Return period string untuk kuartal saat ini, contoh: "2025-Q2"
     */
    fun getCurrentQuarterPeriod(): String {
        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        val quarter = (month - 1) / 3 + 1
        return "$year-Q$quarter"
    }

    /**
     * Cek apakah sudah waktunya generate review, dan trigger jika memenuhi syarat:
     * - Bulan adalah akhir kuartal
     * - Belum ada ai_review untuk periode ini
     * - Rule AI_REVIEW aktif di AutomationEngine
     */
    suspend fun checkAndTriggerIfNeeded(employees: List<Employee>) {
        if (!isEndOfQuarter()) return

        val aiRuleActive = AutomationEngine.isRuleActive(AutomationEngine.RuleType.AI_REVIEW)
        if (!aiRuleActive) return

        val period = getCurrentQuarterPeriod()
        val aiRepo = AiReviewRepository()
        val existingReviews = aiRepo.getByPeriod(period)

        // Cek apakah sudah ada review untuk periode ini
        if (existingReviews.isNotEmpty()) return

        // Trigger generate untuk semua karyawan aktif
        generateForAllEmployees(employees.filter { it.employmentStatus == "active" }, period)
    }

    /**
     * Generate AI review untuk semua karyawan secara SEQUENTIAL (bukan parallel).
     * Delay 2 detik antar request untuk menghindari Groq rate limit.
     */
    suspend fun generateForAllEmployees(employees: List<Employee>, period: String) {
        val aiRepo = AiReviewRepository()
        val attendanceRepo = AttendanceRepository()
        val kpiRepo = KpiRepository()
        val leaveRepo = LeaveRepository()
        val peerRepo = PeerReviewRepository()

        val cal = Calendar.getInstance()
        val month = cal.get(Calendar.MONTH) + 1
        val year = cal.get(Calendar.YEAR)
        val monthStr = String.format("%04d-%02d", year, month)

        employees.forEach { employee ->
            try {
                val employeeId = employee.userId.ifEmpty { employee.employeeId }
                val attendanceList = attendanceRepo.getMonthlyAttendance(employeeId, month, year)
                val kpiScores = kpiRepo.getScoresByEmployee(employeeId, period)
                val leaveHistory = leaveRepo.getByEmployee(employeeId)
                val peerReviews = peerRepo.getByTarget(employeeId, monthStr)

                val result = AiReviewEngine.generatePerformanceReview(
                    employee = employee,
                    period = period,
                    attendanceList = attendanceList,
                    kpiScores = kpiScores,
                    leaveHistory = leaveHistory,
                    peerReviews = peerReviews,
                    generatedBy = "system",
                    triggerType = "scheduled"
                )

                result.onSuccess { review -> aiRepo.save(review) }

                // Delay 2 detik untuk menghindari rate limit Groq (30 req/menit)
                delay(2000L)
            } catch (_: Exception) {
                // Skip karyawan yang error, lanjut ke berikutnya
                delay(2000L)
            }
        }
    }
}

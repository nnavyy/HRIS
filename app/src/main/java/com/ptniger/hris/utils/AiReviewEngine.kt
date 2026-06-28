package com.ptniger.hris.utils

import com.ptniger.hris.data.model.*

/**
 * Engine yang mengumpulkan semua data karyawan, membangun konteks,
 * dan memanggil Groq AI untuk menghasilkan performance review.
 */
object AiReviewEngine {

    /**
     * Bangun string konteks dari semua data karyawan.
     * Konteks ini menjadi input untuk prompt AI.
     */
    fun buildEmployeeContext(
        employee: Employee,
        period: String,
        attendanceList: List<Attendance>,
        kpiScores: List<KpiScore>,
        leaveHistory: List<LeaveRequest>,
        peerReviews: List<PeerReview>
    ): String {
        val presentDays = attendanceList.count { it.checkIn.isNotEmpty() }
        val lateDays = attendanceList.count { it.lateMinutes > 0 }
        val totalDays = attendanceList.size
        val approvedLeaves = leaveHistory.count { it.status == "approved" }
        val avgKpi = if (kpiScores.isNotEmpty()) kpiScores.map { it.score }.average() else 0.0
        val avgPeer = if (peerReviews.isNotEmpty()) peerReviews.map { it.score }.average() else 0.0

        return buildString {
            appendLine("=== DATA KARYAWAN ===")
            appendLine("Nama: ${employee.name}")
            appendLine("Posisi: ${employee.position}")
            appendLine("Departemen: ${employee.department}")
            appendLine("Periode Review: $period")
            appendLine()
            appendLine("=== KEHADIRAN ===")
            appendLine("Total hari kerja tercatat: $totalDays")
            appendLine("Hadir: $presentDays hari")
            appendLine("Terlambat: $lateDays kali")
            appendLine("Tidak hadir: ${totalDays - presentDays} hari")
            appendLine()
            appendLine("=== CUTI ===")
            appendLine("Cuti disetujui: $approvedLeaves hari")
            appendLine()
            appendLine("=== KPI ===")
            if (kpiScores.isNotEmpty()) {
                appendLine("Rata-rata skor KPI: ${"%.1f".format(avgKpi)}/100")
                kpiScores.take(5).forEach { appendLine("- ${it.kpiName}: ${it.score}/100") }
            } else {
                appendLine("Belum ada data KPI.")
            }
            appendLine()
            appendLine("=== PEER REVIEW ===")
            if (peerReviews.isNotEmpty()) {
                appendLine("Rata-rata penilaian rekan: ${"%.1f".format(avgPeer)}/100")
                appendLine("Jumlah reviewer: ${peerReviews.size}")
            } else {
                appendLine("Belum ada peer review untuk periode ini.")
            }
        }
    }

    /**
     * Bangun prompt untuk Groq AI.
     * Format output harus 4 seksi dalam Bahasa Indonesia.
     * TIDAK boleh menyebut angka gaji/payroll.
     */
    fun buildPrompt(context: String, employeeName: String, period: String): String {
        return """
Kamu adalah sistem HR yang profesional. Berdasarkan data kinerja berikut, buatlah performance review untuk karyawan $employeeName periode $period.

DATA KINERJA:
$context

INSTRUKSI:
- Gunakan Bahasa Indonesia yang profesional, objektif, dan konstruktif
- JANGAN menyebutkan angka gaji, payroll, atau kompensasi apapun
- Batasi output maksimal 400 kata
- Format output HARUS mengikuti 4 seksi berikut (gunakan heading yang sama persis):

## Ringkasan Kinerja
[Tulis 2-3 kalimat ringkasan performa secara keseluruhan]

## Kekuatan
[Tulis 2-3 poin kekuatan karyawan berdasarkan data]

## Area Pengembangan
[Tulis 2-3 poin yang perlu ditingkatkan secara konstruktif]

## Rekomendasi
[Tulis 1-2 rekomendasi konkret untuk pengembangan karyawan]
        """.trimIndent()
    }

    /**
     * Generate performance review lengkap untuk seorang karyawan.
     * @return Result<AiReview> review yang siap disimpan ke Firestore
     */
    suspend fun generatePerformanceReview(
        employee: Employee,
        period: String,
        attendanceList: List<Attendance>,
        kpiScores: List<KpiScore>,
        leaveHistory: List<LeaveRequest>,
        peerReviews: List<PeerReview>,
        generatedBy: String,
        triggerType: String = "on_demand"
    ): Result<AiReview> {
        return try {
            val context = buildEmployeeContext(
                employee, period, attendanceList, kpiScores, leaveHistory, peerReviews
            )
            val prompt = buildPrompt(context, employee.name, period)

            val reviewTextResult = GroqAiClient.generateReview(prompt)
            if (reviewTextResult.isFailure) {
                return Result.failure(reviewTextResult.exceptionOrNull() ?: Exception("AI generation failed"))
            }

            val presentDays = attendanceList.count { it.checkIn.isNotEmpty() }
            val lateDays = attendanceList.count { it.lateMinutes > 0 }
            val avgKpi = if (kpiScores.isNotEmpty()) kpiScores.map { it.score }.average() else 0.0

            val review = AiReview(
                employeeId = employee.userId.ifEmpty { employee.employeeId },
                employeeName = employee.name,
                period = period,
                generatedBy = generatedBy,
                triggerType = triggerType,
                reviewText = reviewTextResult.getOrDefault(""),
                kpiScoreSummary = avgKpi,
                attendanceSummary = "Hadir: $presentDays/${attendanceList.size} hari, Terlambat: ${lateDays}x",
                status = "draft"
            )

            Result.success(review)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

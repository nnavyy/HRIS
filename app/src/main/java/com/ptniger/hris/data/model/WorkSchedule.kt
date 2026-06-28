package com.ptniger.hris.data.model

/**
 * Jadwal kerja yang di-assign per karyawan.
 * Default: Senin–Jumat, 08:00–16:00.
 *
 * Contoh kasus (jadwal default):
 *   workStartTime       = "08:00"  → jam masuk resmi
 *   lateThreshold       = "08:15"  → check-in 08:01–08:15 = on-time, 08:16+ = LATE
 *   earlyLeaveBuffer    = "15:45"  → check-out sebelum 15:45 = early leave flag
 *   workEndTime         = "16:00"  → jam pulang resmi
 *   overtimeStartsAfter = "16:10"  → lewat ini → dihitung lembur
 *   maxOvertimeHours    = 4.0      → batas max lembur per hari
 */
data class WorkSchedule(
    val scheduleId: String = "default",
    val name: String = "Jadwal Standar",
    val workDays: List<Int> = listOf(2, 3, 4, 5, 6),  // Calendar.MONDAY..FRIDAY
    val workStartTime: String = "08:00",
    val lateThreshold: String = "08:15",
    val earlyLeaveBuffer: String = "15:45",
    val workEndTime: String = "16:00",
    val overtimeStartsAfter: String = "16:10",
    val maxOvertimeHours: Double = 4.0,
    val createdBy: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

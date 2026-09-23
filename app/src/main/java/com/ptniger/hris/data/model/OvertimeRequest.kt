package com.ptniger.hris.data.model

import com.ptniger.hris.utils.Constants

/**
 * Model Data Surat Perintah Kerja Lembur (SPKL) resmi.
 * Menjamin kepatuhan PP No. 35 Tahun 2021 dan mencegah kebocoran kas perusahaan
 * akibat lembur tanpa penugasan terverifikasi atasan.
 */
data class OvertimeRequest(
    val id: String = "",
    val employeeId: String = "",
    val employeeName: String = "",
    val managerId: String = "",
    val departmentId: String = "",
    val date: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val plannedHours: Double = 0.0,
    val actualAttendanceHours: Double = 0.0,
    val approvedPayableHours: Double = 0.0,
    val reason: String = "",
    val status: String = Constants.OvertimeStatus.PENDING,
    val approvedBy: String = "",
    val approvalNotes: String = "",
    val rejectionReason: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

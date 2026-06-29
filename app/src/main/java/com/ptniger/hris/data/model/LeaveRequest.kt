package com.ptniger.hris.data.model

data class LeaveRequest(
    val leaveId: String = "",
    val employeeId: String = "",
    val employeeName: String = "",
    val departmentId: String = "",
    val managerId: String = "",
    val requesterRole: String = "",
    val type: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val duration: Int = 0,
    val reason: String = "",
    val status: String = "pending",
    val approvedBy: String = "",
    val rejectionReason: String = "",   // alasan penolakan (manual atau auto)
    val autoRejected: Boolean = false,  // true jika ditolak sistem
    val createdAt: Long = System.currentTimeMillis()
)

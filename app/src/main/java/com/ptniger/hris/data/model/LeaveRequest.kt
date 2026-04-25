package com.ptniger.hris.data.model

data class LeaveRequest(
    val leaveId: String = "",
    val employeeId: String = "",
    val employeeName: String = "",
    val type: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val duration: Int = 0,
    val reason: String = "",
    val status: String = "pending",
    val approvedBy: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

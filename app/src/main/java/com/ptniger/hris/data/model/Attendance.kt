package com.ptniger.hris.data.model

data class Attendance(
    val attendanceId: String = "",
    val employeeId: String = "",
    val date: String = "",
    val checkIn: String = "",
    val checkOut: String = "",
    val status: String = "",
    val lateMinutes: Int = 0,
    val overtimeHours: Double = 0.0,
    val location: String = ""
)

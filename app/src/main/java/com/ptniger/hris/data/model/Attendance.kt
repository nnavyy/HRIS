package com.ptniger.hris.data.model

data class Attendance(
    val attendanceId: String = "",
    val employeeId: String = "",
    val date: String = "",
    val type: String = "clock_in", // clock_in or clock_out
    val checkIn: String = "",
    val checkOut: String = "",
    val status: String = "valid", // valid, invalid_location, need_review, etc
    val lateMinutes: Int = 0,
    val overtimeHours: Double = 0.0,
    val location: String = "", // Keeping for backward compatibility (could be address string)
    
    // Selfie & Geofencing Data
    val selfieUrl: String = "",
    val selfieStoragePath: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val accuracy: Float = 0f,
    val officeId: String = "",
    val officeLatitude: Double = 0.0,
    val officeLongitude: Double = 0.0,
    val distanceFromOfficeMeters: Double = 0.0,
    val isWithinOfficeRadius: Boolean = false,
    
    // Device Metadata
    val deviceModel: String = "",
    val deviceId: String = "",
    val appVersion: String = "",
    
    val createdAt: Long = System.currentTimeMillis()
)

package com.ptniger.hris.data.model

data class Attendance(
    val attendanceId: String = "",
    val employeeId: String = "",
    val date: String = "",
    val clockType: String = "clock_in", // clock_in or clock_out
    val checkIn: String = "",
    val checkOut: String = "",
    val attendanceStatus: String = "present", // present, late, absent, leave, holiday
    val validationStatus: String = "valid", // valid, invalid_location, need_review, rejected, approved_manually
    val lateMinutes: Int = 0,
    val overtimeHours: Double = 0.0,
    val location: String = "", // Legacy
    
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
    
    // Anti-Fraud Detection
    val isMockLocation: Boolean = false,
    val serverTimestamp: Long = 0,
    val isTimeTampered: Boolean = false,
    val deviceTimestamp: Long = 0,
    
    // Device Metadata
    val deviceModel: String = "",
    val deviceId: String = "",
    val appVersion: String = "",
    
    // Schedule & Early Leave
    val isEarlyLeave: Boolean = false,       // flag pulang sebelum earlyLeaveBuffer
    val workScheduleId: String = "",         // jadwal yang dipakai saat absen (untuk audit)
    
    // Face Recognition
    val checkInMode: String = "selfie",        // "selfie" | "face_recognition"
    val faceRecognitionSimilarity: Float = 0f, // similarity score saat check-in
    val livenessVerified: Boolean = false,     // apakah liveness check passed
    
    val createdAt: Long = System.currentTimeMillis()
)

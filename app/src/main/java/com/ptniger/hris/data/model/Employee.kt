package com.ptniger.hris.data.model

data class Employee(
    val employeeId: String = "",
    val nik: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val position: String = "",
    val department: String = "",
    val branch: String = "",
    val officeId: String = "", // Added for Geofencing Location binding
    val managerId: String = "", // Added for Payroll workflow
    val joinDate: String = "",
    val employmentStatus: String = "active",
    val baseSalary: Double = 0.0,
    val leaveQuota: Int = 12,
    val userId: String = "",
    val workScheduleId: String = "default",   // jadwal kerja yang di-assign
    val faceEmbedding: List<Float> = emptyList(),
    val faceRegisteredAt: Long = 0L,
    val faceRegisteredBy: String = "",
    val isFaceRegistered: Boolean = false
)

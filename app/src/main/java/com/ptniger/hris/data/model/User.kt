package com.ptniger.hris.data.model

data class User(
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = "",
    val employeeId: String = "",
    val departmentId: String = "",
    val branch: String = "",
    val officeId: String = "", // Added for Geofencing Location binding
    val status: String = "active",
    val createdAt: Long = System.currentTimeMillis()
)

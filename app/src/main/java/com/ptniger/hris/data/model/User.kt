package com.ptniger.hris.data.model

data class User(
    val userId: String = "",
    val uid: String = "",
    val name: String = "",
    val fullName: String = "",
    val email: String = "",
    val role: String = "", // Legacy support
    val roles: List<String> = emptyList(),
    val primaryRole: String = "",
    val employeeId: String = "",
    val nik: String = "",
    val departmentId: String = "",
    val branch: String = "",
    val officeId: String = "",
    val status: String = "active", // Legacy support
    val isActive: Boolean = true,
    val photoUrl: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

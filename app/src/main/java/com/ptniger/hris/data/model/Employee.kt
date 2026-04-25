package com.ptniger.hris.data.model

data class Employee(
    val employeeId: String = "",
    val nik: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val role: String = "", // <-- Ditambahkan untuk filter berdasar role
    val position: String = "",
    val department: String = "",
    val branch: String = "",
    val joinDate: String = "",
    val employmentStatus: String = "active",
    val baseSalary: Double = 0.0,
    val leaveQuota: Int = 12,
    val userId: String = ""
)

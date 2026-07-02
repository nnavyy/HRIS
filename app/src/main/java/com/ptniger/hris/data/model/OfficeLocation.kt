package com.ptniger.hris.data.model

data class OfficeLocation(
    val id: String = "",
    val name: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val allowedRadiusMeters: Double = 100.0,
    val isActive: Boolean = true,

    // Company Profile
    val companyName: String = "",
    val companyAddress: String = "",
    val companyPhone: String = "",
    val companyEmail: String = "",
    val companyNpwp: String = "",
    val companyLogoUrl: String = ""
)

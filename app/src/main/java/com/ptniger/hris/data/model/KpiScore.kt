package com.ptniger.hris.data.model

data class KpiScore(
    val scoreId: String = "",
    val employeeId: String = "",
    val employeeName: String = "",
    val configId: String = "",
    val kpiName: String = "",
    val score: Int = 0,
    val weight: Double = 0.0,
    val weightedScore: Double = 0.0,
    val period: String = "",
    val scoredBy: String = "",
    val source: String = "manual", // "manual" or "auto"
    val autoDetails: String = "",  // details for auto calculated score
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

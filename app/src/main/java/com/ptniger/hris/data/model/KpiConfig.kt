package com.ptniger.hris.data.model

data class KpiConfig(
    val configId: String = "",
    val department: String = "",
    val position: String = "",
    val kpiName: String = "",
    val weight: Double = 0.0,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

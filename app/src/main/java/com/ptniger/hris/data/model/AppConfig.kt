package com.ptniger.hris.data.model

data class AppConfig(
    val configId: String = "",
    val key: String = "",
    val value: String = "",
    val description: String = "",
    val isSecret: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

package com.ptniger.hris.data.model

data class AuditLog(
    val logId: String = "",
    val userId: String = "",
    val userName: String = "",
    val action: String = "",
    val targetCollection: String = "",
    val targetId: String = "",
    val details: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

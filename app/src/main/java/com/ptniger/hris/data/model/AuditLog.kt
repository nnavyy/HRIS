package com.ptniger.hris.data.model

data class AuditLog(
    val logId: String = "",
    val userId: String = "",
    val userName: String = "",
    val actorRole: String = "",
    val action: String = "",
    val module: String = "",
    val targetCollection: String = "",
    val targetId: String = "",
    val targetUserId: String = "",
    val oldValue: String = "",
    val newValue: String = "",
    val details: String = "",
    val ipAddress: String = "",
    val deviceInfo: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

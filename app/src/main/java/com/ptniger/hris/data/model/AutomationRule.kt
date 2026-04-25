package com.ptniger.hris.data.model

data class AutomationRule(
    val ruleId: String = "",
    val name: String = "",
    val type: String = "",
    val description: String = "",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

package com.ptniger.hris.data.model

data class LeavePolicy(
    val policyId: String = "",
    val companyId: String = "default",
    val minAdvanceDays: Int = 3,                    // minimal H-3 sebelum tanggal mulai
    val maxDaysPerRequest: Int = 12,                // max hari per 1 pengajuan
    val allowPastDateSubmission: Boolean = false,   // bolehkah ajukan cuti tanggal lalu?
    val autoRejectOnExpiry: Boolean = true,          // auto tolak kalau melebihi batas
    val updatedBy: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

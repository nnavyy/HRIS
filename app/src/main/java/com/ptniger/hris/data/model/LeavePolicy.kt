package com.ptniger.hris.data.model

data class LeavePolicy(
    val policyId: String = "",
    val companyId: String = "default",
    val minAdvanceDays: Int = 3,                    // minimal H-3 sebelum tanggal mulai
    val maxDaysPerRequest: Int = 12,                // max hari per 1 pengajuan
    val allowPastDateSubmission: Boolean = false,   // bolehkah ajukan cuti tanggal lalu? (global)
    val autoRejectOnExpiry: Boolean = true,          // auto tolak kalau melebihi batas
    val emergencyLeaveTypes: List<String> = listOf("Sakit", "Darurat", "Melahirkan"), // bypass H-N
    val allowPastDateForTypes: List<String> = listOf("Sakit", "Darurat"), // boleh H minus
    val maxPastDays: Int = 3, // batas maksimal mundur hari untuk emergency
    val updatedBy: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

package com.ptniger.hris.data.model

data class PeerReview(
    val reviewId: String = "",
    val targetEmployeeId: String = "",    // karyawan yang dinilai
    val reviewerEmployeeId: String = "",  // karyawan yang menilai
    val period: String = "",              // format: "YYYY-MM"
    val score: Int = 0,                   // 0-100
    val comments: String = "",
    val dimension: String = "",           // KpiDimension yang direview
    val createdAt: Long = System.currentTimeMillis()
)

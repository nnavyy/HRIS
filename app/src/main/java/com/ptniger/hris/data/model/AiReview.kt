package com.ptniger.hris.data.model

data class AiReview(
    val reviewId: String = "",
    val employeeId: String = "",
    val employeeName: String = "",
    val period: String = "",              // format: "2025-Q1"
    val generatedAt: Long = System.currentTimeMillis(),
    val generatedBy: String = "",         // userId HR/Manager yang trigger, atau "system" jika scheduled
    val triggerType: String = "",         // "on_demand" | "scheduled"
    val reviewText: String = "",          // output teks dari Groq
    val modelUsed: String = "llama-3.3-70b-versatile",
    val kpiScoreSummary: Double = 0.0,
    val attendanceSummary: String = "",   // contoh: "Hadir: 20/22 hari, Terlambat: 2x"
    val status: String = "draft"          // "draft" | "published"
)

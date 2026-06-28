package com.ptniger.hris.data.model

/**
 * Dimensi penilaian KPI.
 * Existing: PERFORMANCE, ATTENDANCE, DISCIPLINE, WORKLOAD
 * New (Phase 2): OUTPUT_QUALITY, PEER_REVIEW, GOAL_ACHIEVEMENT, TEAM_CONTRIBUTION
 */
object KpiDimension {
    const val PERFORMANCE        = "performance"
    const val ATTENDANCE         = "attendance"
    const val DISCIPLINE         = "discipline"
    const val WORKLOAD           = "workload"
    const val OUTPUT_QUALITY     = "output_quality"     // NEW
    const val PEER_REVIEW        = "peer_review"        // NEW
    const val GOAL_ACHIEVEMENT   = "goal_achievement"   // NEW
    const val TEAM_CONTRIBUTION  = "team_contribution"  // NEW
}

data class KpiConfig(
    val configId: String = "",
    val department: String = "",
    val position: String = "",
    val kpiName: String = "",
    val weight: Double = 0.0,
    val description: String = "",
    
    // New fields (Phase 2)
    val dimension: String = KpiDimension.PERFORMANCE,    // pakai konstanta KpiDimension
    val goalTarget: Double? = null,                      // untuk tipe GOAL_ACHIEVEMENT
    val goalActual: Double? = null,                      // realisasi target
    val reviewers: List<String> = emptyList(),           // userId reviewer (untuk PEER_REVIEW)
    
    val createdAt: Long = System.currentTimeMillis()
)

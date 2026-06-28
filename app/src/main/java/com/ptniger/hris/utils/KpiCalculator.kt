package com.ptniger.hris.utils

object KpiCalculator {

    data class BonusTier(
        val minScore: Double,
        val bonusPercentage: Double,
        val label: String
    )

    val bonusTiers = listOf(
        BonusTier(90.0, 0.15, "Excellent (15%)"),
        BonusTier(80.0, 0.10, "Good (10%)"),
        BonusTier(70.0, 0.05, "Fair (5%)"),
        BonusTier(60.0, 0.0, "Adequate (0%)"),
        BonusTier(0.0, 0.0, "Needs Improvement")
    )

    fun calculateWeightedScore(scores: List<Pair<Int, Double>>): Double {
        return scores.sumOf { (score, weight) -> score * weight }
    }

    fun getBonusPercentage(totalScore: Double): Double {
        return bonusTiers.firstOrNull { totalScore >= it.minScore }?.bonusPercentage ?: 0.0
    }

    fun getBonusLabel(totalScore: Double): String {
        return bonusTiers.firstOrNull { totalScore >= it.minScore }?.label ?: "N/A"
    }

    fun calculateKpiBonus(baseSalary: Double, totalKpiScore: Double): Double {
        val percentage = getBonusPercentage(totalKpiScore)
        return baseSalary * percentage
    }

    fun calculateNetSalary(
        baseSalary: Double,
        allowance: Double,
        overtimePay: Double,
        kpiBonus: Double,
        deductions: Double
    ): Double {
        return baseSalary + allowance + overtimePay + kpiBonus - deductions
    }

    // ─── NEW FUNCTIONS (Phase 2 KPI Overhaul) ───

    /**
     * Hitung goal achievement score berdasarkan realisasi vs target.
     * Score = (actual / target) * 100, dibatasi maksimum 100.
     */
    fun calculateGoalScore(target: Double, actual: Double): Int {
        if (target <= 0) return 0
        return ((actual / target) * 100).coerceAtMost(100.0).toInt()
    }

    /**
     * Hitung attendance KPI score berdasarkan data absensi bulan tersebut.
     * Formula: (presentDays/totalWorkDays * 70) + (1 - lateRate) * 20 + (1 - absentRate) * 10
     */
    fun calculateAttendanceScore(
        totalWorkDays: Int,
        presentDays: Int,
        lateDays: Int,
        absentDays: Int
    ): Int {
        if (totalWorkDays <= 0) return 0
        val presentRate = presentDays.toDouble() / totalWorkDays
        val lateRate = lateDays.toDouble() / totalWorkDays
        val absentRate = absentDays.toDouble() / totalWorkDays
        val score = (presentRate * 70) + ((1 - lateRate) * 20) + ((1 - absentRate) * 10)
        return score.coerceIn(0.0, 100.0).toInt()
    }

    /**
     * Hitung discipline score berdasarkan jumlah warning & violation.
     * Setiap warning mengurangi 10 poin, setiap violation mengurangi 20 poin.
     * Nilai minimum 0.
     */
    fun calculateDisciplineScore(warningCount: Int, violationCount: Int): Int {
        val score = 100 - (warningCount * 10) - (violationCount * 20)
        return score.coerceAtLeast(0)
    }

    /**
     * Aggregate semua dimensi menjadi satu weighted score.
     * @param scores Map dari dimension name ke score (0-100)
     * @param weights Map dari dimension name ke weight (total harus = 1.0)
     * @return weighted score (0.0 - 100.0)
     */
    fun calculateAggregateKpiScore(
        scores: Map<String, Int>,
        weights: Map<String, Double>
    ): Double {
        if (scores.isEmpty() || weights.isEmpty()) return 0.0
        var totalWeight = 0.0
        var weightedSum = 0.0
        scores.forEach { (dimension, score) ->
            val weight = weights[dimension] ?: 0.0
            weightedSum += score * weight
            totalWeight += weight
        }
        if (totalWeight == 0.0) return 0.0
        return (weightedSum / totalWeight).coerceIn(0.0, 100.0)
    }
}

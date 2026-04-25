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
}

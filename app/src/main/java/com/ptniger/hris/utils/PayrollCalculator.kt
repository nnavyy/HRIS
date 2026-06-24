package com.ptniger.hris.utils

object PayrollCalculator {

    // Overtime Calculation based on UU Cipta Kerja (PP 35/2021)
    fun calculateOvertime(baseSalary: Double, fixedAllowance: Double, overtimeHours: Double): Double {
        if (overtimeHours <= 0) return 0.0
        
        val hourlyWage = (baseSalary + fixedAllowance) / 173.0
        
        // Hour 1 is 1.5x, subsequent hours are 2x
        var totalPay = 0.0
        if (overtimeHours > 0) {
            val hour1 = minOf(1.0, overtimeHours)
            totalPay += hour1 * 1.5 * hourlyWage
        }
        if (overtimeHours > 1) {
            val subsequentHours = overtimeHours - 1.0
            totalPay += subsequentHours * 2.0 * hourlyWage
        }
        
        return totalPay
    }

    // BPJS Kesehatan (1% from Employee), max calculation base is Rp 12.000.000
    fun calculateBpjsKesehatan(baseSalary: Double, fixedAllowance: Double): Double {
        val totalWage = baseSalary + fixedAllowance
        val cap = 12_000_000.0
        val baseCalc = minOf(totalWage, cap)
        return baseCalc * 0.01
    }

    // BPJS Ketenagakerjaan JHT (2% from Employee)
    fun calculateBpjsJht(baseSalary: Double, fixedAllowance: Double): Double {
        val totalWage = baseSalary + fixedAllowance
        return totalWage * 0.02
    }

    // BPJS Ketenagakerjaan JP (1% from Employee), max calculation base is around Rp 10.042.300
    fun calculateBpjsJp(baseSalary: Double, fixedAllowance: Double): Double {
        val totalWage = baseSalary + fixedAllowance
        val cap = 10_042_300.0
        val baseCalc = minOf(totalWage, cap)
        return baseCalc * 0.01
    }

    fun calculateNetSalary(
        baseSalary: Double,
        allowance: Double,
        overtimePay: Double,
        kpiBonus: Double,
        bpjsKes: Double,
        bpjsJht: Double,
        bpjsJp: Double,
        otherDeductions: Double
    ): Double {
        val gross = baseSalary + allowance + overtimePay + kpiBonus
        val totalDeductions = bpjsKes + bpjsJht + bpjsJp + otherDeductions
        return gross - totalDeductions
    }
}

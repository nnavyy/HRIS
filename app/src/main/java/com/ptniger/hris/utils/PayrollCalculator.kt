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

    // ─── NEW FUNCTIONS (Phase 1 Payroll Overhaul) ───

    /** Total tunjangan: makan + transport + jabatan */
    fun calculateTotalAllowance(
        allowanceMeal: Double,
        allowanceTransport: Double,
        allowancePosition: Double
    ): Double = allowanceMeal + allowanceTransport + allowancePosition

    /**
     * BPJS JKK (Jaminan Kecelakaan Kerja) — ditanggung perusahaan (PP 44/2015)
     * Default rate 0.0024 = 0.24% (risiko kerja tingkat 1)
     */
    fun calculateBpjsJkk(baseSalary: Double, jkkRate: Double = 0.0024): Double =
        baseSalary * jkkRate

    /**
     * BPJS JKM (Jaminan Kematian) — ditanggung perusahaan (PP 44/2015)
     * Rate tetap 0.3%
     */
    fun calculateBpjsJkm(baseSalary: Double, jkmRate: Double = 0.003): Double =
        baseSalary * jkmRate

    /**
     * PPh 21 metode TER (Tarif Efektif Rata-rata) berdasarkan PMK 168/2023.
     * Berlaku mulai Januari 2024.
     *
     * Kategori:
     * - Kategori A : TK/0
     * - Kategori B : TK/1, TK/2, TK/3, K/0
     * - Kategori C : K/1, K/2, K/3
     *
     * Tabel tarif (grossMonthly → tariff %) diimplementasi sebagai lookup.
     */
    fun calculatePph21Ter(grossMonthly: Double, ptkpStatus: String): Double {
        val category = when (ptkpStatus.uppercase()) {
            "TK/0"                          -> "A"
            "TK/1", "TK/2", "TK/3", "K/0" -> "B"
            "K/1", "K/2", "K/3"            -> "C"
            else                            -> "A"
        }

        // Tarif Efektif Rata-rata Bulanan per PMK 168/2023
        // Format: Pair(batas_atas_penghasilan, tarif_persen)
        // Jika penghasilan <= batas_atas → gunakan tarif ini
        val tableA = listOf(
            5_400_000.0 to 0.0,
            5_650_000.0 to 0.0025,
            5_950_000.0 to 0.005,
            6_300_000.0 to 0.0075,
            6_750_000.0 to 0.01,
            7_500_000.0 to 0.0125,
            8_550_000.0 to 0.015,
            9_650_000.0 to 0.0175,
            10_050_000.0 to 0.02,
            10_350_000.0 to 0.0225,
            10_700_000.0 to 0.025,
            11_050_000.0 to 0.03,
            11_600_000.0 to 0.035,
            12_500_000.0 to 0.04,
            13_750_000.0 to 0.05,
            15_100_000.0 to 0.06,
            16_950_000.0 to 0.07,
            19_750_000.0 to 0.08,
            24_150_000.0 to 0.09,
            26_450_000.0 to 0.10,
            28_000_000.0 to 0.11,
            30_050_000.0 to 0.12,
            32_400_000.0 to 0.13,
            35_400_000.0 to 0.14,
            39_100_000.0 to 0.15,
            43_850_000.0 to 0.16,
            47_800_000.0 to 0.17,
            51_400_000.0 to 0.18,
            56_300_000.0 to 0.19,
            62_200_000.0 to 0.20,
            68_600_000.0 to 0.21,
            77_500_000.0 to 0.22,
            89_000_000.0 to 0.23,
            Double.MAX_VALUE to 0.24
        )
        val tableB = listOf(
            6_200_000.0 to 0.0,
            6_500_000.0 to 0.0025,
            6_850_000.0 to 0.005,
            7_300_000.0 to 0.0075,
            9_200_000.0 to 0.01,
            10_750_000.0 to 0.015,
            11_250_000.0 to 0.02,
            11_600_000.0 to 0.025,
            12_600_000.0 to 0.03,
            13_600_000.0 to 0.04,
            14_950_000.0 to 0.05,
            16_400_000.0 to 0.06,
            18_450_000.0 to 0.07,
            21_850_000.0 to 0.08,
            26_000_000.0 to 0.09,
            27_700_000.0 to 0.10,
            29_350_000.0 to 0.11,
            31_450_000.0 to 0.12,
            33_950_000.0 to 0.13,
            37_100_000.0 to 0.14,
            41_100_000.0 to 0.15,
            45_800_000.0 to 0.16,
            49_500_000.0 to 0.17,
            53_800_000.0 to 0.18,
            58_500_000.0 to 0.19,
            64_000_000.0 to 0.20,
            71_000_000.0 to 0.21,
            80_000_000.0 to 0.22,
            93_000_000.0 to 0.23,
            Double.MAX_VALUE to 0.24
        )
        val tableC = listOf(
            6_600_000.0 to 0.0,
            6_950_000.0 to 0.0025,
            7_350_000.0 to 0.005,
            7_800_000.0 to 0.0075,
            8_850_000.0 to 0.01,
            9_800_000.0 to 0.0125,
            10_350_000.0 to 0.015,
            10_700_000.0 to 0.02,
            11_050_000.0 to 0.025,
            11_600_000.0 to 0.03,
            12_500_000.0 to 0.04,
            14_150_000.0 to 0.05,
            15_550_000.0 to 0.06,
            17_050_000.0 to 0.07,
            19_500_000.0 to 0.08,
            22_700_000.0 to 0.09,
            24_200_000.0 to 0.10,
            26_600_000.0 to 0.11,
            28_100_000.0 to 0.12,
            30_100_000.0 to 0.13,
            32_600_000.0 to 0.14,
            35_600_000.0 to 0.15,
            39_600_000.0 to 0.16,
            43_200_000.0 to 0.17,
            47_500_000.0 to 0.18,
            51_200_000.0 to 0.19,
            56_600_000.0 to 0.20,
            62_600_000.0 to 0.21,
            70_600_000.0 to 0.22,
            83_200_000.0 to 0.23,
            Double.MAX_VALUE to 0.24
        )

        val table = when (category) {
            "A" -> tableA
            "B" -> tableB
            else -> tableC
        }

        val rate = table.firstOrNull { grossMonthly <= it.first }?.second ?: 0.24
        return grossMonthly * rate
    }

    /**
     * Kalkulasi gaji bersih dengan semua komponen (full net salary).
     * gross = baseSalary + tunjangan + overtime + kpiBonus
     * net = gross - (bpjsKes + bpjsJht + bpjsJp + pph21 + otherDeductions)
     */
    fun calculateFullNetSalary(
        baseSalary: Double,
        allowanceMeal: Double,
        allowanceTransport: Double,
        allowancePosition: Double,
        overtimePay: Double,
        kpiBonus: Double,
        bpjsKes: Double,
        bpjsJht: Double,
        bpjsJp: Double,
        pph21: Double,
        otherDeductions: Double
    ): Double {
        val totalAllowance = calculateTotalAllowance(allowanceMeal, allowanceTransport, allowancePosition)
        val gross = baseSalary + totalAllowance + overtimePay + kpiBonus
        val totalDeductions = bpjsKes + bpjsJht + bpjsJp + pph21 + otherDeductions
        return gross - totalDeductions
    }
}

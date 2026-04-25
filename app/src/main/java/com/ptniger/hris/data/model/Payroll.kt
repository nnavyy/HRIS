package com.ptniger.hris.data.model

data class Payroll(
    val payrollId: String = "",
    val employeeId: String = "",
    val employeeName: String = "",
    val month: Int = 0,
    val year: Int = 0,
    val baseSalary: Double = 0.0,
    val allowance: Double = 0.0,
    val overtimePay: Double = 0.0,
    val kpiScore: Double = 0.0,
    val kpiBonus: Double = 0.0,
    val deductions: Double = 0.0,
    val netSalary: Double = 0.0,
    val status: String = "draft",
    val generatedAt: Long = System.currentTimeMillis()
)

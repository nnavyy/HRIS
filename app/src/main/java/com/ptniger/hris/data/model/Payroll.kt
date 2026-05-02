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
    val generatedAt: Long = System.currentTimeMillis(),
    
    // Authorization Context
    val managerId: String = "",
    val departmentId: String = "",
    
    // Approval Workflow Fields
    val requestedByFinanceId: String = "",
    val requestedAt: Long = 0,
    val approvedByManagerId: String = "",
    val approvedAt: Long = 0,
    val approvalNotes: String = "",
    val rejectedByManagerId: String = "",
    val rejectedAt: Long = 0,
    val rejectionReason: String = "",
    val finalizedByFinanceId: String = "",
    val finalizedAt: Long = 0,
    val paidByFinanceId: String = "",
    val paidAt: Long = 0
)

package com.ptniger.hris.data.model

data class Payroll(
    val payrollId: String = "",
    val employeeId: String = "",
    val employeeName: String = "",
    val month: Int = 0,
    val year: Int = 0,
    val baseSalary: Double = 0.0,
    val allowance: Double = 0.0,              // legacy field (kept for backward compat)
    
    // New: itemized allowances from contract (Phase 1)
    val allowanceMeal: Double = 0.0,
    val allowanceTransport: Double = 0.0,
    val allowancePosition: Double = 0.0,
    
    val overtimeHours: Double = 0.0,
    val overtimePay: Double = 0.0,
    val kpiScore: Double = 0.0,
    val kpiBonus: Double = 0.0,
    
    // Potongan karyawan (existing)
    val bpjsKesehatan: Double = 0.0,
    val bpjsJht: Double = 0.0,
    val bpjsJp: Double = 0.0,
    
    // New: tanggungan perusahaan (informasi saja, tidak dipotong dari gaji)
    val bpjsJkk: Double = 0.0,
    val bpjsJkm: Double = 0.0,
    
    // New: PPh 21 TER
    val pph21: Double = 0.0,
    val ptkpStatus: String = "TK/0",
    
    // New: kontrak referensi
    val contractId: String = "",
    
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

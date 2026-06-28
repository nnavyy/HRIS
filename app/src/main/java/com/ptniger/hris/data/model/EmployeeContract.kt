package com.ptniger.hris.data.model

data class EmployeeContract(
    val contractId: String = "",
    val employeeId: String = "",
    val effectiveDate: String = "",          // format: YYYY-MM-DD
    val baseSalary: Double = 0.0,
    val allowanceMeal: Double = 0.0,         // tunjangan makan
    val allowanceTransport: Double = 0.0,    // tunjangan transport
    val allowancePosition: Double = 0.0,     // tunjangan jabatan
    val bpjsJkkRate: Double = 0.0024,        // PP 44/2015 risiko kerja 1: 0.24%
    val bpjsJkmRate: Double = 0.003,         // PP 44/2015 jaminan kematian: 0.3%
    val ptkpStatus: String = "TK/0",         // TK/0, TK/1, K/0, K/1, K/2, K/3 untuk PPh 21
    val overrideOvertimeMultiplier: Boolean = false,
    val overtime1xMultiplier: Double = 1.5,  // jam lembur ke-1
    val overtimeNxMultiplier: Double = 2.0,  // jam lembur ke-2 dst
    val signedByEmployee: Boolean = false,
    val signedAt: Long = 0,
    val signatureData: String = "",          // base64 string dari tanda tangan digital
    val createdBy: String = "",              // userId HR yang membuat kontrak
    val createdAt: Long = System.currentTimeMillis()
)

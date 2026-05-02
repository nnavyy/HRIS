package com.ptniger.hris.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.ptniger.hris.data.model.Payroll
import com.ptniger.hris.utils.AutomationEngine
import com.ptniger.hris.utils.Constants
import com.ptniger.hris.utils.KpiCalculator
import kotlinx.coroutines.tasks.await

class PayrollRepository {
    private val db = FirebaseFirestore.getInstance()
    private val col = db.collection(Constants.Collections.PAYROLLS)
    private val kpiRepo = KpiRepository()

    suspend fun generate(
        employeeId: String, employeeName: String,
        month: Int, year: Int,
        baseSalary: Double, allowance: Double,
        overtimePay: Double, deductions: Double
    ): Result<String> {
        return try {
            val period = String.format("%04d-%02d", year, month)
            
            // Check if payroll KPI bonus automation is enabled
            val kpiBonusEnabled = AutomationEngine.isRuleActive(AutomationEngine.RuleType.PAYROLL)
            
            val kpiScore = if (kpiBonusEnabled) kpiRepo.getTotalWeightedScore(employeeId, period) else 0.0
            val kpiBonus = if (kpiBonusEnabled) KpiCalculator.calculateKpiBonus(baseSalary, kpiScore) else 0.0
            val net = KpiCalculator.calculateNetSalary(baseSalary, allowance, overtimePay, kpiBonus, deductions)

            val payroll = Payroll(
                employeeId = employeeId, employeeName = employeeName,
                month = month, year = year, baseSalary = baseSalary,
                allowance = allowance, overtimePay = overtimePay,
                kpiScore = kpiScore, kpiBonus = kpiBonus,
                deductions = deductions, netSalary = net
            )
            val ref = col.add(payroll).await()
            
            // Audit log if enabled
            val auditEnabled = AutomationEngine.isRuleActive(AutomationEngine.RuleType.AUDIT)
            if (auditEnabled) {
                AuditLogRepository().log(
                    userId = employeeId,
                    userName = employeeName,
                    action = "PAYROLL_GENERATED",
                    module = "Payroll",
                    targetCollection = Constants.Collections.PAYROLLS,
                    targetId = ref.id,
                    details = "Period: $period, Net: $net, KPI Bonus: $kpiBonus (enabled=$kpiBonusEnabled)"
                )
            }
            
            Result.success(ref.id)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getByEmployee(employeeId: String): List<Payroll> {
        return try {
            col.whereEqualTo("employeeId", employeeId)
                .orderBy("generatedAt", Query.Direction.DESCENDING)
                .get().await().documents.mapNotNull {
                    it.toObject(Payroll::class.java)?.copy(payrollId = it.id)
                }
        } catch (e: Exception) { emptyList() }
    }

    suspend fun getByPeriod(month: Int, year: Int): List<Payroll> {
        return try {
            col.whereEqualTo("month", month).whereEqualTo("year", year)
                .get().await().documents.mapNotNull {
                    it.toObject(Payroll::class.java)?.copy(payrollId = it.id)
                }
        } catch (e: Exception) { emptyList() }
    }

    suspend fun getAll(): List<Payroll> {
        return try {
            col.orderBy("generatedAt", Query.Direction.DESCENDING)
                .get().await().documents.mapNotNull {
                    it.toObject(Payroll::class.java)?.copy(payrollId = it.id)
                }
        } catch (e: Exception) { emptyList() }
    }

    suspend fun updateStatus(payrollId: String, status: String): Result<Unit> {
        return try {
            col.document(payrollId).update("status", status).await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun requestApproval(payrollId: String, financeId: String): Result<Unit> {
        return try {
            val doc = col.document(payrollId).get().await()
            val payroll = doc.toObject(Payroll::class.java) ?: throw Exception("Payroll not found")
            if (payroll.status != Constants.PayrollStatus.DRAFT) throw Exception("Only draft payrolls can request approval")
            
            col.document(payrollId).update(
                mapOf(
                    "status" to Constants.PayrollStatus.PENDING_APPROVAL,
                    "requestedByFinanceId" to financeId,
                    "requestedAt" to System.currentTimeMillis()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun processApproval(payrollId: String, managerId: String, isApproved: Boolean, notes: String): Result<Unit> {
        return try {
            val doc = col.document(payrollId).get().await()
            val payroll = doc.toObject(Payroll::class.java) ?: throw Exception("Payroll not found")
            if (payroll.status != Constants.PayrollStatus.PENDING_APPROVAL) throw Exception("Payroll is not pending approval")
            if (payroll.managerId != managerId) throw Exception("Unauthorized approval: You are not the manager for this payroll")
            
            val status = if (isApproved) Constants.PayrollStatus.APPROVED else Constants.PayrollStatus.REJECTED
            val updates = mutableMapOf<String, Any>(
                "status" to status,
                "approvalNotes" to notes
            )
            if (isApproved) {
                updates["approvedByManagerId"] = managerId
                updates["approvedAt"] = System.currentTimeMillis()
            } else {
                updates["rejectedByManagerId"] = managerId
                updates["rejectedAt"] = System.currentTimeMillis()
                updates["rejectionReason"] = notes
            }
            
            col.document(payrollId).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getPendingApprovals(managerId: String): List<Payroll> {
        return try {
            col.whereEqualTo("status", Constants.PayrollStatus.PENDING_APPROVAL)
               .whereEqualTo("managerId", managerId)
                .get().await().documents.mapNotNull {
                    it.toObject(Payroll::class.java)?.copy(payrollId = it.id)
                }
        } catch (e: Exception) { emptyList() }
    }

    suspend fun finalizePayroll(payrollId: String, financeId: String): Result<Unit> {
        return try {
            val doc = col.document(payrollId).get().await()
            val payroll = doc.toObject(Payroll::class.java) ?: throw Exception("Payroll not found")
            if (payroll.status != Constants.PayrollStatus.APPROVED) throw Exception("Only approved payrolls can be finalized")
            
            col.document(payrollId).update(
                mapOf(
                    "status" to Constants.PayrollStatus.FINALIZED,
                    "finalizedByFinanceId" to financeId,
                    "finalizedAt" to System.currentTimeMillis()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun markPayrollAsPaid(payrollId: String, financeId: String): Result<Unit> {
        return try {
            val doc = col.document(payrollId).get().await()
            val payroll = doc.toObject(Payroll::class.java) ?: throw Exception("Payroll not found")
            if (payroll.status != Constants.PayrollStatus.FINALIZED) throw Exception("Only finalized payrolls can be paid")
            
            col.document(payrollId).update(
                mapOf(
                    "status" to Constants.PayrollStatus.PAID,
                    "paidByFinanceId" to financeId,
                    "paidAt" to System.currentTimeMillis()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }
}

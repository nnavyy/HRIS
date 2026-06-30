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
            
            // Fetch active contract for itemized allowances & PTKP status
            val contractRepo = ContractRepository()
            val contract = contractRepo.getActiveContract(employeeId)
            
            val allowanceMeal = contract?.allowanceMeal ?: 0.0
            val allowanceTransport = contract?.allowanceTransport ?: 0.0
            val allowancePosition = contract?.allowancePosition ?: 0.0
            val ptkpStatus = contract?.ptkpStatus ?: "TK/0"
            val contractId = contract?.contractId ?: ""
            
            // Total allowance (itemized if contract exists, else legacy allowance)
            val totalAllowance = if (contract != null)
                com.ptniger.hris.utils.PayrollCalculator.calculateTotalAllowance(allowanceMeal, allowanceTransport, allowancePosition)
            else allowance
            
            // Check if payroll KPI bonus automation is enabled
            val kpiBonusEnabled = AutomationEngine.isRuleActive(AutomationEngine.RuleType.PAYROLL)
            val kpiScore = if (kpiBonusEnabled) kpiRepo.getTotalWeightedScore(employeeId, period) else 0.0
            val kpiBonus = if (kpiBonusEnabled) KpiCalculator.calculateKpiBonus(baseSalary, kpiScore) else 0.0
            
            // BPJS potongan karyawan (existing functions, unchanged)
            val bpjsKes = com.ptniger.hris.utils.PayrollCalculator.calculateBpjsKesehatan(baseSalary, totalAllowance)
            val bpjsJht = com.ptniger.hris.utils.PayrollCalculator.calculateBpjsJht(baseSalary, totalAllowance)
            val bpjsJp  = com.ptniger.hris.utils.PayrollCalculator.calculateBpjsJp(baseSalary, totalAllowance)
            
            // BPJS tanggungan perusahaan (informasi, tidak dipotong dari gaji)
            val jkkRate = contract?.bpjsJkkRate ?: 0.0024
            val jkmRate = contract?.bpjsJkmRate ?: 0.003
            val bpjsJkk = com.ptniger.hris.utils.PayrollCalculator.calculateBpjsJkk(baseSalary, jkkRate)
            val bpjsJkm = com.ptniger.hris.utils.PayrollCalculator.calculateBpjsJkm(baseSalary, jkmRate)
            
            // PPh 21 via metode TER (PMK 168/2023)
            val grossMonthly = baseSalary + totalAllowance + overtimePay + kpiBonus
            val pph21 = com.ptniger.hris.utils.PayrollCalculator.calculatePph21Ter(grossMonthly, ptkpStatus)
            
            // Net salary
            val net = com.ptniger.hris.utils.PayrollCalculator.calculateFullNetSalary(
                baseSalary = baseSalary,
                allowanceMeal = allowanceMeal,
                allowanceTransport = allowanceTransport,
                allowancePosition = allowancePosition,
                overtimePay = overtimePay,
                kpiBonus = kpiBonus,
                bpjsKes = bpjsKes,
                bpjsJht = bpjsJht,
                bpjsJp = bpjsJp,
                pph21 = pph21,
                otherDeductions = deductions
            )

            val payroll = Payroll(
                employeeId = employeeId, employeeName = employeeName,
                month = month, year = year, baseSalary = baseSalary,
                allowance = totalAllowance,
                allowanceMeal = allowanceMeal,
                allowanceTransport = allowanceTransport,
                allowancePosition = allowancePosition,
                overtimePay = overtimePay,
                kpiScore = kpiScore, kpiBonus = kpiBonus,
                bpjsKesehatan = bpjsKes, bpjsJht = bpjsJht, bpjsJp = bpjsJp,
                bpjsJkk = bpjsJkk, bpjsJkm = bpjsJkm,
                pph21 = pph21, ptkpStatus = ptkpStatus,
                contractId = contractId,
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
                    details = "Period: $period, Net: $net, PPh21: $pph21, KPI Bonus: $kpiBonus, Contract: $contractId"
                )
            }
            
            Result.success(ref.id)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun generateRaw(payroll: Payroll): Result<String> {
        return try {
            val ref = col.add(payroll).await()
            Result.success(ref.id)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getByEmployee(employeeId: String): List<Payroll> {
        return try {
            col.whereEqualTo("employeeId", employeeId)
                .get().await().documents.mapNotNull {
                    it.toObject(Payroll::class.java)?.copy(payrollId = it.id)
                }.sortedByDescending { it.generatedAt }
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

    suspend fun processApproval(payrollId: String, approverId: String, isApproved: Boolean, notes: String): Result<Unit> {
        return try {
            // Validasi: hanya Manager yang boleh approve
            val approverUser = db.collection("users")
                .document(approverId).get().await()
                .toObject(com.ptniger.hris.data.model.User::class.java)
            val approverRole = approverUser?.primaryRole?.ifEmpty { approverUser.role } ?: ""
            if (approverRole != Constants.Role.MANAGER && approverRole != Constants.Role.SUPER_ADMIN) {
                return Result.failure(Exception("Hanya Manager yang dapat menyetujui payroll."))
            }

            val doc = col.document(payrollId).get().await()
            val payroll = doc.toObject(Payroll::class.java) ?: throw Exception("Payroll not found")
            if (payroll.status != Constants.PayrollStatus.PENDING_APPROVAL) throw Exception("Payroll is not pending approval")
            
            val status = if (isApproved) Constants.PayrollStatus.APPROVED else Constants.PayrollStatus.REJECTED
            val updates = mutableMapOf<String, Any>(
                "status" to status,
                "approvalNotes" to notes
            )
            if (isApproved) {
                updates["approvedByManagerId"] = approverId
                updates["approvedAt"] = System.currentTimeMillis()
            } else {
                updates["rejectedByManagerId"] = approverId
                updates["rejectedAt"] = System.currentTimeMillis()
                updates["rejectionReason"] = notes
            }
            
            col.document(payrollId).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getTeamPayrolls(managerId: String): List<Payroll> {
        return try {
            // First, get payrolls assigned to this manager
            val byManager = col.whereEqualTo("managerId", managerId)
                .get().await().documents.mapNotNull {
                    it.toObject(Payroll::class.java)?.copy(payrollId = it.id)
                }
            
            // Also get all pending_approval payrolls (in case managerId wasn't set)
            val pendingApproval = col.whereEqualTo("status", Constants.PayrollStatus.PENDING_APPROVAL)
                .get().await().documents.mapNotNull {
                    it.toObject(Payroll::class.java)?.copy(payrollId = it.id)
                }
            
            // Combine and deduplicate
            (byManager + pendingApproval).distinctBy { it.payrollId }
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

package com.ptniger.hris.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.ptniger.hris.data.model.Payroll
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
            val kpiScore = kpiRepo.getTotalWeightedScore(employeeId, period)
            val kpiBonus = KpiCalculator.calculateKpiBonus(baseSalary, kpiScore)
            val net = KpiCalculator.calculateNetSalary(baseSalary, allowance, overtimePay, kpiBonus, deductions)

            val payroll = Payroll(
                employeeId = employeeId, employeeName = employeeName,
                month = month, year = year, baseSalary = baseSalary,
                allowance = allowance, overtimePay = overtimePay,
                kpiScore = kpiScore, kpiBonus = kpiBonus,
                deductions = deductions, netSalary = net
            )
            val ref = col.add(payroll).await()
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
}

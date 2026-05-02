package com.ptniger.hris.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.ptniger.hris.data.model.AuditLog
import com.ptniger.hris.data.model.AutomationRule
import com.ptniger.hris.utils.Constants
import kotlinx.coroutines.tasks.await

class AuditLogRepository {
    private val db = FirebaseFirestore.getInstance()
    private val logCol = db.collection(Constants.Collections.AUDIT_LOGS)
    private val ruleCol = db.collection(Constants.Collections.AUTOMATION_RULES)

    // Audit Logs
    suspend fun log(
        userId: String, 
        userName: String, 
        action: String,
        targetCollection: String, 
        targetId: String, 
        details: String,
        actorRole: String = "",
        module: String = "",
        targetUserId: String = "",
        oldValue: String = "",
        newValue: String = "",
        ipAddress: String = "",
        deviceInfo: String = ""
    ): Result<String> {
        return try {
            val entry = AuditLog(
                userId = userId, 
                userName = userName, 
                actorRole = actorRole,
                action = action,
                module = module,
                targetCollection = targetCollection, 
                targetId = targetId, 
                targetUserId = targetUserId,
                oldValue = oldValue,
                newValue = newValue,
                details = details,
                ipAddress = ipAddress,
                deviceInfo = deviceInfo
            )
            val ref = logCol.add(entry).await()
            Result.success(ref.id)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getAll(): List<AuditLog> {
        return try {
            logCol.orderBy("createdAt", Query.Direction.DESCENDING).limit(100)
                .get().await().documents.mapNotNull {
                    it.toObject(AuditLog::class.java)?.copy(logId = it.id)
                }
        } catch (e: Exception) { emptyList() }
    }

    suspend fun getCount(): Int {
        return try { logCol.get().await().size() } catch (e: Exception) { 0 }
    }

    // Automation Rules
    suspend fun getRules(): List<AutomationRule> {
        return try {
            ruleCol.get().await().documents.mapNotNull {
                it.toObject(AutomationRule::class.java)?.copy(ruleId = it.id)
            }
        } catch (e: Exception) { emptyList() }
    }

    suspend fun toggleRule(ruleId: String, isActive: Boolean): Result<Unit> {
        return try {
            ruleCol.document(ruleId).update("isActive", isActive).await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun addRule(rule: AutomationRule): Result<String> {
        return try {
            val ref = ruleCol.add(rule).await()
            Result.success(ref.id)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun seedDefaultRules() {
        val existing = getRules()
        if (existing.isNotEmpty()) return
        val defaults = listOf(
            AutomationRule(name = "Attendance Auto Late", type = "attendance",
                description = "Otomatis tandai terlambat jika check-in > 08:15"),
            AutomationRule(name = "Leave Quota Check", type = "leave",
                description = "Otomatis tolak cuti jika kuota tidak cukup"),
            AutomationRule(name = "Payroll KPI Bonus", type = "payroll",
                description = "Hitung bonus otomatis dari skor KPI"),
            AutomationRule(name = "Notification Auto Send", type = "notification",
                description = "Kirim notifikasi otomatis saat event penting"),
            AutomationRule(name = "Account Auto Create", type = "account",
                description = "Buat akun ESS otomatis saat karyawan baru ditambah"),
            AutomationRule(name = "Audit Log Auto Record", type = "audit",
                description = "Catat perubahan data sensitif secara otomatis")
        )
        defaults.forEach { addRule(it) }
    }
}

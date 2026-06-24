package com.ptniger.hris.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.ptniger.hris.data.model.LeaveRequest
import com.ptniger.hris.utils.AutomationEngine
import com.ptniger.hris.utils.Constants
import kotlinx.coroutines.tasks.await

class LeaveRepository {
    private val db = FirebaseFirestore.getInstance()
    private val col = db.collection(Constants.Collections.LEAVE_REQUESTS)
    private val employeeRepo = EmployeeRepository()

    /**
     * Submit a leave request.
     * If the "leave" automation rule is active, auto-checks quota and rejects if insufficient.
     */
    suspend fun submit(leave: LeaveRequest): Result<String> {
        return try {
            // Check if Leave Quota automation is enabled
            val quotaCheckEnabled = AutomationEngine.isRuleActive(AutomationEngine.RuleType.LEAVE)
            
            if (quotaCheckEnabled) {
                val employee = employeeRepo.getByUserId(leave.employeeId) 
                    ?: employeeRepo.getById(leave.employeeId)
                if (employee != null && employee.leaveQuota <= 0) {
                    return Result.failure(Exception("Kuota cuti habis. Pengajuan cuti otomatis ditolak oleh sistem."))
                }
            }
            
            val ref = col.add(leave).await()
            
            // Auto audit log if enabled
            val auditEnabled = AutomationEngine.isRuleActive(AutomationEngine.RuleType.AUDIT)
            if (auditEnabled) {
                AuditLogRepository().log(
                    userId = leave.employeeId,
                    userName = leave.employeeName,
                    action = "LEAVE_SUBMITTED",
                    module = "Leave",
                    targetCollection = Constants.Collections.LEAVE_REQUESTS,
                    targetId = ref.id,
                    details = "Type: ${leave.type}, From: ${leave.startDate}, To: ${leave.endDate}, Reason: ${leave.reason}"
                )
            }
            
            Result.success(ref.id)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getByEmployee(employeeId: String): List<LeaveRequest> {
        return try {
            // Try with orderBy first (requires Firestore index)
            col.whereEqualTo("employeeId", employeeId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get().await().documents.mapNotNull {
                    it.toObject(LeaveRequest::class.java)?.copy(leaveId = it.id)
                }
        } catch (e: Exception) {
            // Fallback without orderBy if index doesn't exist yet
            try {
                col.whereEqualTo("employeeId", employeeId)
                    .get().await().documents.mapNotNull {
                        it.toObject(LeaveRequest::class.java)?.copy(leaveId = it.id)
                    }.sortedByDescending { it.createdAt }
            } catch (e2: Exception) { emptyList() }
        }
    }

    suspend fun getPending(departmentId: String = ""): List<LeaveRequest> {
        return try {
            var query: Query = col.whereEqualTo("status", Constants.LeaveStatus.PENDING)
            if (departmentId.isNotEmpty()) {
                query = query.whereEqualTo("departmentId", departmentId)
            }
            query.orderBy("createdAt", Query.Direction.DESCENDING)
                .get().await().documents.mapNotNull {
                    it.toObject(LeaveRequest::class.java)?.copy(leaveId = it.id)
                }
        } catch (e: Exception) {
            try {
                var query: Query = col.whereEqualTo("status", Constants.LeaveStatus.PENDING)
                if (departmentId.isNotEmpty()) {
                    query = query.whereEqualTo("departmentId", departmentId)
                }
                query.get().await().documents.mapNotNull {
                        it.toObject(LeaveRequest::class.java)?.copy(leaveId = it.id)
                    }.sortedByDescending { it.createdAt }
            } catch (e2: Exception) { emptyList() }
        }
    }

    suspend fun getAll(): List<LeaveRequest> {
        return try {
            col.orderBy("createdAt", Query.Direction.DESCENDING)
                .get().await().documents.mapNotNull {
                    it.toObject(LeaveRequest::class.java)?.copy(leaveId = it.id)
                }
        } catch (e: Exception) { emptyList() }
    }

    suspend fun approve(leaveId: String, approvedBy: String): Result<Unit> {
        return try {
            col.document(leaveId).update(
                mapOf("status" to Constants.LeaveStatus.APPROVED, "approvedBy" to approvedBy)
            ).await()
            
            val auditEnabled = AutomationEngine.isRuleActive(AutomationEngine.RuleType.AUDIT)
            if (auditEnabled) {
                AuditLogRepository().log(
                    userId = approvedBy,
                    userName = approvedBy,
                    action = "LEAVE_APPROVED",
                    module = "Leave",
                    targetCollection = Constants.Collections.LEAVE_REQUESTS,
                    targetId = leaveId,
                    details = "Approved by $approvedBy"
                )
            }
            
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun reject(leaveId: String, approvedBy: String): Result<Unit> {
        return try {
            col.document(leaveId).update(
                mapOf("status" to Constants.LeaveStatus.REJECTED, "approvedBy" to approvedBy)
            ).await()
            
            val auditEnabled = AutomationEngine.isRuleActive(AutomationEngine.RuleType.AUDIT)
            if (auditEnabled) {
                AuditLogRepository().log(
                    userId = approvedBy,
                    userName = approvedBy,
                    action = "LEAVE_REJECTED",
                    module = "Leave",
                    targetCollection = Constants.Collections.LEAVE_REQUESTS,
                    targetId = leaveId,
                    details = "Rejected by $approvedBy"
                )
            }
            
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getPendingCount(departmentId: String = ""): Int {
        return try {
            var query: Query = col.whereEqualTo("status", Constants.LeaveStatus.PENDING)
            if (departmentId.isNotEmpty()) {
                query = query.whereEqualTo("departmentId", departmentId)
            }
            query.get().await().size()
        } catch (e: Exception) { 0 }
    }
}

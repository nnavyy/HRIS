package com.ptniger.hris.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.ptniger.hris.data.model.LeaveRequest
import com.ptniger.hris.utils.AutomationEngine
import com.ptniger.hris.utils.Constants
import com.ptniger.hris.utils.LeaveEmailNotifier
import com.ptniger.hris.utils.LeaveValidator
import kotlinx.coroutines.tasks.await

class LeaveRepository {
    private val db = FirebaseFirestore.getInstance()
    private val col = db.collection(Constants.Collections.LEAVE_REQUESTS)
    private val employeeRepo = EmployeeRepository()

    /**
     * Submit a leave request with full validation:
     * 1. Fetch LeavePolicy + employee quota
     * 2. Validate via LeaveValidator
     * 3. Auto-reject if validation fails + policy says so
     * 4. Save + notify manager (in-app + email queue)
     */
    suspend fun submit(leave: LeaveRequest): Result<String> {
        return try {
            // 1. Fetch policy & employee
            val policy = LeavePolicyRepository().getActivePolicy()
            val employee = employeeRepo.getByUserId(leave.employeeId)
                ?: employeeRepo.getById(leave.employeeId)
            val currentQuota = employee?.leaveQuota ?: 12

            // 2. Validasi
            val validation = LeaveValidator.validate(
                startDateStr = leave.startDate,
                endDateStr = leave.endDate,
                duration = leave.duration,
                leaveQuota = currentQuota,
                policy = policy
            )

            // 3. Jika tidak valid
            if (!validation.isValid) {
                if (validation.autoReject) {
                    // Simpan ke Firestore sebagai rejected (tampil di riwayat)
                    val rejectedLeave = leave.copy(
                        status = Constants.LeaveStatus.REJECTED,
                        approvedBy = "system",
                        autoRejected = true,
                        rejectionReason = validation.errorMessage
                    )
                    val ref = col.add(rejectedLeave).await()
                    if (AutomationEngine.isRuleActive(AutomationEngine.RuleType.AUDIT)) {
                        AuditLogRepository().log(
                            userId = leave.employeeId, userName = leave.employeeName,
                            action = "LEAVE_AUTO_REJECTED", module = "Leave",
                            targetCollection = Constants.Collections.LEAVE_REQUESTS, targetId = ref.id,
                            details = "Auto-rejected: ${validation.errorMessage}"
                        )
                    }
                }
                return Result.failure(Exception(validation.errorMessage))
            }

            // 4. Valid → simpan
            val ref = col.add(leave).await()

            // 5. Notif in-app ke manager
            if (AutomationEngine.isRuleActive(AutomationEngine.RuleType.LEAVE)) {
                employee?.managerId?.takeIf { it.isNotEmpty() }?.let { mgrId ->
                    val manager = employeeRepo.getById(mgrId)
                    manager?.userId?.takeIf { it.isNotEmpty() }?.let { uid ->
                        NotificationRepository().send(
                            userId = uid,
                            title = "Pengajuan Cuti Baru",
                            message = "${leave.employeeName} mengajukan ${leave.type} " +
                                    "(${leave.duration} hari) mulai ${leave.startDate}.",
                            type = "leave_request"
                        )
                    }
                    // 6. Email ke manager (fire and forget)
                    try {
                        if (manager?.email?.isNotEmpty() == true) {
                            LeaveEmailNotifier.notifyManagerOfNewRequest(
                                leave = leave, manager = manager, managerEmail = manager.email
                            )
                        }
                    } catch (_: Exception) {}
                }
            }

            // 7. Audit log
            if (AutomationEngine.isRuleActive(AutomationEngine.RuleType.AUDIT)) {
                AuditLogRepository().log(
                    userId = leave.employeeId, userName = leave.employeeName,
                    action = "LEAVE_SUBMITTED", module = "Leave",
                    targetCollection = Constants.Collections.LEAVE_REQUESTS, targetId = ref.id,
                    details = "Type: ${leave.type}, From: ${leave.startDate}, To: ${leave.endDate}, Reason: ${leave.reason}"
                )
            }

            Result.success(ref.id)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getByEmployee(employeeId: String): List<LeaveRequest> {
        return try {
            col.whereEqualTo("employeeId", employeeId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get().await().documents.mapNotNull {
                    it.toObject(LeaveRequest::class.java)?.copy(leaveId = it.id)
                }
        } catch (e: Exception) {
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
            
            // Email ke karyawan (fire and forget)
            try {
                val leave = col.document(leaveId).get().await().toObject(LeaveRequest::class.java)
                val employee = leave?.let {
                    employeeRepo.getByUserId(it.employeeId) ?: employeeRepo.getById(it.employeeId)
                }
                if (leave != null && employee?.email?.isNotEmpty() == true) {
                    val approver = employeeRepo.getByUserId(approvedBy)
                    LeaveEmailNotifier.notifyEmployeeOfDecision(
                        leave = leave, employeeEmail = employee.email,
                        decision = "approved", approverName = approver?.name ?: approvedBy
                    )
                }
            } catch (_: Exception) {}

            if (AutomationEngine.isRuleActive(AutomationEngine.RuleType.AUDIT)) {
                AuditLogRepository().log(
                    userId = approvedBy, userName = approvedBy,
                    action = "LEAVE_APPROVED", module = "Leave",
                    targetCollection = Constants.Collections.LEAVE_REQUESTS, targetId = leaveId,
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
            
            // Email ke karyawan (fire and forget)
            try {
                val leave = col.document(leaveId).get().await().toObject(LeaveRequest::class.java)
                val employee = leave?.let {
                    employeeRepo.getByUserId(it.employeeId) ?: employeeRepo.getById(it.employeeId)
                }
                if (leave != null && employee?.email?.isNotEmpty() == true) {
                    val approver = employeeRepo.getByUserId(approvedBy)
                    LeaveEmailNotifier.notifyEmployeeOfDecision(
                        leave = leave, employeeEmail = employee.email,
                        decision = "rejected", approverName = approver?.name ?: approvedBy
                    )
                }
            } catch (_: Exception) {}

            if (AutomationEngine.isRuleActive(AutomationEngine.RuleType.AUDIT)) {
                AuditLogRepository().log(
                    userId = approvedBy, userName = approvedBy,
                    action = "LEAVE_REJECTED", module = "Leave",
                    targetCollection = Constants.Collections.LEAVE_REQUESTS, targetId = leaveId,
                    details = "Rejected by $approvedBy"
                )
            }
            
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun rejectWithReason(leaveId: String, approvedBy: String, reason: String): Result<Unit> {
        return try {
            col.document(leaveId).update(
                mapOf(
                    "status" to Constants.LeaveStatus.REJECTED,
                    "approvedBy" to approvedBy,
                    "autoRejected" to true,
                    "rejectionReason" to reason
                )
            ).await()
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

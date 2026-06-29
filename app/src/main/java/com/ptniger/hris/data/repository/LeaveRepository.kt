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

            // [LA-02] Resolve managerId and requesterRole
            val resolvedManagerId = employee?.managerId ?: ""
            val resolvedRole = employee?.let {
                try {
                    val userDoc = db.collection("users")
                        .whereEqualTo("employeeId", it.employeeId).get().await()
                    userDoc.documents.firstOrNull()
                        ?.toObject(com.ptniger.hris.data.model.User::class.java)?.primaryRole ?: "employee"
                } catch (e: Exception) { "employee" }
            } ?: "employee"

            val enrichedLeave = leave.copy(
                managerId = resolvedManagerId,
                requesterRole = resolvedRole
            )

            // 2. Validasi dengan tipe cuti (LP-02/LP-03)
            val validation = LeaveValidator.validate(
                startDateStr = enrichedLeave.startDate,
                endDateStr = enrichedLeave.endDate,
                duration = enrichedLeave.duration,
                leaveQuota = currentQuota,
                policy = policy,
                leaveType = enrichedLeave.type
            )

            // 3. Jika tidak valid
            if (!validation.isValid) {
                if (validation.autoReject) {
                    val rejectedLeave = enrichedLeave.copy(
                        status = Constants.LeaveStatus.REJECTED,
                        approvedBy = "system",
                        autoRejected = true,
                        rejectionReason = validation.errorMessage
                    )
                    val ref = col.add(rejectedLeave).await()
                    if (AutomationEngine.isRuleActive(AutomationEngine.RuleType.AUDIT)) {
                        AuditLogRepository().log(
                            userId = enrichedLeave.employeeId, userName = enrichedLeave.employeeName,
                            action = "LEAVE_AUTO_REJECTED", module = "Leave",
                            targetCollection = Constants.Collections.LEAVE_REQUESTS, targetId = ref.id,
                            details = "Auto-rejected: ${validation.errorMessage}"
                        )
                    }
                }
                return Result.failure(Exception(validation.errorMessage))
            }

            // 4. Valid → simpan
            val ref = col.add(enrichedLeave).await()

            // Update kuota jika diperlukan
            if (validation.deductsQuota && employee != null && employee.employeeId.isNotEmpty()) {
                val newQuota = (currentQuota - enrichedLeave.duration).coerceAtLeast(0)
                employeeRepo.updateLeaveQuota(employee.employeeId, newQuota)
            }

            // 5. Routing Notifikasi (LA-02)
            if (AutomationEngine.isRuleActive(AutomationEngine.RuleType.LEAVE)) {
                if (resolvedRole == Constants.Role.MANAGER || resolvedManagerId.isEmpty()) {
                    // Manager ajukan cuti → cari HR untuk approve
                    val hrUsers = db.collection("users")
                        .whereEqualTo("primaryRole", Constants.Role.HR).get().await()
                    val hrUser = hrUsers.documents.firstOrNull()?.toObject(com.ptniger.hris.data.model.User::class.java)
                    hrUser?.userId?.let { hrId ->
                        NotificationRepository().send(
                            userId = hrId,
                            title = "Pengajuan Cuti (Manager)",
                            message = "${enrichedLeave.employeeName} (Manager) mengajukan ${enrichedLeave.type} " +
                                    "(${enrichedLeave.duration} hari) mulai ${enrichedLeave.startDate}. Approval diperlukan dari HR.",
                            type = "leave_request_manager"
                        )
                        try {
                            val hrEmployee = employeeRepo.getByUserId(hrId)
                            if (hrEmployee?.email?.isNotEmpty() == true) {
                                LeaveEmailNotifier.notifyManagerOfNewRequest(
                                    leave = enrichedLeave, manager = hrEmployee, managerEmail = hrEmployee.email
                                )
                            }
                        } catch (_: Exception) {}
                    }
                } else {
                    // Karyawan biasa → notif ke manager-nya
                    val manager = employeeRepo.getById(resolvedManagerId)
                    manager?.userId?.takeIf { it.isNotEmpty() }?.let { mgrUserId ->
                        NotificationRepository().send(
                            userId = mgrUserId,
                            title = "Pengajuan Cuti Baru",
                            message = "${enrichedLeave.employeeName} mengajukan ${enrichedLeave.type} " +
                                    "(${enrichedLeave.duration} hari) mulai ${enrichedLeave.startDate}.",
                            type = "leave_request"
                        )
                        try {
                            if (manager.email.isNotEmpty()) {
                                LeaveEmailNotifier.notifyManagerOfNewRequest(
                                    leave = enrichedLeave, manager = manager, managerEmail = manager.email
                                )
                            }
                        } catch (_: Exception) {}
                    }
                }
            }

            // 7. Audit log
            if (AutomationEngine.isRuleActive(AutomationEngine.RuleType.AUDIT)) {
                AuditLogRepository().log(
                    userId = enrichedLeave.employeeId, userName = enrichedLeave.employeeName,
                    action = "LEAVE_SUBMITTED", module = "Leave",
                    targetCollection = Constants.Collections.LEAVE_REQUESTS, targetId = ref.id,
                    details = "Type: ${enrichedLeave.type}, From: ${enrichedLeave.startDate}, To: ${enrichedLeave.endDate}, Reason: ${enrichedLeave.reason}"
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

    suspend fun getPendingForApprover(
        currentUserEmployeeId: String,
        currentRole: String
    ): List<LeaveRequest> {
        return try {
            when (currentRole) {
                Constants.Role.MANAGER -> {
                    col.whereEqualTo("status", Constants.LeaveStatus.PENDING)
                        .whereEqualTo("managerId", currentUserEmployeeId)
                        .get().await().documents.mapNotNull {
                            it.toObject(LeaveRequest::class.java)?.copy(leaveId = it.id)
                        }.sortedByDescending { it.createdAt }
                }
                Constants.Role.HR -> {
                    val managerLeaves = col.whereEqualTo("status", Constants.LeaveStatus.PENDING)
                        .whereEqualTo("requesterRole", Constants.Role.MANAGER)
                        .get().await().documents.mapNotNull {
                            it.toObject(LeaveRequest::class.java)?.copy(leaveId = it.id)
                        }
                    val noManagerLeaves = col.whereEqualTo("status", Constants.LeaveStatus.PENDING)
                        .whereEqualTo("managerId", "")
                        .get().await().documents.mapNotNull {
                            it.toObject(LeaveRequest::class.java)?.copy(leaveId = it.id)
                        }.filter { it.requesterRole != Constants.Role.MANAGER }
                    (managerLeaves + noManagerLeaves).sortedByDescending { it.createdAt }
                }
                Constants.Role.SUPER_ADMIN -> {
                    col.whereEqualTo("status", Constants.LeaveStatus.PENDING)
                        .get().await().documents.mapNotNull {
                            it.toObject(LeaveRequest::class.java)?.copy(leaveId = it.id)
                        }.sortedByDescending { it.createdAt }
                }
                else -> emptyList()
            }
        } catch (e: Exception) { emptyList() }
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

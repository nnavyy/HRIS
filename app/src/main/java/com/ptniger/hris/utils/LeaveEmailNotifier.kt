package com.ptniger.hris.utils

import com.google.firebase.firestore.FirebaseFirestore
import com.ptniger.hris.data.model.Employee
import com.ptniger.hris.data.model.LeaveRequest
import kotlinx.coroutines.tasks.await

/**
 * Writes to Firestore `email_queue` collection.
 * Firebase Cloud Function picks up new docs and sends via SMTP.
 */
object LeaveEmailNotifier {

    private val db = FirebaseFirestore.getInstance()

    suspend fun notifyManagerOfNewRequest(
        leave: LeaveRequest,
        manager: Employee,
        managerEmail: String
    ): Result<Unit> {
        return try {
            db.collection("email_queue").add(
                mapOf(
                    "to" to managerEmail,
                    "type" to "leave_request",
                    "subject" to "📋 Pengajuan Cuti Baru: ${leave.employeeName}",
                    "templateData" to mapOf(
                        "employeeName" to leave.employeeName,
                        "leaveType" to leave.type,
                        "startDate" to leave.startDate,
                        "endDate" to leave.endDate,
                        "duration" to "${leave.duration} hari",
                        "reason" to leave.reason,
                        "managerName" to manager.name,
                        "approvalUrl" to "Buka aplikasi HRIS → Approval Cuti"
                    ),
                    "status" to "pending",
                    "createdAt" to System.currentTimeMillis(),
                    "employeeId" to leave.employeeId
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun notifyEmployeeOfDecision(
        leave: LeaveRequest,
        employeeEmail: String,
        decision: String,       // "approved" atau "rejected"
        approverName: String
    ): Result<Unit> {
        return try {
            val isApproved = decision == "approved"
            db.collection("email_queue").add(
                mapOf(
                    "to" to employeeEmail,
                    "type" to "leave_decision",
                    "subject" to if (isApproved) "✅ Cuti Kamu Disetujui" else "❌ Cuti Kamu Ditolak",
                    "templateData" to mapOf(
                        "employeeName" to leave.employeeName,
                        "leaveType" to leave.type,
                        "startDate" to leave.startDate,
                        "endDate" to leave.endDate,
                        "duration" to "${leave.duration} hari",
                        "decision" to if (isApproved) "DISETUJUI" else "DITOLAK",
                        "approverName" to approverName,
                        "message" to if (isApproved)
                            "Pengajuan cuti kamu telah disetujui. Selamat beristirahat!"
                        else
                            "Pengajuan cutimu tidak dapat disetujui. Hubungi HR untuk info lebih lanjut."
                    ),
                    "status" to "pending",
                    "createdAt" to System.currentTimeMillis()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }
}

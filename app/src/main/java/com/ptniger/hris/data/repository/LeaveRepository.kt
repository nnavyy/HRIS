package com.ptniger.hris.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.ptniger.hris.data.model.LeaveRequest
import com.ptniger.hris.utils.Constants
import kotlinx.coroutines.tasks.await

class LeaveRepository {
    private val db = FirebaseFirestore.getInstance()
    private val col = db.collection(Constants.Collections.LEAVE_REQUESTS)

    suspend fun submit(leave: LeaveRequest): Result<String> {
        return try {
            val ref = col.add(leave).await()
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
        } catch (e: Exception) { emptyList() }
    }

    suspend fun getPending(): List<LeaveRequest> {
        return try {
            col.whereEqualTo("status", Constants.LeaveStatus.PENDING)
                .orderBy("createdAt", Query.Direction.DESCENDING)
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
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun reject(leaveId: String, approvedBy: String): Result<Unit> {
        return try {
            col.document(leaveId).update(
                mapOf("status" to Constants.LeaveStatus.REJECTED, "approvedBy" to approvedBy)
            ).await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getPendingCount(): Int {
        return try {
            col.whereEqualTo("status", Constants.LeaveStatus.PENDING).get().await().size()
        } catch (e: Exception) { 0 }
    }
}

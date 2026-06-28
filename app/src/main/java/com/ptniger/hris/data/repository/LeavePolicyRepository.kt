package com.ptniger.hris.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.ptniger.hris.data.model.LeavePolicy
import kotlinx.coroutines.tasks.await

class LeavePolicyRepository {
    private val db = FirebaseFirestore.getInstance()
    private val doc = db.collection("leave_policies").document("default")

    suspend fun getActivePolicy(): LeavePolicy {
        return try {
            doc.get().await().toObject(LeavePolicy::class.java) ?: LeavePolicy()
        } catch (e: Exception) { LeavePolicy() }
    }

    suspend fun update(policy: LeavePolicy): Result<Unit> {
        return try {
            doc.set(policy).await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }
}

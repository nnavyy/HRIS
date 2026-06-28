package com.ptniger.hris.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.ptniger.hris.data.model.AiReview
import kotlinx.coroutines.tasks.await

class AiReviewRepository {
    private val db = FirebaseFirestore.getInstance()
    private val col = db.collection("ai_reviews")

    suspend fun save(review: AiReview): Result<String> {
        return try {
            val ref = col.add(review).await()
            Result.success(ref.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getByEmployee(employeeId: String): List<AiReview> {
        return try {
            col.whereEqualTo("employeeId", employeeId)
                .orderBy("generatedAt", Query.Direction.DESCENDING)
                .get().await()
                .documents
                .mapNotNull { it.toObject(AiReview::class.java)?.copy(reviewId = it.id) }
        } catch (e: Exception) {
            try {
                col.whereEqualTo("employeeId", employeeId)
                    .get().await()
                    .documents
                    .mapNotNull { it.toObject(AiReview::class.java)?.copy(reviewId = it.id) }
                    .sortedByDescending { it.generatedAt }
            } catch (e2: Exception) { emptyList() }
        }
    }

    suspend fun getByPeriod(period: String): List<AiReview> {
        return try {
            col.whereEqualTo("period", period)
                .orderBy("generatedAt", Query.Direction.DESCENDING)
                .get().await()
                .documents
                .mapNotNull { it.toObject(AiReview::class.java)?.copy(reviewId = it.id) }
        } catch (e: Exception) {
            try {
                col.whereEqualTo("period", period)
                    .get().await()
                    .documents
                    .mapNotNull { it.toObject(AiReview::class.java)?.copy(reviewId = it.id) }
                    .sortedByDescending { it.generatedAt }
            } catch (e2: Exception) { emptyList() }
        }
    }

    /** Ubah status review menjadi "published" */
    suspend fun publish(reviewId: String): Result<Unit> {
        return try {
            col.document(reviewId).update("status", "published").await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Ambil review terbaru untuk seorang karyawan */
    suspend fun getLatest(employeeId: String): AiReview? {
        return try {
            col.whereEqualTo("employeeId", employeeId)
                .orderBy("generatedAt", Query.Direction.DESCENDING)
                .limit(1)
                .get().await()
                .documents
                .firstOrNull()
                ?.let { it.toObject(AiReview::class.java)?.copy(reviewId = it.id) }
        } catch (e: Exception) {
            getByEmployee(employeeId).firstOrNull()
        }
    }
}

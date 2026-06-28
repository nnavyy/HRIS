package com.ptniger.hris.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.ptniger.hris.data.model.PeerReview
import kotlinx.coroutines.tasks.await

class PeerReviewRepository {
    private val db = FirebaseFirestore.getInstance()
    private val col = db.collection("peer_reviews")

    /**
     * Submit a peer review.
     * Validates that reviewer is not reviewing themselves.
     */
    suspend fun submit(review: PeerReview): Result<String> {
        return try {
            if (review.reviewerEmployeeId == review.targetEmployeeId) {
                return Result.failure(Exception("Tidak bisa melakukan peer review untuk diri sendiri."))
            }
            val ref = col.add(review).await()
            Result.success(ref.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get all peer reviews for a target employee in a given period.
     */
    suspend fun getByTarget(employeeId: String, period: String): List<PeerReview> {
        return try {
            col.whereEqualTo("targetEmployeeId", employeeId)
                .whereEqualTo("period", period)
                .get().await()
                .documents
                .mapNotNull { it.toObject(PeerReview::class.java)?.copy(reviewId = it.id) }
        } catch (e: Exception) { emptyList() }
    }

    /**
     * Calculate average peer review score for a target employee in a given period.
     */
    suspend fun getAverageScore(employeeId: String, period: String): Double {
        val reviews = getByTarget(employeeId, period)
        if (reviews.isEmpty()) return 0.0
        return reviews.map { it.score }.average()
    }

    /**
     * Check if a reviewer has already submitted a review for the target in this period.
     */
    suspend fun hasPeerReviewed(reviewerId: String, targetId: String, period: String): Boolean {
        return try {
            col.whereEqualTo("reviewerEmployeeId", reviewerId)
                .whereEqualTo("targetEmployeeId", targetId)
                .whereEqualTo("period", period)
                .get().await()
                .isEmpty.not()
        } catch (e: Exception) { false }
    }

    /**
     * Get all reviews submitted by a specific reviewer.
     */
    suspend fun getByReviewer(reviewerId: String, period: String): List<PeerReview> {
        return try {
            col.whereEqualTo("reviewerEmployeeId", reviewerId)
                .whereEqualTo("period", period)
                .get().await()
                .documents
                .mapNotNull { it.toObject(PeerReview::class.java)?.copy(reviewId = it.id) }
        } catch (e: Exception) { emptyList() }
    }
}

package com.ptniger.hris.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.ptniger.hris.data.model.Notification
import com.ptniger.hris.utils.AutomationEngine
import com.ptniger.hris.utils.Constants
import kotlinx.coroutines.tasks.await

class NotificationRepository {
    private val db = FirebaseFirestore.getInstance()
    private val col = db.collection(Constants.Collections.NOTIFICATIONS)

    suspend fun send(userId: String, title: String, message: String, type: String): Result<String> {
        return try {
            // Check if notification automation is enabled
            val notifEnabled = AutomationEngine.isRuleActive(AutomationEngine.RuleType.NOTIFICATION)
            if (!notifEnabled) {
                return Result.success("skipped_notification_disabled")
            }
            
            val notif = Notification(userId = userId, title = title, message = message, type = type)
            val ref = col.add(notif).await()
            Result.success(ref.id)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getByUser(userId: String): List<Notification> {
        return try {
            col.whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get().await().documents.mapNotNull {
                    it.toObject(Notification::class.java)?.copy(notificationId = it.id)
                }
        } catch (e: Exception) { emptyList() }
    }

    suspend fun markAsRead(notifId: String): Result<Unit> {
        return try {
            col.document(notifId).update("isRead", true).await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getUnreadCount(userId: String): Int {
        return try {
            col.whereEqualTo("userId", userId).whereEqualTo("isRead", false).get().await().size()
        } catch (e: Exception) { 0 }
    }
}

package com.ptniger.hris.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.ptniger.hris.data.model.AppConfig
import com.ptniger.hris.utils.Constants
import kotlinx.coroutines.tasks.await

class AppConfigRepository {
    private val db = FirebaseFirestore.getInstance()
    private val col = db.collection(Constants.Collections.APP_CONFIGS)

    suspend fun getConfig(key: String): AppConfig? {
        return try {
            col.whereEqualTo("key", key).get().await().documents.firstOrNull()?.let {
                it.toObject(AppConfig::class.java)?.copy(configId = it.id)
            }
        } catch (e: Exception) { null }
    }

    suspend fun getAllConfigs(): List<AppConfig> {
        return try {
            col.get().await().documents.mapNotNull {
                it.toObject(AppConfig::class.java)?.copy(configId = it.id)
            }
        } catch (e: Exception) { emptyList() }
    }

    suspend fun saveConfig(config: AppConfig): Result<String> {
        return try {
            val existing = getConfig(config.key)
            if (existing != null) {
                col.document(existing.configId).set(config.copy(configId = existing.configId, updatedAt = System.currentTimeMillis())).await()
                Result.success(existing.configId)
            } else {
                val ref = col.add(config).await()
                Result.success(ref.id)
            }
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getGroqApiKey(): String {
        return getConfig("groq_api_key")?.value ?: ""
    }
}

package com.ptniger.hris.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.ptniger.hris.data.model.KpiConfig
import com.ptniger.hris.data.model.KpiScore
import com.ptniger.hris.utils.Constants
import kotlinx.coroutines.tasks.await

class KpiRepository {
    private val db = FirebaseFirestore.getInstance()
    private val configCol = db.collection(Constants.Collections.KPI_CONFIGS)
    private val scoreCol = db.collection(Constants.Collections.KPI_SCORES)

    // KPI Config
    suspend fun addConfig(config: KpiConfig): Result<String> {
        return try {
            val ref = configCol.add(config).await()
            Result.success(ref.id)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getConfigs(departmentId: String): List<KpiConfig> {
        return try {
            configCol.whereEqualTo("departmentId", departmentId).get().await().documents.mapNotNull {
                it.toObject(KpiConfig::class.java)?.copy(configId = it.id)
            }
        } catch (e: Exception) { emptyList() }
    }

    suspend fun getActiveConfigs(departmentId: String, period: String): List<KpiConfig> {
        return try {
            configCol
                .whereEqualTo("departmentId", departmentId)
                .whereEqualTo("period", period)
                .whereEqualTo("isActive", true)
                .get().await().documents.mapNotNull {
                    it.toObject(KpiConfig::class.java)?.copy(configId = it.id)
                }
        } catch (e: Exception) { emptyList() }
    }

    suspend fun getAllConfigs(): List<KpiConfig> {
        return try {
            configCol.get().await().documents.mapNotNull {
                it.toObject(KpiConfig::class.java)?.copy(configId = it.id)
            }
        } catch (e: Exception) { emptyList() }
    }

    suspend fun deleteConfig(id: String): Result<Unit> {
        return try {
            configCol.document(id).delete().await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    // KPI Scores
    suspend fun submitScore(score: KpiScore): Result<String> {
        return try {
            val ref = scoreCol.add(score).await()
            Result.success(ref.id)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun updateScore(
        employeeId: String, employeeName: String, configId: String, kpiName: String,
        score: Int, weight: Double, period: String, scoredBy: String,
        source: String, autoDetails: String
    ): Result<String> {
        return try {
            // Check if score already exists for this period and config
            val existing = scoreCol
                .whereEqualTo("employeeId", employeeId)
                .whereEqualTo("configId", configId)
                .whereEqualTo("period", period)
                .get().await().documents.firstOrNull()

            val kpiScore = KpiScore(
                employeeId = employeeId,
                employeeName = employeeName,
                configId = configId,
                kpiName = kpiName,
                score = score,
                weight = weight,
                weightedScore = score * weight,
                period = period,
                scoredBy = scoredBy,
                source = source,
                autoDetails = autoDetails,
                updatedAt = System.currentTimeMillis()
            )

            if (existing != null) {
                // Update existing
                scoreCol.document(existing.id).set(kpiScore.copy(scoreId = existing.id, createdAt = existing.getLong("createdAt") ?: System.currentTimeMillis())).await()
                Result.success(existing.id)
            } else {
                // Insert new
                val ref = scoreCol.add(kpiScore).await()
                Result.success(ref.id)
            }
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getScoresByEmployee(employeeId: String, period: String): List<KpiScore> {
        return try {
            scoreCol.whereEqualTo("employeeId", employeeId)
                .whereEqualTo("period", period)
                .get().await().documents.mapNotNull {
                    it.toObject(KpiScore::class.java)?.copy(scoreId = it.id)
                }
        } catch (e: Exception) { emptyList() }
    }

    suspend fun getTotalWeightedScore(employeeId: String, period: String): Double {
        val scores = getScoresByEmployee(employeeId, period)
        return scores.sumOf { it.weightedScore }
    }

    suspend fun getScoresByPeriod(period: String): List<KpiScore> {
        return try {
            scoreCol.whereEqualTo("period", period)
                .get().await().documents.mapNotNull {
                    it.toObject(KpiScore::class.java)?.copy(scoreId = it.id)
                }
        } catch (e: Exception) { emptyList() }
    }
}

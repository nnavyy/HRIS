package com.ptniger.hris.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.ptniger.hris.data.model.OfficeLocation
import com.ptniger.hris.utils.Constants
import kotlinx.coroutines.tasks.await

class OfficeLocationRepository {
    private val db = FirebaseFirestore.getInstance()
    private val col = db.collection(Constants.Collections.OFFICE_LOCATIONS)

    suspend fun getAll(): List<OfficeLocation> {
        return try {
            col.get().await().documents.mapNotNull {
                it.toObject(OfficeLocation::class.java)?.copy(id = it.id)
            }
        } catch (e: Exception) { emptyList() }
    }

    suspend fun getActiveLocations(): List<OfficeLocation> {
        return try {
            col.whereEqualTo("isActive", true).get().await().documents.mapNotNull {
                it.toObject(OfficeLocation::class.java)?.copy(id = it.id)
            }
        } catch (e: Exception) { emptyList() }
    }

    suspend fun getById(id: String): OfficeLocation? {
        return try {
            val doc = col.document(id).get().await()
            doc.toObject(OfficeLocation::class.java)?.copy(id = doc.id)
        } catch (e: Exception) { null }
    }

    suspend fun add(location: OfficeLocation): Result<String> {
        return try {
            val ref = col.add(location).await()
            Result.success(ref.id)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun update(id: String, location: OfficeLocation): Result<Unit> {
        return try {
            col.document(id).set(location).await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun toggleStatus(id: String, isActive: Boolean): Result<Unit> {
        return try {
            col.document(id).update("isActive", isActive).await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }
}

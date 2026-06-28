package com.ptniger.hris.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.ptniger.hris.data.model.EmployeeContract
import com.ptniger.hris.utils.DateUtils
import kotlinx.coroutines.tasks.await

class ContractRepository {
    private val db = FirebaseFirestore.getInstance()
    private val col = db.collection("employee_contracts")

    suspend fun create(contract: EmployeeContract): Result<String> {
        return try {
            val ref = col.add(contract).await()
            Result.success(ref.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get the active contract for an employee.
     * Active = effectiveDate <= today AND signedByEmployee == true
     * Returns the most recent one if multiple exist.
     */
    suspend fun getActiveContract(employeeId: String): EmployeeContract? {
        return try {
            val today = DateUtils.serverToday()
            col.whereEqualTo("employeeId", employeeId)
                .whereEqualTo("signedByEmployee", true)
                .orderBy("effectiveDate", Query.Direction.DESCENDING)
                .get().await()
                .documents
                .mapNotNull { it.toObject(EmployeeContract::class.java)?.copy(contractId = it.id) }
                .firstOrNull { it.effectiveDate <= today }
        } catch (e: Exception) {
            // Fallback without orderBy if index doesn't exist
            try {
                val today = DateUtils.serverToday()
                col.whereEqualTo("employeeId", employeeId)
                    .whereEqualTo("signedByEmployee", true)
                    .get().await()
                    .documents
                    .mapNotNull { it.toObject(EmployeeContract::class.java)?.copy(contractId = it.id) }
                    .filter { it.effectiveDate <= today }
                    .maxByOrNull { it.effectiveDate }
            } catch (e2: Exception) { null }
        }
    }

    suspend fun getContractHistory(employeeId: String): List<EmployeeContract> {
        return try {
            col.whereEqualTo("employeeId", employeeId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get().await()
                .documents
                .mapNotNull { it.toObject(EmployeeContract::class.java)?.copy(contractId = it.id) }
        } catch (e: Exception) {
            try {
                col.whereEqualTo("employeeId", employeeId)
                    .get().await()
                    .documents
                    .mapNotNull { it.toObject(EmployeeContract::class.java)?.copy(contractId = it.id) }
                    .sortedByDescending { it.createdAt }
            } catch (e2: Exception) { emptyList() }
        }
    }

    suspend fun signContract(contractId: String, signatureBase64: String): Result<Unit> {
        return try {
            col.document(contractId).update(
                mapOf(
                    "signedByEmployee" to true,
                    "signedAt" to System.currentTimeMillis(),
                    "signatureData" to signatureBase64
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUnsignedContracts(employeeId: String): List<EmployeeContract> {
        return try {
            col.whereEqualTo("employeeId", employeeId)
                .whereEqualTo("signedByEmployee", false)
                .get().await()
                .documents
                .mapNotNull { it.toObject(EmployeeContract::class.java)?.copy(contractId = it.id) }
        } catch (e: Exception) { emptyList() }
    }
}

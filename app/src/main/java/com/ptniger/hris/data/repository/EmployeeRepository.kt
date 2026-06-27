package com.ptniger.hris.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.ptniger.hris.data.model.Employee
import com.ptniger.hris.utils.Constants
import kotlinx.coroutines.tasks.await

class EmployeeRepository {
    private val db = FirebaseFirestore.getInstance()
    private val col = db.collection(Constants.Collections.EMPLOYEES)

    suspend fun getAll(): List<Employee> {
        return try {
            col.get().await().documents.mapNotNull {
                it.toObject(Employee::class.java)?.copy(employeeId = it.id)
            }
        } catch (e: Exception) { emptyList() }
    }

    suspend fun getByDepartment(dept: String): List<Employee> {
        return try {
            col.whereEqualTo("department", dept).get().await().documents.mapNotNull {
                it.toObject(Employee::class.java)?.copy(employeeId = it.id)
            }
        } catch (e: Exception) { emptyList() }
    }

    suspend fun getByRole(role: String): List<Employee> {
        return try {
            col.whereEqualTo("role", role).get().await().documents.mapNotNull {
                it.toObject(Employee::class.java)?.copy(employeeId = it.id)
            }
        } catch (e: Exception) { emptyList() }
    }

    suspend fun getById(id: String): Employee? {
        return try {
            val doc = col.document(id).get().await()
            doc.toObject(Employee::class.java)?.copy(employeeId = doc.id)
        } catch (e: Exception) { null }
    }

    suspend fun getByUserId(userId: String): Employee? {
        return try {
            val docs = col.whereEqualTo("userId", userId).get().await()
            docs.documents.firstOrNull()?.let {
                it.toObject(Employee::class.java)?.copy(employeeId = it.id)
            }
        } catch (e: Exception) { null }
    }

    suspend fun getByEmail(email: String): Employee? {
        return try {
            val docs = col.whereEqualTo("email", email).get().await()
            docs.documents.firstOrNull()?.let {
                it.toObject(Employee::class.java)?.copy(employeeId = it.id)
            }
        } catch (e: Exception) { null }
    }

    suspend fun add(employee: Employee): Result<String> {
        return try {
            val docRef = col.add(employee).await()
            Result.success(docRef.id)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun batchAdd(employees: List<Employee>): Result<Int> {
        return try {
            var count = 0
            // Firestore batches can hold up to 500 operations. We will use chunks of 450 to be safe.
            val chunks = employees.chunked(450)
            for (chunk in chunks) {
                val batch = db.batch()
                for (emp in chunk) {
                    val docRef = col.document() // Auto ID
                    batch.set(docRef, emp.copy(employeeId = docRef.id))
                    count++
                }
                batch.commit().await()
            }
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun update(id: String, employee: Employee): Result<Unit> {
        return try {
            col.document(id).set(employee).await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun delete(id: String): Result<Unit> {
        return try {
            col.document(id).delete().await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getCount(): Int {
        return try { col.get().await().size() } catch (e: Exception) { 0 }
    }

    suspend fun updateLeaveQuota(id: String, newQuota: Int): Result<Unit> {
        return try {
            col.document(id).update("leaveQuota", newQuota).await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }
}

package com.ptniger.hris.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.ptniger.hris.data.model.OvertimeRequest
import com.ptniger.hris.utils.Constants
import kotlinx.coroutines.tasks.await

class OvertimeRepository {
    private val db = FirebaseFirestore.getInstance()
    private val col = db.collection(Constants.Collections.OVERTIME_REQUESTS)

    /**
     * Mengajukan SPKL baru dengan kepatuhan PP No. 35/2021 (maks 4 jam/hari).
     */
    suspend fun submitRequest(request: OvertimeRequest): Result<String> {
        return try {
            if (request.plannedHours > 4.0) {
                return Result.failure(Exception("Batas lembur legal maksimal 4 jam per hari (PP 35/2021)."))
            }
            val ref = col.add(request).await()
            Result.success(ref.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun approve(requestId: String, approvedBy: String, notes: String = ""): Result<Unit> {
        return try {
            val doc = col.document(requestId).get().await()
            val req = doc.toObject(OvertimeRequest::class.java) ?: throw Exception("SPKL tidak ditemukan")
            
            // Jam yang disetujui dibayarkan adalah sesuai plannedHours jika tidak ada override
            val payable = req.plannedHours

            col.document(requestId).update(
                mapOf(
                    "status" to Constants.OvertimeStatus.APPROVED,
                    "approvedBy" to approvedBy,
                    "approvalNotes" to notes,
                    "approvedPayableHours" to payable
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun reject(requestId: String, rejectedBy: String, reason: String): Result<Unit> {
        return try {
            col.document(requestId).update(
                mapOf(
                    "status" to Constants.OvertimeStatus.REJECTED,
                    "approvedBy" to rejectedBy,
                    "rejectionReason" to reason
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getByEmployee(employeeId: String): List<OvertimeRequest> {
        return try {
            col.whereEqualTo("employeeId", employeeId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get().await().documents.mapNotNull {
                    it.toObject(OvertimeRequest::class.java)?.copy(id = it.id)
                }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Mengambil daftar SPKL pending untuk Manajer berdasarkan hierarki bawahan.
     */
    suspend fun getPendingForManager(
        managerEmployeeId: String,
        managerUserId: String = "",
        departmentId: String = "",
        subordinateEmpIds: Set<String> = emptySet()
    ): List<OvertimeRequest> {
        return try {
            val all = col.whereEqualTo("status", Constants.OvertimeStatus.PENDING)
                .get().await().documents.mapNotNull {
                    it.toObject(OvertimeRequest::class.java)?.copy(id = it.id)
                }
            all.filter { req ->
                (subordinateEmpIds.isNotEmpty() && req.employeeId in subordinateEmpIds) ||
                (managerEmployeeId.isNotEmpty() && req.managerId.equals(managerEmployeeId, ignoreCase = true)) ||
                (managerUserId.isNotEmpty() && req.managerId.equals(managerUserId, ignoreCase = true)) ||
                (departmentId.isNotEmpty() && req.departmentId.equals(departmentId, ignoreCase = true))
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Menghitung total jam lembur yang disetujui (APPROVED) untuk bulan dan tahun tertentu.
     * Digunakan oleh Payroll Generator untuk rekonsiliasi lembur resmi.
     */
    suspend fun getApprovedOvertimeHoursForMonth(employeeId: String, month: Int, year: Int): Double {
        return try {
            val monthStr = String.format("%04d-%02d", year, month)
            val list = col.whereEqualTo("employeeId", employeeId)
                .whereEqualTo("status", Constants.OvertimeStatus.APPROVED)
                .get().await().documents.mapNotNull {
                    it.toObject(OvertimeRequest::class.java)
                }
            list.filter { it.date.startsWith(monthStr) }
                .sumOf { if (it.approvedPayableHours > 0) it.approvedPayableHours else it.plannedHours }
        } catch (e: Exception) {
            0.0
        }
    }
}

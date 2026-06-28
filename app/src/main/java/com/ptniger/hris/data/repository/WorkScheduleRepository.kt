package com.ptniger.hris.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.ptniger.hris.data.model.WorkSchedule
import kotlinx.coroutines.tasks.await

class WorkScheduleRepository {
    private val col = FirebaseFirestore.getInstance().collection("work_schedules")

    suspend fun getById(scheduleId: String): WorkSchedule {
        return try {
            col.document(scheduleId).get().await()
                .toObject(WorkSchedule::class.java) ?: WorkSchedule()
        } catch (e: Exception) { WorkSchedule() }
    }

    suspend fun getDefault(): WorkSchedule = getById("default")

    suspend fun getForEmployee(workScheduleId: String): WorkSchedule {
        if (workScheduleId.isBlank()) return WorkSchedule()
        return getById(workScheduleId)
    }

    suspend fun getAll(): List<WorkSchedule> {
        return try {
            col.get().await().documents.mapNotNull {
                it.toObject(WorkSchedule::class.java)?.copy(scheduleId = it.id)
            }
        } catch (e: Exception) { emptyList() }
    }

    suspend fun save(schedule: WorkSchedule): Result<Unit> {
        return try {
            col.document(schedule.scheduleId).set(schedule).await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }
}

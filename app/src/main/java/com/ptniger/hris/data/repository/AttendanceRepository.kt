package com.ptniger.hris.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.ptniger.hris.data.model.Attendance
import com.ptniger.hris.utils.Constants
import com.ptniger.hris.utils.DateUtils
import kotlinx.coroutines.tasks.await

class AttendanceRepository {
    private val db = FirebaseFirestore.getInstance()
    private val col = db.collection(Constants.Collections.ATTENDANCE)

    suspend fun checkIn(employeeId: String, location: String): Result<String> {
        return try {
            val time = DateUtils.nowTime()
            val status = if (DateUtils.isLate(time)) Constants.AttendanceStatus.LATE
                         else Constants.AttendanceStatus.PRESENT
            val late = DateUtils.calculateLateMinutes(time)
            val att = Attendance(
                employeeId = employeeId, date = DateUtils.today(),
                checkIn = time, status = status,
                lateMinutes = late, location = location
            )
            val ref = col.add(att).await()
            Result.success(ref.id)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun checkOut(attendanceId: String): Result<Unit> {
        return try {
            col.document(attendanceId).update("checkOut", DateUtils.nowTime()).await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getTodayAttendance(employeeId: String): Attendance? {
        return try {
            col.whereEqualTo("employeeId", employeeId)
                .whereEqualTo("date", DateUtils.today())
                .get().await().documents.firstOrNull()?.let {
                    it.toObject(Attendance::class.java)?.copy(attendanceId = it.id)
                }
        } catch (e: Exception) { null }
    }

    suspend fun getMonthlyAttendance(employeeId: String, month: Int, year: Int): List<Attendance> {
        return try {
            val prefix = String.format("%04d-%02d", year, month)
            col.whereEqualTo("employeeId", employeeId)
                .orderBy("date", Query.Direction.ASCENDING)
                .get().await().documents.mapNotNull {
                    it.toObject(Attendance::class.java)?.copy(attendanceId = it.id)
                }.filter { it.date.startsWith(prefix) }
        } catch (e: Exception) { emptyList() }
    }

    suspend fun getAllToday(): List<Attendance> {
        return try {
            col.whereEqualTo("date", DateUtils.today()).get().await().documents.mapNotNull {
                it.toObject(Attendance::class.java)?.copy(attendanceId = it.id)
            }
        } catch (e: Exception) { emptyList() }
    }

    suspend fun getTodayPresentCount(): Int {
        return try {
            col.whereEqualTo("date", DateUtils.today()).get().await().size()
        } catch (e: Exception) { 0 }
    }
}

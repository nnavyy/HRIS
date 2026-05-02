package com.ptniger.hris.data.repository

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.ptniger.hris.data.model.Attendance
import com.ptniger.hris.data.model.OfficeLocation
import com.ptniger.hris.utils.AutomationEngine
import com.ptniger.hris.utils.Constants
import com.ptniger.hris.utils.DateUtils
import com.ptniger.hris.utils.LocationUtils
import kotlinx.coroutines.tasks.await
import java.util.UUID

class AttendanceRepository {
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val col = db.collection(Constants.Collections.ATTENDANCE)

    suspend fun submitAttendance(
        attendance: Attendance, 
        imageUri: Uri, 
        office: OfficeLocation?
    ): Result<String> {
        return try {
            val authUid = FirebaseAuth.getInstance().currentUser?.uid 
                ?: throw IllegalStateException("User not authenticated")

            // 1. Upload Selfie using Auth UID for security rules
            val storageRef = storage.reference.child("attendance_selfies/${authUid}/${UUID.randomUUID()}.jpg")
            storageRef.putFile(imageUri).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()

            // 2. Calculate Distance if Office is provided
            var distance = 0.0
            var isWithinRadius = false
            var validationStatus = Constants.AttendanceStatus.VALID

            if (office != null) {
                distance = LocationUtils.calculateDistance(
                    attendance.latitude, attendance.longitude,
                    office.latitude, office.longitude
                ).toDouble()
                isWithinRadius = distance <= office.allowedRadiusMeters
                if (!isWithinRadius) {
                    validationStatus = Constants.AttendanceStatus.INVALID_LOCATION
                }
            } else {
                validationStatus = Constants.AttendanceStatus.NEED_REVIEW
            }

            // 3. Determine attendanceStatus (late/present) — respects Automation Rule
            var attendanceStatus = Constants.AttendanceStatus.PRESENT
            if (attendance.clockType == Constants.AttendanceType.CLOCK_IN) {
                val autoLateEnabled = AutomationEngine.isRuleActive(AutomationEngine.RuleType.ATTENDANCE)
                if (autoLateEnabled && DateUtils.isLate(attendance.checkIn)) {
                    attendanceStatus = Constants.AttendanceStatus.LATE
                }
            }

            val lateMinutes = if (attendanceStatus == Constants.AttendanceStatus.LATE) {
                DateUtils.calculateLateMinutes(attendance.checkIn)
            } else 0

            // 4. Prepare Final Attendance Object
            val finalAttendance = attendance.copy(
                selfieUrl = downloadUrl,
                selfieStoragePath = storageRef.path,
                distanceFromOfficeMeters = distance,
                isWithinOfficeRadius = isWithinRadius,
                validationStatus = validationStatus,
                attendanceStatus = attendanceStatus,
                lateMinutes = lateMinutes,
                officeId = office?.id ?: "",
                officeLatitude = office?.latitude ?: 0.0,
                officeLongitude = office?.longitude ?: 0.0
            )

            // 5. Save to Firestore
            val ref = col.add(finalAttendance).await()
            
            // 6. Audit Log — respects Automation Rule
            val auditEnabled = AutomationEngine.isRuleActive(AutomationEngine.RuleType.AUDIT)
            if (auditEnabled) {
                AuditLogRepository().log(
                    userId = authUid,
                    userName = "Employee ${attendance.employeeId}",
                    actorRole = "employee",
                    action = if (validationStatus == Constants.AttendanceStatus.VALID) "CLOCK_IN_VALID" else "CLOCK_IN_INVALID_LOCATION",
                    module = "Attendance",
                    targetCollection = Constants.Collections.ATTENDANCE,
                    targetId = ref.id,
                    targetUserId = authUid,
                    details = "clockType=${attendance.clockType}, attendanceStatus=${attendanceStatus}, validationStatus=${validationStatus}, distance=${distance}m, selfiePath=${storageRef.path}"
                )
            }
            
            Result.success(ref.id)
        } catch (e: Exception) { Result.failure(e) }
    }

    @Deprecated("Use submitAttendance() with selfie and GPS validation")
    suspend fun checkIn(employeeId: String, location: String): Result<String> {
        throw IllegalStateException("Selfie and GPS validation are required for check-in. Use submitAttendance().")
    }

    @Deprecated("Use submitAttendance() with selfie and GPS validation")
    suspend fun checkOut(attendanceId: String): Result<Unit> {
        throw IllegalStateException("Selfie and GPS validation are required for check-out. Use submitAttendance().")
    }

    suspend fun updateValidationStatus(attendanceId: String, newStatus: String): Result<Unit> {
        return try {
            col.document(attendanceId).update("validationStatus", newStatus).await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getPendingReviews(): List<Attendance> {
        return try {
            col.whereIn("validationStatus", listOf(Constants.AttendanceStatus.INVALID_LOCATION, Constants.AttendanceStatus.NEED_REVIEW))
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get().await().documents.mapNotNull {
                    it.toObject(Attendance::class.java)?.copy(attendanceId = it.id)
                }
        } catch (e: Exception) { emptyList() }
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

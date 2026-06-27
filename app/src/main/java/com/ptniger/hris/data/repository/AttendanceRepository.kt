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
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.UUID

class AttendanceRepository {
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val col = db.collection(Constants.Collections.ATTENDANCE)

    suspend fun submitAttendance(
        attendance: Attendance, 
        imageUri: Uri, 
        office: OfficeLocation?,
        context: android.content.Context
    ): Result<String> {
        return try {
            val authUid = FirebaseAuth.getInstance().currentUser?.uid 
                ?: throw IllegalStateException("User not authenticated")

            // 1. Upload Selfie to Cloudinary using OkHttp
            var downloadUrl = ""
            var storagePath = ""
            val cloudName = "dxn0pj04j"
            val uploadPreset = "hris_upload"
            
            try {
                val inputStream = context.contentResolver.openInputStream(imageUri)
                val bytes = inputStream?.readBytes() ?: throw Exception("Tidak bisa membaca gambar selfie")
                inputStream.close()
                
                val timestamp = System.currentTimeMillis()
                val result = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    val client = okhttp3.OkHttpClient.Builder()
                        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                        .build()
                    val requestBody = okhttp3.MultipartBody.Builder()
                        .setType(okhttp3.MultipartBody.FORM)
                        .addFormDataPart("file", "selfie.jpg", bytes.toRequestBody("image/jpeg".toMediaTypeOrNull()))
                        .addFormDataPart("upload_preset", uploadPreset)
                        .addFormDataPart("public_id", "attendance_${authUid}_${timestamp}")
                        .build()
                    
                    val request = okhttp3.Request.Builder()
                        .url("https://api.cloudinary.com/v1_1/$cloudName/image/upload")
                        .post(requestBody)
                        .build()
                        
                    val response = client.newCall(request).execute()
                    val responseBody = response.body?.string()
                    if (response.isSuccessful && responseBody != null) {
                        val json = org.json.JSONObject(responseBody)
                        Pair(json.getString("secure_url"), json.getString("public_id"))
                    } else {
                        throw Exception("Upload foto gagal (${response.code}): ${responseBody?.take(200) ?: "no response"}")
                    }
                }
                downloadUrl = result.first
                storagePath = result.second
            } catch (e: Exception) {
                return Result.failure(Exception("Upload selfie gagal: ${e.message ?: e.javaClass.simpleName}"))
            }

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

            // 4. Prepare Final Attendance Data — time values already come from server via ViewModel
            val checkInTime = attendance.checkIn
            val checkOutTime = attendance.checkOut
            val isCheckIn = attendance.clockType == Constants.AttendanceType.CLOCK_IN
            
            if (isCheckIn) {
                // For Check-In: Create new record
                val finalAttendance = attendance.copy(
                    selfieUrl = downloadUrl,
                    selfieStoragePath = storagePath,
                    distanceFromOfficeMeters = distance,
                    isWithinOfficeRadius = isWithinRadius,
                    validationStatus = validationStatus,
                    attendanceStatus = attendanceStatus,
                    lateMinutes = lateMinutes,
                    officeId = office?.id ?: "",
                    officeLatitude = office?.latitude ?: 0.0,
                    officeLongitude = office?.longitude ?: 0.0,
                    checkIn = checkInTime,
                    deviceModel = android.os.Build.MODEL
                )
                val ref = col.add(finalAttendance).await()
                
                if (AutomationEngine.isRuleActive(AutomationEngine.RuleType.AUDIT)) {
                    AuditLogRepository().log(
                        userId = authUid, userName = "Employee ${attendance.employeeId}", actorRole = "employee",
                        action = if (validationStatus == Constants.AttendanceStatus.VALID) "CLOCK_IN_VALID" else "CLOCK_IN_INVALID_LOCATION",
                        module = "Attendance", targetCollection = Constants.Collections.ATTENDANCE, targetId = ref.id,
                        targetUserId = authUid, details = "status=${attendanceStatus}, validation=${validationStatus}, distance=${distance}m"
                    )
                }
                return Result.success(ref.id)
            } else {
                // For Check-Out: Find existing today's record and UPDATE
                val todayRecord = getTodayAttendance(attendance.employeeId)
                if (todayRecord != null && todayRecord.attendanceId.isNotEmpty()) {
                    val checkOutNow = checkOutTime.ifEmpty { DateUtils.nowTime() }
                    val overtimeHours = DateUtils.calculateOvertimeHours(checkOutNow)
                    
                    col.document(todayRecord.attendanceId).update(
                        mapOf(
                            "checkOut" to checkOutNow,
                            "overtimeHours" to overtimeHours,
                            "distanceFromOfficeMeters" to distance,
                            "isWithinOfficeRadius" to isWithinRadius,
                            "isMockLocation" to attendance.isMockLocation,
                            "serverTimestamp" to attendance.serverTimestamp,
                            "deviceTimestamp" to attendance.deviceTimestamp,
                            "isTimeTampered" to attendance.isTimeTampered
                        )
                    ).await()
                    
                    if (AutomationEngine.isRuleActive(AutomationEngine.RuleType.AUDIT)) {
                        AuditLogRepository().log(
                            userId = authUid, userName = "Employee ${attendance.employeeId}", actorRole = "employee",
                            action = "CLOCK_OUT", module = "Attendance", targetCollection = Constants.Collections.ATTENDANCE, 
                            targetId = todayRecord.attendanceId, targetUserId = authUid, 
                            details = "checkOut=$checkOutNow, overtime=$overtimeHours"
                        )
                    }
                    return Result.success(todayRecord.attendanceId)
                } else {
                    return Result.failure(Exception("Tidak bisa check-out: Anda belum check-in hari ini."))
                }
            }
        } catch (e: Exception) {
            return Result.failure(e)
        }
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
                .get().await().documents.mapNotNull {
                    it.toObject(Attendance::class.java)?.copy(attendanceId = it.id)
                }.sortedByDescending { it.date }
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
                .get().await().documents.mapNotNull {
                    it.toObject(Attendance::class.java)?.copy(attendanceId = it.id)
                }.filter { it.date.startsWith(prefix) }
                .sortedBy { it.date }
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

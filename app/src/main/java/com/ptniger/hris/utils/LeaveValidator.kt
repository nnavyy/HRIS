package com.ptniger.hris.utils

import com.ptniger.hris.data.model.LeavePolicy
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

object LeaveValidator {

    data class ValidationResult(
        val isValid: Boolean,
        val autoReject: Boolean = false,
        val errorMessage: String = "",
        val deductsQuota: Boolean = true
    )

    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun validate(
        startDateStr: String,
        endDateStr: String,
        duration: Int,
        leaveQuota: Int,
        policy: LeavePolicy,
        leaveType: String = ""
    ): ValidationResult {

        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.time

        val startDate = try { sdf.parse(startDateStr) } catch (e: Exception) {
            return ValidationResult(false, false, "Format tanggal tidak valid.")
        }
        val endDate = try { sdf.parse(endDateStr) } catch (e: Exception) {
            return ValidationResult(false, false, "Format tanggal selesai tidak valid.")
        }

        val isEmergency = policy.emergencyLeaveTypes.contains(leaveType)
        val allowPast = policy.allowPastDateSubmission || policy.allowPastDateForTypes.contains(leaveType)
        val deductsQuota = !isEmergency

        // 1. Tanggal mulai tidak di masa lalu
        if (!allowPast && startDate != null && startDate.before(today)) {
            return ValidationResult(
                isValid = false, autoReject = policy.autoRejectOnExpiry,
                errorMessage = "Tanggal mulai cuti sudah lewat. Pengajuan tidak dapat diproses untuk tanggal yang telah berlalu.",
                deductsQuota = false
            )
        }
        
        // 1b. Jika allowPast karena tipe khusus, cek batas mundurnya
        if (startDate != null && startDate.before(today) && policy.allowPastDateForTypes.contains(leaveType)) {
            val pastDays = TimeUnit.MILLISECONDS.toDays(today.time - startDate.time)
            if (pastDays > policy.maxPastDays) {
                return ValidationResult(
                    isValid = false, autoReject = policy.autoRejectOnExpiry,
                    errorMessage = "Cuti $leaveType hanya dapat diajukan maksimal ${policy.maxPastDays} hari setelah kejadian.",
                    deductsQuota = false
                )
            }
        }

        // 2. Minimal H-N sebelum tanggal mulai
        if (!isEmergency && policy.minAdvanceDays > 0 && startDate != null) {
            val diffDays = TimeUnit.MILLISECONDS.toDays(startDate.time - today.time)
            if (diffDays < policy.minAdvanceDays) {
                return ValidationResult(
                    isValid = false, autoReject = policy.autoRejectOnExpiry,
                    errorMessage = "Pengajuan harus dilakukan minimal ${policy.minAdvanceDays} hari sebelum tanggal mulai " +
                            "(sisa ${diffDays} hari).",
                    deductsQuota = false
                )
            }
        }

        // 3. Durasi tidak melebihi batas per request
        if (duration > policy.maxDaysPerRequest) {
            return ValidationResult(
                isValid = false, autoReject = false,
                errorMessage = "Durasi ($duration hari) melebihi batas maksimal per pengajuan (${policy.maxDaysPerRequest} hari).",
                deductsQuota = false
            )
        }

        // 4. Kuota sisa (hanya jika deductsQuota)
        if (deductsQuota) {
            if (leaveQuota <= 0) {
                return ValidationResult(
                    isValid = false, autoReject = true,
                    errorMessage = "Kuota cuti tahunan sudah habis. Pengajuan otomatis ditolak.",
                    deductsQuota = false
                )
            }
            if (duration > leaveQuota) {
                return ValidationResult(
                    isValid = false, autoReject = false,
                    errorMessage = "Durasi ($duration hari) melebihi sisa kuota ($leaveQuota hari).",
                    deductsQuota = false
                )
            }
        }

        return ValidationResult(isValid = true, deductsQuota = deductsQuota)
    }

    /** Cek apakah pengajuan PENDING sudah kadaluarsa (tanggal mulai sudah lewat) */
    fun isExpired(startDateStr: String): Boolean {
        val today = Calendar.getInstance().time
        val startDate = try { sdf.parse(startDateStr) } catch (e: Exception) { return false }
        return startDate != null && startDate.before(today)
    }
}

package com.ptniger.hris.utils

import com.ptniger.hris.data.model.LeavePolicy
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

object LeaveValidator {

    data class ValidationResult(
        val isValid: Boolean,
        val autoReject: Boolean = false,
        val errorMessage: String = ""
    )

    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun validate(
        startDateStr: String,
        endDateStr: String,
        duration: Int,
        leaveQuota: Int,
        policy: LeavePolicy
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

        // 1. Tanggal mulai tidak di masa lalu
        if (!policy.allowPastDateSubmission && startDate != null && startDate.before(today)) {
            return ValidationResult(
                isValid = false, autoReject = policy.autoRejectOnExpiry,
                errorMessage = "Tanggal mulai cuti sudah lewat. Pengajuan tidak dapat diproses untuk tanggal yang telah berlalu."
            )
        }

        // 2. Minimal H-N sebelum tanggal mulai
        if (policy.minAdvanceDays > 0 && startDate != null) {
            val diffDays = TimeUnit.MILLISECONDS.toDays(startDate.time - today.time)
            if (diffDays < policy.minAdvanceDays) {
                return ValidationResult(
                    isValid = false, autoReject = policy.autoRejectOnExpiry,
                    errorMessage = "Pengajuan harus dilakukan minimal ${policy.minAdvanceDays} hari sebelum tanggal mulai " +
                            "(sisa ${diffDays} hari)."
                )
            }
        }

        // 3. Durasi tidak melebihi batas per request
        if (duration > policy.maxDaysPerRequest) {
            return ValidationResult(
                isValid = false, autoReject = false,
                errorMessage = "Durasi ($duration hari) melebihi batas maksimal per pengajuan (${policy.maxDaysPerRequest} hari)."
            )
        }

        // 4. Kuota sisa
        if (leaveQuota <= 0) {
            return ValidationResult(
                isValid = false, autoReject = true,
                errorMessage = "Kuota cuti tahunan sudah habis. Pengajuan otomatis ditolak."
            )
        }
        if (duration > leaveQuota) {
            return ValidationResult(
                isValid = false, autoReject = false,
                errorMessage = "Durasi ($duration hari) melebihi sisa kuota ($leaveQuota hari)."
            )
        }

        return ValidationResult(isValid = true)
    }

    /** Cek apakah pengajuan PENDING sudah kadaluarsa (tanggal mulai sudah lewat) */
    fun isExpired(startDateStr: String): Boolean {
        val today = Calendar.getInstance().time
        val startDate = try { sdf.parse(startDateStr) } catch (e: Exception) { return false }
        return startDate != null && startDate.before(today)
    }
}

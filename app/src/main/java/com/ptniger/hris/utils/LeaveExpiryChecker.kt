package com.ptniger.hris.utils

import com.ptniger.hris.data.repository.EmployeeRepository
import com.ptniger.hris.data.repository.LeaveRepository
import com.ptniger.hris.data.repository.NotificationRepository

/**
 * Dipanggil saat HR / Manager login untuk auto-expire
 * pengajuan cuti yang pending tapi tanggalnya sudah lewat.
 */
object LeaveExpiryChecker {

    suspend fun checkAndExpireOverdueLeaves() {
        val leaveRepo = LeaveRepository()
        val notifRepo = NotificationRepository()
        val empRepo = EmployeeRepository()

        val allPending = leaveRepo.getPending("")   // semua pending tanpa filter dept
        val expired = allPending.filter { LeaveValidator.isExpired(it.startDate) }

        expired.forEach { leave ->
            leaveRepo.rejectWithReason(
                leaveId = leave.leaveId,
                approvedBy = "system",
                reason = "AUTO-EXPIRED: Tanggal cuti (${leave.startDate}) sudah berlalu tanpa approval."
            )

            // Notif in-app ke karyawan
            val employee = empRepo.getByUserId(leave.employeeId)
                ?: empRepo.getById(leave.employeeId)
            employee?.userId?.takeIf { it.isNotEmpty() }?.let { uid ->
                notifRepo.send(
                    userId = uid,
                    title = "Pengajuan Cuti Kadaluarsa",
                    message = "Pengajuan ${leave.type} untuk ${leave.startDate} otomatis ditolak " +
                            "karena belum diapprove sebelum tanggal tersebut.",
                    type = "leave_expired"
                )
            }

            // Email ke karyawan (fire and forget)
            try {
                if (employee?.email?.isNotEmpty() == true) {
                    LeaveEmailNotifier.notifyEmployeeOfDecision(
                        leave = leave, employeeEmail = employee.email,
                        decision = "rejected", approverName = "Sistem Otomatis"
                    )
                }
            } catch (_: Exception) {}
        }
    }
}

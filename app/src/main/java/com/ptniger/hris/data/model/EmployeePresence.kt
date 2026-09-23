package com.ptniger.hris.data.model

/**
 * Snapshot status kehadiran karyawan hari ini.
 * Di-compute dari Attendance + LeaveRequest + WorkSchedule.
 * Bukan disimpan di Firestore — selalu dihitung saat diperlukan.
 */
data class EmployeePresence(
    val employeeId: String = "",
    val employeeName: String = "",
    val employeePosition: String = "",
    val employeeDepartment: String = "",
    val officeId: String = "",
    val officeName: String = "",           // "Kantor Pusat", "Cabang Ungaran", dll

    val presenceStatus: PresenceStatus = PresenceStatus.UNKNOWN,
    val workMode: String = "onsite",       // "onsite" | "wfh"
    val checkInTime: String = "",          // "08:05" jika sudah check-in
    val checkOutTime: String = "",
    val lateMinutes: Int = 0,

    val isFaceRegistered: Boolean = false
)

enum class PresenceStatus(val label: String) {
    PRESENT_ONSITE("Hadir di Kantor"),
    PRESENT_WFH   ("Hadir — WFH"),
    LATE_ONSITE   ("Terlambat"),
    NOT_YET       ("Belum Absen"),
    ON_LEAVE      ("Cuti"),
    SICK          ("Sakit"),
    PERMISSION    ("Izin"),
    ABSENT        ("Tidak Hadir"),
    OFF_DAY       ("Hari Libur"),
    UNKNOWN       ("—")
}

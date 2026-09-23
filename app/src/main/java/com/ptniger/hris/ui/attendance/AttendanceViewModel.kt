package com.ptniger.hris.ui.attendance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ptniger.hris.data.model.Attendance
import com.ptniger.hris.data.repository.AttendanceRepository
import com.ptniger.hris.utils.Constants
import com.ptniger.hris.utils.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AttendanceViewModel : ViewModel() {
    private val repo = AttendanceRepository()
    private val _state = MutableStateFlow(AttendanceState())
    val state: StateFlow<AttendanceState> = _state

    fun loadTodayAttendance(employeeId: String, userEmail: String = "") {
        if (employeeId.isEmpty()) {
            _state.value = AttendanceState(message = "Employee ID belum terhubung. Hubungi HR.")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            try {
                // Try finding the correct employee doc ID
                val resolvedId = resolveEmployeeId(employeeId, userEmail)
                val today = repo.getTodayAttendance(resolvedId)
                val monthly = repo.getMonthlyAttendance(resolvedId, DateUtils.currentMonth(), DateUtils.currentYear())
                val calendar = buildCalendar(monthly)
                val employee = com.ptniger.hris.data.repository.EmployeeRepository().getById(resolvedId)
                _state.value = AttendanceState(
                    hasCheckedIn = today != null,
                    checkInTime = today?.checkIn ?: "",
                    checkOutTime = today?.checkOut ?: "",
                    isLate = today?.attendanceStatus == Constants.AttendanceStatus.LATE,
                    attendanceId = today?.attendanceId ?: "",
                    monthlyCalendar = calendar,
                    isLoading = false,
                    resolvedEmployeeId = resolvedId,
                    isFaceRegistered = employee?.isFaceRegistered == true
                )
            } catch (e: Exception) {
                _state.value = AttendanceState(
                    isLoading = false,
                    message = "Gagal memuat data absensi: ${e.message}"
                )
            }
        }
    }

    /**
     * Resolves employee document ID by trying multiple lookup strategies:
     * 1. Direct document ID
     * 2. By userId field
     * 3. By email field (fallback)
     */
    private suspend fun resolveEmployeeId(employeeId: String, userEmail: String = ""): String {
        val employeeRepo = com.ptniger.hris.data.repository.EmployeeRepository()
        
        // 1. Try direct doc ID
        val byId = employeeRepo.getById(employeeId)
        if (byId != null) return byId.employeeId
        
        // 2. Try by userId field
        val byUserId = employeeRepo.getByUserId(employeeId)
        if (byUserId != null) return byUserId.employeeId
        
        // 3. Try by email
        if (userEmail.isNotEmpty()) {
            val byEmail = employeeRepo.getByEmail(userEmail)
            if (byEmail != null) return byEmail.employeeId
        }
        
        return employeeId // fallback to original
    }

    fun submitAttendance(
        employeeId: String,
        imageUri: android.net.Uri,
        latitude: Double,
        longitude: Double,
        clockType: String,
        context: android.content.Context,
        isMockDetected: Boolean = false,
        userEmail: String = "",
        checkInMode: String = "selfie",
        faceRecognitionSimilarity: Float = 0f,
        livenessVerified: Boolean = false
    ) {
        if (employeeId.isEmpty()) {
            _state.value = _state.value.copy(message = "Employee ID belum terhubung")
            return
        }
        
        // Block mock/fake GPS immediately
        if (isMockDetected) {
            _state.value = _state.value.copy(
                message = "⛔ Absensi DITOLAK: Terdeteksi penggunaan Fake GPS / Lokasi Palsu. Tindakan ini tercatat dan akan dilaporkan.",
                isLoading = false
            )
            return
        }
        
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, message = "Memproses absensi...")
            try {
                com.ptniger.hris.utils.TrafficLatencyGuard.runWithTrafficGuard("Absensi") {
                    val officeRepo = com.ptniger.hris.data.repository.OfficeLocationRepository()
                    val employeeRepo = com.ptniger.hris.data.repository.EmployeeRepository()
                    
                    // Resolve employee with multiple fallbacks
                    var employee = employeeRepo.getById(employeeId)
                    if (employee == null) {
                        employee = employeeRepo.getByUserId(employeeId)
                    }
                    if (employee == null && userEmail.isNotEmpty()) {
                        employee = employeeRepo.getByEmail(userEmail)
                    }
                    
                    var office: com.ptniger.hris.data.model.OfficeLocation? = null
                    if (employee?.officeId?.isNotEmpty() == true) {
                        office = officeRepo.getById(employee.officeId)
                    }
                    
                    if (employee == null) {
                        _state.value = _state.value.copy(message = "Akun Anda belum dihubungkan dengan data Karyawan. Hubungi HR untuk Integrasi Akun Sistem.", isLoading = false)
                        return@runWithTrafficGuard
                    }

                    if (office == null) {
                        val allOffices = officeRepo.getAll().filter { it.isActive }
                        if (allOffices.isNotEmpty()) {
                            // Fallback: Cari kantor terdekat dari lokasi pengguna saat ini
                            var closestOffice = allOffices.first()
                            var minDistance = Float.MAX_VALUE
                            for (o in allOffices) {
                                val results = FloatArray(1)
                                android.location.Location.distanceBetween(latitude, longitude, o.latitude, o.longitude, results)
                                if (results[0] < minDistance) {
                                    minDistance = results[0]
                                    closestOffice = o
                                }
                            }
                            office = closestOffice
                        } else {
                            _state.value = _state.value.copy(message = "Absensi gagal: Tidak ada lokasi kantor aktif di sistem. Harap tambahkan Lokasi Kantor terlebih dahulu.", isLoading = false)
                            return@runWithTrafficGuard
                        }
                    }

                    val results = FloatArray(1)
                    android.location.Location.distanceBetween(latitude, longitude, office.latitude, office.longitude, results)
                    val distance = results[0]

                    if (distance > office.allowedRadiusMeters) {
                        _state.value = _state.value.copy(message = "Absensi gagal: Anda berada di luar jangkauan (${distance.toInt()} meter). Harus dalam ${office.allowedRadiusMeters.toInt()} meter dari kantor.", isLoading = false)
                        return@runWithTrafficGuard
                    }

                    // Validasi Keras Anti-Manipulasi Jam (Anti-Time Tampering Protection)
                    val serverTimeMs = DateUtils.getServerTimeMillis()
                    val deviceTime = System.currentTimeMillis()
                    val timeDiffMs = Math.abs(deviceTime - serverTimeMs)
                    val isTimeTampered = timeDiffMs > 3 * 60 * 1000 // Selisih > 3 menit

                    if (isTimeTampered) {
                        val diffMinutes = timeDiffMs / (60 * 1000)
                        _state.value = _state.value.copy(
                            message = "⛔ Absensi DITOLAK: Jam ponsel Anda tidak sinkron (selisih $diffMinutes menit dari server resmi). Silakan aktifkan opsi 'Tanggal & Waktu Otomatis' di Pengaturan HP Anda.",
                            isLoading = false
                        )
                        return@runWithTrafficGuard
                    }

                    // Waktu dan tanggal 100% dikunci ke Zona Waktu Resmi Kantor Cabang Terkait (WIB / WITA / WIT)
                    val officeTz = DateUtils.resolveTimeZone(office.timeZone)
                    val tzLabel = DateUtils.getTimeZoneLabel(officeTz)
                    val serverTime = DateUtils.formatTime(serverTimeMs, officeTz)
                    val serverDate = DateUtils.formatDate(serverTimeMs, officeTz)

                    val resolvedEmpId = employee.employeeId
                    
                    val attendance = Attendance(
                        employeeId = resolvedEmpId,
                        date = serverDate,
                        clockType = clockType,
                        checkIn = if (clockType == Constants.AttendanceType.CLOCK_IN) serverTime else "",
                        checkOut = if (clockType == Constants.AttendanceType.CLOCK_OUT) serverTime else "",
                        latitude = latitude,
                        longitude = longitude,
                        isMockLocation = isMockDetected,
                        serverTimestamp = serverTimeMs,
                        deviceTimestamp = deviceTime,
                        isTimeTampered = false,
                        checkInMode = checkInMode,
                        faceRecognitionSimilarity = faceRecognitionSimilarity,
                        livenessVerified = livenessVerified
                    )

                    repo.submitAttendance(attendance, imageUri, office, context).fold(
                        onSuccess = {
                            _state.value = _state.value.copy(message = "Absensi berhasil! (Waktu Server: $serverTime $tzLabel - Kantor: ${office.name})", isLoading = false)
                            loadTodayAttendance(resolvedEmpId)
                        },
                        onFailure = {
                            _state.value = _state.value.copy(message = "Gagal: ${it.message ?: it.javaClass.simpleName}", isLoading = false)
                        }
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(message = "Error: ${e.message ?: e.javaClass.simpleName}", isLoading = false)
            }
        }
    }

    fun loadAllToday(userId: String = "", departmentId: String = "", userObj: com.ptniger.hris.data.model.User? = null) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            var list = repo.getAllToday()
            val empRepo = com.ptniger.hris.data.repository.EmployeeRepository()
            val allEmps = empRepo.getAll()
            
            // Map employeeId to Name
            val namesMap = allEmps.associate { it.employeeId to it.name }
            
            if (userId.isNotEmpty() || departmentId.isNotEmpty() || userObj != null) {
                val mgrUser = userObj ?: com.ptniger.hris.data.model.User(userId = userId, departmentId = departmentId)
                val mgrEmp = allEmps.find { it.userId == userId }
                val teamIds = com.ptniger.hris.utils.HierarchyHelper.getSubordinateEmployeeIds(mgrUser, mgrEmp, allEmps)
                
                list = list.filter { it.employeeId in teamIds }
            }
            _state.value = _state.value.copy(todayList = list, employeeNames = namesMap, isLoading = false)
        }
    }


    private fun buildCalendar(attendance: List<Attendance>): List<Pair<Int, String>> {
        val result = mutableListOf<Pair<Int, String>>()
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.YEAR, DateUtils.currentYear())
        cal.set(java.util.Calendar.MONTH, DateUtils.currentMonth() - 1)
        val daysInMonth = cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
        
        cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
        // DAY_OF_WEEK: Sunday=1, Monday=2... Saturday=7
        // UI uses Monday-first: S(Mon)=0, S(Tue)=1, R(Wed)=2, K(Thu)=3, J(Fri)=4, S(Sat)=5, M(Sun)=6
        val firstDayOfWeek = cal.get(java.util.Calendar.DAY_OF_WEEK)
        val paddingDays = if (firstDayOfWeek == java.util.Calendar.SUNDAY) 6 else firstDayOfWeek - 2
        
        for (i in 0 until paddingDays) {
            result.add(-1 to "")
        }

        for (d in 1..daysInMonth) {
            cal.set(java.util.Calendar.DAY_OF_MONTH, d)
            val dayOfWeek = cal.get(java.util.Calendar.DAY_OF_WEEK)
            val isWeekend = dayOfWeek == java.util.Calendar.SATURDAY || dayOfWeek == java.util.Calendar.SUNDAY
            
            val dateStr = String.format("%04d-%02d-%02d", DateUtils.currentYear(), DateUtils.currentMonth(), d)
            val att = attendance.find { it.date == dateStr }
            val status = att?.attendanceStatus ?: if (isWeekend) "holiday" else ""
            result.add(d to status)
        }
        return result
    }

    fun clearMessage() { _state.value = _state.value.copy(message = null) }
}

data class AttendanceState(
    val isLoading: Boolean = false,
    val hasCheckedIn: Boolean = false,
    val checkInTime: String = "",
    val checkOutTime: String = "",
    val isLate: Boolean = false,
    val attendanceId: String = "",
    val message: String? = null,
    val monthlyCalendar: List<Pair<Int, String>> = emptyList(),
    val todayList: List<Attendance> = emptyList(),
    val resolvedEmployeeId: String = "",
    val isFaceRegistered: Boolean = false,
    val employeeNames: Map<String, String> = emptyMap()
)

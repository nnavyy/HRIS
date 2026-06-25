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

    fun loadTodayAttendance(employeeId: String) {
        if (employeeId.isEmpty()) {
            _state.value = AttendanceState(message = "Employee ID belum terhubung. Hubungi HR.")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            try {
                val today = repo.getTodayAttendance(employeeId)
                val monthly = repo.getMonthlyAttendance(employeeId, DateUtils.currentMonth(), DateUtils.currentYear())
                val calendar = buildCalendar(monthly)
                _state.value = AttendanceState(
                    hasCheckedIn = today != null,
                    checkInTime = today?.checkIn ?: "",
                    checkOutTime = today?.checkOut ?: "",
                    isLate = today?.attendanceStatus == Constants.AttendanceStatus.LATE,
                    attendanceId = today?.attendanceId ?: "",
                    monthlyCalendar = calendar,
                    isLoading = false
                )
            } catch (e: Exception) {
                _state.value = AttendanceState(
                    isLoading = false,
                    message = "Gagal memuat data absensi: ${e.message}"
                )
            }
        }
    }

    fun submitAttendance(
        employeeId: String,
        imageUri: android.net.Uri,
        latitude: Double,
        longitude: Double,
        clockType: String,
        context: android.content.Context
    ) {
        if (employeeId.isEmpty()) {
            _state.value = _state.value.copy(message = "Employee ID belum terhubung")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, message = "Memproses absensi...")
            try {
                val officeRepo = com.ptniger.hris.data.repository.OfficeLocationRepository()
                val employeeRepo = com.ptniger.hris.data.repository.EmployeeRepository()
                // Try by document ID first, then by userId field
                var employee = employeeRepo.getById(employeeId)
                if (employee == null) {
                    employee = employeeRepo.getByUserId(employeeId)
                }
                
                val office = if (employee?.officeId?.isNotEmpty() == true) {
                    officeRepo.getById(employee.officeId)
                } else {
                    // Fallback: get any active office
                    val allOffices = officeRepo.getAll()
                    allOffices.firstOrNull { it.isActive }
                }

                if (office == null) {
                    _state.value = _state.value.copy(message = "Absensi gagal: Lokasi kantor belum ditetapkan.", isLoading = false)
                    return@launch
                }

                val results = FloatArray(1)
                android.location.Location.distanceBetween(latitude, longitude, office.latitude, office.longitude, results)
                val distance = results[0]

                if (distance > office.allowedRadiusMeters) {
                    _state.value = _state.value.copy(message = "Absensi gagal: Anda berada di luar jangkauan (${distance.toInt()} meter). Harus dalam ${office.allowedRadiusMeters.toInt()} meter dari kantor.", isLoading = false)
                    return@launch
                }

                val now = DateUtils.nowTime()
                
                // If it's a check-out, we actually need to get the existing attendance ID, 
                // but submitAttendance in repository creates a new record.
                // For MVP, submitAttendance creates a new record for both check-in and check-out.
                // We will handle logic accordingly.
                val attendance = Attendance(
                    employeeId = employeeId,
                    date = DateUtils.today(),
                    clockType = clockType,
                    checkIn = if (clockType == Constants.AttendanceType.CLOCK_IN) now else "",
                    checkOut = if (clockType == Constants.AttendanceType.CLOCK_OUT) now else "",
                    latitude = latitude,
                    longitude = longitude
                )

                repo.submitAttendance(attendance, imageUri, office, context).fold(
                    onSuccess = {
                        _state.value = _state.value.copy(message = "Absensi berhasil!", isLoading = false)
                        loadTodayAttendance(employeeId)
                    },
                    onFailure = {
                        _state.value = _state.value.copy(message = "Gagal: ${it.message ?: it.javaClass.simpleName}", isLoading = false)
                    }
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(message = "Error: ${e.message ?: e.javaClass.simpleName}", isLoading = false)
            }
        }
    }

    fun loadAllToday(userId: String = "", departmentId: String = "") {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            var list = repo.getAllToday()
            if (userId.isNotEmpty() || departmentId.isNotEmpty()) {
                val empRepo = com.ptniger.hris.data.repository.EmployeeRepository()
                val allEmps = empRepo.getAll()
                val teamIds = allEmps.filter {
                    it.managerId == userId || (departmentId.isNotEmpty() && it.department.equals(departmentId, ignoreCase = true))
                }.map { it.employeeId }.toSet()
                
                list = list.filter { it.employeeId in teamIds }
            }
            _state.value = _state.value.copy(todayList = list, isLoading = false)
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
    val todayList: List<Attendance> = emptyList()
)

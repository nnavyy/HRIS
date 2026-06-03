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
        clockType: String
    ) {
        if (employeeId.isEmpty()) {
            _state.value = _state.value.copy(message = "Employee ID belum terhubung")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, message = "Memproses absensi...")
            try {
                val officeRepo = com.ptniger.hris.data.repository.OfficeLocationRepository()
                val activeOffices = officeRepo.getActiveLocations()
                val office = activeOffices.firstOrNull()

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

                repo.submitAttendance(attendance, imageUri, office).fold(
                    onSuccess = {
                        _state.value = _state.value.copy(message = "Absensi berhasil!", isLoading = false)
                        loadTodayAttendance(employeeId)
                    },
                    onFailure = {
                        _state.value = _state.value.copy(message = "Gagal: ${it.message}", isLoading = false)
                    }
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(message = "Error: ${e.message}", isLoading = false)
            }
        }
    }

    fun loadAllToday() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val list = repo.getAllToday()
            _state.value = _state.value.copy(todayList = list, isLoading = false)
        }
    }


    private fun buildCalendar(attendance: List<Attendance>): List<Pair<Int, String>> {
        val result = mutableListOf<Pair<Int, String>>()
        val daysInMonth = java.util.Calendar.getInstance().let {
            it.set(java.util.Calendar.YEAR, DateUtils.currentYear())
            it.set(java.util.Calendar.MONTH, DateUtils.currentMonth() - 1)
            it.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
        }
        for (d in 1..daysInMonth) {
            val dateStr = String.format("%04d-%02d-%02d", DateUtils.currentYear(), DateUtils.currentMonth(), d)
            val att = attendance.find { it.date == dateStr }
            val status = att?.attendanceStatus ?: if (d % 7 == 0 || d % 7 == 6) "holiday" else ""
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

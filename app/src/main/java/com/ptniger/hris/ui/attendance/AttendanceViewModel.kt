package com.ptniger.hris.ui.attendance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ptniger.hris.data.model.Attendance
import com.ptniger.hris.data.repository.AttendanceRepository
import com.ptniger.hris.utils.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AttendanceViewModel : ViewModel() {
    private val repo = AttendanceRepository()
    private val _state = MutableStateFlow(AttendanceState())
    val state: StateFlow<AttendanceState> = _state

    fun loadTodayAttendance(employeeId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val today = repo.getTodayAttendance(employeeId)
            val monthly = repo.getMonthlyAttendance(employeeId, DateUtils.currentMonth(), DateUtils.currentYear())
            val calendar = buildCalendar(monthly)
            _state.value = AttendanceState(
                hasCheckedIn = today != null,
                checkInTime = today?.checkIn ?: "",
                checkOutTime = today?.checkOut ?: "",
                isLate = today?.status == "late",
                attendanceId = today?.attendanceId ?: "",
                monthlyCalendar = calendar,
                isLoading = false
            )
        }
    }

    fun checkIn(employeeId: String, location: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            repo.checkIn(employeeId, location).fold(
                onSuccess = { _state.value = _state.value.copy(message = "Check-in berhasil!"); loadTodayAttendance(employeeId) },
                onFailure = { _state.value = _state.value.copy(message = "Error: ${it.message}", isLoading = false) }
            )
        }
    }

    fun checkOut() {
        viewModelScope.launch {
            val id = _state.value.attendanceId
            if (id.isNotEmpty()) {
                repo.checkOut(id).fold(
                    onSuccess = { _state.value = _state.value.copy(checkOutTime = DateUtils.nowTime(), message = "Check-out berhasil!") },
                    onFailure = { _state.value = _state.value.copy(message = "Error: ${it.message}") }
                )
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
        val daysInMonth = 30
        for (d in 1..daysInMonth) {
            val dateStr = String.format("%04d-%02d-%02d", DateUtils.currentYear(), DateUtils.currentMonth(), d)
            val att = attendance.find { it.date == dateStr }
            val status = att?.status ?: if (d % 7 == 0 || d % 7 == 6) "holiday" else ""
            result.add(d to status)
        }
        return result
    }
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

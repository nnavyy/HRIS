package com.ptniger.hris.ui.employee

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ptniger.hris.data.model.Attendance
import com.ptniger.hris.data.repository.AttendanceRepository
import com.ptniger.hris.data.repository.AuditLogRepository
import com.ptniger.hris.data.repository.EmployeeRepository
import com.ptniger.hris.data.repository.OfficeLocationRepository
import com.ptniger.hris.utils.Constants
import com.ptniger.hris.utils.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AttendanceViewModel : ViewModel() {
    private val attendanceRepo = AttendanceRepository()
    private val employeeRepo = EmployeeRepository()
    private val officeRepo = OfficeLocationRepository()
    private val auditRepo = AuditLogRepository()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun submitAttendance(
        employeeId: String,
        employeeName: String,
        type: String, // clock_in or clock_out
        imageUri: Uri,
        latitude: Double,
        longitude: Double,
        accuracy: Float
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _message.value = null

            try {
                // 1. Get Employee to find their linked officeId
                val employee = employeeRepo.getByUserId(employeeId) // Assuming employeeId passed here is actually userId for MVP, otherwise getById
                    ?: employeeRepo.getById(employeeId)
                    ?: throw Exception("Employee record not found")

                // 2. Get Office Details
                val office = if (employee.officeId.isNotEmpty()) {
                    officeRepo.getById(employee.officeId)
                } else null

                // 3. Prepare Attendance Object
                val time = DateUtils.nowTime()
                val isLate = if (type == Constants.AttendanceType.CLOCK_IN) DateUtils.isLate(time) else false
                val lateMinutes = if (isLate) DateUtils.calculateLateMinutes(time) else 0

                val att = Attendance(
                    employeeId = employee.employeeId,
                    date = DateUtils.today(),
                    type = type,
                    checkIn = if (type == Constants.AttendanceType.CLOCK_IN) time else "",
                    checkOut = if (type == Constants.AttendanceType.CLOCK_OUT) time else "",
                    lateMinutes = lateMinutes,
                    latitude = latitude,
                    longitude = longitude,
                    accuracy = accuracy,
                    // device info can be added here
                )

                // 4. Submit to Repository (Handles Storage upload & distance math)
                val result = attendanceRepo.submitAttendance(att, imageUri, office)
                
                result.onSuccess { attId ->
                    // Get the saved attendance to check status for Audit Log
                    val savedAtt = attendanceRepo.getTodayAttendance(employee.employeeId)
                    
                    auditRepo.log(
                        userId = employee.userId,
                        userName = employee.name,
                        action = "ATTENDANCE_SUBMITTED",
                        targetCollection = Constants.Collections.ATTENDANCE,
                        targetId = attId,
                        details = "$type submitted. Status: ${savedAtt?.status ?: "Unknown"}. Distance: ${savedAtt?.distanceFromOfficeMeters}m"
                    )
                    
                    _message.value = "Attendance submitted successfully! Status: ${savedAtt?.status}"
                }.onFailure {
                    _message.value = "Failed to submit attendance: ${it.message}"
                }

            } catch (e: Exception) {
                _message.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}

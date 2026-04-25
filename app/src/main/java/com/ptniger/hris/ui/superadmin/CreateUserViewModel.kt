package com.ptniger.hris.ui.superadmin

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ptniger.hris.data.model.Employee
import com.ptniger.hris.data.model.OfficeLocation
import com.ptniger.hris.data.model.User
import com.ptniger.hris.data.repository.AuditLogRepository
import com.ptniger.hris.data.repository.AuthRepository
import com.ptniger.hris.data.repository.EmployeeRepository
import com.ptniger.hris.data.repository.OfficeLocationRepository
import com.ptniger.hris.utils.Constants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CreateUserViewModel : ViewModel() {
    private val authRepo = AuthRepository()
    private val employeeRepo = EmployeeRepository()
    private val officeRepo = OfficeLocationRepository()
    private val auditRepo = AuditLogRepository()

    private val _officeLocations = MutableStateFlow<List<OfficeLocation>>(emptyList())
    val officeLocations: StateFlow<List<OfficeLocation>> = _officeLocations.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        loadOffices()
    }

    private fun loadOffices() {
        viewModelScope.launch {
            _officeLocations.value = officeRepo.getActiveLocations()
        }
    }

    fun createUser(
        context: Context,
        email: String,
        password: String,
        name: String,
        role: String,
        officeId: String,
        department: String,
        position: String,
        baseSalary: Double,
        superAdminId: String,
        superAdminName: String
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _message.value = null

            val user = User(
                name = name,
                email = email,
                role = role,
                officeId = officeId,
                departmentId = department
            )

            // Create Auth via Secondary App
            val result = authRepo.createUserByAdmin(context, email, password, user)
            
            result.onSuccess { newUserId ->
                // Create Employee Profile
                val employee = Employee(
                    name = name,
                    email = email,
                    role = role,
                    officeId = officeId,
                    department = department,
                    position = position,
                    baseSalary = baseSalary,
                    userId = newUserId
                )
                
                val empResult = employeeRepo.add(employee)
                empResult.onSuccess { empId ->
                    // Audit Log
                    auditRepo.log(
                        userId = superAdminId,
                        userName = superAdminName,
                        action = "CREATE_USER",
                        targetCollection = Constants.Collections.USERS,
                        targetId = newUserId,
                        details = "Created user $name with role $role at office $officeId"
                    )
                    _message.value = "User created successfully!"
                }.onFailure {
                    _message.value = "Failed to create employee profile: ${it.message}"
                }
            }.onFailure {
                _message.value = "Failed to create auth: ${it.message}"
            }
            
            _isLoading.value = false
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}

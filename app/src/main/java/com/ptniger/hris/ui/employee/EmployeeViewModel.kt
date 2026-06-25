package com.ptniger.hris.ui.employee

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ptniger.hris.data.model.Employee
import com.ptniger.hris.data.repository.EmployeeRepository
import com.ptniger.hris.data.model.OfficeLocation
import com.ptniger.hris.data.repository.OfficeLocationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EmployeeViewModel : ViewModel() {
    private val repo = EmployeeRepository()
    private val officeRepo = OfficeLocationRepository()
    private val authRepo = com.ptniger.hris.data.repository.AuthRepository()

    private val _employees = MutableStateFlow<List<Employee>>(emptyList())
    val employees: StateFlow<List<Employee>> = _employees

    private val _officeLocations = MutableStateFlow<List<OfficeLocation>>(emptyList())
    val officeLocations: StateFlow<List<OfficeLocation>> = _officeLocations.asStateFlow()

    private val _users = MutableStateFlow<List<com.ptniger.hris.data.model.User>>(emptyList())
    val users: StateFlow<List<com.ptniger.hris.data.model.User>> = _users.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    fun loadAll() { 
        viewModelScope.launch { 
            _isLoading.value = true
            _employees.value = repo.getAll()
            _officeLocations.value = officeRepo.getAll()
            _users.value = authRepo.getAllUsers()
            _isLoading.value = false 
        } 
    }

    fun save(employee: Employee, isNew: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = if (isNew) repo.add(employee) else repo.update(employee.employeeId, employee)
            result.fold(
                onSuccess = { _message.value = if (isNew) "Karyawan berhasil ditambah" else "Data berhasil diupdate"; loadAll() },
                onFailure = { _message.value = "Error: ${it.message}" }
            )
            _isLoading.value = false
        }
    }

    fun delete(id: String) {
        viewModelScope.launch {
            repo.delete(id).fold(
                onSuccess = { _message.value = "Karyawan dihapus"; loadAll() },
                onFailure = { _message.value = "Error: ${it.message}" }
            )
        }
    }

    fun clearMessage() { _message.value = null }
}

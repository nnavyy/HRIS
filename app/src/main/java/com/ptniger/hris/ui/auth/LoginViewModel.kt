package com.ptniger.hris.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ptniger.hris.data.model.User
import com.ptniger.hris.data.repository.AuthRepository
import com.ptniger.hris.utils.AutomationEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {
    private val authRepo = AuthRepository()

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState

    init { checkExistingSession() }

    private fun checkExistingSession() {
        _uiState.value = _uiState.value.copy(isRestoringSession = true)
        if (authRepo.currentUser != null) {
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isLoading = true)
                authRepo.getCurrentUserData().fold(
                    onSuccess = { user ->
                        _uiState.value = _uiState.value.copy(isLoading = false, isRestoringSession = false, loggedInUser = user)
                        // Pre-load automation rules for the session
                        viewModelScope.launch { try { AutomationEngine.refreshRules() } catch (_: Exception) {} }
                    },
                    onFailure = {
                        _uiState.value = _uiState.value.copy(isLoading = false, isRestoringSession = false)
                    }
                )
            }
        } else {
            _uiState.value = _uiState.value.copy(isRestoringSession = false)
        }
    }

    fun login(email: String, password: String, selectedRole: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            authRepo.login(email, password).fold(
                onSuccess = { user ->
                    // Validasi role: cek apakah user memiliki role yang dipilih
                    val userRoles = user.roles.ifEmpty { listOf(user.role, user.primaryRole) }
                    val hasRole = userRoles.any { it == selectedRole } ||
                        user.role == selectedRole ||
                        user.primaryRole == selectedRole

                    if (!hasRole) {
                        // Role tidak cocok — logout dan tampilkan error
                        authRepo.logout()
                        val roleName = com.ptniger.hris.utils.RoleManager.getRoleDisplayName(selectedRole)
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Akun ini tidak memiliki akses sebagai $roleName. Pilih role yang sesuai."
                        )
                        return@fold
                    }

                    _uiState.value = _uiState.value.copy(isLoading = false, loggedInUser = user)
                    // Pre-load automation rules for the session
                    viewModelScope.launch { try { AutomationEngine.refreshRules() } catch (_: Exception) {} }
                },
                onFailure = { e ->
                    // Log error code for debugging
                    val errorCode = if (e is com.google.firebase.auth.FirebaseAuthException) e.errorCode else e.javaClass.simpleName
                    android.util.Log.e("LoginViewModel", "Login error code: $errorCode | message: ${e.message}")
                    _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "Login gagal")
                }
            )
        }
    }

    fun clearLoginState() {
        authRepo.logout()
        _uiState.value = LoginUiState(isRestoringSession = false)
    }
}

data class LoginUiState(
    val isLoading: Boolean = false,
    val isRestoringSession: Boolean = false,
    val error: String? = null,
    val loggedInUser: User? = null
)

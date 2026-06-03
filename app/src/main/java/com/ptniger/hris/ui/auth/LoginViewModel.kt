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
        if (authRepo.currentUser != null) {
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isLoading = true)
                authRepo.getCurrentUserData().fold(
                    onSuccess = { user ->
                        _uiState.value = _uiState.value.copy(isLoading = false, loggedInUser = user)
                        // Pre-load automation rules for the session
                        viewModelScope.launch { try { AutomationEngine.refreshRules() } catch (_: Exception) {} }
                    },
                    onFailure = {
                        _uiState.value = _uiState.value.copy(isLoading = false)
                    }
                )
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            authRepo.login(email, password).fold(
                onSuccess = { user ->
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
        _uiState.value = LoginUiState()
    }
}

data class LoginUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val loggedInUser: User? = null
)

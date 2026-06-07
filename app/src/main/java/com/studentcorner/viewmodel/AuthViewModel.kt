package com.studentcorner.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studentcorner.data.model.User
import com.studentcorner.data.repository.FirebaseRepository
import com.studentcorner.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val user: User? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null,
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: FirebaseRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        observeAuthState()
    }

    private fun observeAuthState() {
        viewModelScope.launch {
            repository.authStateFlow.collect { isLoggedIn ->
                _uiState.value = _uiState.value.copy(isLoggedIn = isLoggedIn)
                if (isLoggedIn) loadCurrentUser()
            }
        }
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            when (val result = repository.getCurrentUser()) {
                is Result.Success -> _uiState.value = _uiState.value.copy(user = result.data)
                is Result.Error -> { /* silently ignore */ }
                is Result.Loading -> {}
            }
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = repository.signInWithEmail(email, password)) {
                is Result.Success -> _uiState.value = _uiState.value.copy(isLoading = false)
                is Result.Error -> _uiState.value = _uiState.value.copy(
                    isLoading = false, errorMessage = result.message
                )
                is Result.Loading -> {}
            }
        }
    }

    fun signUp(username: String, email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = repository.signUpWithEmail(username, email, password)) {
                is Result.Success -> _uiState.value = _uiState.value.copy(isLoading = false)
                is Result.Error -> _uiState.value = _uiState.value.copy(
                    isLoading = false, errorMessage = result.message
                )
                is Result.Loading -> {}
            }
        }
    }

    fun sendPasswordReset(email: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = repository.sendPasswordReset(email)) {
                is Result.Success -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    successMessage = "Password reset email sent. Please check your inbox."
                )
                is Result.Error -> _uiState.value = _uiState.value.copy(
                    isLoading = false, errorMessage = result.message
                )
                is Result.Loading -> {}
            }
        }
    }

    fun signOut() {
        repository.signOut()
        _uiState.value = AuthUiState()
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }

    fun updateUsername(newUsername: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            when (val result = repository.updateUsername(newUsername)) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        user = _uiState.value.user?.copy(username = newUsername),
                        successMessage = "Username updated!"
                    )
                }
                is Result.Error -> _uiState.value = _uiState.value.copy(
                    isLoading = false, errorMessage = result.message
                )
                is Result.Loading -> {}
            }
        }
    }
}

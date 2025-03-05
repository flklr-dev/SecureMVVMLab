package com.example.securemvvm.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.securemvvm.model.repository.UserRepository
import com.example.securemvvm.model.repository.UserPreferencesRepository
import com.example.securemvvm.viewmodel.utils.ValidationUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.util.Log

private const val TAG = "RegistrationViewModel"

@HiltViewModel
class RegistrationViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {
    var email by mutableStateOf("")
        private set
    
    var password by mutableStateOf("")
        private set
    
    var confirmPassword by mutableStateOf("")
        private set
    
    var showPasswordRequirements by mutableStateOf(false)
    
    // Password validation flags
    val hasMinLength get() = password.length >= 8
    val hasUppercase get() = password.any { it.isUpperCase() }
    val hasLowercase get() = password.any { it.isLowerCase() }
    val hasNumber get() = password.any { it.isDigit() }
    val hasSpecialChar get() = password.any { !it.isLetterOrDigit() }
    
    val isPasswordValid get() = hasMinLength && hasUppercase && hasLowercase && hasNumber && hasSpecialChar
    val isFormValid get() = ValidationUtils.validateEmail(email) && 
                           isPasswordValid && 
                           password == confirmPassword

    private val _registrationState = MutableStateFlow<RegistrationState>(RegistrationState.Initial)
    val registrationState: StateFlow<RegistrationState> = _registrationState.asStateFlow()

    fun updateEmail(value: String) {
        email = value
    }

    fun updatePassword(value: String) {
        password = value
    }

    fun updateConfirmPassword(value: String) {
        confirmPassword = value
    }

    fun register() {
        when {
            !ValidationUtils.validateEmail(email) -> {
                _registrationState.value = RegistrationState.Error("Invalid email format")
                Log.w(TAG, "Registration attempt failed: Invalid email format")
                return
            }
            !isPasswordValid -> {
                _registrationState.value = RegistrationState.Error(
                    "Password must meet all requirements"
                )
                Log.w(TAG, "Registration attempt failed: Invalid password format")
                return
            }
            password != confirmPassword -> {
                _registrationState.value = RegistrationState.Error("Passwords do not match")
                Log.w(TAG, "Registration attempt failed: Passwords don't match")
                return
            }
        }

        viewModelScope.launch {
            _registrationState.value = RegistrationState.Loading
            try {
                userRepository.registerUser(email, password)
                    .onSuccess {
                        _registrationState.value = RegistrationState.Success
                        Log.i(TAG, "Registration successful")
                    }
                    .onFailure { throwable ->
                        val errorMessage = when (throwable) {
                            is IllegalArgumentException -> "This email is already registered. Please use a different email."
                            else -> "Registration failed: ${getSafeErrorMessage(throwable)}"
                        }
                        _registrationState.value = RegistrationState.Error(errorMessage)
                        Log.e(TAG, errorMessage)
                    }
            } catch (e: Exception) {
                val safeErrorMessage = "An unexpected error occurred"
                _registrationState.value = RegistrationState.Error(safeErrorMessage)
                Log.e(TAG, "$safeErrorMessage: ${getSafeErrorMessage(e)}")
            }
        }
    }

    private fun getSafeErrorMessage(throwable: Throwable): String {
        return when (throwable) {
            is SecurityException -> "Security validation failed"
            is IllegalArgumentException -> "Invalid input provided"
            else -> "Unknown error occurred"
        }
    }
}

sealed interface RegistrationState {
    data object Initial : RegistrationState
    data object Loading : RegistrationState
    data object Success : RegistrationState
    data class Error(val message: String) : RegistrationState
} 
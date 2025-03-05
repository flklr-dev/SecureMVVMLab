package com.example.securemvvm.viewmodel

import android.content.Context
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.securemvvm.model.repository.UserRepository
import com.example.securemvvm.model.repository.UserPreferencesRepository
import com.example.securemvvm.model.repository.InvalidCredentialsException
import com.example.securemvvm.viewmodel.utils.ValidationUtils
import com.example.securemvvm.model.User
import com.example.securemvvm.model.security.BiometricHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.util.Log
import kotlinx.coroutines.flow.asStateFlow
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State

private const val TAG = "LoginViewModel" // For logging

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val biometricHelper: BiometricHelper
) : ViewModel() {
    private val _email = mutableStateOf("")
    val email: String
        get() = _email.value
    
    private val _password = mutableStateOf("")
    val password: String
        get() = _password.value

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Initial)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

    private val _passwordError = MutableStateFlow("")
    val passwordError: StateFlow<String> = _passwordError.asStateFlow()

    fun updateEmail(value: String) {
        _email.value = value.trim()
    }

    fun updatePassword(value: String) {
        _password.value = value
    }

    fun login(activity: FragmentActivity, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            try {
                userRepository.login(email, password)
                    .onSuccess { user ->
                        // Save user data first
                        saveUserData(user)
                        // Then proceed with biometric authentication as second factor
                        biometricHelper.showBiometricPrompt(
                            activity,
                            onSuccess = {
                                _loginState.value = LoginState.Success(user)
                                onSuccess()
                            },
                            onError = { error ->
                                _loginState.value = LoginState.Error("Fingerprint verification failed. Please try again.")
                                clearUserData() // Clear saved data if biometric auth fails
                            }
                        )
                    }
                    .onFailure { throwable ->
                        _loginState.value = LoginState.Error(getSafeErrorMessage(throwable))
                    }
            } catch (e: Exception) {
                _loginState.value = LoginState.Error(getSafeErrorMessage(e))
            }
        }
    }

    private fun saveUserData(user: User) {
        userPreferencesRepository.saveUserId(user.id)
        userPreferencesRepository.saveUserEmail(user.email)
    }

    private fun clearUserData() {
        userPreferencesRepository.clearUserPreferences()
    }

    fun canUseBiometric(context: Context): Boolean {
        return biometricHelper.canAuthenticate(context)
    }

    private fun getSafeErrorMessage(throwable: Throwable): String {
        return when (throwable) {
            is SecurityException -> "Security validation failed"
            is IllegalArgumentException -> "Invalid input provided"
            is InvalidCredentialsException -> "Invalid email or password. Please try again."
            else -> "An unexpected error occurred. Please try again later."
        }
    }

    fun validatePassword(password: String) {
        if (!isPasswordValid(password)) {
            _passwordError.value = "Password must be at least 8 characters long"
        } else {
            _passwordError.value = ""
        }
    }

    private fun isPasswordValid(password: String): Boolean {
        return password.length >= 8
    }
}

sealed interface LoginState {
    data object Initial : LoginState
    data object Loading : LoginState
    data class Success(val user: User) : LoginState
    data class Error(val message: String) : LoginState
} 
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
import com.example.securemvvm.model.security.SecureStorageManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.util.Log
import kotlinx.coroutines.flow.asStateFlow
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import com.example.securemvvm.navigation.Screen

private const val TAG = "LoginViewModel" // For logging

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val biometricHelper: BiometricHelper,
    private val secureStorageManager: SecureStorageManager
) : ViewModel() {
    private val _email = mutableStateOf(userPreferencesRepository.getLastLoggedInEmail() ?: "")
    val email: String
        get() = _email.value
    
    private val _password = mutableStateOf("")
    val password: String
        get() = _password.value

    private val _otp = mutableStateOf("")
    val otp: String
        get() = _otp.value

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Initial)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

    private val _passwordError = MutableStateFlow("")
    val passwordError: StateFlow<String> = _passwordError.asStateFlow()

    private var _hasBiometricBeenPrompted = false
    val hasBiometricBeenPrompted: Boolean
        get() = _hasBiometricBeenPrompted

    fun updateEmail(value: String) {
        _email.value = value.trim()
    }

    fun updatePassword(value: String) {
        _password.value = value
    }

    fun updateOtp(value: String) {
        _otp.value = value.trim()
    }

    fun login(activity: FragmentActivity, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            try {
                userRepository.login(email, password).onSuccess { user ->
                    // Always send OTP for additional security
                    userRepository.sendOTP(email)
                    _loginState.value = LoginState.OTPRequired
                }.onFailure { e ->
                    _loginState.value = LoginState.Error(getSafeErrorMessage(e))
                }
            } catch (e: Exception) {
                _loginState.value = LoginState.Error(getSafeErrorMessage(e))
            }
        }
    }

    fun loginWithBiometric(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            try {
                val storedPassword = secureStorageManager.getStoredPassword()
                val lastEmail = userPreferencesRepository.getLastLoggedInEmail()
                
                Log.d(TAG, "Attempting biometric login - Stored password exists: ${storedPassword != null}")
                Log.d(TAG, "Last email exists: ${lastEmail != null}")
                
                if (storedPassword != null && lastEmail != null) {
                    _email.value = lastEmail // Set the email before login
                    _password.value = storedPassword // Set the password before login - This was missing!
                    
                    userRepository.login(lastEmail, storedPassword)
                        .onSuccess { user ->
                            Log.d(TAG, "Biometric login successful")
                            saveUserData(user)
                            _loginState.value = LoginState.Success(user)
                            onSuccess()
                        }.onFailure { e ->
                            Log.e(TAG, "Biometric login failed", e)
                            // Clear stored credentials on failure
                            secureStorageManager.clearStoredPassword()
                            userPreferencesRepository.clearUserPreferences()
                            _loginState.value = LoginState.Error("Biometric authentication failed. Please login with password.")
                        }
                } else {
                    Log.d(TAG, "No stored credentials found")
                    _loginState.value = LoginState.Error(
                        if (storedPassword == null && lastEmail == null) 
                            "Please login with password first to enable biometric login"
                        else if (storedPassword == null)
                            "No stored password found. Please login with password"
                        else
                            "No stored email found. Please login with password"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception during biometric login", e)
                _loginState.value = LoginState.Error("An error occurred during biometric authentication. Please try again.")
            }
        }
    }

    fun loginWithPassword() {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            try {
                userRepository.login(email, password).onSuccess { user ->
                    saveUserData(user)
                    _loginState.value = LoginState.Success(user)
                }.onFailure { e ->
                    _loginState.value = LoginState.Error(getSafeErrorMessage(e))
                }
            } catch (e: Exception) {
                _loginState.value = LoginState.Error(getSafeErrorMessage(e))
            }
        }
    }

    private fun saveUserData(user: User) {
        userPreferencesRepository.saveUserId(user.id)
        userPreferencesRepository.saveUserEmail(email)
        // Store the password for biometric login
        secureStorageManager.storePassword(_password.value)
    }

    fun logout() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Starting logout process")
                // Clear all stored data
                userPreferencesRepository.clearUserPreferences()
                secureStorageManager.clearStoredPassword()
                
                // Reset view model state
                _email.value = ""
                _password.value = ""
                _loginState.value = LoginState.Initial
                
                Log.d(TAG, "Logout completed successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error during logout", e)
                _loginState.value = LoginState.Error("Error during logout")
            }
        }
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
        if (!ValidationUtils.validatePassword(password)) {
            _passwordError.value = "Password must be at least 8 characters long and meet complexity requirements."
        } else {
            _passwordError.value = ""
        }
    }

    fun requestOTP() {
        viewModelScope.launch {
            userRepository.sendOTP(email).onSuccess {
                // Handle success (e.g., show a message)
            }.onFailure { throwable ->
                // Handle failure (e.g., show an error message)
            }
        }
    }

    fun verifyOTP(inputOtp: String): Boolean {
        return try {
            val isValid = userRepository.verifyOTP(inputOtp)
            if (isValid) {
                viewModelScope.launch {
                    userRepository.login(email, password).onSuccess { user ->
                        saveUserData(user)
                        _loginState.value = LoginState.Success(user)
                    }
                }
                true
            } else {
                _loginState.value = LoginState.Error("Invalid verification code. Please try again.")
                false
            }
        } catch (e: Exception) {
            _loginState.value = LoginState.Error("Error verifying code. Please try again.")
            false
        }
    }

    fun hasStoredCredentials(): Boolean {
        return secureStorageManager.getStoredPassword() != null && userPreferencesRepository.getLastLoggedInEmail() != null
    }

    fun clearStoredCredentials() {
        secureStorageManager.clearStoredPassword()
        userPreferencesRepository.clearUserPreferences()
        _email.value = ""
        _password.value = ""
    }

    fun quickLogin(activity: FragmentActivity, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            try {
                userRepository.login(email, password)
                    .onSuccess { user ->
                        saveUserData(user)
                        _loginState.value = LoginState.Success(user)
                        onSuccess()
                    }.onFailure { e ->
                        _loginState.value = LoginState.Error(getSafeErrorMessage(e))
                    }
            } catch (e: Exception) {
                _loginState.value = LoginState.Error(getSafeErrorMessage(e))
            }
        }
    }

    fun setBiometricPrompted() {
        _hasBiometricBeenPrompted = true
    }
}

sealed interface LoginState {
    data object Initial : LoginState
    data object Loading : LoginState
    data class Success(val user: User) : LoginState
    data class Error(val message: String) : LoginState
    data object OTPRequired : LoginState
} 
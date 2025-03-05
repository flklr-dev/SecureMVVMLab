package com.example.securemvvm.model.repository

import com.example.securemvvm.model.security.SecureStorageManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferencesRepository @Inject constructor(
    private val secureStorageManager: SecureStorageManager
) {
    companion object {
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_SETTINGS = "user_settings"
    }

    fun saveUserId(userId: String) {
        secureStorageManager.saveSecureString(KEY_USER_ID, userId)
    }

    fun getUserId(): String? {
        return secureStorageManager.getSecureString(KEY_USER_ID)
    }

    fun saveUserEmail(email: String) {
        secureStorageManager.saveSecureString(KEY_USER_EMAIL, email)
    }

    fun getUserEmail(): String? {
        return secureStorageManager.getSecureString(KEY_USER_EMAIL)
    }

    fun saveUserSettings(settings: String) {
        secureStorageManager.saveSecureString(KEY_USER_SETTINGS, settings)
    }

    fun getUserSettings(): String? {
        return secureStorageManager.getSecureString(KEY_USER_SETTINGS)
    }

    fun clearUserPreferences() {
        listOf(KEY_USER_ID, KEY_USER_EMAIL, KEY_USER_SETTINGS).forEach { key ->
            secureStorageManager.getSecureString(key)?.let {
                secureStorageManager.saveSecureString(key, "")
            }
        }
    }
} 
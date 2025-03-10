package com.example.securemvvm.model.repository

import com.example.securemvvm.model.User
import com.example.securemvvm.model.security.SecureStorageManager
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Log

@Singleton
class UserPreferencesRepository @Inject constructor(
    private val secureStorageManager: SecureStorageManager
) {
    companion object {
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_SETTINGS = "user_settings"
        private const val KEY_LAST_LOGGED_IN_EMAIL = "last_logged_in_email"
        private const val TAG = "UserPreferencesRepo"
    }

    fun saveUserId(userId: String) {
        secureStorageManager.saveSecureString(KEY_USER_ID, userId)
    }

    fun getUserId(): String? {
        return secureStorageManager.getSecureString(KEY_USER_ID)
    }

    fun saveUserEmail(email: String) {
        secureStorageManager.saveSecureString(KEY_USER_EMAIL, email)
        // Also save as last logged in email
        saveLastLoggedInEmail(email)
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
        Log.d(TAG, "Clearing user preferences")
        listOf(KEY_USER_ID, KEY_USER_EMAIL, KEY_USER_SETTINGS).forEach { key ->
            secureStorageManager.getSecureString(key)?.let {
                secureStorageManager.saveSecureString(key, "")
            }
        }
        // Don't clear last logged in email when clearing preferences
    }

    fun getUserByToken(token: String): User? {
        val userId = secureStorageManager.getSecureString("user_id")
        return if (userId != null) {
            val userEmail = secureStorageManager.getSecureString("user_email")
            if (userEmail != null) {
                User(
                    id = userId,
                    email = userEmail,
                    username = userEmail.substringBefore("@"),
                    authToken = token
                )
            } else {
                null
            }
        } else {
            null
        }
    }

    fun saveLastLoggedInEmail(email: String) {
        Log.d(TAG, "Saving last logged in email: $email")
        secureStorageManager.saveSecureString(KEY_LAST_LOGGED_IN_EMAIL, email)
    }

    fun getLastLoggedInEmail(): String? {
        val email = secureStorageManager.getSecureString(KEY_LAST_LOGGED_IN_EMAIL)
        Log.d(TAG, "Retrieved last logged in email: $email")
        return email
    }
} 
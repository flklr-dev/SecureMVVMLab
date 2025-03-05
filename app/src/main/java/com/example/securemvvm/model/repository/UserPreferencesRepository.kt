package com.example.securemvvm.model.repository

import com.example.securemvvm.model.User
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

    fun getUserByToken(token: String): User? {
        // Implement logic to retrieve user based on the token
        // For example, you might want to query the database or shared preferences
        // This is a placeholder; you need to implement the actual retrieval logic

        // Example: Retrieve user data from shared preferences or database
        val userId = secureStorageManager.getSecureString("user_id") // Assuming you store user ID in secure storage
        return if (userId != null) {
            // Query the database to get the user details
            // Replace with actual database query logic
            val user = queryUserFromDatabase(userId)
            user
        } else {
            null
        }
    }

    // Placeholder function for querying user from the database
    private fun queryUserFromDatabase(userId: String): User? {
        // Implement your database query logic here
        return null // Replace with actual user retrieval logic
    }
} 
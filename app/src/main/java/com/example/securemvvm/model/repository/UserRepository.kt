package com.example.securemvvm.model.repository

import com.example.securemvvm.model.User
import com.example.securemvvm.network.ApiService
import com.example.securemvvm.model.security.SecureStorageManager
import com.example.securemvvm.model.database.EncryptedDatabaseManager
import com.example.securemvvm.model.security.TwoFactorAuthManager
import javax.inject.Inject
import javax.inject.Singleton
import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.content.ContentValues
import android.util.Log
import java.util.UUID
import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

@Singleton
class UserRepository @Inject constructor(
    private val apiService: ApiService,
    private val secureStorageManager: SecureStorageManager,
    private val databaseManager: EncryptedDatabaseManager,
    private val twoFactorAuthManager: TwoFactorAuthManager,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val context: Context
) {
    companion object {
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val TAG = "UserRepository"
    }

    private val sharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            "secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    suspend fun login(email: String, password: String): Result<User> = withContext(Dispatchers.IO) {
        try {
            val passwordHash = hashPassword(password)
            Log.d(TAG, "Login attempt - Email: $email, Hash: $passwordHash")
            
            val db = databaseManager.getReadableEncryptedDatabase()
            
            db.use { database ->
                val checkUserCursor = database.rawQuery(
                    "SELECT password_hash FROM users WHERE email = ?",
                    arrayOf(email)
                )
                
                if (checkUserCursor.moveToFirst()) {
                    val storedHash = checkUserCursor.getString(0)
                    Log.d(TAG, "Found user - Stored hash: $storedHash, Provided hash: $passwordHash")
                    checkUserCursor.close()
                } else {
                    Log.d(TAG, "No user found with email: $email")
                    checkUserCursor.close()
                    return@withContext Result.failure(Exception("Invalid email or password"))
                }

                val cursor = database.rawQuery(
                    """
                    SELECT id, email 
                    FROM users 
                    WHERE email = ? AND password_hash = ?
                    """,
                    arrayOf(email, passwordHash)
                )
                
                if (cursor.moveToFirst()) {
                    val userId = cursor.getString(cursor.getColumnIndexOrThrow("id"))
                    val userEmail = cursor.getString(cursor.getColumnIndexOrThrow("email"))
                    cursor.close()
                    
                    // Generate auth token
                    val authToken = UUID.randomUUID().toString()
                    secureStorageManager.saveSecureString(KEY_AUTH_TOKEN, authToken)
                    
                    // Save user preferences
                    userPreferencesRepository.saveUserId(userId)
                    userPreferencesRepository.saveUserEmail(email)
                    
                    val user = User(
                        id = userId,
                        email = userEmail,
                        username = email.substringBefore("@"),
                        authToken = authToken
                    )
                    
                    Log.d(TAG, "Login successful for user: $email")
                    Result.success(user)
                } else {
                    cursor.close()
                    Log.w(TAG, "Login failed: Invalid credentials for user: $email")
                    Result.failure(Exception("Invalid email or password. Please check your credentials and try again."))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Login failed with exception", e)
            Result.failure(Exception("An error occurred during login. Please try again."))
        }
    }

    fun getStoredAuthToken(): String? {
        return secureStorageManager.getSecureString(KEY_AUTH_TOKEN)
    }

    fun clearAuthData() {
        secureStorageManager.getSecureString(KEY_AUTH_TOKEN)?.let {
            secureStorageManager.saveSecureString(KEY_AUTH_TOKEN, "")
        }
    }

    suspend fun registerUser(email: String, password: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val passwordHash = hashPassword(password)
            Log.d(TAG, "Attempting to register user - Email: $email, Hash: $passwordHash")
            
            val db = databaseManager.getWritableEncryptedDatabase()
            
            db.use { database ->
                // Check if user already exists
                val cursor = database.rawQuery(
                    "SELECT COUNT(*) FROM users WHERE email = ?",
                    arrayOf(email)
                )
                
                cursor.use {
                    it.moveToFirst()
                    if (it.getInt(0) > 0) {
                        Log.d(TAG, "User already exists with email: $email")
                        return@withContext Result.failure(IllegalArgumentException("Email already registered"))
                    }
                }

                // Generate user ID and 2FA secret
                val userId = UUID.randomUUID().toString()
                val twoFactorSecret = twoFactorAuthManager.generateSecretKey()
                
                // Insert new user
                val values = ContentValues().apply {
                    put("id", userId)
                    put("email", email)
                    put("password_hash", passwordHash)
                    put("two_factor_secret", twoFactorSecret)
                    put("created_at", System.currentTimeMillis())
                    put("updated_at", System.currentTimeMillis())
                }
                
                val newRowId = database.insert("users", null, values)
                
                if (newRowId == -1L) {
                    Log.e(TAG, "Failed to insert new user into database")
                    return@withContext Result.failure(Exception("Failed to create user"))
                }
                
                // Verify the user was created
                val verificationCursor = database.rawQuery(
                    "SELECT * FROM users WHERE id = ?",
                    arrayOf(userId)
                )
                
                if (verificationCursor.moveToFirst()) {
                    Log.d(TAG, "Successfully created user with ID: $userId")
                    verificationCursor.close()
                } else {
                    Log.e(TAG, "User verification failed after insert")
                    verificationCursor.close()
                    return@withContext Result.failure(Exception("Failed to verify user creation"))
                }
                
                // Store user preferences
                userPreferencesRepository.saveUserId(userId)
                userPreferencesRepository.saveUserEmail(email)
                
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Registration failed", e)
            Result.failure(e)
        }
    }

    suspend fun loginUser(email: String, password: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val passwordHash = hashPassword(password)
            val db = databaseManager.getReadableEncryptedDatabase()
            
            db.use { database ->
                val cursor = database.rawQuery(
                    "SELECT COUNT(*) FROM users WHERE email = ? AND password_hash = ?",
                    arrayOf(email, passwordHash)
                )
                cursor.use {
                    it.moveToFirst()
                    val exists = it.getInt(0) > 0
                    Result.success(exists)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun hashPassword(password: String): String {
        // Use a more reliable hashing method
        return try {
            val md = java.security.MessageDigest.getInstance("SHA-256")
            val hash = md.digest(password.toByteArray(Charsets.UTF_8))
            hash.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error hashing password", e)
            // Fallback to simple hash if something goes wrong
            password.hashCode().toString()
        }
    }

    fun verifyLogin(email: String, passwordHash: String, totpCode: String): Boolean {
        val db = databaseManager.getReadableEncryptedDatabase()
        return try {
            val cursor = db.rawQuery(
                "SELECT two_factor_secret FROM users WHERE email = ? AND password_hash = ?",
                arrayOf(email, passwordHash)
            )
            
            if (cursor.moveToFirst()) {
                val secret = cursor.getString(0)
                cursor.close()
                // Verify TOTP code
                twoFactorAuthManager.verifyTOTP(secret, totpCode)
            } else {
                cursor.close()
                false
            }
        } catch (e: Exception) {
            false
        } finally {
            db.close()
        }
    }

    fun getTwoFactorQRCode(email: String): Bitmap? {
        val db = databaseManager.getReadableEncryptedDatabase()
        return try {
            val cursor = db.rawQuery(
                "SELECT username, two_factor_secret FROM users WHERE email = ?",
                arrayOf(email)
            )
            
            if (cursor.moveToFirst()) {
                val username = cursor.getString(0)
                val secret = cursor.getString(1)
                cursor.close()
                twoFactorAuthManager.generateQRCodeBitmap(
                    secret,
                    username,
                    "SecureMVVM"
                )
            } else {
                cursor.close()
                null
            }
        } catch (e: Exception) {
            null
        } finally {
            db.close()
        }
    }

    // Add this debug function to check database contents
    fun debugPrintUsers() {
        val db = databaseManager.getReadableEncryptedDatabase()
        try {
            db.use { database ->
                val cursor = database.rawQuery("SELECT id, email, password_hash FROM users", null)
                if (cursor.moveToFirst()) {
                    do {
                        val id = cursor.getString(cursor.getColumnIndexOrThrow("id"))
                        val email = cursor.getString(cursor.getColumnIndexOrThrow("email"))
                        val hash = cursor.getString(cursor.getColumnIndexOrThrow("password_hash"))
                        Log.d(TAG, "Stored user: ID=$id, Email=$email, Hash=$hash")
                    } while (cursor.moveToNext())
                } else {
                    Log.d(TAG, "No users found in database")
                }
                cursor.close()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading users", e)
        }
    }

    fun saveSessionToken(token: String) {
        sharedPreferences.edit().putString("session_token", token).apply()
    }

    fun getSessionToken(): String? {
        return sharedPreferences.getString("session_token", null)
    }

    fun clearSessionToken() {
        sharedPreferences.edit().remove("session_token").apply()
    }
} 
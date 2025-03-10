package com.example.securemvvm.model.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Log

@Singleton
class SecureStorageManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val securePreferences = EncryptedSharedPreferences.create(
        context,
        "secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    private val keyAlias = "SecureStorageKey"
    private val sharedPrefs = context.getSharedPreferences("SecureStorage", Context.MODE_PRIVATE)

    init {
        if (!keyStore.containsAlias(keyAlias)) {
            createKey()
        }
    }

    private fun createKey() {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            "AndroidKeyStore"
        )
        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .build()

        keyGenerator.init(keyGenParameterSpec)
        keyGenerator.generateKey()
    }

    fun saveSecureString(key: String, value: String) {
        securePreferences.edit().putString(key, value).apply()
    }

    fun getSecureString(key: String): String? {
        return securePreferences.getString(key, null)
    }

    fun clearSecureStorage() {
        securePreferences.edit().clear().apply()
    }

    fun storePassword(password: String) {
        try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val secretKey = keyStore.getKey(keyAlias, null) as SecretKey
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)

            val encrypted = cipher.doFinal(password.toByteArray())
            val iv = cipher.iv

            // Store both the IV and encrypted data
            sharedPrefs.edit()
                .putString("stored_password", Base64.encodeToString(encrypted, Base64.DEFAULT))
                .putString("stored_password_iv", Base64.encodeToString(iv, Base64.DEFAULT))
                .apply()
            
            Log.d("SecureStorageManager", "Password stored successfully")
        } catch (e: Exception) {
            Log.e("SecureStorageManager", "Error storing password", e)
        }
    }

    fun getStoredPassword(): String? {
        val encryptedPassword = sharedPrefs.getString("stored_password", null)
        val iv = sharedPrefs.getString("stored_password_iv", null)
        
        if (encryptedPassword == null || iv == null) {
            Log.d("SecureStorageManager", "No stored password found")
            return null
        }

        try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val secretKey = keyStore.getKey(keyAlias, null) as SecretKey
            val ivSpec = GCMParameterSpec(128, Base64.decode(iv, Base64.DEFAULT))
            
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)
            val decrypted = cipher.doFinal(Base64.decode(encryptedPassword, Base64.DEFAULT))
            
            val password = String(decrypted)
            Log.d("SecureStorageManager", "Password retrieved successfully")
            return password
        } catch (e: Exception) {
            Log.e("SecureStorageManager", "Error retrieving password", e)
            return null
        }
    }

    fun clearStoredPassword() {
        sharedPrefs.edit()
            .remove("stored_password")
            .remove("stored_password_iv")
            .apply()
    }
} 
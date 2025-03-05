package com.example.securemvvm.model.security

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SecureStorageManagerTest {

    private lateinit var secureStorageManager: SecureStorageManager

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        secureStorageManager = SecureStorageManager(context)
        // Clear any existing data before each test
        secureStorageManager.clearSecureStorage()
    }

    @Test
    fun saveAndRetrieveSecureString() {
        // Given
        val key = "test_key"
        val value = "sensitive_data"

        // When
        secureStorageManager.saveSecureString(key, value)
        val retrievedValue = secureStorageManager.getSecureString(key)

        // Then
        assertEquals(value, retrievedValue)
    }

    @Test
    fun getNonExistentKey_returnsNull() {
        // When
        val result = secureStorageManager.getSecureString("non_existent_key")

        // Then
        assertNull(result)
    }

    @Test
    fun clearStorage_removesAllData() {
        // Given
        val key1 = "key1"
        val key2 = "key2"
        secureStorageManager.saveSecureString(key1, "value1")
        secureStorageManager.saveSecureString(key2, "value2")

        // When
        secureStorageManager.clearSecureStorage()

        // Then
        assertNull(secureStorageManager.getSecureString(key1))
        assertNull(secureStorageManager.getSecureString(key2))
    }

    @Test
    fun overwriteExistingValue() {
        // Given
        val key = "test_key"
        val initialValue = "initial_value"
        val newValue = "new_value"

        // When
        secureStorageManager.saveSecureString(key, initialValue)
        secureStorageManager.saveSecureString(key, newValue)
        val retrievedValue = secureStorageManager.getSecureString(key)

        // Then
        assertEquals(newValue, retrievedValue)
    }

    @Test
    fun saveEmptyString() {
        // Given
        val key = "empty_key"
        val value = ""

        // When
        secureStorageManager.saveSecureString(key, value)
        val retrievedValue = secureStorageManager.getSecureString(key)

        // Then
        assertEquals(value, retrievedValue)
    }
} 
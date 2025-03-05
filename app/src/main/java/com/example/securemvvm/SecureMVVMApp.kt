package com.example.securemvvm

import android.app.Application
import android.util.Log
import com.example.securemvvm.model.database.EncryptedDatabaseManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class SecureMVVMApp : Application() {
    @Inject
    lateinit var databaseManager: EncryptedDatabaseManager

    override fun onCreate() {
        super.onCreate()
        try {
            databaseManager.verifyDatabaseSetup()
            Log.d("SecureMVVMApp", "Database setup verified successfully")
        } catch (e: Exception) {
            Log.e("SecureMVVMApp", "Failed to verify database setup", e)
        }
    }
} 
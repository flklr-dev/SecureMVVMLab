package com.example.securemvvm.model.database

import android.content.Context
import android.util.Log
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SQLiteOpenHelper
import javax.inject.Inject
import javax.inject.Named

class EncryptedDatabaseManager @Inject constructor(
    context: Context,
    @Named("database_encryption_key") private val encryptionKey: String
) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    init {
        SQLiteDatabase.loadLibs(context)
    }

    override fun onCreate(db: SQLiteDatabase) {
        try {
            // Create users table
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS users (
                    id TEXT PRIMARY KEY,
                    email TEXT NOT NULL UNIQUE,
                    password_hash TEXT NOT NULL,
                    two_factor_secret TEXT,
                    created_at INTEGER NOT NULL,
                    updated_at INTEGER NOT NULL
                )
            """)
            Log.d("EncryptedDatabaseManager", "Database and users table created successfully")
        } catch (e: Exception) {
            Log.e("EncryptedDatabaseManager", "Error creating database", e)
            throw e
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        try {
            // Handle database upgrades
            db.execSQL("DROP TABLE IF EXISTS users")
            onCreate(db)
            Log.d("EncryptedDatabaseManager", "Database upgraded successfully")
        } catch (e: Exception) {
            Log.e("EncryptedDatabaseManager", "Error upgrading database", e)
            throw e
        }
    }

    fun getWritableEncryptedDatabase(): SQLiteDatabase {
        return super.getWritableDatabase(encryptionKey)
    }

    fun getReadableEncryptedDatabase(): SQLiteDatabase {
        return super.getReadableDatabase(encryptionKey)
    }

    fun verifyDatabaseSetup() {
        val db = getReadableEncryptedDatabase()
        try {
            db.use { database ->
                // Check if users table exists
                val cursor = database.rawQuery(
                    "SELECT name FROM sqlite_master WHERE type='table' AND name='users'",
                    null
                )
                val tableExists = cursor.moveToFirst()
                cursor.close()
                
                if (!tableExists) {
                    Log.e("EncryptedDatabaseManager", "Users table does not exist!")
                    // Recreate the table
                    onCreate(database)
                } else {
                    Log.d("EncryptedDatabaseManager", "Database verification successful")
                }
            }
        } catch (e: Exception) {
            Log.e("EncryptedDatabaseManager", "Database verification failed", e)
            throw e
        }
    }

    companion object {
        private const val DATABASE_NAME = "secure_database.db"
        private const val DATABASE_VERSION = 1
    }
} 
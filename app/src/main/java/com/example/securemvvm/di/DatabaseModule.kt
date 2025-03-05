package com.example.securemvvm.di

import android.content.Context
import com.example.securemvvm.model.database.EncryptedDatabaseManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Named("database_encryption_key")
    fun provideDatabaseEncryptionKey(): String {
        // In a real app, this should be securely generated and stored
        return "your_secure_encryption_key_here"
    }

    @Provides
    @Singleton
    fun provideEncryptedDatabaseManager(
        @ApplicationContext context: Context,
        @Named("database_encryption_key") encryptionKey: String
    ): EncryptedDatabaseManager {
        return EncryptedDatabaseManager(context, encryptionKey)
    }
} 
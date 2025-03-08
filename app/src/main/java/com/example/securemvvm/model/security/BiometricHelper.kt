package com.example.securemvvm.model.security

import android.content.Context
import android.util.Log
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import javax.inject.Inject

class BiometricHelper @Inject constructor() {

    fun canAuthenticate(context: Context): Boolean {
        val biometricManager = BiometricManager.from(context)
        return biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == 
            BiometricManager.BIOMETRIC_SUCCESS
    }

    fun showBiometricPrompt(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                Log.d("BiometricHelper", "Authentication succeeded")
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                Log.d("BiometricHelper", "Authentication error code: $errorCode, message: $errString")
                when (errorCode) {
                    BiometricPrompt.ERROR_NO_BIOMETRICS -> 
                        onError("No biometric features enrolled on this device")
                    BiometricPrompt.ERROR_HW_UNAVAILABLE -> 
                        onError("Biometric features are currently unavailable")
                    BiometricPrompt.ERROR_LOCKOUT -> 
                        onError("Too many attempts. Please try again later")
                    else -> onError(errString.toString())
                }
            }

            override fun onAuthenticationFailed() {
                Log.d("BiometricHelper", "Authentication failed")
                onError("Biometric authentication failed")
            }
        }

        val biometricPrompt = BiometricPrompt(activity, executor, callback)
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric login")
            .setSubtitle("Log in using your biometric credential")
            .setNegativeButtonText("Cancel")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
} 
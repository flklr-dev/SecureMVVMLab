package com.example.securemvvm.viewmodel.utils

object ValidationUtils {
    private const val MIN_PASSWORD_LENGTH = 8
    private val EMAIL_REGEX = Regex("[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+")

    fun validateEmail(email: String): Boolean {
        return email.matches(EMAIL_REGEX)
    }

    fun validatePassword(password: String): Boolean {
        return password.length >= MIN_PASSWORD_LENGTH &&
                password.any { it.isDigit() } &&
                password.any { it.isUpperCase() } &&
                password.any { it.isLowerCase() } &&
                password.any { !it.isLetterOrDigit() }
    }
} 
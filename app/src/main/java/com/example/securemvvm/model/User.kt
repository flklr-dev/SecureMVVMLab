package com.example.securemvvm.model

data class User(
    val id: String,
    val username: String,
    val email: String,
    // Note: Never store passwords in plain text
    val authToken: String? = null
) 
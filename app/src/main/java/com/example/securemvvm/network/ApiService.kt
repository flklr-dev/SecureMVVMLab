package com.example.securemvvm.network

import com.example.securemvvm.model.User
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("login")
    suspend fun login(
        @Body credentials: Map<String, String>
    ): Response<User>

    @POST("register")
    suspend fun register(
        @Body userData: Map<String, String>
    ): Response<User>
} 
package com.example

import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("api/auth/token")
    suspend fun login(
        @Body request: LoginRequest
    ): LoginResponse
}

data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val accessToken: String,
    val email: String
)

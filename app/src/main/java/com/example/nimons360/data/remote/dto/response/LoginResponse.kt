package com.example.nimons360.data.remote.dto.response

data class LoginResponse(
    val token: String,
    val expiresAt: String,
    val user: UserResponse
)

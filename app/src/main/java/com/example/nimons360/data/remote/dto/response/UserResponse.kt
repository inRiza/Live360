package com.example.nimons360.data.remote.dto.response

data class UserResponse(
    val id: Int,
    val nim: String,
    val email: String,
    val fullName: String,
    val createdAt: String,
    val updatedAt: String
)

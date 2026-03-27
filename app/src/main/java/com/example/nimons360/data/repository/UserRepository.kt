package com.example.nimons360.data.repository

import com.example.nimons360.data.remote.api.ApiService
import com.example.nimons360.data.remote.dto.response.UserResponse
import com.example.nimons360.utils.Result
import javax.inject.Inject

class UserRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun getProfile(): Result<UserResponse> = Result.Error("not implemented")
    suspend fun updateProfile(fullName: String): Result<UserResponse> = Result.Error("not implemented")
}

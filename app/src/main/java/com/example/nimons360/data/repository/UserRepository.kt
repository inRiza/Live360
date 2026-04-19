package com.example.nimons360.data.repository

import com.example.nimons360.data.remote.api.ApiService
import com.example.nimons360.data.remote.dto.common.UserData
import com.example.nimons360.data.remote.dto.request.UpdateProfileRequest
import com.example.nimons360.utils.Result
import javax.inject.Inject

class UserRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun getProfile(): Result<UserData> {
        return try {
            val res = apiService.getProfile()
            if (res.isSuccessful) {
                val body = res.body()?.data
                if (body != null) Result.Success(
                    UserData(
                        id = body.id,
                        nim = body.nim,
                        email = body.email,
                        fullName = body.fullName
                    )
                )
                else Result.Error("User Profile Empty response")
            } else {
                Result.Error("Failed: ${res.code()}")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Unknown error")
        }
    }

    suspend fun updateProfile(fullName: String): Result<UserData> {
        return try {
            val res = apiService.updateProfile(UpdateProfileRequest(fullName = fullName))

            if (res.isSuccessful) {
                val body = res.body()?.data
                if (body != null) Result.Success(
                    UserData(
                        id = body.id,
                        nim = body.nim,
                        email = body.email,
                        fullName = body.fullName
                    )
                )
                else Result.Error("Update Profile Empty response")
            } else {
                Result.Error("Failed: ${res.code()}")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Unknown error")
        }
    }
}

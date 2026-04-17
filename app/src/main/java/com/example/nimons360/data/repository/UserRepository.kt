package com.example.nimons360.data.repository

import com.example.nimons360.data.remote.api.ApiService
import com.example.nimons360.data.remote.dto.common.UserData
import com.example.nimons360.data.remote.dto.response.ApiMeGet200Response
import com.example.nimons360.utils.Result
import javax.inject.Inject

class UserRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun getProfile(): Result<UserData> = Result.Error("not implemented")
    suspend fun updateProfile(fullName: String): Result<UserData> = Result.Error("not implemented")
}

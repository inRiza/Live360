package com.example.nimons360.data.remote.api.auth

import com.example.nimons360.data.remote.dto.request.LoginRequest
import com.example.nimons360.data.remote.dto.response.BaseResponse
import com.example.nimons360.data.remote.dto.response.LoginResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApiService {
    @POST("api/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<BaseResponse<LoginResponse>>
}
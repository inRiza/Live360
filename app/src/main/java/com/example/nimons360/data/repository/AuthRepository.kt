package com.example.nimons360.data.repository

import com.example.nimons360.data.local.preference.TokenPreference
import com.example.nimons360.data.remote.api.ApiService
import com.example.nimons360.data.remote.dto.response.LoginResponse
import com.example.nimons360.utils.Result
import javax.inject.Inject

class AuthRepository @Inject constructor(
    private val apiService: ApiService,
    private val tokenPreference: TokenPreference
) {
    suspend fun login(email: String, password: String): Result<LoginResponse> =
        Result.Error("not implemented")

    fun logout() {}

    fun isLoggedIn(): Boolean = false
}

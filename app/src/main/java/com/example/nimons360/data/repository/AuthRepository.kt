package com.example.nimons360.data.repository

import com.example.nimons360.data.local.preference.TokenPreference
import com.example.nimons360.data.remote.api.ApiService
import com.example.nimons360.data.remote.dto.request.LoginRequest
import com.example.nimons360.data.remote.dto.response.LoginResponse
import com.example.nimons360.utils.Result
import javax.inject.Inject

class AuthRepository @Inject constructor(
    private val apiService: ApiService,
    private val tokenPreference: TokenPreference
) {
    suspend fun login(email: String, password: String): Result<LoginResponse> {
        return try {
            val request = LoginRequest(email, password)
            val response = apiService.login(request)

            // Check Response
            if (response.isSuccessful) {
                val baseResponse = response.body()
                val loginData = baseResponse?.data

                // Success
                if (loginData != null) {
                    tokenPreference.saveToken(loginData.token)
                    Result.Success(loginData)
                    // Failed
                } else {
                    Result.Error("Data response kosong")
                }

            } else {
                // Error
                val errorBodyString = response.errorBody()?.string()
                var errorMessage = "Login gagal: Kode ${response.code()}"

                if (!errorBodyString.isNullOrEmpty()) {
                    try {
                        val jsonObject = org.json.JSONObject(errorBodyString)
                        val errorObject = jsonObject.getJSONObject("error")
                        val serverMessage = errorObject.getString("message")
                        errorMessage = serverMessage
                    } catch (e: Exception) {
                        errorMessage = "Terjadi kesalahan: $errorBodyString"
                    }
                }

                Result.Error(errorMessage)
            }
        } catch (e: Exception) {
            Result.Error("Terjadi kesalahan jaringan: ${e.localizedMessage}")
        }
    }

    fun logout() {
        tokenPreference.clear()
    }

    fun isLoggedIn(): Boolean {
        val token = tokenPreference.getToken()
        return ! token.isNullOrEmpty()
    }
}

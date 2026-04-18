package com.example.nimons360.utils

import android.content.Context
import android.content.Intent
import com.example.nimons360.data.local.preference.TokenPreference
import com.example.nimons360.ui.auth.login.LoginActivity
import okhttp3.Interceptor
import okhttp3.Response

class TokenExpiredInterceptor(
    private val tokenPreference: TokenPreference,
    private val context: Context
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val requestBuilder = originalRequest.newBuilder()

        // Bearer Auth
        val token = tokenPreference.getToken()
        if (!token.isNullOrEmpty()) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        val request = requestBuilder.build()
        val response = chain.proceed(request)

        // Tangkap status 409
        if (response.code == 409) {
            tokenPreference.clear()

            // Redirect ke LoginActivity
            val intent = Intent(context, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            context.startActivity(intent)
        }

        return response
    }
}
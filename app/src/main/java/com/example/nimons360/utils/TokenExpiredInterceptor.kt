package com.example.nimons360.utils

import com.example.nimons360.data.local.preference.TokenPreference
import okhttp3.Interceptor
import okhttp3.Response

class TokenExpiredInterceptor(
    private val tokenPreference: TokenPreference
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        if (response.code != 401) return response

        val path = request.url.encodedPath
        val isLoginEndpoint = path.endsWith("/api/login")
        val hadAuthHeader = !request.header("Authorization").isNullOrBlank()
        val hasSavedToken = !tokenPreference.getToken().isNullOrBlank()

        if (!isLoginEndpoint && (hadAuthHeader || hasSavedToken)) {
            tokenPreference.clear()
        }

        return response
    }
}

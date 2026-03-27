package com.example.nimons360.utils

import com.example.nimons360.data.local.preference.TokenPreference
import okhttp3.Interceptor

class TokenExpiredInterceptor(
    private val tokenPreference: TokenPreference
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): okhttp3.Response = chain.proceed(chain.request())
}

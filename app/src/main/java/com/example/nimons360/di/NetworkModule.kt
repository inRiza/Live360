package com.example.nimons360.di

import android.util.Log
import com.example.nimons360.BuildConfig
import com.example.nimons360.data.local.preference.TokenPreference
import com.example.nimons360.data.remote.api.ApiService
import com.example.nimons360.utils.TokenExpiredInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideOkHttpClient(
        tokenPreference: TokenPreference
    ): OkHttpClient {
        val builder = OkHttpClient.Builder()

        builder.addInterceptor { chain ->
            val original = chain.request()
            val token = tokenPreference.getToken()
            val isLoginEndpoint = original.url.encodedPath.endsWith("/api/login")

            val requestBuilder = original.newBuilder()
                .header("Accept", "application/json")

            if (!token.isNullOrBlank() && !isLoginEndpoint && original.header("Authorization") == null) {
                requestBuilder.header("Authorization", "Bearer $token")
            }

            val request = requestBuilder.build()

            chain.proceed(request)
        }

        builder.addInterceptor(TokenExpiredInterceptor(tokenPreference))

        if (BuildConfig.DEBUG) {
            val logging = HttpLoggingInterceptor { message ->
                Log.d("Nimons360Http", message)
            }.apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            builder.addInterceptor(logging)
        }

        return builder.build()
    }

    @Provides
    @Singleton
    fun provideApiService(
        okHttpClient: OkHttpClient
    ): ApiService = Retrofit.Builder()
        .baseUrl("https://mad.labpro.hmif.dev/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ApiService::class.java)
}

package com.example.nimons360.di

import android.content.Context
import com.example.nimons360.data.local.preference.TokenPreference
import com.example.nimons360.data.remote.api.ApiService
import com.example.nimons360.utils.TokenExpiredInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    // Interceptor TokenPreference dan ApplicationContext
    @Provides
    @Singleton
    fun provideTokenExpiredInterceptor(
        tokenPreference: TokenPreference,
        @ApplicationContext context: Context
    ): TokenExpiredInterceptor {
        return TokenExpiredInterceptor(tokenPreference, context)
    }

    // OkHttpClient dengan Interceptor
    @Provides
    @Singleton
    fun provideOkHttpClient(
        tokenExpiredInterceptor: TokenExpiredInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(tokenExpiredInterceptor)
            .build()
    }

    // Memasangkan OkHttpClient kedalam Retrofit
    @Provides
    @Singleton
    fun provideApiService(okHttpClient: OkHttpClient): ApiService {
        return Retrofit.Builder()
            .baseUrl("https://mad.labpro.hmif.dev")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
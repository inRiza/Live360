package com.example.nimons360.di

import android.content.Context
import com.example.nimons360.data.local.preference.LocationPreference
import com.example.nimons360.data.local.preference.NotificationPreference
import com.example.nimons360.data.local.preference.TokenPreference
import com.example.nimons360.data.remote.api.ApiService
import com.example.nimons360.data.repository.NotificationRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideTokenPreference(
        @ApplicationContext context: Context
    ): TokenPreference = TokenPreference(context)

    @Provides
    @Singleton
    fun provideNotificationPreference(
        @ApplicationContext context: Context
    ): NotificationPreference = NotificationPreference(context)

    @Provides
    @Singleton
    fun provideLocationPreference(
        @ApplicationContext context: Context
    ): LocationPreference = LocationPreference(context)

    @Provides
    @Singleton
    fun provideNotificationRepository(
        apiService: ApiService
    ): NotificationRepository = NotificationRepository(apiService)
}
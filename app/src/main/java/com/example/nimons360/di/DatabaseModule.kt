package com.example.nimons360.di

import android.content.Context
import androidx.room.Room
import com.example.nimons360.data.local.db.AppDatabase
import com.example.nimons360.data.local.db.dao.PinnedFamilyDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "nimons360.db").build()

    @Provides
    fun providePinnedFamilyDao(db: AppDatabase): PinnedFamilyDao = db.pinnedFamilyDao()
}

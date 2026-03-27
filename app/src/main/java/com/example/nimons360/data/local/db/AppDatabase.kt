package com.example.nimons360.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.nimons360.data.local.db.dao.PinnedFamilyDao
import com.example.nimons360.data.local.db.entity.PinnedFamilyEntity

@Database(entities = [PinnedFamilyEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun pinnedFamilyDao(): PinnedFamilyDao
}

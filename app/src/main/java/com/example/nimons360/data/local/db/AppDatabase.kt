package com.example.nimons360.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.nimons360.data.local.db.dao.MarkedLocationDao
import com.example.nimons360.data.local.db.dao.PinnedFamilyDao
import com.example.nimons360.data.local.db.entity.MarkedLocationEntity
import com.example.nimons360.data.local.db.entity.MarkedLocationPhotoEntity
import com.example.nimons360.data.local.db.entity.PinnedFamilyEntity

@Database(
    entities = [
        PinnedFamilyEntity::class,
        MarkedLocationEntity::class,
        MarkedLocationPhotoEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun pinnedFamilyDao(): PinnedFamilyDao
    abstract fun markedLocationDao(): MarkedLocationDao
}

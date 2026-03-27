package com.example.nimons360.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.nimons360.data.local.db.entity.PinnedFamilyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PinnedFamilyDao {
    @Query("SELECT * FROM pinned_families")
    fun getAll(): Flow<List<PinnedFamilyEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM pinned_families WHERE id = :familyId)")
    suspend fun isPinned(familyId: Int): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PinnedFamilyEntity)

    @Query("DELETE FROM pinned_families WHERE id = :familyId")
    suspend fun deleteById(familyId: Int)
}

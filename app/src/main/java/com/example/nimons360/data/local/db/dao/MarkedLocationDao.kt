package com.example.nimons360.data.local.db.dao

import androidx.room.*
import com.example.nimons360.data.local.db.entity.MarkedLocationEntity
import com.example.nimons360.data.local.db.entity.MarkedLocationPhotoEntity
import kotlinx.coroutines.flow.Flow

data class MarkedLocationWithPhotos(
    val location: MarkedLocationEntity,
    val photos: List<MarkedLocationPhotoEntity>
)

@Dao
interface MarkedLocationDao {

    @Query("SELECT * FROM marked_locations ORDER BY createdAt DESC")
    fun getAllFlow(): Flow<List<MarkedLocationEntity>>

    @Query("SELECT * FROM marked_locations ORDER BY createdAt DESC")
    suspend fun getAll(): List<MarkedLocationEntity>

    @Query("SELECT * FROM marked_locations WHERE id = :id")
    suspend fun getById(id: String): MarkedLocationEntity?

    @Query("SELECT * FROM marked_location_photos WHERE locationId = :locationId")
    suspend fun getPhotosForLocation(locationId: String): List<MarkedLocationPhotoEntity>

    @Query("SELECT * FROM marked_location_photos WHERE locationId = :locationId")
    fun getPhotosFlow(locationId: String): Flow<List<MarkedLocationPhotoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocation(entity: MarkedLocationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhotos(photos: List<MarkedLocationPhotoEntity>)

    @Update
    suspend fun updateLocation(entity: MarkedLocationEntity)

    @Query("DELETE FROM marked_locations WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM marked_location_photos WHERE locationId = :locationId")
    suspend fun deletePhotosForLocation(locationId: String)

    @Query("SELECT * FROM marked_location_photos")
    suspend fun getAllPhotos(): List<MarkedLocationPhotoEntity>
}

package com.example.nimons360.data.repository

import android.content.Context
import com.example.nimons360.data.local.db.dao.MarkedLocationDao
import com.example.nimons360.data.local.db.dao.MarkedLocationWithPhotos
import com.example.nimons360.data.local.db.entity.MarkedLocationEntity
import com.example.nimons360.data.local.db.entity.MarkedLocationPhotoEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MarkedLocationRepository @Inject constructor(
    private val dao: MarkedLocationDao,
    @ApplicationContext private val context: Context
) {

    fun getAllWithPhotos(): Flow<List<MarkedLocationWithPhotos>> {
        return dao.getAllFlow().map { locations ->
            locations.map { location ->
                val photos = dao.getPhotosForLocation(location.id)
                MarkedLocationWithPhotos(location, photos)
            }
        }
    }

    suspend fun getWithPhotos(id: String): MarkedLocationWithPhotos? {
        val location = dao.getById(id) ?: return null
        val photos = dao.getPhotosForLocation(location.id)
        return MarkedLocationWithPhotos(location, photos)
    }

    /**
     * Insert a new marked location.
     * sourcePhotoPaths: list of file paths (from camera/gallery temp files) to be copied into internal storage.
     */
    suspend fun insert(
        entity: MarkedLocationEntity,
        sourcePhotoPaths: List<String>
    ) {
        dao.insertLocation(entity)
        val photoEntities = copyPhotosToStorage(entity.id, sourcePhotoPaths)
        if (photoEntities.isNotEmpty()) {
            dao.insertPhotos(photoEntities)
        }
    }

    /**
     * Update an existing marked location.
     * keptPhotoPaths: existing stored paths to keep (others will be deleted).
     * newSourcePhotoPaths: new photos to copy in.
     */
    suspend fun update(
        entity: MarkedLocationEntity,
        keptPhotoPaths: List<String>,
        newSourcePhotoPaths: List<String>
    ) {
        dao.updateLocation(entity)

        // Delete photos that are no longer kept
        val existing = dao.getPhotosForLocation(entity.id)
        val toDelete = existing.filter { it.filePath !in keptPhotoPaths }
        toDelete.forEach { photo ->
            File(photo.filePath).takeIf { it.exists() }?.delete()
        }

        // Delete their DB entries
        if (toDelete.isNotEmpty()) {
            dao.deletePhotosForLocation(entity.id)
            // Re-insert only the kept ones
            val keptEntities = existing.filter { it.filePath in keptPhotoPaths }
            if (keptEntities.isNotEmpty()) dao.insertPhotos(keptEntities)
        }

        // Copy new photos
        val newEntities = copyPhotosToStorage(entity.id, newSourcePhotoPaths)
        if (newEntities.isNotEmpty()) {
            dao.insertPhotos(newEntities)
        }
    }

    /**
     * Delete a marked location and all associated photos from filesystem.
     */
    suspend fun delete(id: String) {
        // Delete photo files from filesystem
        val photos = dao.getPhotosForLocation(id)
        photos.forEach { photo ->
            File(photo.filePath).takeIf { it.exists() }?.delete()
        }
        // Also delete the folder
        val folder = getLocationPhotoDir(id)
        if (folder.exists()) folder.deleteRecursively()

        // Room CASCADE will delete photos table entries
        dao.deleteById(id)
    }

    private fun copyPhotosToStorage(locationId: String, sourcePaths: List<String>): List<MarkedLocationPhotoEntity> {
        val dir = getLocationPhotoDir(locationId)
        if (!dir.exists()) dir.mkdirs()

        return sourcePaths.mapNotNull { srcPath ->
            val srcFile = File(srcPath)
            if (!srcFile.exists()) return@mapNotNull null

            val destFile = File(dir, "${System.nanoTime()}_${srcFile.name}")
            runCatching { srcFile.copyTo(destFile, overwrite = true) }.getOrNull() ?: return@mapNotNull null

            MarkedLocationPhotoEntity(
                id = "photo-${System.nanoTime()}",
                locationId = locationId,
                filePath = destFile.absolutePath
            )
        }
    }

    private fun getLocationPhotoDir(locationId: String): File {
        return File(context.filesDir, "marked_location_photos/$locationId")
    }
}

package com.example.nimons360.data.local.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "marked_location_photos",
    foreignKeys = [
        ForeignKey(
            entity = MarkedLocationEntity::class,
            parentColumns = ["id"],
            childColumns = ["locationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("locationId")]
)
data class MarkedLocationPhotoEntity(
    @PrimaryKey val id: String,
    val locationId: String,
    val filePath: String,
    val createdAt: Long = System.currentTimeMillis()
)

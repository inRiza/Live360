package com.example.nimons360.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pinned_families")
data class PinnedFamilyEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val iconUrl: String
)

package com.example.nimons360.data.remote.dto.common

import com.google.gson.annotations.SerializedName

data class FavoriteLocationDto(
    @SerializedName("id")
    val id: String,
    @SerializedName("label")
    val label: String,
    @SerializedName("address")
    val address: String,
    @SerializedName("latitude")
    val latitude: Double,
    @SerializedName("longitude")
    val longitude: Double,
    @SerializedName("createdAt")
    val createdAt: Long
)

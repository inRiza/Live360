package com.example.nimons360.data.remote.dto.common

import com.google.gson.annotations.SerializedName

data class LiveUserInfoDto(
    @SerializedName("userId")
    val userId: Int, // Int? -> Int
    @SerializedName("fullName")
    val fullName: String,
    @SerializedName("email")
    val email: String,
    @SerializedName("latitude")
    val latitude: Double,
    @SerializedName("longitude")
    val longitude: Double,
    @SerializedName("rotation")
    val rotation: Float,
    @SerializedName("batteryLevel")
    val batteryLevel: Int,
    @SerializedName("isCharging")
    val isCharging: Boolean,
    @SerializedName("internetStatus")
    val internetStatus: String
)

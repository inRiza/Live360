package com.example.nimons360.data.remote.websocket.model
import com.google.gson.annotations.SerializedName
data class MemberPresencePayload(
    @SerializedName("userId") val userId: Int,
    @SerializedName("email") val email: String,
    @SerializedName("fullName") val fullName: String,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("rotation") val rotation: Float,
    @SerializedName("batteryLevel") val batteryLevel: Int,
    @SerializedName("isCharging") val isCharging: Boolean,
    @SerializedName("internetStatus") val internetStatus: String,
    @SerializedName("metadata") val metadata: Map<String, Any> = emptyMap()
)

package com.example.nimons360.data.remote.websocket.model

data class MemberPresencePayload(
    val userId: Int,
    val email: String,
    val fullName: String,
    val latitude: Double,
    val longitude: Double,
    val rotation: Float,
    val batteryLevel: Int,
    val isCharging: Boolean,
    val internetStatus: String,
    val metadata: Map<String, Any> = emptyMap()
)

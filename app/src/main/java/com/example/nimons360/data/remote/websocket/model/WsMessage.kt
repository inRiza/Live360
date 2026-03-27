package com.example.nimons360.data.remote.websocket.model

data class WsMessage<T>(
    val type: String,
    val payload: T,
    val timestamp: String
)

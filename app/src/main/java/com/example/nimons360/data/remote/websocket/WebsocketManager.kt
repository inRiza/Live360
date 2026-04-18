package com.example.nimons360.data.remote.websocket

import com.example.nimons360.data.local.preference.TokenPreference
import com.example.nimons360.data.remote.websocket.model.UpdatePresencePayload
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebSocketManager @Inject constructor(
    private val tokenPreference: TokenPreference
) {
    fun connect() {}
    fun disconnect() {}
    fun sendPresence(payload: UpdatePresencePayload) {}
}

package com.example.nimons360.data.remote.websocket

import com.example.nimons360.data.local.preference.TokenPreference
import com.example.nimons360.data.remote.websocket.model.MemberPresencePayload
import com.example.nimons360.data.remote.websocket.model.UpdatePresencePayload
import com.example.nimons360.utils.Constants
import com.google.gson.Gson
import com.google.gson.JsonParser
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.time.Instant

@Singleton
class WebSocketManager @Inject constructor(
    private val tokenPreference: TokenPreference,
    private val okHttpClient: OkHttpClient
) {

    private companion object {
        const val NORMAL_CLOSE_CODE = 1000
        const val ABNORMAL_CLOSE_CODE = 1006
        const val MAX_RECONNECT_DELAY_MS = 5_000L
    }

    sealed interface Event {
        data object Connected : Event
        data class Disconnected(val code: Int, val reason: String) : Event
        data class PresenceReceived(val payload: MemberPresencePayload) : Event
        data class Error(val message: String) : Event
    }

    private val gson = Gson()
    private var webSocket: WebSocket? = null
    private var isConnecting = false
    private var reconnectAttempt = 0
    private var shouldReconnect = false
    private var reconnectJob: Job? = null
    private val reconnectScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 32)
    val events: SharedFlow<Event> = _events.asSharedFlow()

    @Synchronized
    fun connect() {
        shouldReconnect = true
        if (webSocket != null || isConnecting) return

        val token = tokenPreference.getToken()
        if (token.isNullOrBlank()) {
            shouldReconnect = false
            _events.tryEmit(Event.Error("Missing auth token for websocket"))
            return
        }

        openSocket(token)
    }

    @Synchronized
    private fun openSocket(token: String) {
        if (webSocket != null || isConnecting) return
        isConnecting = true

        reconnectJob?.cancel()
        reconnectJob = null

        val request = Request.Builder()
            .url(Constants.WS_URL)
            .header("Authorization", "Bearer $token")
            .build()

        webSocket = okHttpClient.newWebSocket(
            request,
            object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    isConnecting = false
                    reconnectAttempt = 0
                    _events.tryEmit(Event.Connected)
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    handleIncomingMessage(text)
                }

                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    _events.tryEmit(Event.Disconnected(code, reason))
                    webSocket.close(code, reason)
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    isConnecting = false
                    _events.tryEmit(Event.Disconnected(code, reason))
                    this@WebSocketManager.webSocket = null
                    scheduleReconnectIfNeeded()
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    isConnecting = false
                    this@WebSocketManager.webSocket = null
                    _events.tryEmit(Event.Disconnected(ABNORMAL_CLOSE_CODE, "connection_failure"))
                    _events.tryEmit(Event.Error(t.message ?: "WebSocket failure"))
                    scheduleReconnectIfNeeded()
                }
            }
        )
    }

    @Synchronized 
    fun disconnect() {
        shouldReconnect = false
        reconnectAttempt = 0
        reconnectJob?.cancel()
        reconnectJob = null
        isConnecting = false
        webSocket?.close(NORMAL_CLOSE_CODE, "Client disconnect")
        webSocket = null
    }

    @Synchronized
    private fun scheduleReconnectIfNeeded() {
        if (!shouldReconnect || webSocket != null || isConnecting) return
        if (reconnectJob?.isActive == true) return

        val delayMs = (1_000L * (1L shl reconnectAttempt.coerceAtMost(2))).coerceAtMost(MAX_RECONNECT_DELAY_MS)
        reconnectAttempt += 1
        reconnectJob = reconnectScope.launch {
            delay(delayMs)
            connect()
        }
    }

    fun sendPresence(payload: UpdatePresencePayload) {
        val socket = webSocket
        if (socket == null) {
            _events.tryEmit(Event.Error("WebSocket not connected"))
            return
        }

        val wsMessage = mapOf(
            "type" to "update_presence",
            "payload" to payload,
            "timestamp" to Instant.now().toString()
        )
        val sent = socket.send(gson.toJson(wsMessage))
        if (!sent) {
            _events.tryEmit(Event.Error("Failed to send presence"))
        }
    }

    fun sendPing() {
        val socket = webSocket ?: return
        val wsMessage = mapOf(
            "type" to "ping",
            "payload" to emptyMap<String, Any>(),
            "timestamp" to Instant.now().toString()
        )
        socket.send(gson.toJson(wsMessage))
    }

    private fun handleIncomingMessage(rawMessage: String) {
        try {
            val root = JsonParser.parseString(rawMessage).asJsonObject
            val type = root.get("type")?.asString.orEmpty()
            val payloadElement = root.get("payload")
            if (payloadElement == null || !payloadElement.isJsonObject) return

            if (type == "member_presence_updated") {
                val payload = gson.fromJson(payloadElement, MemberPresencePayload::class.java)
                _events.tryEmit(Event.PresenceReceived(payload))
            }

            // if (type.contains("presence", ignoreCase = true) || type.contains("member", ignoreCase = true)) {
            //     val payload = gson.fromJson(payloadElement, MemberPresencePayload::class.java)
            //     _events.tryEmit(Event.PresenceReceived(payload))
            // }
        } catch (_: Exception) {
        }
    }
}

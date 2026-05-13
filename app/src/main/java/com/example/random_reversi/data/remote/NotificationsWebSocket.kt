package com.example.random_reversi.data.remote

import com.example.random_reversi.BuildConfig
import com.example.random_reversi.data.SessionManager
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

object NotificationsWebSocket {
    private val gson = Gson()
    private var webSocket: WebSocket? = null

    private val _notificationEvent = MutableStateFlow<JsonObject?>(null)
    val notificationEvent: StateFlow<JsonObject?> = _notificationEvent.asStateFlow()

    private val _connectionState = MutableStateFlow("closed")
    val connectionState: StateFlow<String> = _connectionState.asStateFlow()

    fun connect() {
        if (_connectionState.value == "connected" || _connectionState.value == "connecting") return

        val token = SessionManager.getToken() ?: return

        val baseUrl = BuildConfig.API_BASE_URL
            .replace("http://", "ws://")
            .replace("https://", "wss://")
            .trimEnd('/')

        val wsUrl = "$baseUrl/ws/notifications?token=$token"

        val client = ApiClient.okHttpClient.newBuilder()
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .pingInterval(30, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder()
            .url(wsUrl)
            .build()

        _connectionState.value = "connecting"

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                _connectionState.value = "connected"
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val json = gson.fromJson(text, JsonObject::class.java)
                    _notificationEvent.value = json
                } catch (_: Exception) {}
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                _connectionState.value = "closing"
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                _connectionState.value = "closed"
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                _connectionState.value = "error"
                // Implementar reconexión si fuera necesario
            }
        })
    }

    fun disconnect() {
        webSocket?.close(1000, "App paused or logged out")
        webSocket = null
        _connectionState.value = "closed"
    }
}

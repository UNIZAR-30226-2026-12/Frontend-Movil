package com.example.random_reversi.data.remote

import com.example.random_reversi.BuildConfig
import com.example.random_reversi.data.SessionManager
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

data class GameState(
    val board: List<List<Int?>> = emptyList(),
    val current_player: String? = null,
    val valid_moves: List<List<Int>> = emptyList(),
    val game_over: Boolean = false,
    val scores: Map<String, Int> = emptyMap(),
    val winner: String? = null
)

data class WsMessage(
    val type: String,
    val payload: JsonObject? = null
)

class GameWebSocket(
    private val gameId: Int
) {
    private val gson = Gson()
    private var webSocket: WebSocket? = null

    private val _gameState = MutableStateFlow(GameState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private val _myColor = MutableStateFlow<String?>(null)
    val myColor: StateFlow<String?> = _myColor.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val chatMessages: StateFlow<List<Pair<String, String>>> = _chatMessages.asStateFlow()

    private val _connectionState = MutableStateFlow("connecting")
    val connectionState: StateFlow<String> = _connectionState.asStateFlow()

    private val _roomPlayers = MutableStateFlow<List<JsonObject>>(emptyList())
    val roomPlayers: StateFlow<List<JsonObject>> = _roomPlayers.asStateFlow()

    fun connect() {
        val token = SessionManager.getToken() ?: return

        // Convertir URL HTTP a WS
        val baseUrl = BuildConfig.API_BASE_URL
            .replace("http://", "ws://")
            .replace("https://", "wss://")
            .trimEnd('/')

        val wsUrl = "$baseUrl/ws/play/$gameId?token=$token"

        val client = OkHttpClient.Builder()
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .pingInterval(30, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder()
            .url(wsUrl)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                _connectionState.value = "connected"
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val msg = gson.fromJson(text, WsMessage::class.java)
                    handleMessage(msg)
                } catch (e: Exception) {
                    // Intentar parsear como JSON genérico
                    try {
                        val json = gson.fromJson(text, JsonObject::class.java)
                        val type = json.get("type")?.asString ?: return
                        handleMessage(WsMessage(type, json.getAsJsonObject("payload")))
                    } catch (_: Exception) {}
                }
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
            }
        })
    }

    private fun handleMessage(msg: WsMessage) {
        when (msg.type) {
            "player_assignment" -> {
                msg.payload?.get("color")?.asString?.let {
                    _myColor.value = it
                }
            }

            "game_state_update" -> {
                msg.payload?.let { payload ->
                    try {
                        val board = if (payload.has("board")) {
                            val boardArray = payload.getAsJsonArray("board")
                            boardArray.map { row ->
                                row.asJsonArray.map { cell ->
                                    if (cell.isJsonNull) null else cell.asInt
                                }
                            }
                        } else emptyList()

                        val validMoves = if (payload.has("valid_moves")) {
                            payload.getAsJsonArray("valid_moves").map { move ->
                                move.asJsonArray.map { it.asInt }
                            }
                        } else emptyList()

                        val scores = if (payload.has("scores")) {
                            val scoresObj = payload.getAsJsonObject("scores")
                            scoresObj.entrySet().associate { it.key to it.value.asInt }
                        } else emptyMap()

                        _gameState.value = GameState(
                            board = board,
                            current_player = payload.get("current_player")?.asString,
                            valid_moves = validMoves,
                            game_over = payload.get("game_over")?.asBoolean ?: false,
                            scores = scores,
                            winner = payload.get("winner")?.asString
                        )
                    } catch (_: Exception) {}
                }
            }

            "chat_message" -> {
                msg.payload?.let { payload ->
                    val sender = payload.get("sender")?.asString ?: "?"
                    val message = payload.get("message")?.asString ?: ""
                    _chatMessages.value = _chatMessages.value + (sender to message)
                }
            }

            "room_sync" -> {
                msg.payload?.let { payload ->
                    if (payload.has("players")) {
                        val playersArray = payload.getAsJsonArray("players")
                        _roomPlayers.value = playersArray.map { it.asJsonObject }
                    }
                }
            }

            "waiting_for_player" -> {
                _connectionState.value = "waiting"
            }
        }
    }

    fun sendMove(row: Int, col: Int) {
        val json = gson.toJson(mapOf(
            "action" to "make_move",
            "row" to row,
            "col" to col
        ))
        webSocket?.send(json)
    }

    fun sendChat(message: String) {
        val json = gson.toJson(mapOf(
            "action" to "chat",
            "message" to message
        ))
        webSocket?.send(json)
    }

    fun sendReady(ready: Boolean) {
        val json = gson.toJson(mapOf(
            "action" to "set_ready",
            "ready" to ready
        ))
        webSocket?.send(json)
    }

    fun sendSurrender() {
        val json = gson.toJson(mapOf("action" to "surrender"))
        webSocket?.send(json)
    }

    fun disconnect() {
        webSocket?.close(1000, "User left")
        webSocket = null
    }
}

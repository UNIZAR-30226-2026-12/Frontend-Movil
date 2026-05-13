package com.example.random_reversi.data.remote

import com.example.random_reversi.BuildConfig
import com.example.random_reversi.data.SessionManager
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
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
    val board: List<List<String?>> = emptyList(),
    val current_player: String? = null,
    val valid_moves: List<List<Int>> = emptyList(),
    val game_over: Boolean = false,
    val scores: Map<String, Int> = emptyMap(),
    val winner: String? = null,
    val abandoned_pieces: List<String> = emptyList(),
    val paused_pieces: List<String> = emptyList(),
    val paused_usernames: List<String> = emptyList(),
    val username_by_piece: Map<String, String> = emptyMap(),
    val skill_tiles: List<List<Int>> = emptyList(),
    val fixed_pieces: List<List<Int>> = emptyList()
)

// Inventario de habilidades por color de ficha
data class SkillsInventory(
    val black: List<String> = emptyList(),
    val white: List<String> = emptyList(),
    val red: List<String> = emptyList(),
    val blue: List<String> = emptyList()
)

data class SkillUsedEvent(
    val abilityId: String,
    val username: String,
    val isMine: Boolean
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

    private val _roomStatus = MutableStateFlow("waiting")
    val roomStatus: StateFlow<String> = _roomStatus.asStateFlow()

    private val _skillsInventory = MutableStateFlow(SkillsInventory())
    val skillsInventory: StateFlow<SkillsInventory> = _skillsInventory.asStateFlow()

    private val _skillUsedEvent = MutableStateFlow<SkillUsedEvent?>(null)
    val skillUsedEvent: StateFlow<SkillUsedEvent?> = _skillUsedEvent.asStateFlow()

    private fun JsonElement?.asStringOrNull(): String? {
        if (this == null || isJsonNull || !isJsonPrimitive || !asJsonPrimitive.isString) return null
        return asString
    }

    private fun JsonElement?.asIntOrNull(): Int? {
        if (this == null || isJsonNull || !isJsonPrimitive || !asJsonPrimitive.isNumber) return null
        return try {
            asInt
        } catch (_: Exception) {
            null
        }
    }

    private fun JsonElement?.asBooleanOr(default: Boolean): Boolean {
        if (this == null || isJsonNull || !isJsonPrimitive || !asJsonPrimitive.isBoolean) return default
        return asBoolean
    }

    private fun JsonObject.arrayOrEmpty(key: String): JsonArray? {
        if (!has(key) || get(key).isJsonNull || !get(key).isJsonArray) return null
        return getAsJsonArray(key)
    }

    private fun JsonObject.objectOrNull(key: String): JsonObject? {
        if (!has(key) || get(key).isJsonNull || !get(key).isJsonObject) return null
        return getAsJsonObject(key)
    }

    private fun normalizePieceCell(raw: JsonElement): String? {
        return try {
            if (raw.isJsonNull) return null
            if (raw.isJsonPrimitive) {
                val primitive = raw.asJsonPrimitive
                when {
                    primitive.isString -> {
                        when (val value = primitive.asString.lowercase().trim()) {
                            "black", "white", "red", "blue" -> value
                            else -> null
                        }
                    }
                    primitive.isNumber -> {
                        when (primitive.asInt) {
                            0 -> "black"
                            1 -> "white"
                            2 -> "red"
                            3 -> "blue"
                            else -> null
                        }
                    }
                    else -> null
                }
            } else null
        } catch (_: Exception) { null }
    }

    fun connect() {
        val token = SessionManager.getToken() ?: return

        // Convertir URL HTTP a WS
        val baseUrl = BuildConfig.API_BASE_URL
            .replace("http://", "ws://")
            .replace("https://", "wss://")
            .trimEnd('/')

        val wsUrl = "$baseUrl/ws/play/$gameId?token=$token"

        // Reutilizamos el cliente singleton (mismo pool de hilos y conexiones)
        // Solo sobreescribimos los timeouts específicos de WebSocket
        val client = ApiClient.okHttpClient.newBuilder()
            .readTimeout(0, TimeUnit.MILLISECONDS)   // sin timeout de lectura (stream abierto)
            .pingInterval(30, TimeUnit.SECONDS)       // keep-alive
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

                        val board = payload.arrayOrEmpty("board")?.let { boardArray ->
                            boardArray.mapNotNull { row ->
                                if (row.isJsonArray) {
                                    row.asJsonArray.map { cell -> normalizePieceCell(cell) }
                                } else if (row.isJsonNull) {
                                    null  // fila nula → se descarta
                                } else null
                            }
                        } ?: emptyList()

                        val validMoves = payload.arrayOrEmpty("valid_moves")?.mapNotNull { move ->
                                when {
                                    move.isJsonArray -> {
                                        val arr = move.asJsonArray
                                        if (arr.size() >= 2) listOf(arr[0].asInt, arr[1].asInt) else null
                                    }
                                    move.isJsonObject -> {
                                        val obj = move.asJsonObject
                                        if (obj.has("row") && obj.has("col")) listOf(obj.get("row").asInt, obj.get("col").asInt) else null
                                    }
                                    else -> null
                                }
                            }
                            ?: emptyList()

                        val scoreObj = payload.objectOrNull("score") ?: payload.objectOrNull("scores")
                        val scores = scoreObj?.entrySet()
                            ?.mapNotNull { entry ->
                                val value = entry.value.asIntOrNull() ?: return@mapNotNull null
                                entry.key to value
                            }
                            ?.toMap()
                            ?: emptyMap()

                        val abandonedPieces = payload.arrayOrEmpty("abandoned_pieces")
                            ?.mapNotNull { piece -> piece.asStringOrNull()?.lowercase() }
                            ?: emptyList()

                        val pausedPieces = payload.arrayOrEmpty("paused_pieces")
                            ?.mapNotNull { piece -> piece.asStringOrNull()?.lowercase() }
                            ?: emptyList()

                        val pausedUsernames = payload.arrayOrEmpty("paused_usernames")
                            ?.mapNotNull { username -> username.asStringOrNull() }
                            ?: emptyList()

                        val usernameByPiece = payload.objectOrNull("username_by_piece")
                            ?.entrySet()
                            ?.mapNotNull { entry ->
                                val username = entry.value.asStringOrNull() ?: return@mapNotNull null
                                entry.key to username
                            }
                            ?.toMap()
                            ?: emptyMap()

                        // Parsear inventario de habilidades
                        val skillsInvObj = payload.objectOrNull("skills_inventory")
                        if (skillsInvObj != null) {
                            val blackSkills = skillsInvObj.arrayOrEmpty("black")
                                ?.mapNotNull { it.asStringOrNull() } ?: emptyList()
                            val whiteSkills = skillsInvObj.arrayOrEmpty("white")
                                ?.mapNotNull { it.asStringOrNull() } ?: emptyList()
                            val redSkills = skillsInvObj.arrayOrEmpty("red")
                                ?.mapNotNull { it.asStringOrNull() } ?: emptyList()
                            val blueSkills = skillsInvObj.arrayOrEmpty("blue")
                                ?.mapNotNull { it.asStringOrNull() } ?: emptyList()
                            _skillsInventory.value = SkillsInventory(
                                black = blackSkills, 
                                white = whiteSkills,
                                red = redSkills,
                                blue = blueSkills
                            )
                        }

                        // Parsear casillas de habilidad (skill_tiles)
                        val skillTiles = payload.arrayOrEmpty("skill_tiles")
                            ?.mapNotNull { tile ->
                                if (tile.isJsonArray) {
                                    val arr = tile.asJsonArray
                                    if (arr.size() >= 2) listOf(arr[0].asInt, arr[1].asInt) else null
                                } else null
                            } ?: emptyList()

                        // Parsear fichas fijas (fixed_pieces)
                        val fixedPieces = payload.arrayOrEmpty("fixed_pieces")
                            ?.mapNotNull { tile ->
                                if (tile.isJsonArray) {
                                    val arr = tile.asJsonArray
                                    if (arr.size() >= 2) listOf(arr[0].asInt, arr[1].asInt) else null
                                } else null
                            } ?: emptyList()

                        _gameState.value = GameState(
                            board = board,
                            current_player = payload.get("current_player").asStringOrNull()?.lowercase(),
                            valid_moves = validMoves,
                            game_over = payload.get("game_over").asBooleanOr(false),
                            scores = scores,
                            winner = payload.get("winner").asStringOrNull()?.lowercase(),
                            abandoned_pieces = abandonedPieces,
                            paused_pieces = pausedPieces,
                            paused_usernames = pausedUsernames,
                            username_by_piece = usernameByPiece,
                            skill_tiles = skillTiles,
                            fixed_pieces = fixedPieces
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
                    payload.get("status")?.asString?.let { status ->
                        _roomStatus.value = status
                    }
                    if (payload.has("players")) {
                        val playersArray = payload.getAsJsonArray("players")
                        _roomPlayers.value = playersArray.map { it.asJsonObject }
                    }
                }
            }

            "waiting_for_player" -> {
                _connectionState.value = "waiting"
            }

            "skill_used" -> {
                msg.payload?.let { payload ->
                    val abilityId = payload.get("skill")?.asStringOrNull() ?: return@let
                    val username = payload.get("username")?.asStringOrNull() ?: return@let
                    val isMine = payload.get("is_mine")?.asBooleanOr(false) ?: false
                    _skillUsedEvent.value = SkillUsedEvent(abilityId, username, isMine)
                }
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

    fun sendPause() {
        val json = gson.toJson(mapOf("action" to "pause"))
        webSocket?.send(json)
    }

    fun sendSkillInstant(abilityType: String, targetPlayer: String, inventoryIndex: Int) {
        val json = gson.toJson(mapOf(
            "action" to "use_skill",
            "type" to abilityType,
            "target_player" to targetPlayer,
            "inventory_index" to inventoryIndex
        ))
        webSocket?.send(json)
    }

    fun sendSkillWithGiven(abilityType: String, targetPlayer: String, inventoryIndex: Int, givenSkillIndex: Int) {
        val json = gson.toJson(mapOf(
            "action" to "use_skill",
            "type" to abilityType,
            "target_player" to targetPlayer,
            "inventory_index" to inventoryIndex,
            "given_skill_index" to givenSkillIndex
        ))
        webSocket?.send(json)
    }

    fun sendSkillTargeted(abilityType: String, row: Int, col: Int, targetPlayer: String, inventoryIndex: Int) {
        val json = gson.toJson(mapOf(
            "action" to "use_skill",
            "type" to abilityType,
            "row" to row,
            "col" to col,
            "target_player" to targetPlayer,
            "inventory_index" to inventoryIndex
        ))
        webSocket?.send(json)
    }

    fun sendSkillGravity(direction: String, inventoryIndex: Int) {
        val json = gson.toJson(mapOf(
            "action" to "use_skill",
            "type" to "gravity",
            "direction" to direction,
            "inventory_index" to inventoryIndex
        ))
        webSocket?.send(json)
    }

    fun disconnect() {
        webSocket?.close(1000, "User left")
        webSocket = null
    }
}

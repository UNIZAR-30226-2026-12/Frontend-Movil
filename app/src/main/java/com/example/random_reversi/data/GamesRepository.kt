package com.example.random_reversi.data

import com.example.random_reversi.data.remote.ApiClient
import com.example.random_reversi.data.remote.CreateLobbyRequest
import com.example.random_reversi.data.remote.CreateLobbyResponse
import com.example.random_reversi.data.remote.HistoryEntry
import com.example.random_reversi.data.remote.InviteFriendsRequest
import com.example.random_reversi.data.remote.InviteFriendsResponse
import com.example.random_reversi.data.remote.LobbyStateResponse
import com.example.random_reversi.data.remote.PublicLobby
import com.example.random_reversi.data.remote.ReadyRequest
import com.example.random_reversi.data.remote.HeadToHeadResponse
import com.example.random_reversi.data.remote.UserStatsResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object GamesRepository {

    // ── Lobby ──

    suspend fun createLobby(mode: String): UserResult<CreateLobbyResponse> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.authApiService.createLobby(CreateLobbyRequest(mode))
            if (response.isSuccessful) {
                response.body()?.let { UserResult.Success(it) }
                    ?: UserResult.Error("Respuesta vacía")
            } else {
                UserResult.Error("Error al crear partida (${response.code()})")
            }
        } catch (e: Exception) {
            UserResult.Error(e.message ?: "Error de conexión")
        }
    }

    suspend fun getPublicLobbies(): UserResult<List<PublicLobby>> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.authApiService.getPublicLobbies()
            if (response.isSuccessful) {
                val parsed = response.body()?.lobbies ?: emptyList()
                val normalized = parsed.map { lobby ->
                    val safeMode = lobby.mode?.takeIf { it.isNotBlank() } ?: "1vs1"
                    val normalizedMode = safeMode.lowercase()
                    val computedMax = when {
                        (lobby.max_players ?: 0) > 0 -> lobby.max_players ?: 0
                        normalizedMode == "1vs1vs1vs1" || normalizedMode == "1v1v1v1" -> 4
                        else -> 2
                    }
                    val safePlayers = lobby.players ?: 0
                    val safeStatus = lobby.status?.takeIf { it.isNotBlank() } ?: "waiting"
                    lobby.copy(
                        creator = lobby.creator?.takeIf { it.isNotBlank() } ?: "Jugador",
                        creator_elo = lobby.creator_elo ?: 0,
                        mode = safeMode,
                        players = if (safePlayers > 0) safePlayers else 1,
                        max_players = computedMax,
                        status = safeStatus
                    )
                }
                UserResult.Success(normalized)
            } else {
                UserResult.Error("Error al cargar partidas (${response.code()})")
            }
        } catch (e: Exception) {
            UserResult.Error(e.message ?: "Error de conexión")
        }
    }

    suspend fun joinLobby(gameId: Int): UserResult<String> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.authApiService.joinLobby(gameId)
            if (response.isSuccessful) {
                UserResult.Success("Unido a la partida")
            } else {
                UserResult.Error("No se pudo unir (${response.code()})")
            }
        } catch (e: Exception) {
            UserResult.Error(e.message ?: "Error de conexión")
        }
    }

    suspend fun inviteFriends(mode: String, friendIds: List<Int>): UserResult<InviteFriendsResponse> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.authApiService.inviteFriends(InviteFriendsRequest(mode, friendIds))
            if (response.isSuccessful) {
                response.body()?.let { UserResult.Success(it) }
                    ?: UserResult.Error("Respuesta vacía")
            } else {
                UserResult.Error("Error al invitar (${response.code()})")
            }
        } catch (e: Exception) {
            UserResult.Error(e.message ?: "Error de conexión")
        }
    }

    suspend fun acceptGameInvite(gameId: Int): UserResult<Int> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.authApiService.acceptGameInvite(gameId)
            if (response.isSuccessful) {
                val body = response.body()
                val candidates = listOfNotNull(body?.game_id, body?.lobby_id, gameId).distinct()
                for (candidate in candidates) {
                    val stateCheck = ApiClient.authApiService.getLobbyState(candidate)
                    if (stateCheck.isSuccessful) {
                        return@withContext UserResult.Success(candidate)
                    }
                }
                UserResult.Success(candidates.firstOrNull() ?: gameId)
            } else {
                UserResult.Error("Error al aceptar invitacion (${response.code()})")
            }
        } catch (e: Exception) {
            UserResult.Error(e.message ?: "Error de conexion")
        }
    }

    suspend fun rejectGameInvite(gameId: Int): UserResult<String> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.authApiService.rejectGameInvite(gameId)
            if (response.isSuccessful) {
                UserResult.Success("Invitación rechazada")
            } else {
                UserResult.Error("Error al rechazar invitación (${response.code()})")
            }
        } catch (e: Exception) {
            UserResult.Error(e.message ?: "Error de conexión")
        }
    }

    suspend fun getLobbyState(gameId: Int): UserResult<LobbyStateResponse> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.authApiService.getLobbyState(gameId)
            if (response.isSuccessful) {
                response.body()?.let { UserResult.Success(it) }
                    ?: UserResult.Error("Respuesta vacía")
            } else {
                UserResult.Error("Error al cargar estado (${response.code()})")
            }
        } catch (e: Exception) {
            UserResult.Error(e.message ?: "Error de conexión")
        }
    }

    suspend fun setReady(gameId: Int, ready: Boolean): UserResult<String> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.authApiService.setReady(gameId, ReadyRequest(ready))
            if (response.isSuccessful) {
                UserResult.Success("Listo")
            } else {
                UserResult.Error("Error (${response.code()})")
            }
        } catch (e: Exception) {
            UserResult.Error(e.message ?: "Error de conexión")
        }
    }

    suspend fun leaveLobby(gameId: Int): UserResult<String> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.authApiService.leaveLobby(gameId)
            if (response.isSuccessful) {
                val message = response.body()?.message?.takeIf { it.isNotBlank() } ?: "Has abandonado la sala"
                UserResult.Success(message)
            } else {
                UserResult.Error("Error al salir (${response.code()})")
            }
        } catch (e: Exception) {
            UserResult.Error(e.message ?: "Error de conexión")
        }
    }

    // ── Stats & History ──

    suspend fun getMyStats(): UserResult<UserStatsResponse> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.authApiService.getMyStats()
            if (response.isSuccessful) {
                response.body()?.let { UserResult.Success(it) }
                    ?: UserResult.Error("Respuesta vacía")
            } else {
                UserResult.Error("Error al cargar estadísticas (${response.code()})")
            }
        } catch (e: Exception) {
            UserResult.Error(e.message ?: "Error de conexión")
        }
    }

    suspend fun getUserStats(userId: Int): UserResult<UserStatsResponse> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.authApiService.getUserStats(userId)
            if (response.isSuccessful) {
                response.body()?.let { UserResult.Success(it) }
                    ?: UserResult.Error("Respuesta vacía")
            } else {
                UserResult.Error("Error al cargar estadísticas (${response.code()})")
            }
        } catch (e: Exception) {
            UserResult.Error(e.message ?: "Error de conexión")
        }
    }

    suspend fun getHeadToHead(userId: Int): UserResult<HeadToHeadResponse> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.authApiService.getHeadToHead(userId)
            if (response.isSuccessful) {
                response.body()?.let { UserResult.Success(it) }
                    ?: UserResult.Error("Respuesta vacía")
            } else {
                UserResult.Error("Error al cargar h2h (${response.code()})")
            }
        } catch (e: Exception) {
            UserResult.Error(e.message ?: "Error de conexión")
        }
    }

    suspend fun getMyHistory(limit: Int? = null, mode: String? = null): UserResult<List<HistoryEntry>> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.authApiService.getMyHistory(limit, mode)
            if (response.isSuccessful) {
                UserResult.Success(response.body() ?: emptyList())
            } else {
                UserResult.Error("Error al cargar historial (${response.code()})")
            }
        } catch (e: Exception) {
            UserResult.Error(e.message ?: "Error de conexión")
        }
    }
    suspend fun getUserHistory(userId: Int, limit: Int? = null, mode: String? = null): UserResult<List<HistoryEntry>> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.authApiService.getUserHistory(userId, limit, mode)
            if (response.isSuccessful) {
                UserResult.Success(response.body() ?: emptyList())
            } else {
                UserResult.Error("Error al cargar historial (${response.code()})")
            }
        } catch (e: Exception) {
            UserResult.Error(e.message ?: "Error de conexion")
        }
    }
}


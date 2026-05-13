package com.example.random_reversi.data

import com.example.random_reversi.data.remote.ApiClient
import com.example.random_reversi.data.remote.ChatMessage
import com.example.random_reversi.data.remote.FriendRequestBody
import com.example.random_reversi.data.remote.SendMessageBody
import com.example.random_reversi.data.remote.SocialPanelRaw
import com.example.random_reversi.data.remote.SocialPanelResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object FriendsRepository {

    suspend fun getSocialPanel(): UserResult<SocialPanelResponse> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.authApiService.getSocialPanel()
            if (response.isSuccessful) {
                val raw = response.body() ?: return@withContext UserResult.Error("Respuesta vacia")
                val merged = SocialPanelResponse(
                    friends = raw.online + raw.offline,
                    requests = raw.requests,
                    gameRequests = raw.gameRequests,
                    pausedGames = raw.pausedGames
                )
                UserResult.Success(merged)
            } else {
                UserResult.Error("Error al cargar amigos (${response.code()})")
            }
        } catch (e: Exception) {
            UserResult.Error(e.message ?: "Error de conexion")
        }
    }

    suspend fun sendFriendRequest(username: String): UserResult<String> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.authApiService.sendFriendRequest(FriendRequestBody(username))
            if (response.isSuccessful) {
                UserResult.Success(response.body()?.message ?: "Solicitud enviada")
            } else {
                // Intentar extraer el "detail" del body de error del backend
                val errorBody = response.errorBody()?.string() ?: ""
                val detail = try {
                    com.google.gson.JsonParser.parseString(errorBody)
                        .asJsonObject.get("detail")?.asString
                } catch (_: Exception) { null }

                val errorMsg = detail ?: when (response.code()) {
                    404 -> "Usuario no encontrado"
                    409 -> "Ya existe una solicitud pendiente"
                    else -> "Error al enviar solicitud (${response.code()})"
                }
                UserResult.Error(errorMsg)
            }
        } catch (e: Exception) {
            UserResult.Error(e.message ?: "Error de conexion")
        }
    }

    suspend fun acceptFriendRequest(userId: Int): UserResult<String> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.authApiService.acceptFriendRequest(userId)
            if (response.isSuccessful) {
                UserResult.Success("Amigo aceptado")
            } else {
                UserResult.Error("Error al aceptar (${response.code()})")
            }
        } catch (e: Exception) {
            UserResult.Error(e.message ?: "Error de conexion")
        }
    }

    suspend fun rejectFriendRequest(userId: Int): UserResult<String> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.authApiService.rejectFriendRequest(userId)
            if (response.isSuccessful) {
                UserResult.Success("Solicitud rechazada")
            } else {
                UserResult.Error("Error al rechazar (${response.code()})")
            }
        } catch (e: Exception) {
            UserResult.Error(e.message ?: "Error de conexion")
        }
    }

    suspend fun removeFriend(userId: Int): UserResult<String> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.authApiService.removeFriend(userId)
            if (response.isSuccessful) {
                UserResult.Success("Amigo eliminado")
            } else {
                UserResult.Error("Error al eliminar amigo (${response.code()})")
            }
        } catch (e: Exception) {
            UserResult.Error(e.message ?: "Error de conexion")
        }
    }

    suspend fun getChatHistory(userId: Int): UserResult<List<ChatMessage>> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.authApiService.getChatHistory(userId)
            if (response.isSuccessful) {
                UserResult.Success(response.body()?.messages ?: emptyList())
            } else {
                UserResult.Error("Error al cargar chat (${response.code()})")
            }
        } catch (e: Exception) {
            UserResult.Error(e.message ?: "Error de conexion")
        }
    }

    suspend fun sendChatMessage(userId: Int, message: String): UserResult<ChatMessage> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.authApiService.sendChatMessage(userId, SendMessageBody(message))
            if (response.isSuccessful) {
                response.body()?.message?.let { UserResult.Success(it) }
                    ?: UserResult.Error("Respuesta vacia")
            } else {
                UserResult.Error("Error al enviar mensaje (${response.code()})")
            }
        } catch (e: Exception) {
            UserResult.Error(e.message ?: "Error de conexion")
        }
    }

    suspend fun markChatRead(userId: Int): UserResult<String> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.authApiService.markChatRead(userId)
            if (response.isSuccessful) {
                UserResult.Success("Leido")
            } else {
                UserResult.Error("Error (${response.code()})")
            }
        } catch (e: Exception) {
            UserResult.Error(e.message ?: "Error de conexion")
        }
    }
}

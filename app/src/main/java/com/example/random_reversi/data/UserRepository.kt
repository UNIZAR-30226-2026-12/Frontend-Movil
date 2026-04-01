package com.example.random_reversi.data

import android.content.Context
import android.net.Uri
import com.example.random_reversi.data.remote.ApiClient
import com.example.random_reversi.data.remote.CustomizationUpdateRequest
import com.example.random_reversi.data.remote.UserMeResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

sealed class UserResult<out T> {
    data class Success<T>(val data: T) : UserResult<T>()
    data class Error(val message: String) : UserResult<Nothing>()
}

object UserRepository {
    suspend fun getMe(): UserResult<UserMeResponse> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.authApiService.getMe()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    UserResult.Success(body)
                } else {
                    UserResult.Error("Respuesta vacia del servidor")
                }
            } else {
                UserResult.Error("No se pudo cargar el perfil (${response.code()})")
            }
        } catch (e: Exception) {
            UserResult.Error(e.message ?: "Error de conexion")
        }
    }

    suspend fun updateCustomization(
        preferredPieceColor: String? = null,
        avatarUrl: String? = null
    ): UserResult<UserMeResponse> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.authApiService.updateCustomization(
                CustomizationUpdateRequest(
                    avatar_url = avatarUrl,
                    preferred_piece_color = preferredPieceColor
                )
            )
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    UserResult.Success(body)
                } else {
                    UserResult.Error("Respuesta vacia del servidor")
                }
            } else {
                UserResult.Error("No se pudo guardar la personalizacion (${response.code()})")
            }
        } catch (e: Exception) {
            UserResult.Error(e.message ?: "Error de conexion")
        }
    }

    suspend fun uploadAvatar(context: Context, uri: Uri): UserResult<String> = withContext(Dispatchers.IO) {
        try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return@withContext UserResult.Error("No se pudo leer la imagen")

            val mimeType = context.contentResolver.getType(uri) ?: "image/*"
            val body = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
            val part = MultipartBody.Part.createFormData("file", "avatar_upload", body)

            val response = ApiClient.authApiService.uploadAvatar(part)
            if (response.isSuccessful) {
                val avatarUrl = response.body()?.avatar_url
                if (!avatarUrl.isNullOrBlank()) {
                    UserResult.Success(avatarUrl)
                } else {
                    UserResult.Error("Respuesta invalida al subir avatar")
                }
            } else {
                UserResult.Error("No se pudo subir el avatar (${response.code()})")
            }
        } catch (e: Exception) {
            UserResult.Error(e.message ?: "Error de conexion")
        }
    }
}

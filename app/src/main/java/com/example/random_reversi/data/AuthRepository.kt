package com.example.random_reversi.data

import com.example.random_reversi.data.remote.ApiClient
import com.example.random_reversi.data.remote.ForgotPasswordRequest
import com.example.random_reversi.data.remote.RegisterRequest
import com.example.random_reversi.data.remote.ResetPasswordRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class AuthResult {
    data class Success(val token: String? = null) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

object AuthRepository {
    suspend fun login(email: String, password: String): AuthResult = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.authApiService.login(username = email, password = password)
            if (response.isSuccessful) {
                val token = response.body()?.access_token
                SessionManager.setToken(token)
                AuthResult.Success(token = token)
            } else {
                val errorMsg = when(response.code()) {
                    404 -> "El usuario o correo ingresado no existe"
                    401 -> "Contraseña incorrecta"
                    else -> "Login fallido (${response.code()})"
                }
                AuthResult.Error(errorMsg)
            }
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Error de conexión")
        }
    }

    suspend fun register(username: String, email: String, password: String): AuthResult = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.authApiService.register(
                RegisterRequest(username = username, email = email, password = password)
            )
            if (response.isSuccessful) {
                AuthResult.Success()
            } else {
                AuthResult.Error("Registro fallido (${response.code()})")
            }
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Error de conexión")
        }
    }

    suspend fun forgotPassword(email: String): AuthResult = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.authApiService.forgotPassword(
                ForgotPasswordRequest(email = email)
            )
            if (response.isSuccessful) {
                AuthResult.Success()
            } else if (response.code() == 404) {
                AuthResult.Error("No hay ninguna cuenta asociada a este correo electrónico")
            } else {
                AuthResult.Error("Error al enviar el correo (${response.code()})")
            }
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Error de conexión")
        }
    }

    suspend fun resetPassword(email: String, code: String, newPassword: String): AuthResult = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.authApiService.resetPassword(
                ResetPasswordRequest(email = email, code = code, new_password = newPassword)
            )
            if (response.isSuccessful) {
                AuthResult.Success()
            } else {
                AuthResult.Error("Código incorrecto o expirado")
            }
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Error de conexión")
        }
    }
}

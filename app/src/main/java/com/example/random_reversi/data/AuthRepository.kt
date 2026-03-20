package com.example.random_reversi.data

import com.example.random_reversi.data.remote.ApiClient
import com.example.random_reversi.data.remote.RegisterRequest
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
                AuthResult.Success(token = token)
            } else {
                AuthResult.Error("Login fallido (${response.code()})")
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
}

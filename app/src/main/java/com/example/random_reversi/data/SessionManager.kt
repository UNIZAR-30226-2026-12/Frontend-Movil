package com.example.random_reversi.data

object SessionManager {
    @Volatile
    private var authToken: String? = null

    fun setToken(token: String?) {
        authToken = token
    }

    fun getToken(): String? = authToken

    fun clear() {
        authToken = null
    }
}

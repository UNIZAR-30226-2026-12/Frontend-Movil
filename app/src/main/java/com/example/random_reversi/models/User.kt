package com.example.random_reversi.models

enum class FriendStatus {
    ONLINE, OFFLINE, PLAYING
}

data class User(
    val id: Int,
    val name: String,
    val rr: Int,
    val status: FriendStatus = FriendStatus.OFFLINE,
    // Campos opcionales para invitaciones de juego (de tu interfaz Friend)
    val gameMode: String? = null,
    val playersCount: Int? = null
)
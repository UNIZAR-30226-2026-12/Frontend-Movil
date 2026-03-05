package com.example.random_reversi.models

enum class GameMode(val value: String) {
    ONE_VS_ONE("1vs1"),
    FOUR_PLAYERS("1vs1vs1vs1")
}

data class MatchData(
    val players: List<User>,
    val mode: GameMode
)
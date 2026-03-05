package com.example.random_reversi.models

enum class Piece {
    BLACK, WHITE
}

data class BoardCell(
    val piece: Piece? = null,
    val isFixed: Boolean = false
)

// He extraído esto de tu Record<AbilityId, ...>
enum class AbilityId(val icon: String, val abilityName: String, val needsTarget: Boolean) {
    BOMB("B", "Bomba", true),
    PLACE_FIXED("F+", "Poner ficha fija", true),
    REMOVE_FIXED("F-", "Quitar ficha fija", true),
    FLIP_ENEMY("G", "Girar ficha rival", true),
    PLACE_ANYWHERE("+", "Poner ficha libre", true),
    SKIP_RIVAL_TURN(">>", "Saltar turno rival", false),
    STEAL_RANDOM_ABILITY("R", "Robar habilidad", false),
    // ... añadiremos el resto conforme las necesitemos
}
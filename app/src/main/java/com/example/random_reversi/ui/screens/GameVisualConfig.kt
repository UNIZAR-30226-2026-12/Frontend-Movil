package com.example.random_reversi.ui.screens

import androidx.compose.ui.graphics.Color
import com.example.random_reversi.R

data class BoardPieceStyle1v1(
    val sideA: Color,
    val sideB: Color,
    val sideAName: String,
    val sideBName: String,
    val label: String
)

data class BoardPieceStyle4P(
    val p1: Color,
    val p2: Color,
    val p3: Color,
    val p4: Color,
    val p1Name: String,
    val p2Name: String,
    val p3Name: String,
    val p4Name: String,
    val label: String
)

data class ArenaTheme(
    val boardRes: Int,
    val backgroundRes: Int
)

val PIECE_STYLES_1V1 = listOf(
    BoardPieceStyle1v1(Color(0xFF222222), Color(0xFFEEEEEE), "Negras", "Blancas", "Clasico"),
    BoardPieceStyle1v1(Color(0xFFE74C3C), Color(0xFF3498DB), "Rojas", "Azules", "Fuego y Hielo"),
    BoardPieceStyle1v1(Color(0xFF2ECC71), Color(0xFFF1C40F), "Verdes", "Amarillas", "Selva"),
    BoardPieceStyle1v1(Color(0xFF9B59B6), Color(0xFFE67E22), "Moradas", "Naranjas", "Atardecer"),
    BoardPieceStyle1v1(Color(0xFF1ABC9C), Color(0xFFE84393), "Turquesas", "Rosas", "Neon")
)

val PIECE_STYLES_4P = listOf(
    BoardPieceStyle4P(Color(0xFF18181B), Color(0xFFF8FAFC), Color(0xFFEF4444), Color(0xFF3B82F6), "Negras", "Blancas", "Rojas", "Azules", "Clasico 4P"),
    BoardPieceStyle4P(Color(0xFF22C55E), Color(0xFFFDE047), Color(0xFFA855F7), Color(0xFFF97316), "Verdes", "Amarillas", "Moradas", "Naranjas", "Jungla Solar"),
    BoardPieceStyle4P(Color(0xFF06B6D4), Color(0xFFF43F5E), Color(0xFF84CC16), Color(0xFFFB7185), "Turquesas", "Fucsias", "Lima", "Rosas", "Cyber Pop"),
    BoardPieceStyle4P(Color(0xFFF59E0B), Color(0xFF14B8A6), Color(0xFF8B5CF6), Color(0xFFEF4444), "Doradas", "Turquesas", "Violetas", "Rojas", "Magma Frio"),
    BoardPieceStyle4P(Color(0xFF0EA5E9), Color(0xFFFACC15), Color(0xFFEC4899), Color(0xFF10B981), "Celestes", "Amarillas", "Rosas", "Esmeraldas", "Tropical RGB")
)

fun decodeBoardPiecePreference(value: String?): Pair<Int, Int> {
    if (value.isNullOrBlank()) return 0 to 0

    val compact = Regex("^d(\\d+)-q(\\d+)$").matchEntire(value)
    if (compact != null) {
        val duel = compact.groupValues[1].toIntOrNull() ?: 0
        val quad = compact.groupValues[2].toIntOrNull() ?: 0
        val safeDuel = if (duel in PIECE_STYLES_1V1.indices) duel else 0
        val safeQuad = if (quad in PIECE_STYLES_4P.indices) quad else 0
        return safeDuel to safeQuad
    }

    val normalized = value
        .lowercase()
        .replace("á", "a")
        .replace("é", "e")
        .replace("í", "i")
        .replace("ó", "o")
        .replace("ú", "u")

    val legacy = PIECE_STYLES_1V1.indexOfFirst { style ->
        style.label
            .lowercase()
            .replace("á", "a")
            .replace("é", "e")
            .replace("í", "i")
            .replace("ó", "o")
            .replace("ú", "u") == normalized
    }

    return (if (legacy >= 0) legacy else 0) to 0
}

fun getArenaFromElo1v1(elo: Int): ArenaTheme {
    return when {
        elo < 900 -> ArenaTheme(R.drawable.woodboard, R.drawable.woodbackground)
        elo < 1100 -> ArenaTheme(R.drawable.quartzboard, R.drawable.quartzbackground)
        elo < 1300 -> ArenaTheme(R.drawable.fireboard, R.drawable.firebackground)
        else -> ArenaTheme(R.drawable.iceboard, R.drawable.icebackground)
    }
}

fun getArenaFromElo4p(elo: Int): ArenaTheme {
    return when {
        elo < 900 -> ArenaTheme(R.drawable.woodboard4players, R.drawable.woodbackground)
        elo < 1100 -> ArenaTheme(R.drawable.quartzboard4players, R.drawable.quartzbackground)
        elo < 1300 -> ArenaTheme(R.drawable.fireboard4players, R.drawable.firebackground)
        else -> ArenaTheme(R.drawable.iceboard4players, R.drawable.icebackground)
    }
}


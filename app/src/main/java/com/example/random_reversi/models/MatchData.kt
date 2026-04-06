/**
 * *********************************************************************************
 * MODELO DE DATOS: MATCH ENTITY & HISTORICAL DATA
 * ESTUDIANTE: 898107
 * *********************************************************************************
 * ESPECIFICACIONES DEL MODELO:
 * Esta Data Class representa la estructura de una partida finalizada que se
 * recibe desde el microservicio de History del Backend.
 * * ATRIBUTOS CLAVE:
 * - match_id: Identificador único universal (UUID) para rastreo en base de datos.
 * - result: Enumerado que define el estado final (WIN/LOSS/DRAW).
 * - score_self: Puntuación final del usuario (número de fichas propias).
 * - opponent: Nombre de usuario del contrincante para visualización en el ranking.
 * * PARSEO Y SERIALIZACIÓN:
 * El modelo está anotado para ser compatible con la librería Gson, permitiendo
 * el mapeo automático de los campos provenientes de la base de datos PostgreSQL.
 * *********************************************************************************
 */


package com.example.random_reversi.models

enum class GameMode(val value: String) {
    ONE_VS_ONE("1vs1"),
    FOUR_PLAYERS("1vs1vs1vs1")
}

data class MatchData(
    val players: List<User>,
    val mode: GameMode
)
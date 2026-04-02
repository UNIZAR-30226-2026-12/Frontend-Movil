package com.example.random_reversi.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.random_reversi.R
import com.example.random_reversi.data.remote.GameWebSocket
import com.example.random_reversi.ui.theme.*

private val BOARD_SIZE_4P = 16
private val MutedRed = Color(0xFFB71C1C)
private val MutedBlue = Color(0xFF0D47A1)

@Composable
fun GameBoard1v1v1v1Screen(
    gameId: Int = -1,
    returnTo: String = "online-game",
    onNavigate: (String) -> Unit
) {
    val ws = remember { if (gameId > 0) GameWebSocket(gameId) else null }
    val gameState by ws?.gameState?.collectAsState() ?: remember { mutableStateOf(com.example.random_reversi.data.remote.GameState()) }
    val myColor by ws?.myColor?.collectAsState() ?: remember { mutableStateOf<String?>(null) }
    val connectionState by ws?.connectionState?.collectAsState() ?: remember { mutableStateOf("disconnected") }

    var selectedQuadrant by remember { mutableStateOf<Int?>(null) }
    var showSurrenderConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(gameId) { ws?.connect() }
    DisposableEffect(Unit) { onDispose { ws?.disconnect() } }

    val isMyTurn = gameState.current_player == myColor
    val gameOver = gameState.game_over

    // Map color names to display info
    val colorOrder = listOf("piece_1", "piece_2", "piece_3", "piece_4")
    val colorDisplayNames = mapOf(
        "piece_1" to "J1", "piece_2" to "J2",
        "piece_3" to "J3", "piece_4" to "J4",
        "black" to "Negras", "white" to "Blancas"
    )

    val arenaBg = R.drawable.icebackground
    val boardSkin = R.drawable.iceboard

    Box(modifier = Modifier.fillMaxSize().background(BgColor)) {
        Image(
            painter = painterResource(id = arenaBg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.4f
        )

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // Top bar
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(8.dp).background(
                            when (connectionState) {
                                "connected" -> Color.Green
                                "waiting" -> Color.Yellow
                                "error" -> Color.Red
                                else -> Color.Gray
                            }, CircleShape
                        )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        when (connectionState) {
                            "connected" -> "Conectado"
                            "waiting" -> "Esperando..."
                            else -> connectionState
                        },
                        fontSize = 11.sp, color = TextMutedColor
                    )
                }
                OutlinedButton(
                    onClick = { showSurrenderConfirm = true },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFFCA5A5),
                        containerColor = Color(0xFFF87171).copy(alpha = 0.15f)
                    ),
                    border = BorderStroke(1.dp, Color(0xFFF87171).copy(alpha = 0.35f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(36.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Text("Abandonar", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Turn indicator
            Surface(
                color = when {
                    gameOver -> Color.DarkGray
                    isMyTurn -> PrimaryColor
                    else -> SurfaceColor
                },
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color.White.copy(0.2f))
            ) {
                Text(
                    text = when {
                        gameOver -> gameState.winner?.let {
                            if (it == myColor) "¡VICTORIA!" else "DERROTA"
                        } ?: "PARTIDA FINALIZADA"
                        isMyTurn -> "TU TURNO"
                        else -> "TURNO: ${colorDisplayNames[gameState.current_player] ?: gameState.current_player ?: "..."}"
                    },
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Scores row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                gameState.scores.entries.take(4).forEachIndexed { index, (color, score) ->
                    val isMe = color == myColor
                    val isActive = color == gameState.current_player
                    val pieceColor = when (index) {
                        0 -> Color.Black
                        1 -> Color.White
                        2 -> MutedRed
                        3 -> MutedBlue
                        else -> Color.Gray
                    }
                    ScoreChip4P(
                        label = if (isMe) "Tú" else colorDisplayNames[color] ?: "J${index + 1}",
                        score = score,
                        pieceColor = pieceColor,
                        isActive = isActive
                    )
                }
                // Fallback when no scores yet
                if (gameState.scores.isEmpty()) {
                    repeat(4) {
                        ScoreChip4P("J${it + 1}", 0, when (it) {
                            0 -> Color.Black; 1 -> Color.White; 2 -> MutedRed; else -> MutedBlue
                        }, false)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Board
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF204D2B))
                    .border(4.dp, Color.Black.copy(0.3f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = boardSkin),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillBounds
                )

                val boardData = gameState.board
                val hasBoard = boardData.isNotEmpty()

                if (selectedQuadrant == null) {
                    // Full board view with quadrant selection
                    Column(Modifier.fillMaxSize()) {
                        repeat(2) { rowQ ->
                            Row(Modifier.weight(1f)) {
                                repeat(2) { colQ ->
                                    val qIndex = rowQ * 2 + colQ
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .border(1.dp, Color.White.copy(0.2f))
                                            .clickable { selectedQuadrant = qIndex },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (hasBoard) {
                                            QuadrantPreviewWs(boardData, rowQ * 8, colQ * 8)
                                        }
                                        Box(Modifier.fillMaxSize().background(Color.Black.copy(0.1f)))
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Zoomed quadrant
                    val startRow = (selectedQuadrant!! / 2) * 8
                    val startCol = (selectedQuadrant!! % 2) * 8

                    Column(Modifier.fillMaxSize()) {
                        for (r in startRow until (startRow + 8).coerceAtMost(if (hasBoard) boardData.size else startRow + 8)) {
                            Row(Modifier.weight(1f)) {
                                for (c in startCol until (startCol + 8).coerceAtMost(if (hasBoard && boardData.isNotEmpty()) boardData[0].size else startCol + 8)) {
                                    val cellValue = boardData.getOrNull(r)?.getOrNull(c)
                                    val isValidMove = gameState.valid_moves.any { it.size >= 2 && it[0] == r && it[1] == c }
                                    val canClick = isMyTurn && isValidMove && !gameOver

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .border(0.5.dp, Color.Black.copy(0.1f))
                                            .then(if (canClick) Modifier.clickable {
                                                ws?.sendMove(r, c)
                                                selectedQuadrant = null
                                            } else Modifier),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (canClick) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize(0.35f)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF4ADE80).copy(alpha = 0.4f))
                                            )
                                        }
                                        if (cellValue != null) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize(0.65f)
                                                    .clip(CircleShape)
                                                    .background(
                                                        when (cellValue) {
                                                            0 -> Color.Black
                                                            1 -> Color.White
                                                            2 -> MutedRed
                                                            3 -> MutedBlue
                                                            else -> Color.Gray
                                                        }
                                                    )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Zoom out button
                    Box(Modifier.fillMaxSize().padding(8.dp), contentAlignment = Alignment.BottomEnd) {
                        SmallFloatingActionButton(
                            onClick = { selectedQuadrant = null },
                            containerColor = Color.Black.copy(0.6f),
                            contentColor = Color.White,
                            shape = CircleShape,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Text("-", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (isMyTurn && !gameOver && gameState.valid_moves.isNotEmpty()) {
                Text(
                    "${gameState.valid_moves.size} movimiento(s) disponible(s)",
                    color = PrimaryColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Surrender dialog
        if (showSurrenderConfirm) {
            AlertDialog(
                onDismissRequest = { showSurrenderConfirm = false },
                containerColor = BgColor,
                title = { Text("Abandonar partida", color = Color.White, fontWeight = FontWeight.Bold) },
                text = { Text("¿Seguro que quieres rendirte?", color = TextMutedColor) },
                confirmButton = {
                    Button(
                        onClick = {
                            ws?.sendSurrender()
                            showSurrenderConfirm = false
                            onNavigate(returnTo)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF87171))
                    ) { Text("Rendirme") }
                },
                dismissButton = {
                    TextButton(onClick = { showSurrenderConfirm = false }) {
                        Text("Cancelar", color = TextMutedColor)
                    }
                }
            )
        }

        // Game over
        if (gameOver) {
            val isWinner = gameState.winner == myColor
            AlertDialog(
                onDismissRequest = {},
                containerColor = BgColor,
                title = { Text("Partida Finalizada", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        gameState.scores.entries.forEachIndexed { i, (color, score) ->
                            val label = if (color == myColor) "Tú" else "Jugador ${i + 1}"
                            Text("$label: $score pts", color = Color.White)
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            if (isWinner) "¡VICTORIA!" else "DERROTA",
                            color = if (isWinner) Color(0xFF4ADE80) else Color(0xFFF87171),
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { ws?.disconnect(); onNavigate(returnTo) },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)
                    ) { Text("Volver al Menú") }
                }
            )
        }
    }
}

@Composable
private fun QuadrantPreviewWs(board: List<List<Int?>>, startRow: Int, startCol: Int) {
    Column(Modifier.fillMaxSize().padding(2.dp)) {
        for (r in startRow until (startRow + 8).coerceAtMost(board.size)) {
            Row(Modifier.weight(1f)) {
                val row = board.getOrNull(r)
                for (c in startCol until (startCol + 8).coerceAtMost(row?.size ?: (startCol + 8))) {
                    val cell = row?.getOrNull(c)
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .border(0.1.dp, Color.White.copy(0.05f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (cell != null) {
                            Box(
                                Modifier
                                    .fillMaxSize(0.6f)
                                    .clip(CircleShape)
                                    .background(
                                        when (cell) {
                                            0 -> Color.Black
                                            1 -> Color.White
                                            2 -> MutedRed
                                            3 -> MutedBlue
                                            else -> Color.Gray
                                        }
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScoreChip4P(label: String, score: Int, pieceColor: Color, isActive: Boolean) {
    Surface(
        color = if (isActive) Color.White.copy(0.1f) else Color.Transparent,
        shape = RoundedCornerShape(8.dp),
        border = if (isActive) BorderStroke(1.dp, Color(0xFFFBBF24)) else null
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(pieceColor)
                    .border(0.5.dp, Color.White.copy(0.3f), CircleShape)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Column {
                Text(label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("$score", color = Color(0xFFFBBF24), fontWeight = FontWeight.Black, fontSize = 11.sp)
            }
        }
    }
}


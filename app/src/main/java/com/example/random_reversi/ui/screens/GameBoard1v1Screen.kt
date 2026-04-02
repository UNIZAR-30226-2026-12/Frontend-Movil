package com.example.random_reversi.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.random_reversi.R
import com.example.random_reversi.data.remote.GameWebSocket
import com.example.random_reversi.ui.theme.*

private val BOARD_SIZE = 8

@Composable
fun GameBoard1v1Screen(
    gameId: Int = -1,
    onNavigate: (String) -> Unit
) {
    val ws = remember { if (gameId > 0) GameWebSocket(gameId) else null }
    val gameState by ws?.gameState?.collectAsState() ?: remember { mutableStateOf(com.example.random_reversi.data.remote.GameState()) }
    val myColor by ws?.myColor?.collectAsState() ?: remember { mutableStateOf<String?>(null) }
    val connectionState by ws?.connectionState?.collectAsState() ?: remember { mutableStateOf("disconnected") }
    val chatMessages by ws?.chatMessages?.collectAsState() ?: remember { mutableStateOf(emptyList<Pair<String, String>>()) }

    var showSurrenderConfirm by remember { mutableStateOf(false) }
    var chatInput by remember { mutableStateOf("") }
    var showChat by remember { mutableStateOf(false) }

    // Conectar WebSocket
    LaunchedEffect(gameId) {
        ws?.connect()
    }

    // Desconectar al salir
    DisposableEffect(Unit) {
        onDispose { ws?.disconnect() }
    }

    // Calcular scores
    val myScore = myColor?.let { gameState.scores[it] } ?: 0
    val opponentColor = when (myColor) {
        "black" -> "white"
        "white" -> "black"
        else -> null
    }
    val opponentScore = opponentColor?.let { gameState.scores[it] } ?: 0
    val isMyTurn = gameState.current_player == myColor
    val gameOver = gameState.game_over

    val arenaBg = R.drawable.icebackground
    val boardSkin = R.drawable.iceboard

    Box(modifier = Modifier.fillMaxSize().background(BgColor)) {
        // Fondo
        Image(
            painter = painterResource(id = arenaBg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.5f
        )

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // Top bar: estado conexión + abandonar
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                // Estado de conexión
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
                            "error" -> "Error conexión"
                            "connecting" -> "Conectando..."
                            else -> connectionState
                        },
                        fontSize = 11.sp, color = TextMutedColor
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Chat toggle
                    OutlinedButton(
                        onClick = { showChat = !showChat },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = PrimaryColor,
                            containerColor = PrimaryColor.copy(alpha = 0.1f)
                        ),
                        border = BorderStroke(1.dp, PrimaryColor.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Text("Chat${if (chatMessages.isNotEmpty()) " (${chatMessages.size})" else ""}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    // Abandonar
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
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Indicador de turno
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
                        gameOver -> if (gameState.winner == myColor) "¡VICTORIA!" else if (gameState.winner == null) "EMPATE" else "DERROTA"
                        isMyTurn -> "TU TURNO"
                        else -> "TURNO DEL RIVAL"
                    },
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Scores
            Row(modifier = Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) {
                PlayerMiniCard("Tú", myScore, myColor ?: "black", isMyTurn)
                PlayerMiniCard("Rival", opponentScore, opponentColor ?: "white", !isMyTurn && !gameOver)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Chat panel (colapsable)
            if (showChat) {
                Surface(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 120.dp),
                    color = Color.Black.copy(0.5f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, BorderColor)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Column(
                            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
                        ) {
                            chatMessages.forEach { (sender, msg) ->
                                Text("$sender: $msg", color = TextColor, fontSize = 12.sp)
                            }
                            if (chatMessages.isEmpty()) {
                                Text("Sin mensajes", color = TextMutedColor, fontSize = 12.sp)
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextField(
                                value = chatInput,
                                onValueChange = { chatInput = it },
                                modifier = Modifier.weight(1f).height(40.dp),
                                placeholder = { Text("Mensaje...", fontSize = 12.sp) },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedTextColor = TextColor,
                                    unfocusedTextColor = TextColor
                                ),
                                singleLine = true,
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                            )
                            TextButton(onClick = {
                                if (chatInput.isNotBlank()) {
                                    ws?.sendChat(chatInput.trim())
                                    chatInput = ""
                                }
                            }) {
                                Text("Enviar", color = PrimaryColor, fontSize = 12.sp)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // TABLERO
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF204D2B))
                    .border(4.dp, Color.Black.copy(0.3f), RoundedCornerShape(16.dp))
            ) {
                Image(
                    painter = painterResource(id = boardSkin),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillBounds
                )

                Column(Modifier.fillMaxSize()) {
                    for (row in 0 until BOARD_SIZE) {
                        Row(Modifier.weight(1f)) {
                            for (col in 0 until BOARD_SIZE) {
                                val cellValue = gameState.board.getOrNull(row)?.getOrNull(col)
                                val isValidMove = gameState.valid_moves.any { it.size >= 2 && it[0] == row && it[1] == col }
                                val isMyTurnAndValid = isMyTurn && isValidMove && !gameOver

                                GameCell(
                                    modifier = Modifier.weight(1f),
                                    cellValue = cellValue,
                                    isValidMove = isMyTurnAndValid,
                                    onClick = {
                                        if (isMyTurnAndValid) {
                                            ws?.sendMove(row, col)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Valid moves hint
            if (isMyTurn && !gameOver && gameState.valid_moves.isNotEmpty()) {
                Text(
                    "${gameState.valid_moves.size} movimiento(s) disponible(s)",
                    color = PrimaryColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Surrender confirm dialog
        if (showSurrenderConfirm) {
            AlertDialog(
                onDismissRequest = { showSurrenderConfirm = false },
                containerColor = BgColor,
                title = { Text("Abandonar partida", color = Color.White, fontWeight = FontWeight.Bold) },
                text = { Text("¿Seguro que quieres rendirte? Perderás la partida.", color = TextMutedColor) },
                confirmButton = {
                    Button(
                        onClick = {
                            ws?.sendSurrender()
                            showSurrenderConfirm = false
                            onNavigate("online-game")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF87171))
                    ) {
                        Text("Rendirme")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSurrenderConfirm = false }) {
                        Text("Cancelar", color = TextMutedColor)
                    }
                }
            )
        }

        // Game Over dialog
        if (gameOver) {
            val isWinner = gameState.winner == myColor
            val isDraw = gameState.winner == null
            GameOverDialog(
                myScore = myScore,
                opponentScore = opponentScore,
                isWinner = isWinner,
                isDraw = isDraw,
                onExit = {
                    ws?.disconnect()
                    onNavigate("online-game")
                }
            )
        }
    }
}

@Composable
private fun GameCell(
    modifier: Modifier,
    cellValue: Int?,
    isValidMove: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .border(0.5.dp, Color.Black.copy(0.2f))
            .then(if (isValidMove) Modifier.clickable { onClick() } else Modifier),
        contentAlignment = Alignment.Center
    ) {
        if (isValidMove) {
            Box(
                modifier = Modifier
                    .fillMaxSize(0.35f)
                    .clip(CircleShape)
                    .background(Color(0xFF4ADE80).copy(alpha = 0.4f))
            )
        }
        if (cellValue != null) {
            val pieceColor = when (cellValue) {
                0 -> Color.Black      // black
                1 -> Color.White      // white
                2 -> Color(0xFFB71C1C) // red (4-player)
                3 -> Color(0xFF0D47A1) // blue (4-player)
                else -> Color.Gray
            }
            Box(
                modifier = Modifier
                    .fillMaxSize(0.8f)
                    .clip(CircleShape)
                    .background(pieceColor)
                    .border(1.dp, Color.White.copy(0.2f), CircleShape)
            )
        }
    }
}

@Composable
private fun PlayerMiniCard(name: String, score: Int, colorName: String, isActive: Boolean) {
    val pieceColor = when (colorName) {
        "black" -> Color.Black
        "white" -> Color.White
        else -> Color.Gray
    }
    Surface(
        color = if (isActive) Color.White.copy(0.1f) else Color.Transparent,
        shape = RoundedCornerShape(12.dp),
        border = if (isActive) BorderStroke(1.dp, Color(0xFFFBBF24)) else null
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(8.dp).width(80.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(pieceColor)
                    .border(1.dp, Color.White.copy(0.3f), CircleShape)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text("$score pts", color = Color(0xFFFBBF24), fontWeight = FontWeight.Black, fontSize = 14.sp)
        }
    }
}

@Composable
private fun GameOverDialog(myScore: Int, opponentScore: Int, isWinner: Boolean, isDraw: Boolean, onExit: () -> Unit) {
    AlertDialog(
        onDismissRequest = { },
        containerColor = BgColor,
        title = { Text("Partida Finalizada", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Tú: $myScore pts", color = Color.White)
                Text("Rival: $opponentScore pts", color = Color.White)
                Spacer(Modifier.height(12.dp))
                Text(
                    when {
                        isDraw -> "EMPATE"
                        isWinner -> "¡VICTORIA!"
                        else -> "DERROTA"
                    },
                    color = when {
                        isDraw -> Color.Gray
                        isWinner -> Color(0xFF4ADE80)
                        else -> Color(0xFFF87171)
                    },
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp
                )
            }
        },
        confirmButton = {
            Button(onClick = onExit, colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)) {
                Text("Volver al Menú")
            }
        }
    )
}

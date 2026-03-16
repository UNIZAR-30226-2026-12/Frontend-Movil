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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.random_reversi.R
import com.example.random_reversi.ui.theme.*

// --- Modelos de Datos (Privados para evitar errores de exposición) ---
private enum class PieceColor { BLACK, WHITE }

private data class BoardCell(
    val piece: PieceColor? = null,
    val isFixed: Boolean = false
)

private data class Ability(
    val id: String,
    val name: String,
    val icon: String,
    val needsTarget: Boolean
)

private val BOARD_SIZE = 8

@Composable
fun GameBoard1v1Screen(onNavigate: (String) -> Unit) {
    // Estados del Juego (Mocks iniciales)
    var board by remember { mutableStateOf(createInitialBoard()) }
    var currentTurn by remember { mutableStateOf(PieceColor.BLACK) }
    var questionCells by remember { mutableStateOf(setOf(2 to 7, 6 to 2, 2 to 2, 7 to 6)) }
    var blackInventory by remember { mutableStateOf(listOf(
        Ability("bomb", "Bomba", "B", true),
        Ability("gravity", "Gravedad", "G", false)
    )) }
    var gameOver by remember { mutableStateOf(false) }

    val blackScore = board.flatten().count { it.piece == PieceColor.BLACK }
    val whiteScore = board.flatten().count { it.piece == PieceColor.WHITE }

    // Recursos de imagen
    val arenaBg = R.drawable.icebackground
    val boardSkin = R.drawable.iceboard

    Box(modifier = Modifier.fillMaxSize().background(BgColor)) {
        // 1. Fondo de la Arena
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
            // 2. Botón Abandonar (Estilo Web)
            Spacer(modifier = Modifier.height(48.dp)) // Espacio para bajar todo el contenido

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                OutlinedButton(
                    onClick = { onNavigate("online-game") },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFFCA5A5),
                        containerColor = Color(0xFFF87171).copy(alpha = 0.15f)
                    ),
                    border = BorderStroke(1.dp, Color(0xFFF87171).copy(alpha = 0.35f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(40.dp)
                ) {
                    Text("Abandonar partida", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Indicador de Turno
            StatusTurnBadge(currentTurn, gameOver)

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Info de Jugadores
            Row(modifier = Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) {
                PlayerMiniCard("Jugador", blackScore, PieceColor.BLACK, currentTurn == PieceColor.BLACK)
                PlayerMiniCard("Rival", whiteScore, PieceColor.WHITE, currentTurn == PieceColor.WHITE)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 5. TABLERO
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
                                GameCell(
                                    modifier = Modifier.weight(1f),
                                    cell = board[row][col],
                                    isQuestion = questionCells.contains(row to col),
                                    onClick = { /* Lógica futura */ }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 6. Panel de Habilidades (Justo debajo del tablero)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.Black.copy(alpha = 0.4f),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "HABILIDADES DISPONIBLES",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (blackInventory.isEmpty()) {
                            Text("No tienes habilidades", color = Color.White.copy(0.6f), fontSize = 12.sp)
                        } else {
                            blackInventory.forEach { ability ->
                                AbilityChip(ability) { /* Acción */ }
                            }
                        }
                    }
                }
            }
        }

        if (gameOver) {
            GameOverDialog(blackScore, whiteScore) { onNavigate("online-game") }
        }
    }
}

@Composable
private fun GameCell(
    modifier: Modifier,
    cell: BoardCell,
    isQuestion: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .border(0.5.dp, Color.Black.copy(0.2f))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (isQuestion) {
            Text("?", color = Color(0xFFFBBF24), fontWeight = FontWeight.Black, fontSize = 20.sp)
        }
        if (cell.piece != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize(0.8f)
                    .clip(CircleShape)
                    .background(if (cell.piece == PieceColor.BLACK) Color.Black else Color.White)
                    .then(
                        if (cell.isFixed) Modifier.border(2.dp, Color(0xFFFBBF24), CircleShape)
                        else Modifier
                    )
            )
        }
    }
}

@Composable
private fun StatusTurnBadge(turn: PieceColor, isOver: Boolean) {
    Surface(
        color = if (isOver) Color.DarkGray else if (turn == PieceColor.BLACK) Color.Black else Color.White,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color.White.copy(0.2f))
    ) {
        Text(
            text = if (isOver) "PARTIDA TERMINADA" else "TURNO: ${if (turn == PieceColor.BLACK) "NEGRAS" else "BLANCAS"}",
            color = if (turn == PieceColor.BLACK || isOver) Color.White else Color.Black,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
private fun PlayerMiniCard(name: String, score: Int, color: PieceColor, isActive: Boolean) {
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
                    .background(if (color == PieceColor.BLACK) Color.Black else Color.White)
                    .border(1.dp, Color.White.copy(0.3f), CircleShape)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text("$score pts", color = Color(0xFFFBBF24), fontWeight = FontWeight.Black, fontSize = 14.sp)
        }
    }
}

@Composable
private fun AbilityChip(ability: Ability, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable { onClick() },
        color = Color.White.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
    ) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(20.dp).background(PrimaryColor, RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(ability.icon, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.width(8.dp))
            Text(ability.name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun GameOverDialog(s1: Int, s2: Int, onExit: () -> Unit) {
    AlertDialog(
        onDismissRequest = { },
        containerColor = BgColor,
        title = { Text("Partida Finalizada", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Tú: $s1 pts", color = Color.White)
                Text("Rival: $s2 pts", color = Color.White)
                Spacer(Modifier.height(12.dp))
                Text(if(s1 > s2) "¡VICTORIA! 🎉" else "DERROTA", color = PrimaryColor, fontWeight = FontWeight.Black, fontSize = 18.sp)
            }
        },
        confirmButton = {
            Button(onClick = onExit, colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)) {
                Text("Volver al Menú")
            }
        }
    )
}

private fun createInitialBoard(): List<List<BoardCell>> {
    return List(BOARD_SIZE) { row ->
        MutableList(BOARD_SIZE) { col ->
            when {
                row == 3 && col == 3 -> BoardCell(PieceColor.WHITE)
                row == 3 && col == 4 -> BoardCell(PieceColor.BLACK)
                row == 4 && col == 3 -> BoardCell(PieceColor.BLACK)
                row == 4 && col == 4 -> BoardCell(PieceColor.WHITE)
                else -> BoardCell()
            }
        }
    }
}
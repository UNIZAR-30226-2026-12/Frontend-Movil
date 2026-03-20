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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.random_reversi.R
import com.example.random_reversi.ui.theme.*

// --- Modelos de Datos ---
private enum class PieceColor4P { BLACK, WHITE, RED, BLUE }

private data class BoardCell4P(
    val piece: PieceColor4P? = null,
    val isFixed: Boolean = false
)

private data class Player4P(
    val name: String,
    val score: Int,
    val color: PieceColor4P,
    val rr: Int
)

private val BOARD_SIZE_4P = 16
private val MutedRed = Color(0xFFB71C1C)
private val MutedBlue = Color(0xFF0D47A1)

@Composable
fun GameBoard1v1v1v1Screen(onNavigate: (String) -> Unit) {
    // --- ESTADOS ---
    var board by remember { mutableStateOf(createInitialBoard4P()) }
    var currentTurn by remember { mutableStateOf(PieceColor4P.BLACK) }
    var gameOver by remember { mutableStateOf(false) }

    // Estado para el zoom (null = vista general, 0..3 = cuadrantes)
    var selectedQuadrant by remember { mutableStateOf<Int?>(null) }

    val players = listOf(
        Player4P("Jugador", 4, PieceColor4P.BLACK, 2250),
        Player4P("CyberNinja", 4, PieceColor4P.WHITE, 1420),
        Player4P("NovaMind", 4, PieceColor4P.RED, 1650),
        Player4P("ShadowFox", 4, PieceColor4P.BLUE, 1850)
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

            // 1. Botón Abandonar (Idéntico a 1v1)
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

            // 2. Turno (En español)
            StatusTurnBadge4P(currentTurn, gameOver)

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Marcadores Superiores (P1 y P2)
            Row(modifier = Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) {
                PlayerCard4P(players[0], currentTurn == PieceColor4P.BLACK)
                PlayerCard4P(players[1], currentTurn == PieceColor4P.WHITE)
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 4. TABLERO CON LÓGICA DE ZOOM
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

                if (selectedQuadrant == null) {
                    // VISTA GENERAL (16x16) con selección de cuadrante
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
                                        // Mini tablero 8x8 interno
                                        QuadrantPreview(board, rowQ * 8, colQ * 8)

                                        // Overlay de ayuda visual
                                        Box(
                                            Modifier.fillMaxSize().background(Color.Black.copy(0.1f))
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // VISTA ZOOM (8x8 del cuadrante seleccionado)
                    val startRow = (selectedQuadrant!! / 2) * 8
                    val startCol = (selectedQuadrant!! % 2) * 8

                    Column(Modifier.fillMaxSize()) {
                        for (r in startRow until startRow + 8) {
                            Row(Modifier.weight(1f)) {
                                for (c in startCol until startCol + 8) {
                                    GameCell4P(
                                        modifier = Modifier.weight(1f),
                                        cell = board[r][c],
                                        onClick = {
                                            // Aquí irá la lógica de movimiento
                                            selectedQuadrant = null // Volver tras mover
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Botón para quitar zoom (volver atrás)
                    Box(Modifier.fillMaxSize().padding(8.dp), contentAlignment = Alignment.BottomEnd) {
                        SmallFloatingActionButton(
                            onClick = { selectedQuadrant = null },
                            containerColor = Color.Black.copy(0.6f),
                            contentColor = Color.White,
                            shape = CircleShape,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Text("🔍-", fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 5. Marcadores Inferiores (P3 y P4)
            Row(modifier = Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) {
                PlayerCard4P(players[2], currentTurn == PieceColor4P.RED)
                PlayerCard4P(players[3], currentTurn == PieceColor4P.BLUE)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 6. Panel de Habilidades (Idéntico a 1v1)
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
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        // Estado vacío idéntico a 1v1
                        Text(
                            "No tienes habilidades",
                            color = Color.White.copy(0.6f),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuadrantPreview(board: List<List<BoardCell4P>>, startRow: Int, startCol: Int) {
    Column(Modifier.fillMaxSize().padding(2.dp)) {
        for (r in startRow until startRow + 8) {
            Row(Modifier.weight(1f)) {
                for (c in startCol until startCol + 8) {
                    val cell = board[r][c]
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .border(0.1.dp, Color.White.copy(0.05f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (cell.piece != null) {
                            Box(
                                Modifier
                                    .fillMaxSize(0.6f)
                                    .clip(CircleShape)
                                    .background(getPieceColor(cell.piece))
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GameCell4P(
    modifier: Modifier,
    cell: BoardCell4P,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .border(0.5.dp, Color.Black.copy(0.1f))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (cell.piece != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize(0.65f)
                    .clip(CircleShape)
                    .background(getPieceColor(cell.piece))
                    .then(
                        if (cell.isFixed) Modifier.border(2.dp, Color(0xFFFBBF24), CircleShape)
                        else Modifier
                    )
            )
        }
    }
}

@Composable
private fun StatusTurnBadge4P(turn: PieceColor4P, isOver: Boolean) {
    val colorName = when(turn) {
        PieceColor4P.BLACK -> "NEGRAS"
        PieceColor4P.WHITE -> "BLANCAS"
        PieceColor4P.RED -> "ROJAS"
        PieceColor4P.BLUE -> "AZULES"
    }

    val bgColor = if (isOver) Color.DarkGray else getPieceColor(turn)
    val txtColor = if (turn == PieceColor4P.WHITE) Color.Black else Color.White

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color.White.copy(0.2f))
    ) {
        Text(
            text = if (isOver) "PARTIDA TERMINADA" else "TURNO: $colorName",
            color = txtColor,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
private fun PlayerCard4P(player: Player4P, isActive: Boolean) {
    Surface(
        color = if (isActive) Color.White.copy(0.1f) else Color.Transparent,
        shape = RoundedCornerShape(12.dp),
        border = if (isActive) BorderStroke(1.dp, Color(0xFFFBBF24)) else null,
        modifier = Modifier.width(150.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(getPieceColor(player.color))
                    .border(1.dp, Color.White.copy(0.3f), CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(player.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${player.score} pts", color = Color(0xFFFBBF24), fontWeight = FontWeight.Black, fontSize = 12.sp)
            }
        }
    }
}

private fun getPieceColor(color: PieceColor4P): Color {
    return when(color) {
        PieceColor4P.BLACK -> Color.Black
        PieceColor4P.WHITE -> Color.White
        PieceColor4P.RED -> MutedRed
        PieceColor4P.BLUE -> MutedBlue
    }
}

private fun createInitialBoard4P(): List<List<BoardCell4P>> {
    val board = List(BOARD_SIZE_4P) { MutableList(BOARD_SIZE_4P) { BoardCell4P() } }

    fun placeCluster(r: Int, c: Int, tl: PieceColor4P, tr: PieceColor4P, bl: PieceColor4P, br: PieceColor4P) {
        board[r][c] = BoardCell4P(tl)
        board[r][c + 1] = BoardCell4P(tr)
        board[r + 1][c] = BoardCell4P(bl)
        board[r + 1][c + 1] = BoardCell4P(br)
    }

    placeCluster(3, 3, PieceColor4P.BLACK, PieceColor4P.WHITE, PieceColor4P.RED, PieceColor4P.BLUE)
    placeCluster(3, 11, PieceColor4P.WHITE, PieceColor4P.BLACK, PieceColor4P.BLUE, PieceColor4P.RED)
    placeCluster(11, 3, PieceColor4P.RED, PieceColor4P.BLUE, PieceColor4P.BLACK, PieceColor4P.WHITE)
    placeCluster(11, 11, PieceColor4P.BLUE, PieceColor4P.RED, PieceColor4P.WHITE, PieceColor4P.BLACK)

    return board
}

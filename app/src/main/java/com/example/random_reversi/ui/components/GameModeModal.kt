package com.example.random_reversi.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.random_reversi.ui.theme.*

// Colores de fichas profesionales (aprobados)
private val MutedRed = Color(0xFFB71C1C)
private val MutedBlue = Color(0xFF0D47A1)

@Composable
fun GameModeModal(
    isOpen: Boolean,
    onClose: () -> Unit,
    title: String = "Crear nueva partida",
    subtitle: String = "Elige el modo para tu sala pública",
    onSelectMode: (String) -> Unit
) {
    AppModal(
        isOpen = isOpen,
        onClose = onClose,
        maxWidth = 450.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    fontSize = 14.sp,
                    color = TextMutedColor,
                    textAlign = TextAlign.Center
                )
            }

            // Opciones de modo
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                GameModeCard(
                    title = "1 vs 1",
                    description = "Duelo\n2 jugadores",
                    is16x16 = false,
                    onClick = { onSelectMode("1vs1") }
                )

                GameModeCard(
                    title = "1 vs 1 vs 1 vs 1",
                    description = "Todos contra todos\n4 jugadores",
                    is16x16 = true,
                    onClick = { onSelectMode("1vs1vs1vs1") }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun GameModeCard(
    title: String,
    description: String,
    is16x16: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.05f),
        border = BorderStroke(1.dp, BorderColor)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Contenedor del Tablero (85dp x 85dp fijo)
            Box(
                modifier = Modifier
                    .size(85.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF204D2B))
                    .border(2.dp, Color.Black.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            ) {
                BoardGridPreview(is16x16)
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextColor
                )
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = TextMutedColor,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
private fun BoardGridPreview(is16x16: Boolean) {
    val gridSize = if (is16x16) 16 else 8
    val cellSize = 85.dp / gridSize

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Dibujar Cuadrícula de fondo
        Column {
            repeat(gridSize) {
                Row(modifier = Modifier.weight(1f)) {
                    repeat(gridSize) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .border(0.2.dp, Color.White.copy(alpha = 0.1f))
                        )
                    }
                }
            }
        }

        // 2. Dibujar Piezas y Especiales usando offset (posición exacta)
        if (!is16x16) {
            // MODO 1v1 (Posiciones del frontend web)
            SpecialCell(2, 7, cellSize, 8.sp)
            SpecialCell(6, 2, cellSize, 8.sp)
            SpecialCell(2, 2, cellSize, 8.sp)
            SpecialCell(7, 6, cellSize, 8.sp)

            ReversiPiece(4, 4, Color.White, cellSize)
            ReversiPiece(4, 5, Color.Black, cellSize)
            ReversiPiece(5, 4, Color.Black, cellSize)
            ReversiPiece(5, 5, Color.White, cellSize)
        } else {
            // MODO 1v1v1v1
            SpecialCell(2, 10, cellSize, 5.sp)
            SpecialCell(10, 2, cellSize, 5.sp)
            SpecialCell(15, 12, cellSize, 5.sp)
            SpecialCell(3, 15, cellSize, 5.sp)
            SpecialCell(8, 7, cellSize, 5.sp)

            // Cluster superior izquierda
            ReversiPiece(4, 4, Color.Black, cellSize)
            ReversiPiece(4, 5, Color.White, cellSize)
            ReversiPiece(5, 4, MutedRed, cellSize)
            ReversiPiece(5, 5, MutedBlue, cellSize)

            // Cluster superior derecha
            ReversiPiece(4, 12, Color.White, cellSize)
            ReversiPiece(4, 13, Color.Black, cellSize)
            ReversiPiece(5, 12, MutedBlue, cellSize)
            ReversiPiece(5, 13, MutedRed, cellSize)

            // Cluster inferior izquierda
            ReversiPiece(12, 4, MutedRed, cellSize)
            ReversiPiece(12, 5, MutedBlue, cellSize)
            ReversiPiece(13, 4, Color.Black, cellSize)
            ReversiPiece(13, 5, Color.White, cellSize)

            // Cluster inferior derecha
            ReversiPiece(12, 12, MutedBlue, cellSize)
            ReversiPiece(12, 13, MutedRed, cellSize)
            ReversiPiece(13, 12, Color.White, cellSize)
            ReversiPiece(13, 13, Color.Black, cellSize)
        }
    }
}

@Composable
private fun SpecialCell(row: Int, col: Int, cellSize: androidx.compose.ui.unit.Dp, fontSize: androidx.compose.ui.unit.TextUnit) {
    Box(
        modifier = Modifier
            .size(cellSize)
            .offset(x = cellSize * (col - 1), y = cellSize * (row - 1))
            .background(Color(0xFF6C63FF).copy(alpha = 0.4f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "?",
            color = Color(0xFFFBBF24),
            fontSize = fontSize,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            // 1. Esto quita el margen invisible por defecto de las fuentes en Android
            style = androidx.compose.ui.text.TextStyle(
                platformStyle = androidx.compose.ui.text.PlatformTextStyle(
                    includeFontPadding = false
                )
            ),
            // 2. Esto fuerza a que se centre perfectamente sin importar el tamaño de la caja azul
            modifier = Modifier.wrapContentSize(Alignment.Center, unbounded = true)
        )
    }
}

@Composable
private fun ReversiPiece(row: Int, col: Int, color: Color, cellSize: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .size(cellSize)
            .offset(x = cellSize * (col - 1), y = cellSize * (row - 1)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize(0.85f)
                .clip(CircleShape)
                .background(color)
                .border(0.2.dp, Color.Black.copy(alpha = 0.2f), CircleShape)
        )
    }
}
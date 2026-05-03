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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun GameModeModal(
    isOpen: Boolean,
    onClose: () -> Unit,
    title: String = "Seleccionar modo de juego",
    subtitle: String = "Elige que modo quieres jugar",
    onSelectMode: (String) -> Unit
) {
    if (!isOpen) return

    var step by remember(isOpen) { mutableStateOf("mode") }
    var selectedMode by remember(isOpen) { mutableStateOf<String?>(null) }

    val handleClose: () -> Unit = {
        step = "mode"
        selectedMode = null
        onClose()
    }

    Dialog(
        onDismissRequest = handleClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        // Envoltorio estilo popup-surface del front web
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .widthIn(max = 450.dp)
                .background(Color(0xFFF7F1E5), RoundedCornerShape(16.dp))
                .border(2.dp, Color(0xFF2F2418), RoundedCornerShape(16.dp))
                .padding(24.dp)
        ) {
            // Botón cerrar (X)
            Text(
                text = "✖",
                color = Color(0xFF4D3F31).copy(alpha = 0.6f),
                fontSize = 16.sp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 8.dp, y = (-8).dp)
                    .clickable(onClick = handleClose)
                    .padding(8.dp)
            )

            if (step == "variant") {
                Text(
                    text = "← Atrás",
                    color = Color(0xFF4D3F31),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = (-8).dp, y = (-8).dp)
                        .clickable {
                            step = "mode"
                            selectedMode = null
                        }
                        .padding(8.dp)
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Dynamic header based on step
                val displayTitle = if (step == "variant") {
                    if (selectedMode == "1vs1") "1 vs 1 · Elige variante" else "1 vs 1 vs 1 vs 1 · Elige variante"
                } else title

                val displaySubtitle = if (step == "variant") {
                    "Selecciona si quieres jugar con o sin habilidades especiales"
                } else subtitle

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = displayTitle,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF1F1711),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = displaySubtitle,
                        fontSize = 15.sp,
                        color = Color(0xFF4D3F31),
                        textAlign = TextAlign.Center
                    )
                }

                // Conditional content based on step
                if (step == "mode") {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        GameModeCard(
                            title = "1 vs 1",
                            description = "Duelo de 2 jugadores",
                            is16x16 = false,
                            onClick = {
                                selectedMode = "1vs1"
                                step = "variant"
                            }
                        )
                        GameModeCard(
                            title = "1 vs 1 vs 1 vs 1",
                            description = "Todos contra todos de 4 jugadores",
                            is16x16 = true,
                            onClick = {
                                selectedMode = "1vs1vs1vs1"
                                step = "variant"
                            }
                        )
                    }
                } else {
                    val is16x16 = selectedMode == "1vs1vs1vs1"
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        GameModeCard(
                            title = "Clásico",
                            description = "Juego sin habilidades especiales",
                            is16x16 = is16x16,
                            showSpecialCells = false,
                            onClick = {
                                val mode = checkNotNull(selectedMode) { "selectedMode must not be null in variant step" }
                                onSelectMode(mode)
                            }
                        )
                        GameModeCard(
                            title = "Con Habilidades",
                            description = "Juego con habilidades especiales",
                            is16x16 = is16x16,
                            showSpecialCells = true,
                            onClick = {
                                val mode = checkNotNull(selectedMode) { "selectedMode must not be null in variant step" }
                                onSelectMode("${mode}_skills")
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GameModeCard(
    title: String,
    description: String,
    is16x16: Boolean,
    showSpecialCells: Boolean = true,
    onClick: () -> Unit
) {
    // Estilo exacto del .game-modal__option
    val gradientBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFFFFFFFF).copy(alpha = 0.68f),
            Color(0xFFEBD8B7).copy(alpha = 0.82f)
        ),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(gradientBrush)
            .border(2.dp, Color(0xFF2F2418), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Contenedor del Tablero (estilo game-modal__option-icon web)
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF204D2B))
                    .border(3.dp, Color(0xFF070F0A).copy(alpha = 0.44f), RoundedCornerShape(10.dp))
                    .padding(3.dp)
            ) {
                BoardGridPreview(is16x16, showSpecialCells)
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF1F1711)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    fontSize = 13.sp,
                    color = Color(0xFF4F443A),
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
private fun BoardGridPreview(is16x16: Boolean, showSpecialCells: Boolean = true) {
    val gridSize = if (is16x16) 16 else 8
    val cellSize = 70.dp / gridSize

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
                                .border(0.2.dp, Color.White.copy(alpha = 0.15f))
                        )
                    }
                }
            }
        }

        // 2. Dibujar Piezas y Especiales usando offset
        if (!is16x16) {
            if (showSpecialCells) {
                SpecialCell(2, 7, cellSize, 8.sp)
                SpecialCell(6, 2, cellSize, 8.sp)
                SpecialCell(2, 2, cellSize, 8.sp)
                SpecialCell(7, 6, cellSize, 8.sp)
            }

            ReversiPiece(4, 4, "white", cellSize)
            ReversiPiece(4, 5, "black", cellSize)
            ReversiPiece(5, 4, "black", cellSize)
            ReversiPiece(5, 5, "white", cellSize)
        } else {
            if (showSpecialCells) {
                SpecialCell(2, 10, cellSize, 5.sp)
                SpecialCell(10, 2, cellSize, 5.sp)
                SpecialCell(15, 12, cellSize, 5.sp)
                SpecialCell(3, 15, cellSize, 5.sp)
                SpecialCell(8, 7, cellSize, 5.sp)
            }

            // Superior izquierda
            ReversiPiece(4, 4, "black", cellSize)
            ReversiPiece(4, 5, "white", cellSize)
            ReversiPiece(5, 4, "red", cellSize)
            ReversiPiece(5, 5, "blue", cellSize)

            // Superior derecha
            ReversiPiece(4, 12, "white", cellSize)
            ReversiPiece(4, 13, "black", cellSize)
            ReversiPiece(5, 12, "blue", cellSize)
            ReversiPiece(5, 13, "red", cellSize)

            // Inferior izquierda
            ReversiPiece(12, 4, "red", cellSize)
            ReversiPiece(12, 5, "blue", cellSize)
            ReversiPiece(13, 4, "black", cellSize)
            ReversiPiece(13, 5, "white", cellSize)

            // Inferior derecha
            ReversiPiece(12, 12, "blue", cellSize)
            ReversiPiece(12, 13, "red", cellSize)
            ReversiPiece(13, 12, "white", cellSize)
            ReversiPiece(13, 13, "black", cellSize)
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
            style = androidx.compose.ui.text.TextStyle(
                platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false)
            ),
            modifier = Modifier.wrapContentSize(Alignment.Center, unbounded = true)
        )
    }
}

@Composable
private fun ReversiPiece(row: Int, col: Int, colorType: String, cellSize: androidx.compose.ui.unit.Dp) {
    val brush = when(colorType) {
        "white" -> Brush.radialGradient(listOf(Color(0xFFFFFFFF), Color(0xFFD0D0D0)))
        "black" -> Brush.radialGradient(listOf(Color(0xFF444444), Color(0xFF000000)))
        "red"   -> Brush.radialGradient(listOf(Color(0xFFFF5555), Color(0xFF880000)))
        "blue"  -> Brush.radialGradient(listOf(Color(0xFF55AAFF), Color(0xFF0033AA)))
        else    -> Brush.radialGradient(listOf(Color.Gray, Color.DarkGray))
    }

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
                .background(brush)
                .border(0.5.dp, Color.Black.copy(alpha = 0.3f), CircleShape)
        )
    }
}
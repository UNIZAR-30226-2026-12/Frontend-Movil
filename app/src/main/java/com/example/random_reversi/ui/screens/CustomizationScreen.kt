package com.example.random_reversi.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.random_reversi.ui.theme.*

private data class CustomChip(
    val emoji: String,
    val startXFraction: Float,
    val startYFraction: Float,
    val durationMs: Int,
    val delayMs: Int,
    val isQuestion: Boolean = false
)

private val customChips = listOf(
    CustomChip("⚫", 0.1f, 0.1f, 3000, 0),
    CustomChip("⚪", 0.85f, 0.15f, 3200, 200),
    CustomChip("🔴", 0.2f, 0.45f, 2800, 400),
    CustomChip("🔵", 0.8f, 0.5f, 3100, 600),
    CustomChip("🟢", 0.15f, 0.75f, 2900, 800),
    CustomChip("🟡", 0.9f, 0.8f, 3300, 1000),
    CustomChip("🟣", 0.3f, 0.9f, 3000, 1200),
    CustomChip("🟠", 0.75f, 0.3f, 2700, 1400),
    CustomChip("❓", 0.5f, 0.2f, 3100, 0, true),
    CustomChip("❓", 0.6f, 0.7f, 2900, 500, true),
)

private data class PieceStyle(val sideA: Color, val sideB: Color, val label: String)
private data class BoardColor(val color: Color, val label: String)
private data class AvatarOption(val emoji: String, val label: String)

private val PIECE_STYLES = listOf(
    PieceStyle(Color(0xFF222222), Color(0xFFeeeeee), "Clásico"),
    PieceStyle(Color(0xFFe74c3c), Color(0xFF3498db), "Fuego y Hielo"),
    PieceStyle(Color(0xFF2ecc71), Color(0xFFf1c40f), "Selva"),
    PieceStyle(Color(0xFF9b59b6), Color(0xFFe67e22), "Atardecer"),
    PieceStyle(Color(0xFF1abc9c), Color(0xFFe84393), "Neón"),
)

private val BOARD_COLORS = listOf(
    BoardColor(Color(0xFF2d6a4f), "Verde"),
    BoardColor(Color(0xFF2654a1), "Azul"),
    BoardColor(Color(0xFF6b4226), "Madera"),
    BoardColor(Color(0xFF2c2c3e), "Oscuro"),
    BoardColor(Color(0xFF5b2d8e), "Púrpura"),
)

private val AVATARS = listOf(
    AvatarOption("🟣", "Purple Sun"),
    AvatarOption("🔵", "Blue Fire"),
    AvatarOption("⚪", "White Grass"),
    AvatarOption("⚫", "Black Ice"),
)

@Composable
private fun AnimatedCustomChip(
    chip: CustomChip,
    screenHeight: Dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "custom_chip_${chip.emoji}")
    val yOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(
            animation = tween(chip.durationMs, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(chip.delayMs)
        ),
        label = "yOffset"
    )

    val yPosition = screenHeight * chip.startYFraction

    Text(
        text = chip.emoji,
        fontSize = if (chip.isQuestion) 28.sp else 32.sp,
        modifier = Modifier
            .offset(
                x = (280.dp) * chip.startXFraction,
                y = yPosition + yOffset.dp
            )
            .alpha(0.3f)
    )
}

@Composable
fun CustomizationScreen(
    onNavigate: (screen: String) -> Unit
) {
    var selectedPiece by remember { mutableStateOf(0) }
    var selectedBoard by remember { mutableStateOf(0) }
    var selectedAvatar by remember { mutableStateOf(0) }
    var username by remember { mutableStateOf("Jugador") }
    var isEditingName by remember { mutableStateOf(false) }
    var editNameValue by remember { mutableStateOf(username) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
    ) {
        val screenHeight = maxHeight

        // Fondo animado
        repeat(customChips.size) { index ->
            AnimatedCustomChip(chip = customChips[index], screenHeight = screenHeight)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp), // Aumentamos padding lateral para que no toque bordes
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Header más compacto
            Text(
                text = "Personalización",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextColor
            )
            Text(
                text = "Define tu estilo",
                fontSize = 13.sp,
                color = TextMutedColor,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // CONTENEDOR DE SECCIONES
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // SECCIÓN 1: PERFIL
                Section(title = "Perfil") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Preview Avatar
                        Surface(
                            modifier = Modifier.size(64.dp),
                            shape = CircleShape,
                            color = SurfaceLightColor,
                            border = BorderStroke(2.dp, BorderColor)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(text = AVATARS[selectedAvatar].emoji, fontSize = 36.sp)
                            }
                        }

                        // Selector de avatares
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AVATARS.forEachIndexed { index, avatar ->
                                AvatarOptionButton(
                                    emoji = avatar.emoji,
                                    isSelected = selectedAvatar == index,
                                    onClick = { selectedAvatar = index }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Nombre de usuario optimizado
                    Column {
                        Text("NOMBRE", fontSize = 10.sp, color = TextMutedColor, fontWeight = FontWeight.Bold)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (isEditingName) {
                                TextField(
                                    value = editNameValue,
                                    onValueChange = { editNameValue = it },
                                    modifier = Modifier.weight(1f).height(45.dp),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Black.copy(0.1f),
                                        unfocusedContainerColor = Color.Black.copy(0.1f),
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                        focusedTextColor = TextColor,
                                        unfocusedTextColor = TextColor
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    singleLine = true
                                )
                                Button(
                                    onClick = { username = editNameValue; isEditingName = false },
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                    modifier = Modifier.height(40.dp)
                                ) { Text("OK") }
                            } else {
                                Text(text = username, fontSize = 16.sp, color = TextColor, modifier = Modifier.weight(1f))
                                TextButton(onClick = { isEditingName = true; editNameValue = username }) {
                                    Text("Editar ✏️", fontSize = 12.sp, color = PrimaryColor)
                                }
                            }
                        }
                    }
                }

                // SECCIÓN 2: FICHAS Y TABLERO
                Section(title = "Apariencia de Juego") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Preview Tablero escalada
                        BoardPreview(
                            selectedPiece = selectedPiece,
                            selectedBoard = selectedBoard,
                            modifier = Modifier.size(100.dp) // Reducido para ahorrar espacio
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            ColorSelector(
                                label = "Fichas",
                                options = PIECE_STYLES.mapIndexed { i, s -> i to Pair(s.sideA, s.sideB) },
                                selectedIndex = selectedPiece,
                                isPair = true,
                                onSelect = { selectedPiece = it }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            ColorSelector(
                                label = "Tablero",
                                options = BOARD_COLORS.mapIndexed { i, b -> i to Pair(b.color, b.color) },
                                selectedIndex = selectedBoard,
                                isPair = false,
                                onSelect = { selectedBoard = it }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Botón de Volver
            Button(
                onClick = { onNavigate("menu") },
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Volver al menú", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Section(
    modifier: Modifier = Modifier,
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = SurfaceColor,
        border = BorderStroke(1.dp, BorderColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextColor
            )
            content()
        }
    }
}

@Composable
private fun AvatarOptionButton(emoji: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.size(44.dp).clickable(onClick = onClick),
        shape = CircleShape,
        color = SurfaceLightColor,
        border = BorderStroke(2.dp, if (isSelected) PrimaryColor else BorderColor)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text = emoji, fontSize = 20.sp)
        }
    }
}

@Composable
private fun BoardPreview(
    selectedPiece: Int,
    selectedBoard: Int,
    modifier: Modifier = Modifier
) {
    val boardColor = BOARD_COLORS[selectedBoard].color
    val pieceStyle = PIECE_STYLES[selectedPiece]

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = boardColor
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp)
        ) {
            // Grid 4x4
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                repeat(4) { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        repeat(4) { col ->
                            val isSideA = (row == 1 && col == 1) || (row == 2 && col == 2)
                            val isSideB = (row == 1 && col == 2) || (row == 2 && col == 1)

                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(1f)
                                    .background(
                                        Color.Black.copy(alpha = 0.15f),
                                        RoundedCornerShape(2.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSideA) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize(0.7f)
                                            .clip(CircleShape)
                                            .background(pieceStyle.sideA)
                                    )
                                } else if (isSideB) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize(0.7f)
                                            .clip(CircleShape)
                                            .background(pieceStyle.sideB)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorSelector(
    label: String,
    options: List<Pair<Int, Pair<Color, Color>>>,
    selectedIndex: Int,
    isPair: Boolean,
    onSelect: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = TextMutedColor,
            fontWeight = FontWeight.SemiBold
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { (index, colors) ->
                ColorOptionButton(
                    color1 = colors.first,
                    color2 = colors.second,
                    isPair = isPair,
                    isSelected = selectedIndex == index,
                    onClick = { onSelect(index) }
                )
            }
        }
    }
}

@Composable
private fun ColorOptionButton(
    color1: Color,
    color2: Color,
    isPair: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .size(44.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(
            2.dp,
            if (isSelected) PrimaryColor else BorderColor
        ),
        shadowElevation = if (isSelected) 8.dp else 0.dp
    ) {
        if (isPair) {
            Row(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
                    .background(color1))
                Box(modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
                    .background(color2))
            }
        } else {
            Box(modifier = Modifier
                .fillMaxSize()
                .background(color1))
        }
    }
}

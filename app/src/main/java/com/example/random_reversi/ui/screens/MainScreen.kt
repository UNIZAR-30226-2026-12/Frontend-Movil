package com.example.random_reversi.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.random_reversi.data.UserProfileStore
import com.example.random_reversi.ui.components.AppModal
import com.example.random_reversi.ui.components.GameModeModal
import com.example.random_reversi.ui.theme.*
import com.example.random_reversi.utils.AvatarPresets

private data class FloatingChip(
    val emoji: String,
    val startXFraction: Float,
    val startYFraction: Float,
    val durationMs: Int,
    val delayMs: Int,
    val isQuestion: Boolean = false
)

private val floatingChips = listOf(
    FloatingChip("⚫", 0.1f, 0.1f, 3000, 0),
    FloatingChip("⚪", 0.85f, 0.15f, 3200, 200),
    FloatingChip("🔴", 0.2f, 0.45f, 2800, 400),
    FloatingChip("🔵", 0.8f, 0.5f, 3100, 600),
    FloatingChip("🟢", 0.15f, 0.75f, 2900, 800),
    FloatingChip("🟡", 0.9f, 0.8f, 3300, 1000),
    FloatingChip("🟣", 0.3f, 0.9f, 3000, 1200),
    FloatingChip("🟠", 0.75f, 0.3f, 2700, 1400),
    FloatingChip("❓", 0.5f, 0.2f, 3100, 0, true),
    FloatingChip("❓", 0.6f, 0.7f, 2900, 500, true),
)

@Composable
private fun AnimatedFloatingChip(
    chip: FloatingChip,
    screenHeight: Dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "chip_${chip.emoji}")
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
fun MainScreen(
    onNavigate: (screen: String) -> Unit
) {
    var showGameModeModal by remember { mutableStateOf(false) }
    val profile by UserProfileStore.state.collectAsState()
    val userName = profile.username.ifBlank { "Jugador" }

    LaunchedEffect(Unit) {
        UserProfileStore.refreshFromBackend()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
    ) {
        // Fondo con fichas flotantes
        BoxWithConstraints {
            repeat(floatingChips.size) { index ->
                AnimatedFloatingChip(
                    chip = floatingChips[index],
                    screenHeight = maxHeight
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(120.dp))

            // Título
            Column(
                modifier = Modifier
                    .padding(horizontal = 32.dp)
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Random",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = PrimaryColor,
                    lineHeight = 52.sp
                )
                Text(
                    text = "Reversi",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextColor,
                    lineHeight = 52.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "¿Qué te apetece hacer hoy?",
                    fontSize = 16.sp,
                    color = TextMutedColor
                )
            }

            // Tarjetas de opciones
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(0.9f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Fila 1
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MenuCard(
                        icon = "🌐",
                        title = "Jugar Online",
                        description = "Compite contra otros jugadores",
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate("online-game") }
                    )
                    MenuCard(
                        icon = "🤖",
                        title = "Jugar contra IA",
                        description = "Pon a prueba tu estrategia",
                        modifier = Modifier.weight(1f),
                        onClick = { showGameModeModal = true }
                    )
                }

                // Fila 2
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MenuCard(
                        icon = "🎨",
                        title = "Personalización",
                        description = "Personaliza tu perfil",
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate("customization") }
                    )
                    MenuCard(
                        icon = "👥",
                        title = "Amigos",
                        description = "Gestiona tus amigos",
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate("friends") }
                    )
                }

                MenuCard(
                    icon = "📘",
                    title = "Reglas",
                    description = "Aprende a jugar",
                    modifier = Modifier.fillMaxWidth(),
                    isWide = true,
                    onClick = { onNavigate("rules") }
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Footer
            Text(
                text = "HuQ Games Studio · Universidad de Zaragoza",
                fontSize = 12.sp,
                color = TextMutedColor,
                modifier = Modifier.alpha(0.7f)
            )

            Spacer(modifier = Modifier.height(32.dp))
        }

        // Barra de usuario
        UserBar(
            userName = userName,
            avatarUrl = profile.avatarUrl,
            onProfile = { onNavigate("profile") },
            onLogout = { onNavigate("home") },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 48.dp, end = 12.dp)
        )
    }

    GameModeModal(
        isOpen = showGameModeModal,
        onClose = { showGameModeModal = false },
        onSelectMode = { mode ->
            showGameModeModal = false
        }
    )
}

@Composable
private fun MenuCard(
    icon: String,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    isWide: Boolean = false, // Parámetro para definir si es ancho
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .then(if (isWide) Modifier.fillMaxWidth() else Modifier.aspectRatio(1f))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = SurfaceColor,
        border = BorderStroke(1.dp, BorderColor)
    ) {
        if (isWide) {
            // Diseño horizontal para el botón ancho
            Row(
                modifier = Modifier
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(text = icon, fontSize = 32.sp)
                Column {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextColor
                    )
                    Text(
                        text = description,
                        fontSize = 12.sp,
                        color = TextMutedColor
                    )
                }
            }
        } else {
            // Diseño cuadrado original
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(text = icon, fontSize = 36.sp, modifier = Modifier.padding(bottom = 8.dp))
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextColor,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = description,
                    fontSize = 11.sp,
                    color = TextMutedColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun UserBar(
    userName: String,
    avatarUrl: String?,
    onProfile: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = Color.Black.copy(alpha = 0.3f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .padding(end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .clickable(onClick = onProfile),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape),
                    color = PrimaryColor,
                    shape = CircleShape
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        val presetRes = AvatarPresets.drawableForId(avatarUrl)
                        when {
                            presetRes != null -> {
                                androidx.compose.foundation.Image(
                                    painter = androidx.compose.ui.res.painterResource(id = presetRes),
                                    contentDescription = "Avatar",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            }
                            !avatarUrl.isNullOrBlank() -> {
                                AsyncImage(
                                    model = avatarUrl,
                                    contentDescription = "Avatar",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            }
                            else -> {
                                Text(
                                    text = userName.firstOrNull()?.uppercaseChar()?.toString() ?: "J",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }

                Text(
                    text = userName,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(end = 4.dp)
                )
            }

            Box(
                modifier = Modifier
                    .height(20.dp)
                    .width(1.dp)
                    .background(Color.White.copy(alpha = 0.1f))
            )

            Button(
                onClick = onLogout,
                modifier = Modifier
                    .padding(start = 4.dp)
                    .height(32.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xF8, 0x71, 0x71).copy(alpha = 0.15f)
                ),
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Cerrar Sesión",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xF8, 0x71, 0x71)
                )
            }
        }
    }
}

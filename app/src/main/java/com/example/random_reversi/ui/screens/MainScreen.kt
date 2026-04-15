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
import com.example.random_reversi.ui.components.GameModeModal
import com.example.random_reversi.ui.theme.*
import com.example.random_reversi.utils.AvatarPresets

private data class FloatingChip(
    val emoji: String, val startXFraction: Float, val startYFraction: Float,
    val durationMs: Int, val delayMs: Int, val isQuestion: Boolean = false
)

private val floatingChips = listOf(
    FloatingChip("⚫", 0.1f, 0.1f, 3000, 0), FloatingChip("⚪", 0.85f, 0.15f, 3200, 200),
    FloatingChip("🔴", 0.2f, 0.45f, 2800, 400), FloatingChip("🔵", 0.8f, 0.5f, 3100, 600),
    FloatingChip("🟢", 0.15f, 0.75f, 2900, 800), FloatingChip("🟡", 0.9f, 0.8f, 3300, 1000),
    FloatingChip("🟣", 0.3f, 0.9f, 3000, 1200), FloatingChip("🟠", 0.75f, 0.3f, 2700, 1400),
    FloatingChip("❓", 0.5f, 0.2f, 3100, 0, true), FloatingChip("❓", 0.6f, 0.7f, 2900, 500, true),
)

@Composable
private fun AnimatedFloatingChip(chip: FloatingChip, screenHeight: Dp) {
    val infiniteTransition = rememberInfiniteTransition(label = "chip_${chip.emoji}")
    val yOffset by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 20f,
        animationSpec = infiniteRepeatable(
            animation = tween(chip.durationMs, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse, initialStartOffset = StartOffset(chip.delayMs)
        ), label = "yOffset"
    )
    Text(
        text = chip.emoji, fontSize = if (chip.isQuestion) 28.sp else 32.sp,
        modifier = Modifier.offset(x = (280.dp) * chip.startXFraction, y = screenHeight * chip.startYFraction + yOffset.dp).alpha(0.3f)
    )
}

@Composable
fun MainScreen(onNavigate: (screen: String) -> Unit) {
    var showGameModeModal by remember { mutableStateOf(false) }
    val profile by UserProfileStore.state.collectAsState()
    val userName = profile.username.ifBlank { "Jugador" }

    LaunchedEffect(Unit) { UserProfileStore.refreshFromBackend() }

    Box(modifier = Modifier.fillMaxSize().background(BgColor)) {
        // Fondo con fichas animadas
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            repeat(floatingChips.size) { index -> AnimatedFloatingChip(floatingChips[index], maxHeight) }
        }

        // --- TOP BAR ---
        Row(
            modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter).padding(top = 40.dp, start = 16.dp, end = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // Arriba Izquierda: Reglas
            HudButton(icon = "📘", text = "Reglas", onClick = { onNavigate("rules") })

            // Arriba Derecha: Usuario
            UserBar(
                userName = userName, avatarUrl = profile.avatarUrl,
                onProfile = { onNavigate("profile") }, onLogout = { onNavigate("home") }
            )
        }

        // --- CENTRO: Título y Todos los Botones ---
        Column(
            // Utilizamos un offset negativo en Y para subir todo este bloque hacia arriba
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-30).dp)
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(bottom = 24.dp)) {
                Text("Random", fontSize = 48.sp, fontWeight = FontWeight.ExtraBold, color = PrimaryColor, lineHeight = 52.sp)
                Text("Reversi", fontSize = 48.sp, fontWeight = FontWeight.ExtraBold, color = TextColor, lineHeight = 52.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text("¿Qué te apetece hacer hoy?", fontSize = 15.sp, color = TextMutedColor)
            }

            // Fila 1: Online | Ranking
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MenuCard(icon = "🌐", title = "Jugar Online", description = "Compite contra otros", modifier = Modifier.weight(1f)) { onNavigate("online-game") }
                MenuCard(icon = "🏆", title = "Ranking Global", description = "Consulta el top RR", modifier = Modifier.weight(1f)) { onNavigate("ranking") }
            }
            Spacer(modifier = Modifier.height(12.dp))
            // Fila 2: IA | Personalización
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MenuCard(icon = "🤖", title = "Contra IA", description = "Prueba tu estrategia", modifier = Modifier.weight(1f)) { showGameModeModal = true }
                MenuCard(icon = "🎨", title = "Estilos", description = "Personaliza tu perfil", modifier = Modifier.weight(1f)) { onNavigate("customization") }
            }
            Spacer(modifier = Modifier.height(12.dp))
            // Fila 3: Amigos (Debajo del resto)
            MenuCard(icon = "👥", title = "Amigos", description = "Gestiona tu lista de amigos", modifier = Modifier.fillMaxWidth(), isWide = true) { onNavigate("friends") }
        }

        // --- ABAJO DEL TODO: Footer ---
        Text(
            text = "HuQ Games Studio · Universidad de Zaragoza",
            fontSize = 11.sp,
            color = TextMutedColor,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
                .alpha(0.7f)
        )
    }

    GameModeModal(
        isOpen = showGameModeModal, onClose = { showGameModeModal = false },
        onSelectMode = { mode -> showGameModeModal = false }
    )
}

// Botón estilo Glassmorphism (similar al web)
@Composable
private fun HudButton(icon: String, text: String, onClick: () -> Unit) {
    Surface(
        color = Color.White.copy(alpha = 0.1f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = icon, fontSize = 16.sp)
            Text(text = text, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun MenuCard(icon: String, title: String, description: String, modifier: Modifier = Modifier, isWide: Boolean = false, onClick: () -> Unit) {
    Surface(
        modifier = modifier.then(if (!isWide) Modifier.aspectRatio(1f) else Modifier).clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp), color = SurfaceColor, border = BorderStroke(1.dp, BorderColor)
    ) {
        if (isWide) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(text = icon, fontSize = 32.sp)
                Column {
                    Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextColor)
                    Text(text = description, fontSize = 12.sp, color = TextMutedColor)
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text(text = icon, fontSize = 36.sp, modifier = Modifier.padding(bottom = 8.dp))
                Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextColor, textAlign = TextAlign.Center)
                Text(text = description, fontSize = 11.sp, color = TextMutedColor, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

@Composable
private fun UserBar(userName: String, avatarUrl: String?, onProfile: () -> Unit, onLogout: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(50), color = Color.Black.copy(alpha = 0.3f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(6.dp).padding(end = 6.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.clickable(onClick = onProfile),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    modifier = Modifier.size(30.dp).clip(CircleShape).border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape),
                    color = PrimaryColor, shape = CircleShape
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        val presetRes = AvatarPresets.drawableForId(avatarUrl)
                        when {
                            presetRes != null -> androidx.compose.foundation.Image(painter = androidx.compose.ui.res.painterResource(id = presetRes), contentDescription = null, contentScale = androidx.compose.ui.layout.ContentScale.Crop)
                            !avatarUrl.isNullOrBlank() -> AsyncImage(model = avatarUrl, contentDescription = null, contentScale = androidx.compose.ui.layout.ContentScale.Crop)
                            else -> Text(text = userName.firstOrNull()?.uppercaseChar()?.toString() ?: "J", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
                Text(text = userName, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
            Box(modifier = Modifier.height(18.dp).width(1.dp).background(Color.White.copy(alpha = 0.1f)))
            Button(
                onClick = onLogout, modifier = Modifier.height(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xF8, 0x71, 0x71).copy(alpha = 0.15f)),
                shape = RoundedCornerShape(20.dp), contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
            ) {
                Text(text = "Salir", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xF8, 0x71, 0x71))
            }
        }
    }
}
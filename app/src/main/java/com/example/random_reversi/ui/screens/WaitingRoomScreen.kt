package com.example.random_reversi.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.random_reversi.data.UserProfileStore
import com.example.random_reversi.ui.theme.*
import com.example.random_reversi.utils.AvatarPresets
import kotlinx.coroutines.delay

// --- Modelos de Datos ---
private data class Player(
    val id: Int,
    val name: String,
    val rr: Int,
    var isReady: Boolean = false,
    val streak: List<String> = listOf("V", "D", "V", "V", "D") // Mock de racha
)

@Composable
fun WaitingRoomScreen(
    gameMode: String = "1vs1",
    onNavigate: (String) -> Unit
) {
    val profile by UserProfileStore.state.collectAsState()
    val maxPlayers = if (gameMode == "1vs1") 2 else 4
    val localPlayerName = profile.username.ifBlank { "Jugador" }

    LaunchedEffect(Unit) {
        UserProfileStore.refreshFromBackend()
    }

    // Estado de la lista de jugadores
    var players by remember {
        mutableStateOf(listOf(Player(1, localPlayerName, 1500, false)))
    }

    val isFull = players.size == maxPlayers
    val allReady = isFull && players.all { it.isReady }

    // Simulación: Oponentes se unen a los 3 segundos
    LaunchedEffect(Unit) {
        delay(3000)
        if (gameMode == "1vs1") {
            players = players + Player(2, "CyberNinja", 1420, true)
        } else {
            players = players + listOf(
                Player(2, "CyberNinja", 1420, true),
                Player(3, "ReversiMaster", 1850, true),
                Player(4, "Gamer_Pro", 1600, true)
            )
        }
    }

    // Navegación automática cuando todos están listos
    LaunchedEffect(allReady) {
        if (allReady) {
            delay(1500)
            onNavigate("game-$gameMode")
        }
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .background(BgColor)) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Header
            Text(
                text = "Sala de Espera",
                style = TextStyle(
                    brush = Brush.verticalGradient(listOf(Color.White, Color(0xFFA78BFA))),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                color = PrimaryColor.copy(alpha = 0.15f),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, PrimaryColor.copy(alpha = 0.3f))
            ) {
                Text(
                    text = gameMode,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    color = Color(0xFFA78BFA),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Animación 3D de la ficha central
            FlippingChip3D()

            Spacer(modifier = Modifier.height(20.dp))

            // Texto de estado pulsante
            val infiniteTransition = rememberInfiniteTransition(label = "")
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.4f, targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse), label = ""
            )

            Text(
                text = if (allReady) "INICIANDO PARTIDA..." else if (isFull) "SALA LLENA" else "ESPERANDO JUGADORES...",
                color = TextMutedColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp,
                modifier = Modifier.alpha(alpha)
            )

            Spacer(modifier = Modifier.height(40.dp))

            // --- CAMBIO APLICADO AQUÍ (Lógica Condicional de Diseño) ---
            if (gameMode == "1vs1") {
                // Diseño de Fila Clásica para 2 Jugadores
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.Top
                ) {
                    players.forEach { player ->
                        PlayerSlot(
                            player,
                            isLocal = player.name == localPlayerName,
                            localAvatarUrl = profile.avatarUrl
                        )
                        if (players.size > 1 && player != players.last()) Spacer(modifier = Modifier.width(20.dp))
                    }

                    // Slots vacíos
                    repeat(maxPlayers - players.size) {
                        Spacer(modifier = Modifier.width(20.dp))
                        EmptySlot()
                    }
                }
            } else {
                // Diseño de Cuadrícula 2x2 para 4 Jugadores
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Fila Superior
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        if (players.size > 0) PlayerSlot(players[0], isLocal = players[0].name == localPlayerName, localAvatarUrl = profile.avatarUrl) else EmptySlot()
                        if (players.size > 1) PlayerSlot(players[1], isLocal = players[1].name == localPlayerName, localAvatarUrl = profile.avatarUrl) else EmptySlot()
                    }
                    // Fila Inferior
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        if (players.size > 2) PlayerSlot(players[2], isLocal = players[2].name == localPlayerName, localAvatarUrl = profile.avatarUrl) else EmptySlot()
                        if (players.size > 3) PlayerSlot(players[3], isLocal = players[3].name == localPlayerName, localAvatarUrl = profile.avatarUrl) else EmptySlot()
                    }
                }
            }

            Spacer(modifier = Modifier.height(60.dp))

            // Acciones
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Botón Abandonar
                val backDestination = when (gameMode) {
                    "1vs1", "1vs1vs1vs1" -> "online-game" // Si es online, vuelve a la lista online
                    else -> "menu"                        // Si es IA u otro, vuelve al menú principal
                }

                Button(
                    onClick = { onNavigate(backDestination) },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF87171).copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFF87171).copy(alpha = 0.2f))
                ) {
                    Text("Abandonar", color = Color(0xFFF87171), fontWeight = FontWeight.Bold)
                }

                // Botón Listo
                val isLocalReady = players.find { it.name == localPlayerName }?.isReady ?: false
                Button(
                    onClick = {
                        players = players.map {
                            if (it.name == localPlayerName) it.copy(isReady = !it.isReady) else it
                        }
                    },
                    enabled = isFull,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isLocalReady) Color(0xFF4ADE80) else PrimaryColor,
                        disabledContainerColor = Color.Gray.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (isLocalReady) "¡Listo!" else "Estoy Listo", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun FlippingChip3D() {
    val infiniteTransition = rememberInfiniteTransition(label = "")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = ""
    )

    Box(
        modifier = Modifier
            .size(100.dp)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            },
        contentAlignment = Alignment.Center
    ) {
        // Cara Blanca (se muestra de 90 a 270 grados)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = if (rotation % 360 in 90f..270f) 1f else 0f }
                .background(Color.White, CircleShape)
                .border(4.dp, Color.Gray.copy(alpha = 0.2f), CircleShape)
        )
        // Cara Negra (se muestra el resto del tiempo)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = if (rotation % 360 in 90f..270f) 0f else 1f }
                .background(Color.Black, CircleShape)
                .border(4.dp, Color.White.copy(alpha = 0.2f), CircleShape)
        )
    }
}

@Composable
private fun PlayerSlot(player: Player, isLocal: Boolean, localAvatarUrl: String?) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(80.dp)
    ) {
        Box {
            // Avatar
            Surface(
                modifier = Modifier
                    .size(70.dp)
                    .then(
                        if (player.isReady) Modifier.border(3.dp, Color(0xFF4ADE80), CircleShape)
                        else Modifier.border(2.dp, PrimaryColor.copy(alpha = 0.5f), CircleShape)
                    ),
                shape = CircleShape,
                color = SurfaceColor
            ) {
                Box(contentAlignment = Alignment.Center) {
                    val presetRes = if (isLocal) AvatarPresets.drawableForId(localAvatarUrl) else null
                    when {
                        presetRes != null -> {
                            Image(
                                painter = painterResource(id = presetRes),
                                contentDescription = "Avatar",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        }
                        isLocal && !localAvatarUrl.isNullOrBlank() -> {
                            AsyncImage(
                                model = localAvatarUrl,
                                contentDescription = "Avatar",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        }
                        else -> {
                            Text(player.name.first().toString(), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            // Badge de listo
            if (player.isReady) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(24.dp)
                        .background(Color(0xFF4ADE80), CircleShape)
                        .border(2.dp, BgColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (isLocal) "Tú" else player.name,
            color = if (player.isReady) Color.White else TextMutedColor,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        // Racha (V/D)
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            player.streak.forEach {
                Text(
                    it,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = if (it == "V") Color(0xFF4ADE80) else Color(0xFFF87171)
                )
            }
        }
        Text("${player.rr} RR", color = Color(0xFFFBBF24), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun EmptySlot() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(80.dp)) {
        Box(
            modifier = Modifier
                .size(70.dp)
                .background(Color.White.copy(alpha = 0.05f), CircleShape)
                .border(2.dp, Color.White.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("?", color = Color.White.copy(alpha = 0.2f), fontSize = 24.sp)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text("Esperando...", color = Color.White.copy(alpha = 0.1f), fontSize = 12.sp)
    }
}

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.random_reversi.data.GamesRepository
import com.example.random_reversi.data.UserProfileStore
import com.example.random_reversi.data.UserResult
import com.example.random_reversi.data.remote.LobbyPlayerInfo
import com.example.random_reversi.ui.theme.*
import com.example.random_reversi.utils.AvatarPresets
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun WaitingRoomScreen(
    gameMode: String = "1vs1",
    gameId: Int = -1,
    onNavigate: (String) -> Unit
) {
    val profile by UserProfileStore.state.collectAsState()
    val maxPlayers = if (gameMode == "1vs1") 2 else 4
    val localPlayerName = profile.username.ifBlank { "Jugador" }

    var players by remember { mutableStateOf<List<LobbyPlayerInfo>>(emptyList()) }
    var lobbyStatus by remember { mutableStateOf("waiting") }
    var isLocalReady by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        UserProfileStore.refreshFromBackend()
    }

    val isFull = players.size == maxPlayers
    val allReady = isFull && players.all { it.is_ready }

    // Polling del estado del lobby cada 2 segundos
    LaunchedEffect(gameId) {
        if (gameId > 0) {
            while (true) {
                when (val result = GamesRepository.getLobbyState(gameId)) {
                    is UserResult.Success -> {
                        players = result.data.players
                        lobbyStatus = result.data.status
                        // Sincronizar el estado ready local
                        val me = result.data.players.find { it.username == localPlayerName }
                        if (me != null) isLocalReady = me.is_ready
                    }
                    is UserResult.Error -> {
                        errorMsg = result.message
                    }
                }
                delay(2000)
            }
        }
    }

    // Navegar al juego cuando el lobby pasa a "playing"
    LaunchedEffect(lobbyStatus) {
        if (lobbyStatus == "playing") {
            delay(1000)
            onNavigate("game-$gameMode/$gameId")
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

            FlippingChip3D()

            Spacer(modifier = Modifier.height(20.dp))

            // Texto de estado pulsante
            val infiniteTransition = rememberInfiniteTransition(label = "")
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.4f, targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse), label = ""
            )

            Text(
                text = when {
                    lobbyStatus == "playing" -> "INICIANDO PARTIDA..."
                    allReady -> "INICIANDO PARTIDA..."
                    isFull -> "SALA LLENA - MARCA LISTO"
                    else -> "ESPERANDO JUGADORES..."
                },
                color = TextMutedColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp,
                modifier = Modifier.alpha(alpha)
            )

            // Error message
            errorMsg?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(it, color = Color(0xFFF87171), fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Players layout
            if (gameMode == "1vs1") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.Top
                ) {
                    players.forEach { player ->
                        PlayerSlot(
                            player,
                            isLocal = player.username == localPlayerName,
                            localAvatarUrl = profile.avatarUrl
                        )
                        if (players.size > 1 && player != players.last()) Spacer(modifier = Modifier.width(20.dp))
                    }
                    repeat(maxPlayers - players.size) {
                        if (players.isNotEmpty()) Spacer(modifier = Modifier.width(20.dp))
                        EmptySlot()
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        if (players.size > 0) PlayerSlot(players[0], isLocal = players[0].username == localPlayerName, localAvatarUrl = profile.avatarUrl) else EmptySlot()
                        if (players.size > 1) PlayerSlot(players[1], isLocal = players[1].username == localPlayerName, localAvatarUrl = profile.avatarUrl) else EmptySlot()
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        if (players.size > 2) PlayerSlot(players[2], isLocal = players[2].username == localPlayerName, localAvatarUrl = profile.avatarUrl) else EmptySlot()
                        if (players.size > 3) PlayerSlot(players[3], isLocal = players[3].username == localPlayerName, localAvatarUrl = profile.avatarUrl) else EmptySlot()
                    }
                }
            }

            Spacer(modifier = Modifier.height(60.dp))

            // Acciones
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val scope = rememberCoroutineScope()

                // Abandonar
                Button(
                    onClick = {
                        if (gameId > 0) {
                            scope.launch {
                                GamesRepository.leaveLobby(gameId)
                                onNavigate("online-game")
                            }
                        } else {
                            onNavigate("online-game")
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF87171).copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFF87171).copy(alpha = 0.2f))
                ) {
                    Text("Abandonar", color = Color(0xFFF87171), fontWeight = FontWeight.Bold)
                }

                // Listo
                Button(
                    onClick = {
                        if (gameId > 0) {
                            scope.launch {
                                when (GamesRepository.setReady(gameId)) {
                                    is UserResult.Success -> isLocalReady = !isLocalReady
                                    is UserResult.Error -> {}
                                }
                            }
                        }
                    },
                    enabled = isFull && gameId > 0,
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = if (rotation % 360 in 90f..270f) 1f else 0f }
                .background(Color.White, CircleShape)
                .border(4.dp, Color.Gray.copy(alpha = 0.2f), CircleShape)
        )
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
private fun PlayerSlot(player: LobbyPlayerInfo, isLocal: Boolean, localAvatarUrl: String?) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(80.dp)
    ) {
        Box {
            Surface(
                modifier = Modifier
                    .size(70.dp)
                    .then(
                        if (player.is_ready) Modifier.border(3.dp, Color(0xFF4ADE80), CircleShape)
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
                        !player.avatar_url.isNullOrBlank() -> {
                            val otherPreset = AvatarPresets.drawableForId(player.avatar_url)
                            if (otherPreset != null) {
                                Image(
                                    painter = painterResource(id = otherPreset),
                                    contentDescription = "Avatar",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            } else {
                                AsyncImage(
                                    model = player.avatar_url,
                                    contentDescription = "Avatar",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            }
                        }
                        else -> {
                            Text(player.username.first().toString(), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            if (player.is_ready) {
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
            text = if (isLocal) "Tú" else player.username,
            color = if (player.is_ready) Color.White else TextMutedColor,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
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

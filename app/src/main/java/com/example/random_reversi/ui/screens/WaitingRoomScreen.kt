package com.example.random_reversi.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
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
import com.example.random_reversi.data.remote.HistoryEntry
import com.example.random_reversi.data.remote.GameWebSocket
import com.example.random_reversi.data.remote.LobbyPlayerInfo
import com.example.random_reversi.ui.navigation.NavigationMessages
import com.example.random_reversi.ui.theme.BgColor
import com.example.random_reversi.ui.theme.PrimaryColor
import com.example.random_reversi.ui.theme.SurfaceColor
import com.example.random_reversi.ui.theme.TextColor
import com.example.random_reversi.ui.theme.TextMutedColor
import com.example.random_reversi.utils.AvatarPresets
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private data class WaitingChip(
    val emoji: String,
    val startXFraction: Float,
    val startYFraction: Float,
    val durationMs: Int,
    val delayMs: Int,
    val isQuestion: Boolean = false
)

private val waitingChips = listOf(
    WaitingChip("\u26AB", 0.1f, 0.1f, 3000, 0),
    WaitingChip("\u26AA", 0.85f, 0.15f, 3200, 200),
    WaitingChip("\uD83D\uDD34", 0.2f, 0.45f, 2800, 400),
    WaitingChip("\uD83D\uDD35", 0.8f, 0.5f, 3100, 600),
    WaitingChip("\uD83D\uDFE2", 0.15f, 0.75f, 2900, 800),
    WaitingChip("\uD83D\uDFE1", 0.9f, 0.8f, 3300, 1000),
    WaitingChip("\uD83D\uDFE3", 0.3f, 0.9f, 3000, 1200),
    WaitingChip("\uD83D\uDFE0", 0.75f, 0.3f, 2700, 1400),
    WaitingChip("\u2753", 0.5f, 0.2f, 3100, 0, true),
    WaitingChip("\u2753", 0.6f, 0.7f, 2900, 500, true),
)

private fun buildHistoryPreview(history: List<HistoryEntry>, gameMode: String): List<String> {
    val acceptedModes = if (gameMode == "1vs1") {
        setOf("1vs1", "1v1")
    } else {
        setOf("1vs1vs1vs1", "1v1v1v1")
    }

    val recent = history
        .filter { acceptedModes.contains(it.mode.lowercase()) }
        .take(5)
        .map { item ->
            if (gameMode == "1vs1") {
                when (item.result.lowercase()) {
                    "ganada", "win", "victoria" -> "V"
                    "perdida", "loss", "derrota" -> "D"
                    else -> "E"
                }
            } else {
                val normalized = item.result.trim()
                when {
                    normalized.startsWith("1") -> "1º"
                    normalized.startsWith("2") -> "2º"
                    normalized.startsWith("3") -> "3º"
                    normalized.startsWith("4") -> "4º"
                    else -> "-"
                }
            }
        }
        .reversed()
        .toMutableList()

    while (recent.size < 5) recent.add(0, "-")
    return recent
}

private fun historySymbolColor(symbol: String): Color = when (symbol) {
    "V", "1" -> Color(0xFF4ADE80)
    "D", "4" -> Color(0xFFF87171)
    "2" -> Color(0xFF9CA3AF)
    "3" -> Color(0xFF9CA3AF)
    "E" -> Color(0xFFE5E7EB)
    "1º" -> Color(0xFF4ADE80)
    "4º" -> Color(0xFFF87171)
    "2º" -> Color(0xFF9CA3AF)
    "3º" -> Color(0xFF9CA3AF)
    else -> TextMutedColor.copy(alpha = 0.7f)
}

@Composable
private fun AnimatedWaitingChip(chip: WaitingChip) {
    val infiniteTransition = rememberInfiniteTransition(label = "waiting_chip")
    val yOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(
            animation = tween(chip.durationMs, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(chip.delayMs)
        ),
        label = "chip_offset"
    )

    Text(
        text = chip.emoji,
        fontSize = if (chip.isQuestion) 28.sp else 32.sp,
        modifier = Modifier
            .offset(x = 280.dp * chip.startXFraction, y = 700.dp * chip.startYFraction + yOffset.dp)
            .alpha(0.28f)
    )
}

@Composable
private fun WaitingRoomBackground() {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        waitingChips.forEach { chip ->
            AnimatedWaitingChip(chip = chip)
        }
    }
}

@Composable
fun WaitingRoomScreen(
    gameMode: String = "1vs1",
    gameId: Int = -1,
    returnTo: String = "online-game",
    opponentName: String? = null,
    onNavigate: (String) -> Unit
) {
    val profile by UserProfileStore.state.collectAsState()
    val scope = rememberCoroutineScope()
    val ws = remember(gameId) { if (gameId > 0) GameWebSocket(gameId) else null }
    val wsRoomPlayers by ws?.roomPlayers?.collectAsState() ?: remember { mutableStateOf(emptyList()) }
    val wsRoomStatus by ws?.roomStatus?.collectAsState() ?: remember { mutableStateOf("waiting") }
    val wsConnectionState by ws?.connectionState?.collectAsState() ?: remember { mutableStateOf("disconnected") }
    val maxPlayers = if (gameMode == "1vs1") 2 else 4
    val localPlayerName = profile.username.ifBlank { "Jugador" }

    var players by remember { mutableStateOf<List<LobbyPlayerInfo>>(emptyList()) }
    var lobbyStatus by remember { mutableStateOf("waiting") }
    var isLocalReady by remember { mutableStateOf(false) }
    var isUpdatingReady by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var inlineToast by remember { mutableStateOf<String?>(null) }
    var forcedExitTriggered by remember { mutableStateOf(false) }
    var triedRecoverJoin by remember { mutableStateOf(false) }

    val historyByPlayer = remember { mutableStateMapOf<Int, List<String>>() }
    val loadedHistoryPlayers = remember { mutableStateMapOf<Int, Boolean>() }

    LaunchedEffect(Unit) {
        UserProfileStore.refreshFromBackend()
    }

    LaunchedEffect(gameId) {
        if (gameId > 0) ws?.connect()
    }

    DisposableEffect(gameId) {
        onDispose { ws?.disconnect() }
    }

    val isFull = players.size == maxPlayers
    val allReady = isFull && players.all { it.is_ready }

    suspend fun loadMissingHistories(currentPlayers: List<LobbyPlayerInfo>) {
        currentPlayers.forEach { player ->
            if (loadedHistoryPlayers[player.id] == true) return@forEach
            loadedHistoryPlayers[player.id] = true
            when (val result = GamesRepository.getUserHistory(player.id)) {
                is UserResult.Success -> {
                    historyByPlayer[player.id] = buildHistoryPreview(result.data, gameMode)
                }
                is UserResult.Error -> {
                    historyByPlayer[player.id] = listOf("-", "-", "-", "-", "-")
                }
            }
        }
    }

    LaunchedEffect(wsRoomPlayers, wsRoomStatus, localPlayerName) {
        if (wsRoomPlayers.isEmpty()) return@LaunchedEffect

        val mappedPlayers = wsRoomPlayers.mapNotNull { player ->
            try {
                val id = player.get("id")?.asInt ?: return@mapNotNull null
                val username = player.get("username")?.asString ?: return@mapNotNull null
                val rr = player.get("rr")?.asInt ?: 1000
                val avatarUrl = if (player.has("avatar_url") && !player.get("avatar_url").isJsonNull) {
                    player.get("avatar_url").asString
                } else {
                    null
                }
                val isReady = player.get("is_ready")?.asBoolean ?: false

                LobbyPlayerInfo(
                    id = id,
                    username = username,
                    rr = rr,
                    avatar_url = avatarUrl,
                    is_ready = isReady
                )
            } catch (_: Exception) {
                null
            }
        }

        if (mappedPlayers.isNotEmpty()) {
            val previousPlayers = players
            players = mappedPlayers
            lobbyStatus = wsRoomStatus
            mappedPlayers.find { it.username == localPlayerName }?.let { me ->
                isLocalReady = me.is_ready
                if (isUpdatingReady) {
                    isUpdatingReady = false
                }
            }
            loadMissingHistories(mappedPlayers)
            errorMsg = null

            if (!forcedExitTriggered && previousPlayers.isNotEmpty() && mappedPlayers.size < previousPlayers.size && wsRoomStatus != "playing") {
                val departed = previousPlayers.firstOrNull { prev -> mappedPlayers.none { now -> now.id == prev.id } }
                forcedExitTriggered = true
                val departedName = departed?.username
                    ?: previousPlayers.firstOrNull { it.username != localPlayerName }?.username
                    ?: opponentName
                    ?: "Un jugador"
                NavigationMessages.pushFriendsToast("$departedName ha abandonado la sala.")
                onNavigate(returnTo)
                return@LaunchedEffect
            }
        }
    }

    LaunchedEffect(gameId, localPlayerName, wsConnectionState) {
        if (gameId <= 0) return@LaunchedEffect

        while (isActive) {
            val wsIsReady = wsConnectionState == "connected"
            if (wsIsReady) {
                delay(1200)
                continue
            }

            when (val result = GamesRepository.getLobbyState(gameId)) {
                is UserResult.Success -> {
                    val previousPlayers = players
                    val nextPlayers = result.data.players

                    players = nextPlayers
                    lobbyStatus = result.data.status

                    val me = nextPlayers.find { it.username == localPlayerName }
                    if (me != null) {
                        isLocalReady = me.is_ready
                        if (isUpdatingReady) {
                            isUpdatingReady = false
                        }
                    }

                    loadMissingHistories(nextPlayers)

                    if (!forcedExitTriggered && previousPlayers.isNotEmpty() && nextPlayers.size < previousPlayers.size && result.data.status != "playing") {
                        val departed = previousPlayers.firstOrNull { prev -> nextPlayers.none { now -> now.id == prev.id } }
                        forcedExitTriggered = true
                        val departedName = departed?.username
                            ?: previousPlayers.firstOrNull { it.username != localPlayerName }?.username
                            ?: opponentName
                            ?: "Un jugador"
                        NavigationMessages.pushFriendsToast("$departedName ha abandonado la sala.")
                        onNavigate(returnTo)
                        return@LaunchedEffect
                    }
                }
                is UserResult.Error -> {
                    val isForbidden = result.message.contains("403")
                    val wsHasRoomContext = wsConnectionState == "connected" || wsConnectionState == "waiting"
                    if (!isForbidden || !wsHasRoomContext) {
                        errorMsg = result.message
                    }
                    if (!forcedExitTriggered && isForbidden && players.isNotEmpty()) {
                        forcedExitTriggered = true
                        val departedName = players.firstOrNull { it.username != localPlayerName }?.username
                            ?: opponentName
                            ?: "Un jugador"
                        NavigationMessages.pushFriendsToast("$departedName ha abandonado la sala.")
                        onNavigate(returnTo)
                        return@LaunchedEffect
                    }
                    if (isForbidden && !triedRecoverJoin && returnTo == "friends") {
                        triedRecoverJoin = true
                        when (val joinResult = GamesRepository.joinLobby(gameId)) {
                            is UserResult.Success -> {
                                errorMsg = null
                            }
                            is UserResult.Error -> {
                                errorMsg = joinResult.message
                            }
                        }
                    }
                    val isRoomGone = result.message.contains("404")
                    if (!forcedExitTriggered && isRoomGone) {
                        forcedExitTriggered = true
                        val departedName = players.firstOrNull { it.username != localPlayerName }?.username
                            ?: opponentName
                            ?: "El otro jugador"
                        NavigationMessages.pushFriendsToast("$departedName ha abandonado la sala.")
                        onNavigate(returnTo)
                        return@LaunchedEffect
                    }
                }
            }
            delay(1200)
        }
    }

    LaunchedEffect(lobbyStatus) {
        if (lobbyStatus == "playing") {
            delay(900)
            onNavigate("game-$gameMode/$gameId/$returnTo")
        }
    }

    LaunchedEffect(inlineToast) {
        if (inlineToast == null) return@LaunchedEffect
        delay(2400)
        inlineToast = null
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
    ) {
        WaitingRoomBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
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

            val infiniteTransition = rememberInfiniteTransition(label = "waiting_status")
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.4f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
                label = "status_alpha"
            )

            Text(
                text = when {
                    lobbyStatus == "playing" -> "INICIANDO PARTIDA..."
                    allReady -> "INICIANDO PARTIDA..."
                    isFull -> "SALA LLENA"
                    else -> "ESPERANDO JUGADORES..."
                },
                color = TextMutedColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp,
                modifier = Modifier.alpha(alpha)
            )

            errorMsg?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(it, color = Color(0xFFF87171), fontSize = 12.sp)
            }

            inlineToast?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(it, color = Color(0xFFFBBF24), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(34.dp))

            if (gameMode == "1vs1") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.Top
                ) {
                    players.forEachIndexed { index, player ->
                        PlayerSlot(
                            player = player,
                            isLocal = player.username == localPlayerName,
                            localAvatarUrl = profile.avatarUrl,
                            historyPreview = historyByPlayer[player.id] ?: listOf("-", "-", "-", "-", "-")
                        )
                        if (index < players.lastIndex) Spacer(modifier = Modifier.width(20.dp))
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
                        if (players.size > 0) PlayerSlot(players[0], players[0].username == localPlayerName, profile.avatarUrl, historyByPlayer[players[0].id] ?: listOf("-", "-", "-", "-", "-")) else EmptySlot()
                        if (players.size > 1) PlayerSlot(players[1], players[1].username == localPlayerName, profile.avatarUrl, historyByPlayer[players[1].id] ?: listOf("-", "-", "-", "-", "-")) else EmptySlot()
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        if (players.size > 2) PlayerSlot(players[2], players[2].username == localPlayerName, profile.avatarUrl, historyByPlayer[players[2].id] ?: listOf("-", "-", "-", "-", "-")) else EmptySlot()
                        if (players.size > 3) PlayerSlot(players[3], players[3].username == localPlayerName, profile.avatarUrl, historyByPlayer[players[3].id] ?: listOf("-", "-", "-", "-", "-")) else EmptySlot()
                    }
                }
            }

            Spacer(modifier = Modifier.height(44.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = {
                        scope.launch {
                            if (gameId > 0) GamesRepository.leaveLobby(gameId)
                            onNavigate(returnTo)
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

                Button(
                    onClick = {
                        if (gameId > 0) {
                            scope.launch {
                                if (isUpdatingReady) return@launch
                                val nextReady = !isLocalReady
                                isUpdatingReady = true
                                val wsIsReady = wsConnectionState == "connected"

                                if (wsIsReady) {
                                    // Igual que en web: si hay WS, enviamos sólo por WS.
                                    ws?.sendReady(nextReady)
                                    launch {
                                        delay(2200)
                                        if (isUpdatingReady) {
                                            isUpdatingReady = false
                                        }
                                    }
                                    return@launch
                                }

                                // Fallback cuando no hay WS: REST y refresh del estado.
                                when (val result = GamesRepository.setReady(gameId, nextReady)) {
                                    is UserResult.Success -> {
                                        when (val refreshed = GamesRepository.getLobbyState(gameId)) {
                                            is UserResult.Success -> {
                                                players = refreshed.data.players
                                                lobbyStatus = refreshed.data.status
                                                val me = refreshed.data.players.find { it.username == localPlayerName }
                                                if (me != null) isLocalReady = me.is_ready
                                            }
                                            is UserResult.Error -> {
                                                inlineToast = refreshed.message
                                            }
                                        }
                                        isUpdatingReady = false
                                    }
                                    is UserResult.Error -> {
                                        inlineToast = result.message
                                        isUpdatingReady = false
                                    }
                                }
                            }
                        }
                    },
                    enabled = isFull && gameId > 0 && !isUpdatingReady,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isLocalReady) Color(0xFF4ADE80) else PrimaryColor,
                        disabledContainerColor = Color.Gray.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        when {
                            isUpdatingReady -> "Actualizando..."
                            isLocalReady -> "Listo"
                            else -> "Estoy Listo"
                        },
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun FlippingChip3D() {
    val infiniteTransition = rememberInfiniteTransition(label = "chip3d")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Box(
        modifier = Modifier
            .size(100.dp)
            .graphicsLayer {
                rotationY = rotation
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
private fun PlayerSlot(
    player: LobbyPlayerInfo,
    isLocal: Boolean,
    localAvatarUrl: String?,
    historyPreview: List<String>
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(90.dp)
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
                    val presetRes = if (isLocal) AvatarPresets.drawableForId(localAvatarUrl) else AvatarPresets.drawableForId(player.avatar_url)
                    when {
                        presetRes != null -> {
                            Image(
                                painter = painterResource(id = presetRes),
                                contentDescription = "Avatar",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        isLocal && !localAvatarUrl.isNullOrBlank() -> {
                            AsyncImage(
                                model = localAvatarUrl,
                                contentDescription = "Avatar",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        !player.avatar_url.isNullOrBlank() -> {
                            AsyncImage(
                                model = player.avatar_url,
                                contentDescription = "Avatar",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
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
            text = player.username,
            color = if (player.is_ready) Color.White else TextMutedColor,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text("${player.rr} RR", color = Color(0xFFFBBF24), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)

        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            historyPreview.take(5).forEach { symbol ->
                Text(symbol, color = historySymbolColor(symbol), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun EmptySlot() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(90.dp)) {
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
        Text("Esperando...", color = Color.White.copy(alpha = 0.3f), fontSize = 12.sp)
    }
}

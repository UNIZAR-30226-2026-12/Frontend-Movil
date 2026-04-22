package com.example.random_reversi.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import coil.compose.AsyncImage
import com.example.random_reversi.R
import com.example.random_reversi.data.GamesRepository
import com.example.random_reversi.data.UserProfileStore
import com.example.random_reversi.data.UserResult
import com.example.random_reversi.data.remote.HistoryEntry
import com.example.random_reversi.data.remote.GameWebSocket
import com.example.random_reversi.data.remote.LobbyPlayerInfo
import com.example.random_reversi.ui.navigation.NavigationMessages
import com.example.random_reversi.ui.theme.*
import com.example.random_reversi.utils.AvatarPresets
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

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

private fun getHistoryTokenImage(symbol: String, index: Int, gameMode: String): Int? {
    if (gameMode == "1vs1") {
        if (symbol == "V" || symbol == "1º") {
            return if (index % 2 == 0) R.drawable.salamovil_ficha_negra_victoria else R.drawable.salamovil_ficha_victoria_blanco
        }
        if (symbol == "D" || symbol == "4º") {
            return if (index % 2 == 0) R.drawable.salamovil_ficha_negra_derrota else R.drawable.salamovil_ficha_blanca_derrota
        }
        if (symbol == "E" || symbol == "2º" || symbol == "3º") {
            return R.drawable.salamovil_empate
        }
        return null
    } else {
        // Fallback for 4 players if needed later
        return null
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
                } else null
                val isReady = player.get("is_ready")?.asBoolean ?: false

                LobbyPlayerInfo(id, username, rr, avatarUrl, isReady)
            } catch (_: Exception) { null }
        }

        if (mappedPlayers.isNotEmpty()) {
            val previousPlayers = players
            players = mappedPlayers
            lobbyStatus = wsRoomStatus
            mappedPlayers.find { it.username == localPlayerName }?.let { me ->
                isLocalReady = me.is_ready
                if (isUpdatingReady) isUpdatingReady = false
            }
            loadMissingHistories(mappedPlayers)
            errorMsg = null

            if (
                returnTo == "friends" &&
                !forcedExitTriggered &&
                previousPlayers.isNotEmpty() &&
                mappedPlayers.size < previousPlayers.size &&
                wsRoomStatus != "playing"
            ) {
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
            when (val result = GamesRepository.getLobbyState(gameId)) {
                is UserResult.Success -> {
                    val previousPlayers = players
                    val nextPlayers = result.data.players

                    players = nextPlayers
                    lobbyStatus = result.data.status

                    val me = nextPlayers.find { it.username == localPlayerName }
                    if (me != null) {
                        isLocalReady = me.is_ready
                        if (isUpdatingReady) isUpdatingReady = false
                    }

                    loadMissingHistories(nextPlayers)

                    if (
                        returnTo == "friends" &&
                        !forcedExitTriggered &&
                        previousPlayers.isNotEmpty() &&
                        nextPlayers.size < previousPlayers.size &&
                        result.data.status != "playing"
                    ) {
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
                    val isRoomGone = result.message.contains("404")
                    val wsHasRoomContext = wsConnectionState == "connected" || wsConnectionState == "waiting"
                    if (!isForbidden || !wsHasRoomContext) {
                        errorMsg = result.message
                    }
                    if (returnTo == "friends" && !forcedExitTriggered && isForbidden && players.isNotEmpty()) {
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
                            is UserResult.Success -> { errorMsg = null }
                            is UserResult.Error -> { errorMsg = joinResult.message }
                        }
                    }
                    if (returnTo == "friends" && !forcedExitTriggered && isRoomGone) {
                        forcedExitTriggered = true
                        val departedName = players.firstOrNull { it.username != localPlayerName }?.username
                            ?: opponentName
                            ?: "El otro jugador"
                        NavigationMessages.pushFriendsToast("$departedName ha abandonado la sala.")
                        onNavigate(returnTo)
                        return@LaunchedEffect
                    }
                    if (returnTo != "friends" && (isForbidden || isRoomGone)) {
                        val localPlayer = players.firstOrNull { it.username == localPlayerName }
                        players = listOf(
                            localPlayer ?: LobbyPlayerInfo(-1, localPlayerName, 0, profile.avatarUrl, isLocalReady)
                        )
                        lobbyStatus = "waiting"
                        errorMsg = null
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

    val statusText = when {
        lobbyStatus == "playing" || allReady -> "INICIANDO PARTIDA..."
        isFull -> "SALA LLENA"
        else -> "ESPERANDO OPONENTES..."
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Fondo General
        Image(
            painter = painterResource(id = R.drawable.nuevofondomovil),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Titulo: SALA DE ESPERA
            Image(
                painter = painterResource(id = R.drawable.salamovil_titulosalaespera2),
                contentDescription = "Sala de Espera",
                modifier = Modifier.width(250.dp),
                contentScale = ContentScale.FillWidth
            )

            // Estado DInamico (Ribete)
            Box(
                modifier = Modifier.offset(y = (-30).dp).width(230.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.salamovil_tituloesperandooponentes),
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.FillWidth
                )
            }

            // Tablero Principal
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                BoxWithConstraints(
                    modifier = Modifier.offset(y = (-39).dp).fillMaxHeight().aspectRatio(408f/612f)
                ) {
                    val h = maxHeight
                    val w = maxWidth
                    Image(
                        painter = painterResource(id = R.drawable.salamovil_cartelsalaespera1v1),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )

                    if (errorMsg != null || inlineToast != null) {
                        Text(
                            text = errorMsg ?: inlineToast ?: "",
                            color = Color.Red,
                            fontSize = 12.sp,
                            modifier = Modifier.align(Alignment.TopCenter).padding(top = 10.dp)
                        )
                    }

                    // Player 1 Card (Top Slot)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(h * 0.28f)
                            .offset(y = h * 0.034f) 
                    ) {
                        PlayerCardOverlay(players.getOrNull(0), 0, gameMode, profile.avatarUrl, historyByPlayer)
                    }

                    // Player 2 Card (Bottom Slot)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(h * 0.28f)
                            .offset(y = h * 0.305f)
                    ) {
                        PlayerCardOverlay(players.getOrNull(1), 1, gameMode, profile.avatarUrl, historyByPlayer)
                    }
                    // Cinta inferior "Prepara tu estrategia" eliminada

                }
            }

            // Acciones Inferiores (Abandonar y Listo)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-40).dp)
                    .padding(horizontal = 24.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.salamovil_abandonar),
                    contentDescription = "Abandonar Sala",
                    modifier = Modifier
                        .weight(1f)
                        .height(105.dp)
                        .clickable {
                            scope.launch {
                                if (gameId > 0) GamesRepository.leaveLobby(gameId)
                                onNavigate(returnTo)
                            }
                        },
                    contentScale = ContentScale.Fit
                )
                
                Spacer(modifier = Modifier.width(16.dp))

                Image(
                    painter = painterResource(id = R.drawable.salamovil_listo),
                    contentDescription = "Estoy Listo",
                    modifier = Modifier
                        .weight(1f)
                        .height(95.dp)
                        .alpha(if (isUpdatingReady) 0.5f else 1f)
                        .graphicsLayer {
                            val scale = if (isLocalReady) 0.93f else 1f
                            scaleX = scale
                            scaleY = scale
                        }
                        .clickable(enabled = isFull && gameId > 0 && !isUpdatingReady) {
                            scope.launch {
                                val nextReady = !isLocalReady
                                isUpdatingReady = true

                                // Notificar por WS si está conectado (fire-and-forget)
                                val wsIsReady = wsConnectionState == "connected" || wsConnectionState == "waiting"
                                if (wsIsReady) {
                                    ws?.sendReady(nextReady)
                                }

                                // SIEMPRE confirmar el estado via HTTP para garantizar la actualización de UI
                                when (val result = GamesRepository.setReady(gameId, nextReady)) {
                                    is UserResult.Success -> {
                                        when (val refreshed = GamesRepository.getLobbyState(gameId)) {
                                            is UserResult.Success -> {
                                                players = refreshed.data.players
                                                lobbyStatus = refreshed.data.status
                                                val me = refreshed.data.players.find { it.username == localPlayerName }
                                                if (me != null) isLocalReady = me.is_ready
                                            }
                                            is UserResult.Error -> { inlineToast = refreshed.message }
                                        }
                                        isUpdatingReady = false
                                    }
                                    is UserResult.Error -> {
                                        inlineToast = result.message
                                        isUpdatingReady = false
                                    }
                                }
                            }
                        },
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}

@Composable
private fun PlayerCardOverlay(
    player: LobbyPlayerInfo?,
    index: Int,
    gameMode: String,
    localAvatarUrl: String?,
    historyByPlayer: Map<Int, List<String>>
) {
    val startPadding = if (index == 0) 75.2.dp else 77.4.dp
    val avatarSize = 78.dp

    if (player == null) {
        // Slot Vacío
        Row(
            modifier = Modifier.fillMaxSize().padding(start = startPadding, end = 32.dp, top = 26.dp, bottom = 26.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(avatarSize)
                    .background(Color(0xFFE5E5E5), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.interrogante),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(0.6f),
                    contentScale = ContentScale.Fit,
                    alpha = 0.5f // Hacer difuminado como en web
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.offset(y = (-6).dp)
            ) {
                Text("Esperando...", color = Color(0xFF4B5563), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("ELO ACTUAL:", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.offset(y = (-8).dp))
                Text("--- RR", color = Color.DarkGray, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.offset(y = (-16).dp))
            }
        }
    } else {
        // Slot Lleno
        val historyPreview = historyByPlayer[player.id] ?: listOf("-", "-", "-", "-", "-")
        Box(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxSize().padding(start = startPadding, end = 32.dp, top = 26.dp, bottom = 26.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(avatarSize)) {
                    val presetRes = AvatarPresets.drawableForId(player.avatar_url)
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .then(
                                if (player.is_ready)
                                    Modifier.border(3.dp, Color(0xFF4ADE80), RoundedCornerShape(12.dp))
                                else
                                    Modifier
                            ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (presetRes != null) {
                            Image(painter = painterResource(id = presetRes), contentDescription = null, contentScale = ContentScale.Crop)
                        } else if (!player.avatar_url.isNullOrBlank()) {
                            AsyncImage(model = player.avatar_url, contentDescription = null, contentScale = ContentScale.Crop)
                        } else {
                            Box(modifier = Modifier.fillMaxSize().background(Color.LightGray), contentAlignment = Alignment.Center) {
                                Text(player.username.first().toString().uppercase(), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    if (player.is_ready) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(24.dp)
                                .background(Color(0xFF4ADE80), CircleShape)
                                .border(2.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.offset(y = (-6).dp)
                ) {
                    Text(player.username, color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("ELO ACTUAL:", color = Color.DarkGray, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.offset(y = (-8).dp))
                    Text("${player.rr} RR", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.offset(y = (-16).dp))
                }
            }
            
            // Fichas de la racha: cada círculo tiene su propia distancia absoluta desde el primero.
            // streakStartX → posición fija del círculo 0 (el más a la izquierda)
            // streakOffsets[i] → distancia de cada círculo respecto al primero (independientes entre sí)
            val streakStartX = if (index == 0) 179.dp else 178.2.dp
            val streakOffsets = listOf(0.dp, 21.5.dp, 43.dp, 65.dp, 87.dp) // ← ajusta cada valor por separado
            val streakSize   = 18.dp
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = streakStartX, y = if (index == 0) (-18).dp else (-26).dp)
                    .width(streakOffsets.last() + streakSize)
                    .height(streakSize)
            ) {
                historyPreview.take(5).forEachIndexed { i, symbol ->
                    val xOff = streakOffsets.getOrElse(i) { streakOffsets.last() }
                    val imgRes = getHistoryTokenImage(symbol, index, gameMode)
                    if (imgRes != null) {
                        Image(
                            painter = painterResource(id = imgRes),
                            contentDescription = null,
                            modifier = Modifier.size(streakSize).offset(x = xOff)
                        )
                    } else {
                        // Círculo vacío con borde punteado
                        Canvas(modifier = Modifier.size(streakSize).offset(x = xOff)) {
                            drawCircle(
                                color = Color.DarkGray,
                                style = Stroke(width = 3f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))),
                                radius = 8.dp.toPx()
                            )
                        }
                    }
                }
            }
        }
    }
}

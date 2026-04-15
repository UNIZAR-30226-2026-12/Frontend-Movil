package com.example.random_reversi.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.random_reversi.data.UserRepository
import com.example.random_reversi.data.UserResult
import com.example.random_reversi.data.remote.GameWebSocket
import com.example.random_reversi.ui.theme.BgColor
import com.example.random_reversi.ui.theme.BorderColor
import com.example.random_reversi.ui.theme.PrimaryColor
import com.example.random_reversi.ui.theme.SurfaceColor
import com.example.random_reversi.ui.theme.TextMutedColor
import com.example.random_reversi.utils.AvatarPresets
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer

private val BOARD_SIZE = 8

data class BoardPlayer(
    val username: String,
    val rr: Int,
    val avatarUrl: String?
)

@Composable
fun GameBoard1v1Screen(
    gameId: Int = -1,
    returnTo: String = "online-game",
    onNavigate: (String) -> Unit
) {
    val ws = remember { if (gameId > 0) GameWebSocket(gameId) else null }
    val gameState by ws?.gameState?.collectAsState()
        ?: remember { mutableStateOf(com.example.random_reversi.data.remote.GameState()) }
    val myColor by ws?.myColor?.collectAsState() ?: remember { mutableStateOf<String?>(null) }
    val roomPlayersRaw by ws?.roomPlayers?.collectAsState()
        ?: remember { mutableStateOf(emptyList()) }
    val chatMessages by ws?.chatMessages?.collectAsState()
        ?: remember { mutableStateOf(emptyList()) }

    var myUsername by remember { mutableStateOf("Jugador") }
    var myElo by remember { mutableStateOf(1000) }
    var myAvatar by remember { mutableStateOf<String?>(null) }
    var duelStyle by remember { mutableStateOf(PIECE_STYLES_1V1.first()) }
    var showSurrenderConfirm by remember { mutableStateOf(false) }
    var showPauseConfirm by remember { mutableStateOf(false) }
    var showChat by remember { mutableStateOf(false) }
    var unreadChatCount by remember { mutableStateOf(0) }
    var processedChatCount by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()
    var previousPausedPieces by remember { mutableStateOf(emptySet<String>()) }
    var reconnectedPieces by remember { mutableStateOf(emptySet<String>()) }

    val parsedPlayers = remember(roomPlayersRaw) {
        roomPlayersRaw.mapNotNull { raw ->
            try {
                val username = raw.get("username")?.asString ?: return@mapNotNull null
                val rr = raw.get("rr")?.asInt ?: 1000
                val avatar =
                    if (raw.has("avatar_url") && !raw.get("avatar_url").isJsonNull) raw.get("avatar_url").asString else null
                BoardPlayer(username = username, rr = rr, avatarUrl = avatar)
            } catch (_: Exception) {
                null
            }
        }
    }

    val myPlayerFromRoom = remember(parsedPlayers, myUsername, myColor) {
        parsedPlayers.firstOrNull { it.username == myUsername }
            ?: when (myColor) {
                "black" -> parsedPlayers.getOrNull(0)
                "white" -> parsedPlayers.getOrNull(1)
                else -> parsedPlayers.firstOrNull()
            }
    }

    val opponentFromRoom = remember(parsedPlayers, myPlayerFromRoom) {
        parsedPlayers.firstOrNull { it.username != myPlayerFromRoom?.username }
    }

    val myDisplayName = myPlayerFromRoom?.username ?: myUsername
    val myDisplayAvatar = myPlayerFromRoom?.avatarUrl ?: myAvatar
    val myDisplayElo = myPlayerFromRoom?.rr ?: myElo

    val opponentName = opponentFromRoom?.username ?: "Rival"
    val opponentAvatar = opponentFromRoom?.avatarUrl
    val opponentElo = opponentFromRoom?.rr ?: 1000

    val effectiveMyPiece = remember(myColor, gameState.username_by_piece, myUsername) {
        myColor ?: gameState.username_by_piece.entries.firstOrNull { it.value == myUsername }?.key
    }

    val isMyTurn = gameState.current_player == effectiveMyPiece
    val gameOver = gameState.game_over
    val pausedPieces = gameState.paused_pieces
    val localIsPaused = effectiveMyPiece != null && pausedPieces.contains(effectiveMyPiece)

    val myScore = if (effectiveMyPiece != null) gameState.scores[effectiveMyPiece] ?: 0 else 0
    val opponentColor = when (effectiveMyPiece) {
        "black" -> "white"
        "white" -> "black"
        else -> "white"
    }
    val opponentScore = gameState.scores[opponentColor] ?: 0
    val opponentIsPaused = pausedPieces.contains(opponentColor)
    val waitingForPausedPlayer =
        gameState.current_player != null && pausedPieces.contains(gameState.current_player)
    val hasOtherPausedPlayer = pausedPieces.any { it != effectiveMyPiece }
    val waitingPausedPlayerName = gameState.username_by_piece[gameState.current_player]
        ?: if (gameState.current_player == effectiveMyPiece) myDisplayName else opponentName
    val localIsReconnected =
        effectiveMyPiece != null && reconnectedPieces.contains(effectiveMyPiece)
    val opponentIsReconnected = reconnectedPieces.contains(opponentColor)

    val myPieceName = if (effectiveMyPiece == "black") duelStyle.sideAName else duelStyle.sideBName
    val opponentPieceName =
        if (effectiveMyPiece == "black") duelStyle.sideBName else duelStyle.sideAName

    val arenaTheme = remember(myDisplayElo) { getArenaFromElo1v1(myDisplayElo) }

    LaunchedEffect(Unit) {
        when (val me = UserRepository.getMe()) {
            is UserResult.Success -> {
                myUsername = me.data.username
                myElo = me.data.elo
                myAvatar = me.data.avatar_url
                val (duelIndex, _) = decodeBoardPiecePreference(me.data.preferred_piece_color)
                duelStyle = PIECE_STYLES_1V1.getOrElse(duelIndex) { PIECE_STYLES_1V1.first() }
            }

            is UserResult.Error -> {
                duelStyle = PIECE_STYLES_1V1.first()
            }
        }
    }

    LaunchedEffect(gameId) {
        ws?.connect()
    }

    LaunchedEffect(chatMessages.size, showChat, myUsername) {
        if (showChat) {
            unreadChatCount = 0
            processedChatCount = chatMessages.size
            return@LaunchedEffect
        }

        if (chatMessages.size > processedChatCount) {
            val newMessages = chatMessages.subList(processedChatCount, chatMessages.size)
            unreadChatCount += newMessages.count { (sender, _) -> sender != myUsername }
            processedChatCount = chatMessages.size
        }
    }

    LaunchedEffect(pausedPieces.joinToString("|")) {
        val currentPaused = pausedPieces.toSet()
        val resumedPieces = previousPausedPieces - currentPaused
        if (resumedPieces.isNotEmpty()) {
            reconnectedPieces = reconnectedPieces + resumedPieces
            resumedPieces.forEach { resumed ->
                scope.launch {
                    delay(2200)
                    reconnectedPieces = reconnectedPieces - resumed
                }
            }
        }
        previousPausedPieces = currentPaused
    }

    DisposableEffect(Unit) {
        onDispose { ws?.disconnect() }
    }

    Box(modifier = Modifier.fillMaxSize().background(BgColor)) {
        Image(
            painter = painterResource(id = arenaTheme.backgroundRes),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.45f
        )

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(46.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                InGameChatButton(
                    unreadCount = unreadChatCount,
                    onClick = { showChat = true }
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (returnTo == "friends" && !gameOver) {
                        OutlinedButton(
                            onClick = { showPauseConfirm = true },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFFE5E7EB),
                                containerColor = Color(0xFF64748B).copy(alpha = 0.2f)
                            ),
                            border = BorderStroke(1.dp, Color(0xFF94A3B8).copy(alpha = 0.45f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text("Pausar", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    OutlinedButton(
                        onClick = { showSurrenderConfirm = true },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFFCA5A5),
                            containerColor = Color(0xFFF87171).copy(alpha = 0.15f)
                        ),
                        border = BorderStroke(1.dp, Color(0xFFF87171).copy(alpha = 0.35f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("Abandonar", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                color = when {
                    gameOver -> Color.DarkGray
                    isMyTurn -> PrimaryColor
                    else -> SurfaceColor
                },
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
            ) {
                Text(
                    text = when {
                        gameOver -> when {
                            gameState.winner == null -> "Empate"
                            gameState.winner == effectiveMyPiece -> "Has ganado"
                            else -> "Has perdido"
                        }

                        waitingForPausedPlayer -> "Partida pausada: Espera a que vuelva $waitingPausedPlayerName"
                        localIsPaused -> "Partida pausada"
                        isMyTurn -> "Tu turno"
                        else -> "Turno del rival"
                    },
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PlayerPanel1v1(
                    modifier = Modifier.weight(1f),
                    name = myDisplayName,
                    rr = myDisplayElo,
                    avatarUrl = myDisplayAvatar,
                    pieceColor = if (effectiveMyPiece == "black") duelStyle.sideA else duelStyle.sideB,
                    pieceLabel = myPieceName,
                    score = myScore,
                    isActive = isMyTurn && !gameOver,
                    paused = localIsPaused,
                    reconnected = localIsReconnected
                )

                PlayerPanel1v1(
                    modifier = Modifier.weight(1f),
                    name = opponentName,
                    rr = opponentElo,
                    avatarUrl = opponentAvatar,
                    pieceColor = if (effectiveMyPiece == "black") duelStyle.sideB else duelStyle.sideA,
                    pieceLabel = opponentPieceName,
                    score = opponentScore,
                    isActive = !isMyTurn && !gameOver,
                    paused = opponentIsPaused,
                    reconnected = opponentIsReconnected
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF204D2B))
                    .border(4.dp, Color.Black.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            ) {
                Image(
                    painter = painterResource(id = arenaTheme.boardRes),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillBounds
                )

                Column(Modifier.fillMaxSize()) {
                    for (row in 0 until BOARD_SIZE) {
                        Row(Modifier.weight(1f)) {
                            for (col in 0 until BOARD_SIZE) {
                                val cellValue = gameState.board.getOrNull(row)?.getOrNull(col)
                                val isValidMove =
                                    gameState.valid_moves.any { it.size >= 2 && it[0] == row && it[1] == col }
                                val canPlayHere = isMyTurn && isValidMove && !gameOver

                                GameCell1v1(
                                    modifier = Modifier.weight(1f),
                                    cellValue = cellValue,
                                    isValidMove = canPlayHere,
                                    style = duelStyle,
                                    onClick = {
                                        if (canPlayHere) {
                                            ws?.sendMove(row, col)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }

        if (showSurrenderConfirm) {
            AlertDialog(
                onDismissRequest = { showSurrenderConfirm = false },
                containerColor = BgColor,
                title = {
                    Text(
                        "Abandonar partida",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        if (returnTo == "friends" && hasOtherPausedPlayer && !localIsPaused)
                            "Como la partida está pausada por el otro jugador, si abandonas ahora no perderás RR y la partida quedará invalidada."
                        else
                            "Si abandonas esta partida en curso, se contará como una derrota en tu historial y perderás puntos RR.",
                        color = TextMutedColor
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            ws?.sendSurrender()
                            showSurrenderConfirm = false
                            ws?.disconnect()
                            onNavigate(returnTo)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF87171))
                    ) {
                        Text("Abandonar partida")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSurrenderConfirm = false }) {
                        Text("Seguir jugando", color = TextMutedColor)
                    }
                }
            )
        }

        if (showPauseConfirm) {
            AlertDialog(
                onDismissRequest = { showPauseConfirm = false },
                containerColor = BgColor,
                title = {
                    Text(
                        "Pausar partida",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        "Podrás reanudar esta partida después desde la pestaña de amigos. Mientras tanto, el rival quedará esperando a que vuelvas.",
                        color = TextMutedColor
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            ws?.sendPause()
                            showPauseConfirm = false
                            ws?.disconnect()
                            onNavigate("friends")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF64748B))
                    ) {
                        Text("Pausar y salir")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPauseConfirm = false }) {
                        Text("Cancelar", color = TextMutedColor)
                    }
                }
            )
        }

        if (showChat) {
            InGameChatOverlay(
                messages = chatMessages,
                myUsername = myUsername,
                onClose = { showChat = false },
                onSend = { message -> ws?.sendChat(message) }
            )
        }

        // PANTALLA DE VICTORIA
        AnimatedVisibility(
            visible = gameOver,
            enter = fadeIn(tween(400)) + scaleIn(
                initialScale = 0.8f,
                animationSpec = spring(dampingRatio = 0.7f)
            ),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            val playerWon = gameState.winner == effectiveMyPiece
            val isDraw = gameState.winner == null
            val rrDelta = if (isDraw) 0 else if (playerWon) 30 else -30

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.75f))
                    .clickable(enabled = false) {}, // Intercepta clicks traseros
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(0.85f),
                    shape = RoundedCornerShape(24.dp),
                    color = SurfaceColor,
                    border = BorderStroke(
                        1.dp,
                        if (playerWon) Color(0xFF4ADE80) else if (isDraw) BorderColor else Color(
                            0xFFF87171
                        )
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = if (isDraw) "🤝" else if (playerWon) "🏆" else "💔",
                            fontSize = 64.sp
                        )
                        Text(
                            text = when {
                                isDraw -> "¡Empate!"
                                playerWon -> "¡Victoria Épica!"
                                else -> "Derrota"
                            },
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold
                        )

                        Surface(
                            color = (if (rrDelta >= 0) Color(0xFF4ADE80) else Color(0xFFF87171)).copy(
                                alpha = 0.15f
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "${if (rrDelta >= 0) "+" else ""}$rrDelta RR",
                                color = if (rrDelta >= 0) Color(0xFF4ADE80) else Color(0xFFF87171),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Color.Black.copy(alpha = 0.2f),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    myDisplayName,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "$myScore pts",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(opponentName, color = TextMutedColor)
                                Text("$opponentScore pts", color = TextMutedColor)
                            }
                        }

                        Button(
                            onClick = {
                                ws?.disconnect()
                                onNavigate(returnTo)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(
                                text = if (returnTo == "online-game") "Volver a Jugar Online" else "Volver a amigos",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectionBadge(connectionState: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(
                    when (connectionState) {
                        "connected" -> Color(0xFF4ADE80)
                        "waiting" -> Color(0xFFFACC15)
                        "error" -> Color(0xFFF87171)
                        else -> Color.Gray
                    },
                    CircleShape
                )
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = when (connectionState) {
                "connected" -> "Conectado"
                "waiting" -> "Esperando..."
                "error" -> "Error conexión"
                "connecting" -> "Conectando..."
                else -> connectionState
            },
            fontSize = 11.sp,
            color = TextMutedColor
        )
    }
}

@Composable
private fun PlayerPanel1v1(
    modifier: Modifier = Modifier,
    name: String,
    rr: Int,
    avatarUrl: String?,
    pieceColor: Color,
    pieceLabel: String,
    score: Int,
    isActive: Boolean,
    paused: Boolean,
    reconnected: Boolean
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = when {
            paused -> Color(0xFF334155).copy(alpha = 0.35f)
            reconnected -> Color(0xFF14532D).copy(alpha = 0.4f)
            else -> Color.Black.copy(alpha = 0.28f)
        },
        border = BorderStroke(1.dp, when {
            paused -> Color(0xFF94A3B8)
            reconnected -> Color(0xFF4ADE80)
            isActive -> Color(0xFFFBBF24)
            else -> BorderColor
        })
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AvatarCircle(name = name, avatarUrl = avatarUrl)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = name,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text("$rr RR", color = Color(0xFFFBBF24), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(pieceColor)
                            .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                    )
                    Text(pieceLabel, color = TextMutedColor, fontSize = 11.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    Text("$score pts", color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp)
                }
            }

            if (paused) {
                Text(
                    "Ha pausado",
                    color = Color(0xFFCBD5E1),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.TopEnd).padding(top = 6.dp, end = 6.dp)
                )
            } else if (reconnected) {
                Text(
                    "Se ha reconectado",
                    color = Color(0xFF86EFAC),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.TopEnd).padding(top = 6.dp, end = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun AvatarCircle(name: String, avatarUrl: String?) {
    Surface(
        modifier = Modifier.size(36.dp),
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.1f)
    ) {
        Box(contentAlignment = Alignment.Center) {
            val presetRes = AvatarPresets.drawableForId(avatarUrl)
            when {
                presetRes != null -> {
                    Image(
                        painter = painterResource(id = presetRes),
                        contentDescription = "Avatar",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                !avatarUrl.isNullOrBlank() -> {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = "Avatar",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                else -> {
                    Text(
                        text = name.firstOrNull()?.uppercase() ?: "?",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

// CELDA ANIMADA EN 3D
@Composable
private fun GameCell1v1(
    modifier: Modifier,
    cellValue: String?,
    isValidMove: Boolean,
    style: BoardPieceStyle1v1,
    onClick: () -> Unit
) {
    // Lógica de rotación en 3D
    val targetRotation = when (cellValue) {
        "white" -> 180f
        "black" -> 0f
        else -> 0f
    }

    val rotationY by animateFloatAsState(
        targetValue = targetRotation,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "piece_flip"
    )

    // Lógica de "Pop" al aparecer la ficha
    var hasAppeared by remember { mutableStateOf(cellValue != null) }
    LaunchedEffect(cellValue) {
        if (cellValue != null) hasAppeared = true
    }
    val scale by animateFloatAsState(
        targetValue = if (hasAppeared) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "piece_pop"
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .border(0.5.dp, Color.Black.copy(alpha = 0.2f))
            .then(if (isValidMove) Modifier.clickable { onClick() } else Modifier),
        contentAlignment = Alignment.Center
    ) {
        // Indicador de movimiento válido
        if (isValidMove) {
            Box(
                modifier = Modifier
                    .fillMaxSize(0.34f)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.8f))
            )
        }

        // Ficha animada
        if (cellValue != null || hasAppeared) {
            // Cambiamos el color de la ficha a la mitad del giro (a los 90º)
            val isFlipped = rotationY > 90f
            val pieceColor = if (isFlipped) style.sideB else style.sideA

            Box(
                modifier = Modifier
                    .fillMaxSize(0.78f)
                    .graphicsLayer {
                        this.rotationY = rotationY
                        this.scaleX = scale
                        this.scaleY = scale
                        this.cameraDistance = 12f * density // Añade perspectiva 3D
                    }
                    .clip(CircleShape)
                    .background(pieceColor)
                    .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
            )
        }
    }
}

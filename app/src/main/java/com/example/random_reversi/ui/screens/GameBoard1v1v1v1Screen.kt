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
import androidx.compose.material3.SmallFloatingActionButton
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

private val BOARD_SIZE_4P = 16
private val PIECE_ORDER_4P = listOf("black", "white", "red", "blue")

data class QuadPlayer(
    val username: String,
    val rr: Int,
    val avatarUrl: String?,
    val piece: String
)

@Composable
fun GameBoard1v1v1v1Screen(
    gameId: Int = -1,
    returnTo: String = "online-game",
    onNavigate: (String) -> Unit
) {
    val ws = remember { if (gameId > 0) GameWebSocket(gameId) else null }
    val gameState by ws?.gameState?.collectAsState() ?: remember { mutableStateOf(com.example.random_reversi.data.remote.GameState()) }
    val myColor by ws?.myColor?.collectAsState() ?: remember { mutableStateOf<String?>(null) }
    val roomPlayersRaw by ws?.roomPlayers?.collectAsState() ?: remember { mutableStateOf(emptyList()) }
    val chatMessages by ws?.chatMessages?.collectAsState() ?: remember { mutableStateOf(emptyList()) }

    var myUsername by remember { mutableStateOf("Jugador") }
    var myElo by remember { mutableStateOf(1000) }
    var myAvatar by remember { mutableStateOf<String?>(null) }
    var quadStyle by remember { mutableStateOf(PIECE_STYLES_4P.first()) }
    var selectedQuadrant by remember { mutableStateOf<Int?>(null) }
    var showLeaveConfirm by remember { mutableStateOf(false) }
    var showPauseConfirm by remember { mutableStateOf(false) }
    var abandonNotice by remember { mutableStateOf<String?>(null) }
    var showChat by remember { mutableStateOf(false) }
    var unreadChatCount by remember { mutableStateOf(0) }
    var processedChatCount by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()
    var previousPausedPieces by remember { mutableStateOf(emptySet<String>()) }
    var reconnectedPieces by remember { mutableStateOf(emptySet<String>()) }

    val players = remember(roomPlayersRaw, myUsername, myElo, myAvatar) {
        val parsed = roomPlayersRaw.mapIndexedNotNull { index, raw ->
            try {
                val username = raw.get("username")?.asString ?: return@mapIndexedNotNull null
                val rr = raw.get("rr")?.asInt ?: 1000
                val avatar = if (raw.has("avatar_url") && !raw.get("avatar_url").isJsonNull) raw.get("avatar_url").asString else null
                QuadPlayer(
                    username = username,
                    rr = rr,
                    avatarUrl = avatar,
                    piece = PIECE_ORDER_4P.getOrElse(index) { PIECE_ORDER_4P.last() }
                )
            } catch (_: Exception) {
                null
            }
        }

        if (parsed.isEmpty()) {
            listOf(
                QuadPlayer(myUsername, myElo, myAvatar, "black"),
                QuadPlayer("Jugador 2", 1000, null, "white"),
                QuadPlayer("Jugador 3", 1000, null, "red"),
                QuadPlayer("Jugador 4", 1000, null, "blue")
            )
        } else {
            parsed
        }
    }

    val playerByPiece = remember(players) { players.associateBy { it.piece } }
    val myPlayer = remember(myColor, playerByPiece, myUsername, myElo, myAvatar) {
        val byColor = playerByPiece[myColor]
        byColor ?: playerByPiece["black"] ?: QuadPlayer(myUsername, myElo, myAvatar, myColor ?: "black")
    }

    val effectiveMyPiece = remember(myColor, gameState.username_by_piece, myUsername) {
        myColor ?: gameState.username_by_piece.entries.firstOrNull { it.value == myUsername }?.key
    }

    val isMyTurn = gameState.current_player == effectiveMyPiece
    val gameOver = gameState.game_over
    val abandonedPieces = gameState.abandoned_pieces
    val pausedPieces = gameState.paused_pieces
    val localIsAbandoned = effectiveMyPiece != null && abandonedPieces.contains(effectiveMyPiece)
    val localIsPaused = effectiveMyPiece != null && pausedPieces.contains(effectiveMyPiece)
    val canPlayThisTurn = isMyTurn && !gameOver && !localIsAbandoned
    val waitingForPausedPlayer = gameState.current_player != null && pausedPieces.contains(gameState.current_player)
    val hasOtherPausedPlayer = pausedPieces.any { it != effectiveMyPiece }

    val arenaTheme = remember(myPlayer.rr) { getArenaFromElo4p(myPlayer.rr) }

    LaunchedEffect(Unit) {
        when (val me = UserRepository.getMe()) {
            is UserResult.Success -> {
                myUsername = me.data.username
                myElo = me.data.elo
                myAvatar = me.data.avatar_url
                val (_, quadIndex) = decodeBoardPiecePreference(me.data.preferred_piece_color)
                quadStyle = PIECE_STYLES_4P.getOrElse(quadIndex) { PIECE_STYLES_4P.first() }
            }
            is UserResult.Error -> {
                quadStyle = PIECE_STYLES_4P.first()
            }
        }
    }

    LaunchedEffect(gameId) { ws?.connect() }

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

    LaunchedEffect(abandonedPieces.joinToString("|"), gameState.username_by_piece) {
        if (abandonedPieces.isEmpty()) return@LaunchedEffect
        val latest = abandonedPieces.last()
        val name = gameState.username_by_piece[latest]
            ?: playerByPiece[latest]?.username
            ?: latest
        abandonNotice = "$name ha abandonado la partida"
    }

    LaunchedEffect(abandonNotice) {
        if (abandonNotice == null) return@LaunchedEffect
        delay(2200)
        abandonNotice = null
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
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(44.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                InGameChatButton(
                    unreadCount = unreadChatCount,
                    onClick = { showChat = true }
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
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
                        onClick = { showLeaveConfirm = true },
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
                    canPlayThisTurn -> PrimaryColor
                    else -> SurfaceColor
                },
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
            ) {
                Text(
                    text = when {
                        gameOver -> if (gameState.winner == effectiveMyPiece) "Has ganado" else "Partida finalizada"
                        waitingForPausedPlayer -> "Partida pausada: Espera a que vuelva ${nameForPiece(gameState.current_player, gameState.username_by_piece, playerByPiece)}"
                        localIsPaused -> "Has pausado la partida"
                        canPlayThisTurn -> "Tu turno"
                        else -> "Turno de ${nameForPiece(gameState.current_player, gameState.username_by_piece, playerByPiece)}"
                    },
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            if (!abandonNotice.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(abandonNotice ?: "", color = Color(0xFFFCA5A5), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PlayerPanel4p(
                        modifier = Modifier.weight(1f),
                        player = playerByPiece["black"],
                        fallbackName = "Jugador 1",
                        fallbackColor = quadStyle.p1Name,
                        avatarOverride = if (effectiveMyPiece == "black") myAvatar else null,
                        score = gameState.scores["black"] ?: 0,
                        pieceColor = quadStyle.p1,
                        isActive = gameState.current_player == "black" && !gameOver,
                        abandoned = abandonedPieces.contains("black"),
                        paused = pausedPieces.contains("black"),
                        reconnected = reconnectedPieces.contains("black")
                    )
                    PlayerPanel4p(
                        modifier = Modifier.weight(1f),
                        player = playerByPiece["white"],
                        fallbackName = "Jugador 2",
                        fallbackColor = quadStyle.p2Name,
                        avatarOverride = if (effectiveMyPiece == "white") myAvatar else null,
                        score = gameState.scores["white"] ?: 0,
                        pieceColor = quadStyle.p2,
                        isActive = gameState.current_player == "white" && !gameOver,
                        abandoned = abandonedPieces.contains("white"),
                        paused = pausedPieces.contains("white"),
                        reconnected = reconnectedPieces.contains("white")
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PlayerPanel4p(
                        modifier = Modifier.weight(1f),
                        player = playerByPiece["red"],
                        fallbackName = "Jugador 3",
                        fallbackColor = quadStyle.p3Name,
                        avatarOverride = if (effectiveMyPiece == "red") myAvatar else null,
                        score = gameState.scores["red"] ?: 0,
                        pieceColor = quadStyle.p3,
                        isActive = gameState.current_player == "red" && !gameOver,
                        abandoned = abandonedPieces.contains("red"),
                        paused = pausedPieces.contains("red"),
                        reconnected = reconnectedPieces.contains("red")
                    )
                    PlayerPanel4p(
                        modifier = Modifier.weight(1f),
                        player = playerByPiece["blue"],
                        fallbackName = "Jugador 4",
                        fallbackColor = quadStyle.p4Name,
                        avatarOverride = if (effectiveMyPiece == "blue") myAvatar else null,
                        score = gameState.scores["blue"] ?: 0,
                        pieceColor = quadStyle.p4,
                        isActive = gameState.current_player == "blue" && !gameOver,
                        abandoned = abandonedPieces.contains("blue"),
                        paused = pausedPieces.contains("blue"),
                        reconnected = reconnectedPieces.contains("blue")
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF204D2B))
                    .border(4.dp, Color.Black.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            ) {
                val boardInset = 12.dp
                val boardData = gameState.board
                val hasBoard = boardData.isNotEmpty()

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(boardInset)
                ) {
                    Image(
                        painter = painterResource(id = arenaTheme.boardRes),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.FillBounds
                    )

                    if (selectedQuadrant == null) {
                        Column(Modifier.fillMaxSize()) {
                            repeat(2) { rowQ ->
                                Row(Modifier.weight(1f)) {
                                    repeat(2) { colQ ->
                                        val qIndex = rowQ * 2 + colQ
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .border(1.dp, Color.White.copy(alpha = 0.2f))
                                                .clickable { selectedQuadrant = qIndex },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (hasBoard) {
                                                QuadrantPreview(
                                                    board = boardData,
                                                    startRow = rowQ * 8,
                                                    startCol = colQ * 8,
                                                    style = quadStyle
                                                )
                                            }
                                            Box(
                                                Modifier
                                                    .fillMaxSize()
                                                    .background(Color.Black.copy(alpha = 0.1f))
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        val startRow = (selectedQuadrant!! / 2) * 8
                        val startCol = (selectedQuadrant!! % 2) * 8

                        Column(Modifier.fillMaxSize()) {
                            for (r in startRow until (startRow + 8)) {
                                Row(Modifier.weight(1f)) {
                                    for (c in startCol until (startCol + 8)) {
                                        val cellValue = boardData.getOrNull(r)?.getOrNull(c)
                                        val isValidMove = gameState.valid_moves.any { it.size >= 2 && it[0] == r && it[1] == c }
                                        val canPlayHere = canPlayThisTurn && isValidMove

                                        GameCell4p(
                                            modifier = Modifier.weight(1f),
                                            cellValue = cellValue,
                                            style = quadStyle,
                                            isValidMove = canPlayHere,
                                            onClick = {
                                                if (canPlayHere) {
                                                    ws?.sendMove(r, c)
                                                    selectedQuadrant = null
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (selectedQuadrant != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        SmallFloatingActionButton(
                            onClick = { selectedQuadrant = null },
                            containerColor = Color.Black.copy(alpha = 0.75f),
                            contentColor = Color.White,
                            shape = RoundedCornerShape(7.dp),
                            modifier = Modifier.size(50.dp)
                        ) {
                            Text("Volver", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }

        if (showLeaveConfirm) {
            AlertDialog(
                onDismissRequest = { showLeaveConfirm = false },
                containerColor = BgColor,
                title = { Text("Abandonar partida", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        if (returnTo == "friends" && hasOtherPausedPlayer && !localIsPaused)
                            "Como hay un jugador en pausa, si abandonas ahora no perderás RR y la partida quedará invalidada."
                        else
                            "Si abandonas la partida, se te registrará automáticamente como 4º puesto.",
                        color = TextMutedColor
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            scope.launch {
                                ws?.sendSurrender()
                                delay(120)
                                showLeaveConfirm = false
                                ws?.disconnect()
                                onNavigate(returnTo)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF87171))
                    ) {
                        Text("Abandonar partida")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLeaveConfirm = false }) {
                        Text("Seguir jugando", color = TextMutedColor)
                    }
                }
            )
        }

        if (showPauseConfirm) {
            AlertDialog(
                onDismissRequest = { showPauseConfirm = false },
                containerColor = BgColor,
                title = { Text("Pausar partida", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        "Podrás reanudar esta partida después desde la pestaña de amigos. Mientras tanto, los rivales quedarán esperando a que vuelvas.",
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

        if (gameOver) {
            val ranking = buildRanking4p(gameState.scores, abandonedPieces)
            val myRank = ranking.firstOrNull { it.piece == effectiveMyPiece }?.rank ?: 4
            val rrDelta = when (myRank) {
                1 -> 50
                2 -> 25
                3 -> 0
                else -> -25
            }
            val winnerText = when (val winnerPiece = gameState.winner) {
                null, "draw" -> "Empate"
                else -> nameForPiece(winnerPiece, gameState.username_by_piece, playerByPiece)
            }

            AlertDialog(
                onDismissRequest = {},
                containerColor = BgColor,
                title = { Text("Partida finalizada", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Ganador: $winnerText", color = Color.White, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Has quedado en ${myRank}º puesto", color = Color.White, fontWeight = FontWeight.Bold)
                            Text(
                                "${if (rrDelta >= 0) "+" else ""}$rrDelta RR",
                                color = if (rrDelta >= 0) Color(0xFF4ADE80) else Color(0xFFF87171),
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        ranking.forEach { row ->
                            val name = nameForPiece(row.piece, gameState.username_by_piece, playerByPiece)
                            val right = if (row.abandoned) "Abandonó" else "${row.score} pts"
                            Text("${row.rank}º - $name: $right", color = Color(0xFFE5E7EB), fontSize = 12.sp)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            ws?.disconnect()
                        onNavigate(returnTo)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)
                    ) {
                        Text(if (returnTo == "online-game") "Volver a Jugar Online" else "Volver a amigos")
                    }
                }
            )
        }
    }
}

data class Ranking4pRow(
    val piece: String,
    val score: Int,
    val abandoned: Boolean,
    val rank: Int
)

private fun buildRanking4p(scores: Map<String, Int>, abandoned: List<String>): List<Ranking4pRow> {
    val active = PIECE_ORDER_4P
        .filter { !abandoned.contains(it) }
        .map { piece -> Ranking4pRow(piece, scores[piece] ?: 0, false, 0) }
        .sortedByDescending { it.score }
        .mapIndexed { idx, row -> row.copy(rank = idx + 1) }

    val abandonedRows = PIECE_ORDER_4P
        .filter { abandoned.contains(it) }
        .map { piece -> Ranking4pRow(piece, scores[piece] ?: 0, true, 4) }

    return (active + abandonedRows).sortedWith(compareBy<Ranking4pRow> { it.rank }.thenByDescending { it.score })
}

private fun nameForPiece(
    piece: String?,
    usernameByPiece: Map<String, String>,
    playerByPiece: Map<String, QuadPlayer>
): String {
    if (piece.isNullOrBlank()) return "-"
    return usernameByPiece[piece] ?: playerByPiece[piece]?.username ?: piece
}

@Composable
private fun PlayerPanel4p(
    modifier: Modifier = Modifier,
    player: QuadPlayer?,
    fallbackName: String,
    fallbackColor: String,
    avatarOverride: String?,
    score: Int,
    pieceColor: Color,
    isActive: Boolean,
    abandoned: Boolean,
    paused: Boolean,
    reconnected: Boolean
) {
    val name = player?.username ?: fallbackName
    val avatar = avatarOverride ?: player?.avatarUrl
    val rr = player?.rr ?: 1000

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = when {
            abandoned -> Color(0xFF7F1D1D).copy(alpha = 0.35f)
            paused -> Color(0xFF334155).copy(alpha = 0.35f)
            reconnected -> Color(0xFF14532D).copy(alpha = 0.4f)
            else -> Color.Black.copy(alpha = 0.28f)
        },
        border = BorderStroke(
            1.dp,
            when {
                abandoned -> Color(0xFFEF4444)
                paused -> Color(0xFF94A3B8)
                reconnected -> Color(0xFF4ADE80)
                isActive -> Color(0xFFFBBF24)
                else -> BorderColor
            }
        )
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AvatarCircle4p(name = name, avatarUrl = avatar)
                Spacer(modifier = Modifier.width(6.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = name,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text("$rr RR", color = Color(0xFFFBBF24), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(pieceColor)
                        .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(fallbackColor, color = TextMutedColor, fontSize = 10.sp)
                Spacer(modifier = Modifier.weight(1f))
                Text("$score", color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp)
            }
            }
            if (abandoned) {
                Text(
                    "Ha abandonado",
                    color = Color(0xFFFCA5A5),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.TopEnd).padding(top = 6.dp, end = 6.dp)
                )
            } else if (paused) {
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
private fun ConnectionBadge4p(connectionState: String) {
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
private fun AvatarCircle4p(name: String, avatarUrl: String?) {
    Surface(
        modifier = Modifier.size(34.dp),
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
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun QuadrantPreview(
    board: List<List<String?>>,
    startRow: Int,
    startCol: Int,
    style: BoardPieceStyle4P
) {
    Column(Modifier.fillMaxSize().padding(2.dp)) {
        for (r in startRow until (startRow + 8)) {
            Row(Modifier.weight(1f)) {
                for (c in startCol until (startCol + 8)) {
                    val cell = board.getOrNull(r)?.getOrNull(c)
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .border(0.1.dp, Color.White.copy(alpha = 0.05f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (cell != null) {
                            Box(
                                Modifier
                                    .fillMaxSize(0.58f)
                                    .clip(CircleShape)
                                    .background(colorFromCell(cell, style))
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GameCell4p(
    modifier: Modifier,
    cellValue: String?,
    style: BoardPieceStyle4P,
    isValidMove: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .border(0.5.dp, Color.Black.copy(alpha = 0.2f))
            .then(if (isValidMove) Modifier.clickable { onClick() } else Modifier),
        contentAlignment = Alignment.Center
    ) {
        if (isValidMove) {
            Box(
                modifier = Modifier
                    .fillMaxSize(0.35f)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.8f))
            )
        }

        if (cellValue != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize(0.72f)
                    .clip(CircleShape)
                    .background(colorFromCell(cellValue, style))
                    .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
            )
        }
    }
}

private fun colorFromCell(cellValue: String, style: BoardPieceStyle4P): Color {
    return when (cellValue) {
        "black" -> style.p1
        "white" -> style.p2
        "red" -> style.p3
        "blue" -> style.p4
        else -> Color.Gray
    }
}

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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.example.random_reversi.ui.components.AppModal
import com.example.random_reversi.ui.theme.TextColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.random_reversi.utils.AvatarImage
import com.example.random_reversi.R
import com.example.random_reversi.data.UserRepository
import com.example.random_reversi.data.UserResult
import com.example.random_reversi.data.remote.GameWebSocket
import com.example.random_reversi.ui.theme.AccentGreen
import com.example.random_reversi.ui.theme.BorderColor
import com.example.random_reversi.ui.theme.PrimaryColor
import com.example.random_reversi.ui.theme.SurfaceColor
import com.example.random_reversi.ui.theme.TextMutedColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer

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
    variant: String = "skills",
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

    // ── Estado de habilidades ───────────────────────────────────────────────────
    val skillsInventory by ws?.skillsInventory?.collectAsState()
        ?: remember { mutableStateOf(com.example.random_reversi.data.remote.SkillsInventory()) }
    var pendingAbility by remember { mutableStateOf<PendingAbilityMobile?>(null) }
    var selectingGravityFor by remember { mutableStateOf<Int?>(null) } // inventoryIndex
    var showGravityMenu by remember { mutableStateOf(false) }
    var skillErrorMessage by remember { mutableStateOf<String?>(null) }
    var skillUsedPopup by remember { mutableStateOf<com.example.random_reversi.data.remote.SkillUsedEvent?>(null) }
    var previousMyInventory by remember { mutableStateOf<List<String>>(emptyList()) }
    var previousOpponentInventories by remember { mutableStateOf<Map<String, List<String>>>(emptyMap()) }
    val skillUsedEventFromWs by ws?.skillUsedEvent?.collectAsState()
        ?: remember { mutableStateOf<com.example.random_reversi.data.remote.SkillUsedEvent?>(null) }

    val players = remember(roomPlayersRaw, myUsername, myElo, myAvatar) {
        val parsed = roomPlayersRaw.mapIndexedNotNull { index, raw ->
            try {
                val username = raw.get("username")?.asString ?: return@mapIndexedNotNull null
                val rr = raw.get("rr")?.asInt ?: 1000
                val avatar =
                    if (raw.has("avatar_url") && !raw.get("avatar_url").isJsonNull) raw.get("avatar_url").asString else null
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
        byColor ?: playerByPiece["black"] ?: QuadPlayer(
            myUsername,
            myElo,
            myAvatar,
            myColor ?: "black"
        )
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
    val waitingForPausedPlayer =
        gameState.current_player != null && pausedPieces.contains(gameState.current_player)
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

    // ── Detectar uso de habilidad por cambio de inventario (4P) ──────────────────
    LaunchedEffect(skillsInventory, myUsername) {
        fun invOf(piece: String): List<String> = when (piece) {
            "black" -> skillsInventory.black
            "white" -> skillsInventory.white
            "red"   -> skillsInventory.red
            "blue"  -> skillsInventory.blue
            else    -> emptyList()
        }
        val myPiece = effectiveMyPiece ?: return@LaunchedEffect
        val myInv = invOf(myPiece)
        val opponentPieces = PIECE_ORDER_4P.filter { it != myPiece }

        // Diff de contenido (multiset): qué elementos salieron / entraron
        fun multisetSubtract(from: List<String>, minus: List<String>): List<String> {
            val remaining = minus.toMutableList()
            return from.filter { item ->
                val idx = remaining.indexOf(item)
                if (idx >= 0) { remaining.removeAt(idx); false } else true
            }
        }

        val myLost   = multisetSubtract(previousMyInventory, myInv)
        val myGained = multisetSubtract(myInv, previousMyInventory)

        // Calcular qué gano en total de todos los rivales
        val allOppGained = opponentPieces.flatMap { piece ->
            val prev = previousOpponentInventories[piece] ?: emptyList()
            multisetSubtract(invOf(piece), prev)
        }

        // YO usé una habilidad
        val iWasRobbed = myLost.any { it in allOppGained }
        if (myLost.isNotEmpty() && !iWasRobbed) {
            val usedSkill = myLost.firstOrNull()
            if (usedSkill != null) {
                skillUsedPopup = com.example.random_reversi.data.remote.SkillUsedEvent(usedSkill, myUsername, true)
            }
        }

        // Detectar que algún rival usó una habilidad
        opponentPieces.forEach { piece ->
            val prevInv = previousOpponentInventories[piece] ?: emptyList()
            val currInv = invOf(piece)
            val oppLost = multisetSubtract(prevInv, currInv)
            // Excluir items que acabaron en MI inventario (fueron transferidos a mí)
            val oppTrueUsed = oppLost.filter { it !in myGained }
            if (oppTrueUsed.isNotEmpty()) {
                val usedSkill = oppTrueUsed.firstOrNull()
                if (usedSkill != null) {
                    val rivalName = gameState.username_by_piece[piece]
                        ?: playerByPiece[piece]?.username ?: piece
                    skillUsedPopup = com.example.random_reversi.data.remote.SkillUsedEvent(usedSkill, rivalName, false)
                }
            }
        }

        previousMyInventory = myInv
        previousOpponentInventories = opponentPieces.associateWith { invOf(it) }
    }

    // ── Detectar evento skill_used del servidor ─────────────────────────────────
    LaunchedEffect(skillUsedEventFromWs) {
        skillUsedEventFromWs?.let { event ->
            skillUsedPopup = event.copy(isMine = event.username == myUsername)
        }
    }

    // ── Auto-ocultar popup tras 2500ms ──────────────────────────────────────────
    LaunchedEffect(skillUsedPopup) {
        if (skillUsedPopup != null) {
            delay(2500)
            skillUsedPopup = null
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.nuevofondomovil),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        val currentPlayerName = nameForPiece(
            gameState.current_player,
            gameState.username_by_piece,
            playerByPiece
        )
        val turnStatusText = when {
            gameOver -> if (gameState.winner == effectiveMyPiece) "Has ganado" else "Partida finalizada"
            waitingForPausedPlayer -> "Partida pausada"
            localIsPaused -> "Partida pausada"
            canPlayThisTurn -> "Tu turno"
            else -> "Turno del rival"
        }
        val displayTurnName = if (canPlayThisTurn) myUsername else currentPlayerName

        Box(
            modifier = Modifier
                .offset(x = 10.dp, y = 35.dp)
                .align(Alignment.TopStart)
        ) {
            Image(
                painter = painterResource(id = R.drawable.cartelturno),
                contentDescription = null,
                modifier = Modifier.size(width = 180.dp, height = 130.dp),
                contentScale = ContentScale.FillBounds
            )
            Column(
                modifier = Modifier.padding(start = 51.5.dp, top = 30.dp),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy((-2).dp)
            ) {
                Text(
                    text = "TURNO ACTUAL",
                    color = Color.Black,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = displayTurnName,
                    color = Color.Black,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = turnStatusText,
                    color = Color(0xFF1B4B3A),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp, end = 20.dp, start = 20.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Botón Pausar — solo visible en partidas de amigos
            if (returnTo == "friends") {
                Image(
                    painter = painterResource(id = R.drawable.botonpausa),
                    contentDescription = "Pausar",
                    modifier = Modifier
                        .height(55.dp)
                        .clickable { showPauseConfirm = true },
                    contentScale = ContentScale.FillHeight
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Image(
                painter = painterResource(id = R.drawable.salamovil_abandonar),
                contentDescription = "Abandonar",
                modifier = Modifier
                    .height(55.dp)
                    .clickable { showLeaveConfirm = true },
                contentScale = ContentScale.FillHeight
            )
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(start = 14.dp, end = 14.dp, bottom = 120.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(158.dp))

            if (!abandonNotice.isNullOrBlank()) {
                Text(
                    abandonNotice ?: "",
                    color = Color(0xFFFCA5A5),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PlayerCard4pIngame(
                        modifier = Modifier.weight(1f),
                        player = playerByPiece["black"],
                        fallbackName = "Jugador 1",
                        avatarOverride = if (effectiveMyPiece == "black") myAvatar else null,
                        score = gameState.scores["black"] ?: 0,
                        isActive = gameState.current_player == "black" && !gameOver,
                        abandoned = abandonedPieces.contains("black"),
                        paused = pausedPieces.contains("black"),
                        reconnected = reconnectedPieces.contains("black")
                    )
                    PlayerCard4pIngame(
                        modifier = Modifier.weight(1f),
                        player = playerByPiece["white"],
                        fallbackName = "Jugador 2",
                        avatarOverride = if (effectiveMyPiece == "white") myAvatar else null,
                        score = gameState.scores["white"] ?: 0,
                        isActive = gameState.current_player == "white" && !gameOver,
                        abandoned = abandonedPieces.contains("white"),
                        paused = pausedPieces.contains("white"),
                        reconnected = reconnectedPieces.contains("white")
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PlayerCard4pIngame(
                        modifier = Modifier.weight(1f),
                        player = playerByPiece["red"],
                        fallbackName = "Jugador 3",
                        avatarOverride = if (effectiveMyPiece == "red") myAvatar else null,
                        score = gameState.scores["red"] ?: 0,
                        isActive = gameState.current_player == "red" && !gameOver,
                        abandoned = abandonedPieces.contains("red"),
                        paused = pausedPieces.contains("red"),
                        reconnected = reconnectedPieces.contains("red")
                    )
                    PlayerCard4pIngame(
                        modifier = Modifier.weight(1f),
                        player = playerByPiece["blue"],
                        fallbackName = "Jugador 4",
                        avatarOverride = if (effectiveMyPiece == "blue") myAvatar else null,
                        score = gameState.scores["blue"] ?: 0,
                        isActive = gameState.current_player == "blue" && !gameOver,
                        abandoned = abandonedPieces.contains("blue"),
                        paused = pausedPieces.contains("blue"),
                        reconnected = reconnectedPieces.contains("blue")
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.88f)
            ) {
                Image(
                    painter = painterResource(id = arenaTheme.backgroundRes),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .border(4.dp, Color(0xFFF7F1E5), RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.FillBounds
                )

                // Grid del tablero
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.82f)
                        .aspectRatio(1f)
                        .align(Alignment.Center)
                ) {
                    val boardData = gameState.board
                    val hasBoard = boardData.isNotEmpty()

                    Image(
                        painter = painterResource(id = if (selectedQuadrant == null) arenaTheme.boardRes else arenaTheme.boardRes1v1),
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
                                                val showValidMoves = canPlayThisTurn && pendingAbility == null
                                                QuadrantPreview(
                                                    board = boardData,
                                                    startRow = rowQ * 8,
                                                    startCol = colQ * 8,
                                                    style = quadStyle,
                                                    skillTiles = gameState.skill_tiles,
                                                    validMoves = if (showValidMoves) gameState.valid_moves else emptyList()
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
                                        key(r * 16 + c) {
                                        val cellValue = boardData.getOrNull(r)?.getOrNull(c)
                                        val isValidMove =
                                            gameState.valid_moves.any { it.size >= 2 && it[0] == r && it[1] == c }
                                        val isSkillTile = gameState.skill_tiles.any { it.size >= 2 && it[0] == r && it[1] == c }
                                        val isFixed = gameState.fixed_pieces.any { it.size >= 2 && it[0] == r && it[1] == c }
                                        val canPlayHere = canPlayThisTurn && isValidMove && pendingAbility == null
                                        // Filtro de objetivo por tipo de habilidad
                                        val isValidPendingTarget = pendingAbility != null && !gameOver && canPlayThisTurn &&
                                            when (pendingAbility!!.abilityId) {
                                                "place_free"  -> cellValue == null
                                                "flip_rival"  -> cellValue != null && cellValue != effectiveMyPiece
                                                "fix_piece"   -> cellValue != null && cellValue == effectiveMyPiece
                                                "unfix_piece" -> cellValue != null && cellValue != effectiveMyPiece && isFixed
                                                else          -> true
                                            }

                                        GameCell4p(
                                            modifier = Modifier.weight(1f),
                                            cellValue = cellValue,
                                            style = quadStyle,
                                            isValidMove = canPlayHere,
                                            isSkillTile = isSkillTile,
                                            isFixed = isFixed,
                                            isPendingTarget = isValidPendingTarget && !canPlayHere,
                                            onClick = {
                                                if (isValidPendingTarget && !canPlayHere) {
                                                    val pa = pendingAbility!!
                                                    val opponents = playerByPiece.keys.filter { it != effectiveMyPiece && it in gameState.scores.keys }
                                                    val targetOpponent = opponents.maxByOrNull { gameState.scores[it] ?: 0 } ?: "black"
                                                    ws?.sendSkillTargeted(pa.abilityId, r, c, targetOpponent, pa.inventoryIndex)
                                                    pendingAbility = null
                                                    selectedQuadrant = null
                                                } else if (canPlayHere) {
                                                    ws?.sendMove(r, c)
                                                    selectedQuadrant = null
                                                }
                                            }
                                        )
                                        } // key
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

            Spacer(modifier = Modifier.height(40.dp))
        }

        // ── Panel inferior: Chat (Independiente / Flotante) ──
        Box(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .wrapContentHeight()
                .align(Alignment.BottomCenter)
                .offset(y = 45.dp) //-10
        ) {
            Image(
                painter = painterResource(id = R.drawable.ingame_paneljuego),
                contentDescription = null,
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.FillWidth
            )
            Row(modifier = Modifier.matchParentSize()) {
                // Chat
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(130.dp)
                            .offset(x = 15.dp, y = 10.dp)
                            .clickable { showChat = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ingame_iconochat),
                            contentDescription = "Chat",
                            modifier = Modifier
                                .size(70.dp)
                                .offset(x = (-5).dp, y = (-6).dp),
                            contentScale = ContentScale.Fit
                        )
                        if (unreadChatCount > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 4.dp, y = (-4).dp)
                                    .size(18.dp)
                                    .background(Color.Red, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (unreadChatCount > 9) "9+" else unreadChatCount.toString(),
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                
                // Habilidades
                val myInventory = when (effectiveMyPiece) {
                    "black" -> skillsInventory.black
                    "white" -> skillsInventory.white
                    "red" -> skillsInventory.red
                    "blue" -> skillsInventory.blue
                    else -> emptyList()
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                        .clipToBounds(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (myInventory.isEmpty()) {
                        if (pendingAbility != null || selectingGravityFor != null) {
                            SkillPendingBar(
                                text = if (selectingGravityFor != null) "Elige dirección" else "Toca casilla objetivo",
                                onCancel = { pendingAbility = null; selectingGravityFor = null; showGravityMenu = false }
                            )
                        } else {
                            Text(
                                text = "Sin habilidades",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.offset(x = (-22).dp, y = 0.dp)
                            )
                        }
                    } else {
                        if (pendingAbility != null) {
                            SkillPendingBar(
                                text = "Toca casilla objetivo",
                                onCancel = { pendingAbility = null }
                            )
                        } else if (selectingGravityFor != null) {
                            GravityDirectionRow(
                                onDirection = { dir ->
                                    ws?.sendSkillGravity(dir, selectingGravityFor!!)
                                    selectingGravityFor = null
                                    showGravityMenu = false
                                },
                                onCancel = { selectingGravityFor = null; showGravityMenu = false }
                            )
                        } else {
                            LazyRow(
                                modifier = Modifier
                                    .width(90.dp) // Ancho fijo: 44dp(hab1) + 8dp(espacio) + 44dp(hab2) + margen = recorta el scroll exactamente a 2
                                    .height(50.dp)
                                    .offset(x = 0.1.dp, y = 4.dp) // x=10 (un poco a la derecha), y=4 (más arriba para no pegar abajo)
                                    .clipToBounds(), // Corta visualmente cualquier cosa que sobresalga de este recuadro al hacer scroll
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                itemsIndexed(myInventory) { idx, abilityId ->
                                    val meta = ABILITY_META_MOVIL[abilityId]
                                        ?: AbilityMeta(abilityId, R.drawable.ingame_casillainterrogante, true)
                                    val isSelected = pendingAbility?.inventoryIndex == idx
                                    val canUse = canPlayThisTurn && !gameOver
                                    SkillButton(
                                        meta = meta,
                                        isSelected = isSelected,
                                        canUse = canUse,
                                        onClick = {
                                            if (!canUse) return@SkillButton
                                            // Inventarios de rivales
                                            val opponentPieces = playerByPiece.keys.filter { it != effectiveMyPiece && it in gameState.scores.keys }
                                            fun inventoryOf(piece: String) = when (piece) {
                                                "black" -> skillsInventory.black
                                                "white" -> skillsInventory.white
                                                "red"   -> skillsInventory.red
                                                "blue"  -> skillsInventory.blue
                                                else    -> emptyList()
                                            }
                                            val anyRivalHasSkills = opponentPieces.any { inventoryOf(it).isNotEmpty() }
                                            val rivalHasFixed = gameState.fixed_pieces.any { cell ->
                                                val pieceAt = gameState.board.getOrNull(cell[0])?.getOrNull(cell[1])
                                                pieceAt != null && pieceAt != effectiveMyPiece
                                            }
                                            val errorMsg = when (abilityId) {
                                                "steal_skill"    -> if (!anyRivalHasSkills) "Ningún rival tiene habilidades que robar." else null
                                                "give_skill"     -> if (myInventory.size < 2) "No tienes ninguna otra habilidad para dar." else null
                                                "exchange_skill" -> when {
                                                    myInventory.size < 2 -> "No tienes ninguna otra habilidad para intercambiar."
                                                    !anyRivalHasSkills   -> "Ningún rival tiene habilidades para intercambiar."
                                                    else                 -> null
                                                }
                                                "unfix_piece"    -> if (!rivalHasFixed) "No hay fichas fijas de ningún rival en el tablero." else null
                                                else -> null
                                            }
                                            if (errorMsg != null) {
                                                skillErrorMessage = errorMsg
                                                return@SkillButton
                                            }
                                            if (abilityId == "gravity") {
                                                selectingGravityFor = idx
                                                showGravityMenu = true
                                            } else if (meta.needsTarget) {
                                                pendingAbility = if (isSelected) null
                                                else PendingAbilityMobile(abilityId, idx)
                                            } else {
                                                val opponents = playerByPiece.keys.filter { it != effectiveMyPiece && it in gameState.scores.keys }
                                                val targetOpponent = opponents.maxByOrNull { gameState.scores[it] ?: 0 } ?: "black"
                                                // Para habilidades sociales necesitamos target_inventory_index
                                                val targetInvIndex = when (abilityId) {
                                                    "steal_skill", "exchange_skill" ->
                                                        // Índice 0: primer habilidad del rival objetivo
                                                        0
                                                    "give_skill" ->
                                                        // Primera habilidad nuestra que NO sea el propio give_skill
                                                        myInventory.indexOfFirst { it != "give_skill" }
                                                            .takeIf { it >= 0 } ?: 0
                                                    else -> 0
                                                }
                                                ws?.sendSkillInstant(abilityId, targetOpponent, idx, targetInvIndex)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        AppModal(
            isOpen = showLeaveConfirm,
            onClose = { showLeaveConfirm = false },
            maxWidth = 360.dp,
            showCloseButton = false
        ) {
            Text(
                "Abandonar partida",
                color = TextColor,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                if (returnTo == "friends" && hasOtherPausedPlayer && !localIsPaused)
                    "Como hay un jugador en pausa, si abandonas ahora no perderás RR y la partida quedará invalidada."
                else
                    "Si abandonas la partida, se te registrará automáticamente como 4º puesto.",
                color = TextMutedColor,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(20.dp))
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
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)
            ) {
                Text("Abandonar partida", color = Color.White)
            }
            TextButton(
                onClick = { showLeaveConfirm = false },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Seguir jugando", color = TextMutedColor)
            }
        }

        AppModal(
            isOpen = showPauseConfirm,
            onClose = { showPauseConfirm = false },
            maxWidth = 360.dp,
            showCloseButton = false
        ) {
            Text(
                "Pausar partida",
                color = TextColor,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Podrás reanudar esta partida después desde la pestaña de amigos. Mientras tanto, los rivales quedarán esperando a que vuelvas.",
                color = TextMutedColor,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = {
                    ws?.sendPause()
                    showPauseConfirm = false
                    ws?.disconnect()
                    onNavigate("friends")
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)
            ) {
                Text("Pausar y salir", color = Color.White)
            }
            TextButton(
                onClick = { showPauseConfirm = false },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancelar", color = TextMutedColor)
            }
        }

        if (showChat) {
            InGameChatOverlay(
                messages = chatMessages,
                myUsername = myUsername,
                onClose = { showChat = false },
                onSend = { message -> ws?.sendChat(message) }
            )
        }

        // ── Pop-up de habilidad usada ────────────────────────────────────────────
        skillUsedPopup?.let { event ->
            SkillUsedPopup(event = event, myUsername = myUsername)
        }

        // Modal: habilidad no disponible
        AppModal(
            isOpen = skillErrorMessage != null,
            onClose = { skillErrorMessage = null },
            maxWidth = 320.dp,
            showCloseButton = false
        ) {
            Text(
                text = "⛔ Habilidad no disponible",
                color = TextColor,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = skillErrorMessage ?: "",
                color = TextMutedColor,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = { skillErrorMessage = null },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)
            ) {
                Text("Entendido", color = Color.White)
            }
        }

        // NUEVA PANTALLA DE VICTORIA
        AnimatedVisibility(
            visible = gameOver,
            enter = fadeIn(tween(400)) + scaleIn(
                initialScale = 0.8f,
                animationSpec = spring(dampingRatio = 0.7f)
            ),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
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

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable(enabled = false) {},
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .wrapContentHeight(),
                    shape = RoundedCornerShape(12.dp),
                    color = SurfaceColor,
                    border = BorderStroke(1.dp, BorderColor)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = if (myRank == 1) "¡Victoria!" else "Ganador: $winnerText",
                            color = TextColor,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold
                        )

                        if (returnTo != "menu") {
                            Surface(
                                color = (if (rrDelta >= 0) AccentGreen else PrimaryColor).copy(alpha = 0.12f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = "${if (rrDelta >= 0) "+" else ""}$rrDelta RR",
                                    color = if (rrDelta >= 0) AccentGreen else PrimaryColor,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(BorderColor.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ranking.forEach { row ->
                                val name = nameForPiece(
                                    row.piece,
                                    gameState.username_by_piece,
                                    playerByPiece
                                )
                                val right = if (row.abandoned) "Abandonó" else "${row.score} pts"
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "${row.rank}º - $name",
                                        color = if (row.piece == effectiveMyPiece) TextColor else TextMutedColor,
                                        fontWeight = if (row.piece == effectiveMyPiece) FontWeight.Bold else FontWeight.Normal
                                    )
                                    Text(
                                        right,
                                        color = if (row.piece == effectiveMyPiece) TextColor else TextMutedColor
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = {
                                ws?.disconnect()
                                onNavigate(returnTo)
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = if (returnTo == "online-game") "Volver a Jugar Online" else "Volver a amigos",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
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
private fun PlayerCard4pIngame(
    modifier: Modifier = Modifier,
    player: QuadPlayer?,
    fallbackName: String,
    avatarOverride: String?,
    score: Int,
    isActive: Boolean,
    abandoned: Boolean,
    paused: Boolean,
    reconnected: Boolean
) {
    val name = player?.username ?: fallbackName
    val avatar = avatarOverride ?: player?.avatarUrl

    Box(modifier = modifier.height(72.dp)) {
        Image(
            painter = painterResource(id = R.drawable.ingame_carteljugador),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Avatar con borde activo
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .offset(x = 8.dp)
                    .rotate(-3f)
                    .border(
                        2.dp,
                        when {
                            abandoned -> Color(0xFFEF4444)
                            reconnected -> Color(0xFF4ADE80)
                            isActive -> Color(0xFFFBBF24)
                            paused -> Color(0xFF94A3B8)
                            else -> Color.Transparent
                        },
                        RoundedCornerShape(6.dp)
                    )
            ) {
                AvatarCircle4p(
                    name = name,
                    avatarUrl = avatar,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .offset(x = 22.dp)
            ) {
                val displayName = if (name.length > 10) name.take(10) + "..." else name
                Text(
                    text = displayName,
                    color = Color.Black,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = when {
                        abandoned -> "Abandonó"
                        reconnected -> "Reconectó"
                        paused -> "Pausado"
                        else -> "$score pts"
                    },
                    color = when {
                        abandoned -> Color(0xFF7F1D1D)
                        reconnected -> Color(0xFF14532D)
                        paused -> Color(0xFF64748B)
                        else -> Color(0xFF5C3D11)
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
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
private fun AvatarCircle4p(name: String, avatarUrl: String?, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = Color.White.copy(alpha = 0.1f)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            AvatarImage(
                avatarUrl = avatarUrl,
                contentDescription = "Avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                fallback = {
                    Text(
                        text = name.firstOrNull()?.uppercase() ?: "?",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            )
        }
    }
}

@Composable
private fun QuadrantPreview(
    board: List<List<String?>>,
    startRow: Int,
    startCol: Int,
    style: BoardPieceStyle4P,
    skillTiles: List<List<Int>> = emptyList(),
    validMoves: List<List<Int>> = emptyList()
) {
    Column(Modifier.fillMaxSize().padding(2.dp)) {
        for (r in startRow until (startRow + 8)) {
            Row(Modifier.weight(1f)) {
                for (c in startCol until (startCol + 8)) {
                    val cell = board.getOrNull(r)?.getOrNull(c)
                    val isSkillTile = skillTiles.any { it.size >= 2 && it[0] == r && it[1] == c }
                    val isMoveTarget = validMoves.any { it.size >= 2 && it[0] == r && it[1] == c }
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .border(0.1.dp, Color.White.copy(alpha = 0.05f)),
                        contentAlignment = Alignment.Center
                    ) {
                        // Fondo morado: solo cuando hay una ficha tapando una casilla de habilidad
                        if (isSkillTile && cell != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFF7C3AED).copy(alpha = 0.60f))
                            )
                        }
                        if (isSkillTile && cell == null) {
                            Image(
                                painter = painterResource(id = R.drawable.ingame_casillainterrogante),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(0.7f),
                                contentScale = ContentScale.Fit
                            )
                        }
                        // Indicador de movimiento válido (Puntito blanco)
                        if (isMoveTarget && cell == null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize(0.35f)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.45f))
                            )
                        }
                        if (cell != null) {
                            val pieceColor = colorFromCell(cell, style)
                            Box(
                                Modifier
                                    .fillMaxSize(0.58f)
                                    .clip(CircleShape)
                                    .background(pieceColor)
                                    .border(0.5.dp, lightenColor(pieceColor, 0.38f), CircleShape)
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
    isSkillTile: Boolean = false,
    isFixed: Boolean = false,
    isPendingTarget: Boolean = false,
    onClick: () -> Unit
) {
    // Lógica de "Pop" al aparecer
    var hasAppeared by remember { mutableStateOf(cellValue != null) }

    // Rotación continua en 3D para 4 colores
    var rotationY by remember { mutableStateOf(0f) }
    var currentColor by remember { mutableStateOf(cellValue) }

    LaunchedEffect(cellValue) {
        if (cellValue != null) {
            hasAppeared = true
            // Si el color cambia (alguien te come), da una voltereta
            if (currentColor != null && currentColor != cellValue) {
                rotationY += 180f
            }
            currentColor = cellValue
        } else {
            // La ficha desapareció (gravedad u otra habilidad): limpiar estado
            hasAppeared = false
            currentColor = null
        }
    }

    val animatedRotation by animateFloatAsState(
        targetValue = rotationY,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "piece_flip_4p"
    )

    val scale by animateFloatAsState(
        targetValue = if (hasAppeared) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "piece_pop_4p"
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .border(0.5.dp, Color.Black.copy(alpha = 0.2f))
            .then(if (isValidMove || isPendingTarget) Modifier.clickable { onClick() } else Modifier),
        contentAlignment = Alignment.Center
    ) {
        // 1a. Fondo morado: solo cuando hay una ficha tapando una casilla de habilidad
        if (isSkillTile && cellValue != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF7C3AED).copy(alpha = 0.60f))
            )
        }

        // 1b. Interrogante — solo si la casilla está vacía
        if (isSkillTile && cellValue == null) {
            Image(
                painter = painterResource(id = R.drawable.ingame_casillainterrogante),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(0.72f),
                contentScale = ContentScale.Fit
            )
        }

        if (isValidMove) {
            Box(
                modifier = Modifier
                    .fillMaxSize(0.35f)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.45f))
            )
        }

        if (cellValue != null || hasAppeared) {
            Box(
                modifier = Modifier
                    .fillMaxSize(0.72f)
                    .graphicsLayer {
                        this.rotationY = animatedRotation
                        this.scaleX = scale
                        this.scaleY = scale
                        this.cameraDistance = 12f * density // Perspectiva 3D
                    }
                    .clip(CircleShape)
                    .background(colorFromCell(currentColor ?: "black", style))
                    .border(
                        width = if (isFixed) 2.dp else 1.5.dp,
                        color = if (isFixed) Color(0xFFFFD700) else lightenColor(colorFromCell(currentColor ?: "black", style), 0.38f),
                        shape = CircleShape
                    )
            )
        }

        // Badge de ficha fija (candado) — esquina inferior derecha
        if (isFixed && cellValue != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(2.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                Box(
                    modifier = Modifier
                        .size(11.dp)
                        .background(Color(0xFFFFD700), CircleShape)
                        .border(0.5.dp, Color(0xFFB8860B), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🔒",
                        fontSize = 6.sp,
                        lineHeight = 6.sp
                    )
                }
            }
        }

        if (isPendingTarget) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFFFD700).copy(alpha = 0.18f))
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

private fun lightenColor(color: Color, fraction: Float = 0.38f): Color {
    return Color(
        red = (color.red + (1f - color.red) * fraction).coerceIn(0f, 1f),
        green = (color.green + (1f - color.green) * fraction).coerceIn(0f, 1f),
        blue = (color.blue + (1f - color.blue) * fraction).coerceIn(0f, 1f),
        alpha = color.alpha
    )
}

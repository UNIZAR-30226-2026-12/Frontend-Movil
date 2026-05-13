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
import com.example.random_reversi.ui.theme.SecondaryColor
import com.example.random_reversi.ui.theme.SurfaceColor
import com.example.random_reversi.ui.theme.TextMutedColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Popup
import com.example.random_reversi.data.remote.SkillsInventory
import com.example.random_reversi.data.remote.SkillUsedEvent

private val BOARD_SIZE = 8

// Metadatos de cada habilidad: nombre, drawable res y si necesita selección de casilla
data class AbilityMeta(val name: String, val drawableRes: Int, val needsTarget: Boolean, val description: String = "")

val ABILITY_META_MOVIL = mapOf(
    "bomb"              to AbilityMeta("Bomba 3x3",            R.drawable.ingame_skill_bomba,                true,  "Elimina todas las fichas en un área 3×3 alrededor de la casilla elegida."),
    "fix_piece"         to AbilityMeta("Fijar ficha",          R.drawable.ingame_skill_ficha_fija,           true,  "Fija una ficha tuya para que no pueda ser volteada por el rival."),
    "unfix_piece"       to AbilityMeta("Quitar fijación",      R.drawable.ingame_skill_quitar_ficha_fija,    true,  "Elimina el estado fijado de una ficha rival."),
    "flip_rival"        to AbilityMeta("Girar rival",          R.drawable.ingame_skill_voltear_ficha,        true,  "Voltea una ficha rival a tu color sin gastar un movimiento."),
    "place_free"        to AbilityMeta("Ficha libre",          R.drawable.ingame_skill_ficha_libre,          true,  "Coloca una ficha tuya en cualquier casilla vacía del tablero."),
    "skip_rival"        to AbilityMeta("Saltar turno",         R.drawable.ingame_skill_saltar_turno,         false, "Hace que el rival pierda su próximo turno."),
    "steal_skill"       to AbilityMeta("Robar habilidad",      R.drawable.ingame_skill_robar_habilidad,      false, "Roba una habilidad aleatoria del inventario rival."),
    "exchange_skill"    to AbilityMeta("Intercambiar hab.",    R.drawable.ingame_skill_intercambiar_habilidad,false, "Intercambia una habilidad aleatoria con el rival."),
    "give_skill"        to AbilityMeta("Dar habilidad",        R.drawable.ingame_skill_dar_habilidad,        false, "Regala una de tus habilidades al rival (a cambio de un beneficio)."),
    "swap_colors"       to AbilityMeta("Cambiar colores",      R.drawable.ingame_skill_intercambio_color,    false, "Intercambia el color de todas las fichas del tablero."),
    "lose_turn"         to AbilityMeta("Perder turno",         R.drawable.ingame_skill_perder_turno,         false, "Sacrifica tu turno actual a cambio de una ventaja futura."),
    "gravity"           to AbilityMeta("Gravedad",             R.drawable.ingame_skill_gravedad,             false, "Mueve todas las fichas del tablero en una dirección elegida.")
)

data class PendingAbilityMobile(val abilityId: String, val inventoryIndex: Int)
data class PendingTransferSkillMobile(val abilityId: String, val inventoryIndex: Int)

data class BoardPlayer(
    val username: String,
    val rr: Int,
    val avatarUrl: String?
)

@Composable
fun GameBoard1v1Screen(
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
    var duelStyle by remember { mutableStateOf(PIECE_STYLES_1V1.first()) }
    var showSurrenderConfirm by remember { mutableStateOf(false) }
    var showPauseConfirm by remember { mutableStateOf(false) }
    var showChat by remember { mutableStateOf(false) }
    var unreadChatCount by remember { mutableStateOf(0) }
    var processedChatCount by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()
    var previousPausedPieces by remember { mutableStateOf(emptySet<String>()) }
    var reconnectedPieces by remember { mutableStateOf(emptySet<String>()) }

    // ── Estado de habilidades ───────────────────────────────────────────────────
    val skillsInventory by ws?.skillsInventory?.collectAsState()
        ?: remember { mutableStateOf(SkillsInventory()) }
    var pendingAbility by remember { mutableStateOf<PendingAbilityMobile?>(null) }
    var pendingTransferSkill by remember { mutableStateOf<PendingTransferSkillMobile?>(null) }
    var selectingGravityFor by remember { mutableStateOf<Int?>(null) } // inventoryIndex
    var showGravityMenu by remember { mutableStateOf(false) }
    var skillErrorMessage by remember { mutableStateOf<String?>(null) }
    var skillUsedPopup by remember { mutableStateOf<SkillUsedEvent?>(null) }
    var previousMyInventory by remember { mutableStateOf<List<String>>(emptyList()) }
    var previousOpponentInventory by remember { mutableStateOf<List<String>>(emptyList()) }
    val skillUsedEventFromWs by ws?.skillUsedEvent?.collectAsState()
        ?: remember { mutableStateOf<SkillUsedEvent?>(null) }

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

    val effectiveMyPiece = myColor
        ?: gameState.username_by_piece.entries.firstOrNull { it.value == myUsername }?.key

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

    // ── Detectar uso de habilidad por cambio de inventario ─────────────────────
    LaunchedEffect(skillsInventory, myUsername) {
        val myInv = if (effectiveMyPiece == "black") skillsInventory.black else skillsInventory.white
        val oppInv = if (effectiveMyPiece == "black") skillsInventory.white else skillsInventory.black

        // Diff de contenido (multiset): qué elementos salieron / entraron en cada inventario
        fun multisetSubtract(from: List<String>, minus: List<String>): List<String> {
            val remaining = minus.toMutableList()
            return from.filter { item ->
                val idx = remaining.indexOf(item)
                if (idx >= 0) { remaining.removeAt(idx); false } else true
            }
        }

        if (previousMyInventory.isNotEmpty() || previousOpponentInventory.isNotEmpty()) {
            val myLost    = multisetSubtract(previousMyInventory,     myInv)
            val myGained  = multisetSubtract(myInv,                   previousMyInventory)
            val oppLost   = multisetSubtract(previousOpponentInventory, oppInv)

            // YO usé una habilidad: algo salió de mi inventario
            // Para steal_skill: pierdo "steal_skill" y gano la robada → myLost=["steal_skill"]
            // Descartamos items que en realidad me los robó el rival (oppGained)
            val oppGained = multisetSubtract(oppInv, previousOpponentInventory)
            val iWasRobbed = myLost.any { it in oppGained }
            if (myLost.isNotEmpty() && !iWasRobbed) {
                val usedSkill = myLost.firstOrNull()
                if (usedSkill != null) {
                    skillUsedPopup = SkillUsedEvent(usedSkill, myUsername, true)
                }
            }

            // El RIVAL usó una habilidad: algo salió de su inventario Y
            // ese item NO acabó en MI inventario (no fue transferido a mí)
            val oppTrueUsed = oppLost.filter { it !in myGained }
            if (oppTrueUsed.isNotEmpty()) {
                val usedSkill = oppTrueUsed.firstOrNull()
                if (usedSkill != null) {
                    skillUsedPopup = SkillUsedEvent(usedSkill, opponentName, false)
                }
            }
        }

        previousMyInventory = myInv
        previousOpponentInventory = oppInv
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

    Box(modifier = Modifier.fillMaxSize()) {
        // ── Fondo (mismo que FriendsScreen) ───────────────────────────
        Image(
            painter = painterResource(id = R.drawable.nuevofondomovil),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // ── Nueva Cabecera Flotante (Turno + Abandonar) ──────────────
        val turnStatusText = when {
            gameOver -> when {
                gameState.winner == null -> "¡Empate!"
                gameState.winner == effectiveMyPiece -> "¡Has ganado!"
                else -> "Has perdido"
            }
            waitingForPausedPlayer -> "Partida pausada"
            localIsPaused -> "Partida pausada"
            isMyTurn -> "Tu turno"
            else -> "Turno del rival"
        }
        // Nombre e ícono del jugador cuyo turno es ahora
        val currentTurnName = if (isMyTurn || gameOver) myDisplayName else opponentName
        val currentTurnPieceName = when (gameState.current_player) {
            "black" -> duelStyle.sideAName
            "white" -> duelStyle.sideBName
            else -> if (isMyTurn) myPieceName else opponentPieceName
        }

        // ── Información de Turno (Independiente / Flotante) ──────────
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
                    text = "$currentTurnName ($currentTurnPieceName)",
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
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(8.dp))
            }
            Image(
                painter = painterResource(id = R.drawable.salamovil_abandonar),
                contentDescription = "Abandonar",
                modifier = Modifier
                    .height(55.dp)
                    .clickable { showSurrenderConfirm = true },
                contentScale = ContentScale.FillHeight
            )
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(start = 14.dp, end = 14.dp, bottom = 120.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(158.dp)) // 38dp original + ~120dp for the removed header area



            // ── Tarjetas de jugadores ─────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PlayerCardIngame(
                    modifier = Modifier.weight(1f),
                    name = myDisplayName,
                    score = myScore,
                    avatarUrl = myDisplayAvatar,
                    isActive = isMyTurn && !gameOver,
                    paused = localIsPaused
                )
                PlayerCardIngame(
                    modifier = Modifier.weight(1f),
                    name = opponentName,
                    score = opponentScore,
                    avatarUrl = opponentAvatar,
                    isActive = !isMyTurn && !gameOver,
                    paused = opponentIsPaused
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Tablero ───────────────────────────────────────────────
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
                                    key(row * BOARD_SIZE + col) {
                                        val cellValue = gameState.board.getOrNull(row)?.getOrNull(col)
                                        val isValidMove = gameState.valid_moves.any { it.size >= 2 && it[0] == row && it[1] == col }
                                        val isSkillTile = gameState.skill_tiles.any { it.size >= 2 && it[0] == row && it[1] == col }
                                        val isFixed = gameState.fixed_pieces.any { it.size >= 2 && it[0] == row && it[1] == col }
                                        val canPlayHere = isMyTurn && isValidMove && !gameOver && pendingAbility == null
                                        // Filtro de objetivo por tipo de habilidad
                                        val isValidPendingTarget = pendingAbility != null && !gameOver && isMyTurn &&
                                            when (pendingAbility!!.abilityId) {
                                                "place_free"  -> cellValue == null
                                                "flip_rival"  -> cellValue != null && cellValue != effectiveMyPiece
                                                "fix_piece"   -> cellValue != null && cellValue == effectiveMyPiece
                                                "unfix_piece" -> cellValue != null && cellValue != effectiveMyPiece && isFixed
                                                else          -> true
                                            }
                                        GameCell1v1(
                                            modifier = Modifier.weight(1f),
                                            cellValue = cellValue,
                                            isValidMove = canPlayHere,
                                            isSkillTile = isSkillTile,
                                            isFixed = isFixed,
                                            isPendingTarget = isValidPendingTarget && !canPlayHere,
                                            style = duelStyle,
                                            onClick = {
                                                if (isValidPendingTarget && !canPlayHere) {
                                                    val pa = pendingAbility!!
                                                    val opponent = if (effectiveMyPiece == "black") "white" else "black"
                                                    ws?.sendSkillTargeted(pa.abilityId, row, col, opponent, pa.inventoryIndex)
                                                    pendingAbility = null
                                                } else if (canPlayHere) {
                                                    ws?.sendMove(row, col)
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
            Spacer(modifier = Modifier.height(40.dp))
        } // Fin de la Column (232)
 
        // ── Panel inferior: Chat + Habilidades (Independiente / Flotante) ──
        Box(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .wrapContentHeight()
                .align(Alignment.BottomCenter)
                .offset(y = -10.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ingame_paneljuego3),
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
                                .offset(x = (-5).dp),
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
                val myInventory = if (effectiveMyPiece == "black") skillsInventory.black else skillsInventory.white
                val opponentColor = if (effectiveMyPiece == "black") "white" else "black"
                val opponentInventory = if (opponentColor == "black") skillsInventory.black else skillsInventory.white

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (myInventory.isEmpty()) {
                        // Habilidad pendiente activa aunque no haya inventario (e.g. gravedad)
                        if (pendingAbility != null || selectingGravityFor != null || pendingTransferSkill != null) {
                            SkillPendingBar(
                                text = if (selectingGravityFor != null) "Elige dirección" else "Toca casilla objetivo",
                                onCancel = { pendingAbility = null; selectingGravityFor = null; showGravityMenu = false; pendingTransferSkill = null }
                            )
                        } else {
                            Text(
                                text = "Sin habilidades",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.offset(x = (-10).dp, y = 5.dp)
                            )
                        }
                    } else {
                        // Barra pendiente flotante encima
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
                        } else if (pendingTransferSkill != null) {
                            val pts = pendingTransferSkill!!
                            val label = if (pts.abilityId == "exchange_skill") "¿Qué habilidad intercambias?" else "¿Qué habilidad das?"
                            SkillPendingBar(
                                text = label,
                                onCancel = { pendingTransferSkill = null }
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(96.dp)
                                    .offset(x = 20.dp, y = 5.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                itemsIndexed(myInventory.chunked(2)) { chunkIdx, chunk ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        chunk.forEachIndexed { i, abilityId ->
                                            val idx = chunkIdx * 2 + i
                                            val meta = ABILITY_META_MOVIL[abilityId]
                                                ?: AbilityMeta(abilityId, R.drawable.ingame_casillainterrogante, true)
                                            val isSelected = pendingAbility?.inventoryIndex == idx
                                            val canUse = isMyTurn && !gameOver
                                            SkillButton(
                                                meta = meta,
                                                isSelected = isSelected,
                                                canUse = canUse,
                                                onClick = {
                                                    if (!canUse) return@SkillButton
                                                    // Si hay un transfer pendiente, este clic elige la habilidad a dar
                                                    val pts = pendingTransferSkill
                                                    if (pts != null && idx != pts.inventoryIndex) {
                                                        ws?.sendSkillWithGiven(pts.abilityId, opponentColor, pts.inventoryIndex, idx)
                                                        pendingTransferSkill = null
                                                        return@SkillButton
                                                    }
                                                    // Validar condiciones de uso
                                                    val rivalHasFixed = gameState.fixed_pieces.any { cell ->
                                                        val pieceAt = gameState.board.getOrNull(cell[0])?.getOrNull(cell[1])
                                                        pieceAt != null && pieceAt != effectiveMyPiece
                                                    }
                                                    val errorMsg = when (abilityId) {
                                                        "steal_skill"    -> if (opponentInventory.isEmpty()) "El rival no tiene habilidades que robar." else null
                                                        "give_skill"     -> if (myInventory.size < 2) "No tienes ninguna otra habilidad para dar al rival." else null
                                                        "exchange_skill" -> when {
                                                            myInventory.size < 2        -> "No tienes ninguna otra habilidad para intercambiar."
                                                            opponentInventory.isEmpty() -> "El rival no tiene habilidades para intercambiar."
                                                            else                        -> null
                                                        }
                                                        "unfix_piece"    -> if (!rivalHasFixed) "No hay fichas fijas del rival en el tablero." else null
                                                        else -> null
                                                    }
                                                    if (errorMsg != null) {
                                                        skillErrorMessage = errorMsg
                                                        return@SkillButton
                                                    }
                                                    if (abilityId == "gravity") {
                                                        selectingGravityFor = idx
                                                        showGravityMenu = true
                                                    } else if (abilityId == "exchange_skill" || abilityId == "give_skill") {
                                                        pendingTransferSkill = PendingTransferSkillMobile(abilityId, idx)
                                                    } else if (meta.needsTarget) {
                                                        pendingAbility = if (isSelected) null
                                                        else PendingAbilityMobile(abilityId, idx)
                                                    } else {
                                                        ws?.sendSkillInstant(abilityId, opponentColor, idx)
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
            }
        }

        // ── Pop-up de habilidad usada ────────────────────────────────────────────
        skillUsedPopup?.let { event ->
            SkillUsedPopup(event = event, myUsername = myUsername)
        }

        AppModal(
            isOpen = showSurrenderConfirm,
            onClose = { showSurrenderConfirm = false },
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
                    "Como la partida está pausada por el otro jugador, si abandonas ahora no perderás RR y la partida quedará invalidada."
                else
                    "Si abandonas esta partida en curso, se contará como una derrota en tu historial y perderás puntos RR.",
                color = TextMutedColor,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = {
                    ws?.sendSurrender()
                    showSurrenderConfirm = false
                    ws?.disconnect()
                    onNavigate(returnTo)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)
            ) {
                Text("Abandonar partida", color = Color.White)
            }
            TextButton(
                onClick = { showSurrenderConfirm = false },
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
                "Podrás reanudar esta partida después desde la pestaña de amigos. Mientras tanto, el rival quedará esperando a que vuelvas.",
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
                            text = when {
                                isDraw -> "¡Empate!"
                                playerWon -> "¡Victoria!"
                                else -> "Derrota"
                            },
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
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(myDisplayName, color = TextColor, fontWeight = FontWeight.Bold)
                                Text("$myScore pts", color = TextColor, fontWeight = FontWeight.Bold)
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
                    AvatarBox(
                        name = name,
                        avatarUrl = avatarUrl,
                        modifier = Modifier
                            .size(40.dp)
                            .offset(x = 5.5.dp)
                            .rotate(-3f)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .offset(x = 16.dp)
                    ) {
                        val displayName = if (name.length > 10) name.take(10) + "..." else name
                        Text(
                            text = displayName,
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
private fun AvatarBox(name: String, avatarUrl: String?, modifier: Modifier = Modifier) {
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
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
                fallback = {
                    Text(
                        text = name.firstOrNull()?.uppercase() ?: "?",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            )
        }
    }
}

@Composable
private fun PlayerCardIngame(
    modifier: Modifier = Modifier,
    name: String,
    score: Int,
    avatarUrl: String?,
    isActive: Boolean,
    paused: Boolean
) {
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
                        if (isActive) Color(0xFFFBBF24) else if (paused) Color(0xFF94A3B8) else Color.Transparent,
                        RoundedCornerShape(6.dp)
                    )
            ) {
                AvatarBox(
                    name = name,
                    avatarUrl = avatarUrl,
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
                    text = if (paused) "Pausado" else "$score pts",
                    color = if (paused) Color(0xFF64748B) else Color(0xFF5C3D11),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
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
    isSkillTile: Boolean = false,
    isFixed: Boolean = false,
    isPendingTarget: Boolean = false,
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
        else hasAppeared = false
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

        // 2. Indicador de movimiento válido (punto blanco semitransparente)
        if (isValidMove) {
            Box(
                modifier = Modifier
                    .fillMaxSize(0.35f)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.45f))
            )
        }

        // 3. Ficha animada — siempre encima
        if (cellValue != null || hasAppeared) {
            val isFlipped = rotationY > 90f
            val pieceColor = if (isFlipped) style.sideB else style.sideA

            Box(
                modifier = Modifier
                    .fillMaxSize(0.78f)
                    .graphicsLayer {
                        this.rotationY = rotationY
                        this.scaleX = scale
                        this.scaleY = scale
                        this.cameraDistance = 12f * density
                    }
                    .clip(CircleShape)
                    .background(pieceColor)
                    .border(
                        width = if (isFixed) 2.dp else 1.dp,
                        color = if (isFixed) Color(0xFFFFD700) else Color.White.copy(alpha = 0.25f),
                        shape = CircleShape
                    )
            )
        }

        // 3b. Badge de ficha fija (candado) — esquina inferior derecha
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

        // 4. Highlight de objetivo pendiente — semitransparente, encima de todo
        if (isPendingTarget) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFFFD700).copy(alpha = 0.18f))
            )
        }
    }
}

// ── Botón de habilidad en el inventario ─────────────────────────────────────
@Composable
fun SkillButton(
    meta: AbilityMeta,
    isSelected: Boolean,
    canUse: Boolean,
    onClick: () -> Unit
) {
    var showTooltip by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .size(44.dp)
            .then(
                if (isSelected)
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .border(2.dp, SecondaryColor, RoundedCornerShape(8.dp))
                else Modifier
            )
            .pointerInput(canUse) {
                detectTapGestures(
                    onLongPress = { showTooltip = true },
                    onTap = { if (canUse) onClick() }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = meta.drawableRes),
            contentDescription = meta.name,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
            alpha = if (canUse) 1f else 0.45f
        )

        // Tooltip emergente al hacer long-press
        if (showTooltip) {
            Popup(
                alignment = Alignment.BottomCenter,
                offset = IntOffset(0, -56),
                onDismissRequest = { showTooltip = false }
            ) {
                Box(
                    modifier = Modifier
                        .widthIn(min = 120.dp, max = 200.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceColor)
                        .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                        .clickable { showTooltip = false }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = meta.name,
                            color = TextColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                        if (meta.description.isNotEmpty()) {
                            Text(
                                text = meta.description,
                                color = TextMutedColor,
                                fontSize = 10.sp,
                                lineHeight = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Barra de "habilidad pendiente": cancela la selección ─────────────────────
@Composable
fun SkillPendingBar(text: String, onCancel: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .offset(x = (-8).dp, y = 5.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(SecondaryColor.copy(alpha = 0.15f))
            .border(1.dp, SecondaryColor.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = text,
            color = TextColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "✕",
            color = TextMutedColor,
            fontSize = 14.sp,
            modifier = Modifier.clickable { onCancel() }.padding(start = 6.dp)
        )
    }
}

// ── Selector de dirección de gravedad ────────────────────────────────────────
@Composable
fun GravityDirectionRow(onDirection: (String) -> Unit, onCancel: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .offset(x = (-8).dp, y = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            "Dirección gravedad:",
            color = TextColor,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf("↑" to "up", "↓" to "down", "←" to "left", "→" to "right").forEach { (label, dir) ->
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(AccentGreen.copy(alpha = 0.15f))
                        .border(1.dp, AccentGreen, RoundedCornerShape(6.dp))
                        .clickable { onDirection(dir) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(label, color = TextColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(PrimaryColor.copy(alpha = 0.15f))
                    .border(1.dp, PrimaryColor, RoundedCornerShape(6.dp))
                    .clickable { onCancel() },
                contentAlignment = Alignment.Center
            ) {
                Text("✕", color = TextColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ── Pop-up animado de habilidad usada ────────────────────────────────────────
@Composable
fun SkillUsedPopup(event: SkillUsedEvent, myUsername: String) {
    val meta = ABILITY_META_MOVIL[event.abilityId]
        ?: AbilityMeta(event.abilityId, R.drawable.ingame_casillainterrogante, false)
    val labelText = if (event.isMine)
        "Has usado ${meta.name}"
    else
        "${event.username} ha usado ${meta.name}"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 140.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        androidx.compose.animation.AnimatedVisibility(
            visible = true,
            enter = androidx.compose.animation.fadeIn(
                androidx.compose.animation.core.tween(250)
            ) + androidx.compose.animation.slideInVertically(
                initialOffsetY = { it / 2 },
                animationSpec = androidx.compose.animation.core.spring(
                    dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy
                )
            ),
            exit = androidx.compose.animation.fadeOut()
        ) {
            androidx.compose.material3.Surface(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                color = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.82f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (event.isMine) androidx.compose.ui.graphics.Color(0xFFFFD700)
                    else androidx.compose.ui.graphics.Color(0xFF94A3B8)
                ),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .wrapContentHeight()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Image(
                        painter = painterResource(id = meta.drawableRes),
                        contentDescription = meta.name,
                        modifier = Modifier.size(52.dp),
                        contentScale = ContentScale.Fit
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = if (event.isMine) "⚡ Tú" else "🗡️ ${event.username}",
                            color = if (event.isMine)
                                androidx.compose.ui.graphics.Color(0xFFFFD700)
                            else
                                androidx.compose.ui.graphics.Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = labelText,
                            color = androidx.compose.ui.graphics.Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }
}

package com.example.random_reversi.ui.screens

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.random_reversi.R
import com.example.random_reversi.data.FriendsRepository
import com.example.random_reversi.data.GamesRepository
import com.example.random_reversi.data.UserResult
import com.example.random_reversi.data.remote.ChatMessage
import com.example.random_reversi.data.remote.FriendInfo
import com.example.random_reversi.data.remote.GameInviteInfo
import com.example.random_reversi.data.remote.PausedGameInfo
import com.example.random_reversi.data.remote.SocialPanelResponse
import com.example.random_reversi.ui.components.GameModeModal
import com.example.random_reversi.ui.navigation.NavigationMessages
import com.example.random_reversi.ui.theme.*
import com.example.random_reversi.utils.AvatarImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun FriendsScreen(onNavigate: (String) -> Unit) {

    val scope = rememberCoroutineScope()

    // ── Estado principal ──────────────────────────────────────────────
    var panel by remember { mutableStateOf<SocialPanelResponse?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    // ── Diálogo "Añadir amigo" ────────────────────────────────────────
    var showAddFriendDialog by remember { mutableStateOf(false) }
    var addFriendUsername by remember { mutableStateOf("") }
    var addFriendError by remember { mutableStateOf<String?>(null) }
    var addFriendSuccess by remember { mutableStateOf<String?>(null) }
    var addFriendLoading by remember { mutableStateOf(false) }

    // ── Chat abierto ──────────────────────────────────────────────────
    var chatFriend by remember { mutableStateOf<FriendInfo?>(null) }
    var chatMessages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var chatInput by remember { mutableStateOf("") }
    var chatLoading by remember { mutableStateOf(false) }

    // ── Toast ─────────────────────────────────────────────────────────
    var toastMessage by remember { mutableStateOf<String?>(null) }

    // ── Diálogo "Duelo" (selector de modo para invitar amigos) ────────────────
    var showDuelModalForFriend by remember { mutableStateOf<FriendInfo?>(null) }
    // Triple(amigo inicial, fullMode, lista de IDs adicionales seleccionados)
    var pending4PDuel by remember { mutableStateOf<Triple<FriendInfo, String, List<Int>>?>(null) }

    // ── Funciones de carga ───────────────────────────────────────────────
    fun loadPanel() {
        scope.launch {
            loading = true
            error = null
            when (val result = FriendsRepository.getSocialPanel()) {
                is UserResult.Success -> panel = result.data
                is UserResult.Error -> error = result.message
            }
            loading = false
        }
    }

    fun showToast(msg: String) {
        toastMessage = msg
    }

    // ── Carga inicial + polling ──────────────────────────────────────
    LaunchedEffect(Unit) {
        NavigationMessages.consumeFriendsToast()?.let { toastMessage = it }
        loadPanel()
        while (true) {
            delay(3000)
            when (val result = FriendsRepository.getSocialPanel()) {
                is UserResult.Success -> {
                    panel = result.data
                    chatFriend?.let { current ->
                        result.data.friends.find { it.id == current.id }?.let { updated ->
                            chatFriend = updated
                        }
                    }
                }
                is UserResult.Error -> Unit
            }
        }
    }

    // Map to ensure consistent status across different lists (e.g. if a friend sends an invite)
    val globalStatusMap = remember(panel) {
        val map = mutableMapOf<Int, String?>()
        // We initialize with the friends list as the source of truth
        panel?.friends?.forEach { map[it.id] = it.status }
        
        // Then we enrich with requests if they have a non-null status
        panel?.requests?.forEach { req ->
            if (req.status != null) map[req.id] = req.status
        }
        
        // And game invitations
        panel?.gameRequests?.forEach { invite ->
            invite.id?.let { inviterId ->
                if (invite.status != null) map[inviterId] = invite.status
            }
        }
        map
    }

    LaunchedEffect(toastMessage) {
        if (toastMessage == null) return@LaunchedEffect
        delay(2800)
        toastMessage = null
    }

    // ══════════════════════════════════════════════════════════════════
    //  Layout principal Estático
    // ══════════════════════════════════════════════════════════════════

    Box(modifier = Modifier.fillMaxSize()) {

        // ── Fondo ────────────────────────────────────────────────────
        Image(
            painter = painterResource(id = R.drawable.nuevofondomovil),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // ── Contenido SIN SCROLL GLOBAL ────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(44.dp))

            // ── Cabecera: título + botón añadir amigo ────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.amigostitulo),
                    contentDescription = "Amigos",
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Botón Añadir Amigo
                Image(
                    painter = painterResource(id = R.drawable.botonaadiramigo),
                    contentDescription = "Añadir amigo",
                    contentScale = ContentScale.FillHeight,
                    modifier = Modifier
                        .height(62.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                addFriendUsername = ""
                                addFriendError = null
                                addFriendSuccess = null
                                showAddFriendDialog = true
                            }
                        )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (loading && panel == null) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryColor)
                }
            } else if (error != null && panel == null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = SurfaceColor.copy(alpha = 0.95f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(error ?: "Error desconocido", color = Color(0xFFF87171), fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { loadPanel() },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)
                        ) {
                            Text("Reintentar", color = Color.White)
                        }
                    }
                }
            } else {
                val data = panel ?: SocialPanelResponse(
                    friends = emptyList(), requests = emptyList(), gameRequests = emptyList(), pausedGames = emptyList()
                )

                // ── CONTENEDOR DE BLOQUES: Ocupan el espacio restante ──
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // ═══════════════════════════════════════════════════════
                    //  1. TUS AMIGOS
                    // ═══════════════════════════════════════════════════════
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.tusamigos),
                            contentDescription = null,
                            contentScale = ContentScale.FillBounds,
                            modifier = Modifier.fillMaxSize()
                        )

                        if (data.friends.isEmpty()) {
                            Image(
                                painter = painterResource(id = R.drawable.sinamigos),
                                contentDescription = "No tienes amigos agregados",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(top = 28.dp)
                                    .fillMaxWidth(0.55f)
                            )
                        } else {
                            val scrollState = rememberScrollState()
                            Box(modifier = Modifier.fillMaxSize()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(top = 42.dp, start = 16.dp, end = 34.dp, bottom = 12.dp)
                                        .verticalScroll(scrollState),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    data.friends.forEach { friend ->
                                        FriendRow(
                                            friend = friend,
                                            globalStatusMap = globalStatusMap,
                                            onProfile = { onNavigate("profile/${friend.id}/${Uri.encode(friend.name)}/friends") },
                                            onChat = {
                                                chatFriend = friend
                                                chatLoading = true
                                                scope.launch {
                                                    when (val result = FriendsRepository.getChatHistory(friend.id)) {
                                                        is UserResult.Success -> chatMessages = result.data
                                                        is UserResult.Error -> chatMessages = emptyList()
                                                    }
                                                    FriendsRepository.markChatRead(friend.id)
                                                    chatLoading = false
                                                }
                                            },
                                            onDuel = { showDuelModalForFriend = friend },
                                            onRemove = {
                                                scope.launch {
                                                    when (FriendsRepository.removeFriend(friend.id)) {
                                                        is UserResult.Success -> loadPanel()
                                                        is UserResult.Error -> showToast("Error al eliminar")
                                                    }
                                                }
                                            }
                                        )
                                    }
                                }
                                if (scrollState.maxValue > 0) {
                                    val safeFraction = (scrollState.value.toFloat() / scrollState.maxValue.toFloat()).coerceIn(0f, 1f)

                                    Column(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .fillMaxHeight()
                                            .padding(end = 26.dp, top = 42.dp, bottom = 12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("▲", color = Color.Black.copy(alpha = 0.4f), fontSize = 10.sp)
                                        androidx.compose.foundation.layout.BoxWithConstraints(
                                            modifier = Modifier
                                                .width(4.dp)
                                                .weight(1f)
                                                .padding(vertical = 2.dp)
                                                .background(Color.Black.copy(alpha = 0.15f), CircleShape)
                                        ) {
                                            val thumbHeight = 24.dp
                                            val maxOffset = maxHeight - thumbHeight
                                            Box(
                                                modifier = Modifier
                                                    .width(4.dp)
                                                    .height(thumbHeight)
                                                    .offset(y = maxOffset * safeFraction)
                                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                            )
                                        }
                                        Text("▼", color = Color.Black.copy(alpha = 0.4f), fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }

                    // ═══════════════════════════════════════════════════════
                    //  2. SOLICITUDES DE AMISTAD
                    // ═══════════════════════════════════════════════════════
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.solicitudes),
                            contentDescription = "Solicitudes",
                            contentScale = ContentScale.FillBounds,
                            modifier = Modifier.fillMaxHeight().fillMaxWidth(0.975f).align(Alignment.CenterStart)
                        )

                        if (data.requests.isEmpty()) {
                            // Layout posicionado: Texto al centro y hombre a la izquierda
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(top = 32.dp)
                            ) {
                                // Texto centrado (MÁS GRANDE)
                                Image(
                                    painter = painterResource(id = R.drawable.sinsolicitudesamistadtexto),
                                    contentDescription = "Sin solicitudes de amistad texto",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .offset(y = 12.dp)
                                        .fillMaxWidth(0.95f)
                                )
                                // Hombre flotando (MÁS A LA IZQUIERDA)
                                Image(
                                    painter = painterResource(id = R.drawable.sinsolicitudesamistadhombre),
                                    contentDescription = "Hombre triste",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                        .padding(end = 60.dp) // Mucha más separación a la derecha para moverlo a la izquierda
                                        .fillMaxWidth(0.28f)
                                )
                            }
                        } else {
                            val scrollState = rememberScrollState()
                            Box(modifier = Modifier.fillMaxSize()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(top = 42.dp, start = 16.dp, end = 34.dp, bottom = 12.dp)
                                        .verticalScroll(scrollState),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    data.requests.forEach { req ->
                                        RequestRow(
                                            friend = req,
                                            globalStatusMap = globalStatusMap,
                                            onAccept = {
                                                scope.launch {
                                                    if (FriendsRepository.acceptFriendRequest(req.id) is UserResult.Success) loadPanel()
                                                }
                                            },
                                            onReject = {
                                                scope.launch {
                                                    if (FriendsRepository.rejectFriendRequest(req.id) is UserResult.Success) loadPanel()
                                                }
                                            }
                                        )
                                    }
                                }
                                if (scrollState.maxValue > 0) {
                                    val safeFraction = (scrollState.value.toFloat() / scrollState.maxValue.toFloat()).coerceIn(0f, 1f)
                                    Column(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .fillMaxHeight()
                                            .padding(end = 26.dp, top = 42.dp, bottom = 12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("▲", color = Color.Black.copy(alpha = 0.4f), fontSize = 10.sp)
                                        androidx.compose.foundation.layout.BoxWithConstraints(
                                            modifier = Modifier
                                                .width(4.dp)
                                                .weight(1f)
                                                .padding(vertical = 2.dp)
                                                .background(Color.Black.copy(alpha = 0.15f), CircleShape)
                                        ) {
                                            val thumbHeight = 24.dp
                                            val maxOffset = maxHeight - thumbHeight
                                            Box(
                                                modifier = Modifier
                                                    .width(4.dp)
                                                    .height(thumbHeight)
                                                    .offset(y = maxOffset * safeFraction)
                                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                            )
                                        }
                                        Text("▼", color = Color.Black.copy(alpha = 0.4f), fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }

                    // ═══════════════════════════════════════════════════════
                    //  3. SOLICITUDES DE JUEGO
                    // ═══════════════════════════════════════════════════════
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.solicitudesjuego),
                            contentDescription = null,
                            contentScale = ContentScale.FillBounds,
                            modifier = Modifier.fillMaxHeight().fillMaxWidth(1.36f).align(Alignment.CenterStart)
                        )

                        if (data.gameRequests.isEmpty()) {
                            Image(
                                painter = painterResource(id = R.drawable.sinsolicitudesjuego),
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(top = 28.dp)
                                    .fillMaxWidth(0.68f)
                            )
                        } else {
                            val scrollState = rememberScrollState()
                            Box(modifier = Modifier.fillMaxSize()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(top = 42.dp, start = 16.dp, end = 34.dp, bottom = 12.dp)
                                        .verticalScroll(scrollState),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    data.gameRequests.forEach { invite ->
                                        GameInviteRow(
                                            invite = invite,
                                            globalStatusMap = globalStatusMap,
                                            onAccept = {
                                                scope.launch {
                                                    when (val result = GamesRepository.acceptGameInvite(invite.lobby_id)) {
                                                        is UserResult.Success -> {
                                                            val mode = when (invite.gameMode?.lowercase()) {
                                                                "1vs1vs1vs1", "1v1v1v1" -> "1vs1vs1vs1"
                                                                else -> "1vs1"
                                                            }
                                                            onNavigate("waiting-room/$mode/${result.data}/friends")
                                                        }
                                                        is UserResult.Error -> showToast(result.message)
                                                    }
                                                }
                                            },
                                            onReject = {
                                                scope.launch {
                                                    if (GamesRepository.rejectGameInvite(invite.lobby_id) is UserResult.Success) loadPanel()
                                                }
                                            }
                                        )
                                    }
                                }
                                if (scrollState.maxValue > 0) {
                                    val safeFraction = (scrollState.value.toFloat() / scrollState.maxValue.toFloat()).coerceIn(0f, 1f)
                                    Column(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .fillMaxHeight()
                                            .padding(end = 26.dp, top = 42.dp, bottom = 12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("▲", color = Color.Black.copy(alpha = 0.4f), fontSize = 10.sp)
                                        androidx.compose.foundation.layout.BoxWithConstraints(
                                            modifier = Modifier
                                                .width(4.dp)
                                                .weight(1f)
                                                .padding(vertical = 2.dp)
                                                .background(Color.Black.copy(alpha = 0.15f), CircleShape)
                                        ) {
                                            val thumbHeight = 24.dp
                                            val maxOffset = maxHeight - thumbHeight
                                            Box(
                                                modifier = Modifier
                                                    .width(4.dp)
                                                    .height(thumbHeight)
                                                    .offset(y = maxOffset * safeFraction)
                                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                            )
                                        }
                                        Text("▼", color = Color.Black.copy(alpha = 0.4f), fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }

                    // ═══════════════════════════════════════════════════════
                    //  4. PARTIDAS PAUSADAS
                    // ═══════════════════════════════════════════════════════
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.partidaspausadas),
                            contentDescription = null,
                            contentScale = ContentScale.FillBounds,
                            modifier = Modifier.fillMaxHeight().fillMaxWidth(1.3f).align(Alignment.CenterStart)
                        )

                        if (data.pausedGames.isEmpty()) {
                            Image(
                                painter = painterResource(id = R.drawable.sinpausadas),
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(top = 10.dp)
                                    .fillMaxWidth(0.55f)
                            )
                        } else {
                            val scrollState = rememberScrollState()
                            Box(modifier = Modifier.fillMaxSize()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(top = 42.dp, start = 16.dp, end = 34.dp, bottom = 12.dp)
                                        .verticalScroll(scrollState),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    data.pausedGames.forEach { paused ->
                                        PausedGameRow(
                                            game = paused,
                                            onResume = {
                                                val mode = when (paused.mode.lowercase().ifEmpty { "1vs1" }) {
                                                    "1vs1vs1vs1", "1v1v1v1" -> "1vs1vs1vs1"
                                                    else -> "1vs1"
                                                }
                                                onNavigate("waiting-room/$mode/${paused.game_id}/friends")
                                            }
                                        )
                                    }
                                }
                                if (scrollState.maxValue > 0) {
                                    val safeFraction = (scrollState.value.toFloat() / scrollState.maxValue.toFloat()).coerceIn(0f, 1f)
                                    Column(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .fillMaxHeight()
                                            .padding(end = 26.dp, top = 42.dp, bottom = 12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("▲", color = Color.Black.copy(alpha = 0.4f), fontSize = 10.sp)
                                        androidx.compose.foundation.layout.BoxWithConstraints(
                                            modifier = Modifier
                                                .width(4.dp)
                                                .weight(1f)
                                                .padding(vertical = 2.dp)
                                                .background(Color.Black.copy(alpha = 0.15f), CircleShape)
                                        ) {
                                            val thumbHeight = 24.dp
                                            val maxOffset = maxHeight - thumbHeight
                                            Box(
                                                modifier = Modifier
                                                    .width(4.dp)
                                                    .height(thumbHeight)
                                                    .offset(y = maxOffset * safeFraction)
                                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                            )
                                        }
                                        Text("▼", color = Color.Black.copy(alpha = 0.4f), fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Botón volver al menú ───────────────────────────
            Image(
                painter = painterResource(id = R.drawable.botonvolvermenu),
                contentDescription = "Volver al menú",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth(0.72f)
                    .heightIn(max = 65.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onNavigate("menu") }
                    )
            )

            Spacer(modifier = Modifier.height(28.dp))
        }

        // ── Toast overlay ────────────────────────────────────────────
        AnimatedVisibility(
            visible = toastMessage != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 56.dp)
        ) {
            Surface(
                color = Color(0xFF1D4ED8),
                shape = RoundedCornerShape(999.dp)
            ) {
                Text(
                    text = toastMessage ?: "",
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  Diálogos
    // ══════════════════════════════════════════════════════════════════

    if (showAddFriendDialog) {
        Dialog(onDismissRequest = { showAddFriendDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SurfaceColor
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Añadir amigo",
                        color = TextColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    OutlinedTextField(
                        value = addFriendUsername,
                        onValueChange = {
                            addFriendUsername = it
                            addFriendError = null
                            addFriendSuccess = null
                        },
                        label = { Text("Nombre de usuario") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryColor,
                            unfocusedBorderColor = BorderColor,
                            focusedLabelColor = PrimaryColor,
                            unfocusedLabelColor = TextMutedColor,
                            focusedTextColor = TextColor,
                            unfocusedTextColor = TextColor,
                            cursorColor = PrimaryColor
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (!addFriendError.isNullOrBlank()) {
                        Text(addFriendError!!, color = Color(0xFFF87171), fontSize = 12.sp)
                    }
                    if (!addFriendSuccess.isNullOrBlank()) {
                        Text(addFriendSuccess!!, color = AccentGreen, fontSize = 12.sp)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showAddFriendDialog = false },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = BorderColor),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Cancelar", color = TextColor)
                        }
                        Button(
                            onClick = {
                                if (addFriendUsername.isBlank()) {
                                    addFriendError = "Introduce un nombre"
                                    return@Button
                                }
                                addFriendLoading = true
                                scope.launch {
                                    when (val result = FriendsRepository.sendFriendRequest(addFriendUsername.trim())) {
                                        is UserResult.Success -> {
                                            addFriendSuccess = result.data
                                            addFriendUsername = ""
                                            loadPanel()
                                        }
                                        is UserResult.Error -> addFriendError = result.message
                                    }
                                    addFriendLoading = false
                                }
                            },
                            enabled = !addFriendLoading,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                if (addFriendLoading) "Enviando..." else "Enviar",
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }

    // ── Modal "Duelo" (selector de modo + variante para invitar amigos) ──────────────────
    showDuelModalForFriend?.let { friend ->
        GameModeModal(
            isOpen = true,
            onClose = { showDuelModalForFriend = null },
            title = "Duelo vs ${friend.name}",
            subtitle = "Elige el modo y variante",
            onSelectMode = { fullMode ->
                // fullMode: "1vs1", "1vs1_skills", "1vs1vs1vs1", "1vs1vs1vs1_skills"
                val isSkills = fullMode.endsWith("_skills")
                val baseMode = if (isSkills) fullMode.removeSuffix("_skills") else fullMode
                val is4P = baseMode == "1vs1vs1vs1"

                if (is4P) {
                    // Para 4P mostrar selector de 2 amigos adicionales
                    showDuelModalForFriend = null
                    pending4PDuel = Triple(friend, fullMode, emptyList())
                } else {
                    showDuelModalForFriend = null
                    scope.launch {
                        // Para 1v1 invitar directamente
                        when (val result = GamesRepository.inviteFriends(baseMode, listOf(friend.id))) {
                            is UserResult.Success -> {
                                // Pasar el modo completo (con _skills) al waiting-room
                                onNavigate("waiting-room/$fullMode/${result.data.game_id}/friends/${Uri.encode(friend.name)}")
                            }
                            is UserResult.Error -> showToast(result.message)
                        }
                    }
                }
            }
        )
    }

    // ── Selector de amigos adicionales para 4P ────────────────────────────────
    pending4PDuel?.let { (initialFriend, fullMode, selectedIds) ->
        val isSkills = fullMode.endsWith("_skills")
        val baseMode = if (isSkills) fullMode.removeSuffix("_skills") else fullMode
        val availableFriends = panel?.friends?.filter { it.id != initialFriend.id } ?: emptyList()

        androidx.compose.ui.window.Dialog(
            onDismissRequest = { pending4PDuel = null },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .widthIn(max = 450.dp)
                    .background(Color(0xFFF7F1E5), RoundedCornerShape(16.dp))
                    .border(2.dp, Color(0xFF2F2418), RoundedCornerShape(16.dp))
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Invitar amigos al 4P",
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        color = Color(0xFF1F1711)
                    )
                    Text(
                        "Selecciona hasta 2 amigos más (${selectedIds.size}/2)",
                        fontSize = 13.sp,
                        color = Color(0xFF4D3F31)
                    )
                    // Amigo inicial (ya seleccionado, no editable)
                    Surface(
                        color = AccentGreen.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(initialFriend.name, color = TextColor, fontWeight = FontWeight.Bold)
                            Text("✓ Invitado", color = AccentGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    // Lista de otros amigos
                    availableFriends.forEach { friend ->
                        val isSelected = friend.id in selectedIds
                        Surface(
                            color = if (isSelected) Color(0xFF8B5CF6).copy(alpha = 0.15f) else Color.White.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    1.5.dp,
                                    if (isSelected) Color(0xFF8B5CF6) else Color(0xFFCBBFA8),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    val newIds = if (isSelected) {
                                        selectedIds - friend.id
                                    } else if (selectedIds.size < 2) {
                                        selectedIds + friend.id
                                    } else selectedIds
                                    pending4PDuel = Triple(initialFriend, fullMode, newIds)
                                }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(friend.name, color = TextColor, fontWeight = FontWeight.SemiBold)
                                if (isSelected) Text("✓", color = Color(0xFF8B5CF6), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    if (availableFriends.isEmpty()) {
                        Text("No tienes más amigos disponibles.", color = TextMutedColor, fontSize = 13.sp)
                    }
                    // Botones
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = { pending4PDuel = null },
                            modifier = Modifier.weight(1f)
                        ) { Text("Cancelar", color = TextMutedColor) }
                        Button(
                            onClick = {
                                if (selectedIds.isEmpty()) {
                                    scope.launch { showToast("Selecciona al menos 1 amigo más") }
                                    return@Button
                                }
                                pending4PDuel = null
                                val allIds = listOf(initialFriend.id) + selectedIds
                                scope.launch {
                                    when (val result = GamesRepository.inviteFriends(baseMode, allIds)) {
                                        is UserResult.Success -> {
                                            onNavigate("waiting-room/$fullMode/${result.data.game_id}/friends/${Uri.encode(initialFriend.name)}")
                                        }
                                        is UserResult.Error -> showToast(result.message)
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = selectedIds.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6))
                        ) { Text("Invitar (${selectedIds.size + 1}/4)", color = Color.White) }
                    }
                }
            }
        }
    }

    if (chatFriend != null) {

        ChatDialog(
            friend = chatFriend!!,
            globalStatusMap = globalStatusMap,
            messages = chatMessages,
            loading = chatLoading,
            input = chatInput,
            onInputChange = { chatInput = it },
            onSend = {
                if (chatInput.isBlank()) return@ChatDialog
                val msg = chatInput.trim()
                chatInput = ""
                scope.launch {
                    when (val result = FriendsRepository.sendChatMessage(chatFriend!!.id, msg)) {
                        is UserResult.Success -> {
                            chatMessages = chatMessages + result.data
                        }
                        is UserResult.Error -> showToast(result.message)
                    }
                }
            },
            onClose = {
                chatFriend = null
                chatMessages = emptyList()
                chatInput = ""
            }
        )
    }
}


// ══════════════════════════════════════════════════════════════════════
//  Fila de amigo (con acciones expandibles)
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun FriendRow(
    friend: FriendInfo,
    globalStatusMap: Map<Int, String?>,
    onProfile: () -> Unit,
    onChat: () -> Unit,
    onDuel: () -> Unit,
    onRemove: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 2.dp)
            .border(2.dp, Color.Black, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .clickable { expanded = !expanded }
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            AvatarWithStatus(friend.avatar_url, friend.name, globalStatusMap[friend.id] ?: friend.status)
            Spacer(modifier = Modifier.width(8.dp))

            Text(
                friend.name,
                color = TextColor,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(0.6f)
            )

            Surface(
                color = Color(0xFFFBBF24),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.Black)
            ) {
                Text(
                    "${friend.rr} RR",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            Spacer(modifier = Modifier.weight(0.1f))

            val unread = friend.unread_count ?: 0
            if (unread > 0) {
                Surface(
                    color = PrimaryColor,
                    shape = CircleShape,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.Black)
                ) {
                    Text(
                        text = if (unread > 9) "9+" else "$unread",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            Box(
                modifier = Modifier
                    .size(20.dp)
                    .border(1.5.dp, Color.Black, RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (expanded) "-" else "+",
                    color = Color.Black,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.offset(y = (-1).dp)
                )
            }
        }

        AnimatedVisibility(visible = expanded) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ActionButton("Perfil", AccentGreen, Modifier.weight(1f)) { onProfile() }
                ActionButton("Chat", Color(0xFF3B82F6), Modifier.weight(1f)) { onChat() }
                ActionButton("Duelo", Color(0xFF8B5CF6), Modifier.weight(1f)) { onDuel() }
                ActionButton("🗑️", Color(0xFFF87171), Modifier.weight(0.5f)) { onRemove() }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Fila de solicitud de amistad
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun RequestRow(
    friend: FriendInfo,
    globalStatusMap: Map<Int, String?>,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 2.dp)
            .border(2.dp, Color.Black, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .clickable { expanded = !expanded }
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            AvatarWithStatus(friend.avatar_url, friend.name, globalStatusMap[friend.id] ?: friend.status)
            Spacer(modifier = Modifier.width(8.dp))

            Text(
                friend.name,
                color = TextColor,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(0.6f)
            )

            Surface(
                color = Color(0xFFFBBF24),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.Black)
            ) {
                Text(
                    "${friend.rr} RR",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.weight(0.1f))

            Box(
                modifier = Modifier
                    .size(20.dp)
                    .border(1.5.dp, Color.Black, RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (expanded) "-" else "+",
                    color = Color.Black,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.offset(y = (-1).dp)
                )
            }
        }

        AnimatedVisibility(visible = expanded) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ActionButton("✓ Aceptar", AccentGreen, Modifier.weight(1f)) { onAccept() }
                ActionButton("✕ Rechazar", Color(0xFFF87171), Modifier.weight(1f)) { onReject() }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Fila de invitación de juego
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun GameInviteRow(
    invite: GameInviteInfo,
    globalStatusMap: Map<Int, String?>,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val modeLabel = when (invite.gameMode?.lowercase()) {
        "1vs1vs1vs1", "1v1v1v1" -> "1vs1vs1vs1"
        else -> "1vs1"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 2.dp)
            .border(2.dp, Color.Black, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .clickable { expanded = !expanded }
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AvatarWithStatus(invite.avatar_url, invite.name ?: "?", invite.id?.let { globalStatusMap[it] } ?: invite.status)
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    invite.name ?: "Desconocido",
                    color = TextColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text("Modo: $modeLabel", color = TextMutedColor, fontSize = 11.sp)
            }
            
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .border(1.5.dp, Color.Black, RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (expanded) "-" else "+",
                    color = Color.Black,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.offset(y = (-1).dp)
                )
            }
        }

        AnimatedVisibility(visible = expanded) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ActionButton("Unirse", AccentGreen, Modifier.weight(1f)) { onAccept() }
                ActionButton("✕ Rechazar", Color(0xFFF87171), Modifier.weight(1f)) { onReject() }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Fila de partida pausada
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun PausedGameRow(
    game: PausedGameInfo,
    onResume: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val modeLabel = when (game.mode.lowercase()) {
        "1vs1vs1vs1", "1v1v1v1" -> "1vs1vs1vs1"
        else -> "1vs1"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 2.dp)
            .border(2.dp, Color.Black, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .clickable { expanded = !expanded }
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Partida $modeLabel",
                    color = TextColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                if (game.participants.isNotEmpty()) {
                    Text(
                        "Con: ${game.participants.joinToString(", ")}",
                        color = TextMutedColor,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .border(1.5.dp, Color.Black, RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (expanded) "-" else "+",
                    color = Color.Black,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.offset(y = (-1).dp)
                )
            }
        }

        AnimatedVisibility(visible = expanded) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ActionButton("Reanudar", AccentGreen, Modifier.weight(1f)) { onResume() }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Diálogo Chat
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun ChatDialog(
    friend: FriendInfo,
    globalStatusMap: Map<Int, String?>,
    messages: List<ChatMessage>,
    loading: Boolean,
    input: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onClose: () -> Unit
) {
    Dialog(onDismissRequest = onClose) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(420.dp),
            shape = RoundedCornerShape(16.dp),
            color = SurfaceColor
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AvatarWithStatus(friend.avatar_url, friend.name, globalStatusMap[friend.id] ?: friend.status)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        friend.name,
                        color = TextColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "✕",
                        color = TextMutedColor,
                        fontSize = 20.sp,
                        modifier = Modifier.clickable(onClick = onClose)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))

                if (loading) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = PrimaryColor, modifier = Modifier.size(32.dp))
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        if (messages.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "No hay mensajes aún.\n¡Saluda!",
                                    color = TextMutedColor,
                                    textAlign = TextAlign.Center,
                                    fontSize = 13.sp
                                )
                            }
                        } else {
                            messages.forEach { msg ->
                                val isMine = msg.sender_id != friend.id
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp),
                                    horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
                                ) {
                                    Surface(
                                        color = if (isMine) PrimaryColor.copy(alpha = 0.15f) else Color(0xFFE8E0D0),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(
                                            msg.message,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            color = TextColor,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = onInputChange,
                        placeholder = { Text("Escribe un mensaje…", fontSize = 13.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryColor,
                            unfocusedBorderColor = BorderColor,
                            focusedTextColor = TextColor,
                            unfocusedTextColor = TextColor,
                            cursorColor = PrimaryColor
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = onSend,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Enviar", color = Color.White, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun AvatarWithStatus(avatarUrl: String?, name: String, status: String?) {
    Box(contentAlignment = Alignment.TopEnd) {
        AvatarSmall(avatarUrl, name)
        
        val s = status?.lowercase()?.trim()
        val statusColor = when (s) {
            "online" -> Color(0xFF4ADE80)
            "playing" -> Color(0xFFFBBF24)
            // Anything else (null, "offline", empty, etc.) is considered offline/red
            else -> Color(0xFFF87171)
        }
        Box(
            modifier = Modifier
                .size(11.dp)
                .offset(x = 1.dp, y = -1.dp)
                .clip(CircleShape)
                .background(statusColor)
                .border(1.dp, Color.Black, CircleShape)
        )
    }
}

@Composable
private fun AvatarSmall(avatarUrl: String?, name: String) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(Color.White)
            .border(1.5.dp, Color.Black, CircleShape)
    ) {
        AvatarImage(
            avatarUrl = avatarUrl,
            contentDescription = name,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            fallback = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(PrimaryColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        )
    }
}

@Composable
private fun ActionButton(
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(32.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.Black),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp)
    ) {
        Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}
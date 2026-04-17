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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import coil.compose.AsyncImage
import com.example.random_reversi.R
import com.example.random_reversi.data.FriendsRepository
import com.example.random_reversi.data.GamesRepository
import com.example.random_reversi.data.UserResult
import com.example.random_reversi.data.remote.ChatMessage
import com.example.random_reversi.data.remote.FriendInfo
import com.example.random_reversi.data.remote.GameInviteInfo
import com.example.random_reversi.data.remote.PausedGameInfo
import com.example.random_reversi.data.remote.SocialPanelResponse
import com.example.random_reversi.ui.navigation.NavigationMessages
import com.example.random_reversi.ui.theme.*
import com.example.random_reversi.utils.AvatarPresets
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ══════════════════════════════════════════════════════════════════════
//  FriendsScreen – Layout fiel al mockup con PNGs ilustrados
// ══════════════════════════════════════════════════════════════════════

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

    // ── Funciones de carga ────────────────────────────────────────────
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
        // Consumir toast de navegación si existe
        NavigationMessages.consumeFriendsToast()?.let { toastMessage = it }
        loadPanel()
    }

    LaunchedEffect(panel) {
        while (true) {
            delay(5000)
            when (val result = FriendsRepository.getSocialPanel()) {
                is UserResult.Success -> panel = result.data
                is UserResult.Error -> Unit
            }
        }
    }

    // Auto-dismiss toast
    LaunchedEffect(toastMessage) {
        if (toastMessage == null) return@LaunchedEffect
        delay(2800)
        toastMessage = null
    }

    // ══════════════════════════════════════════════════════════════════
    //  Layout principal
    // ══════════════════════════════════════════════════════════════════

    Box(modifier = Modifier.fillMaxSize()) {

        // ── Fondo ────────────────────────────────────────────────────
        Image(
            painter = painterResource(id = R.drawable.nuevofondomovil),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // ── Contenido scrollable ─────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
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
                // Título PNG "Amigos"
                Image(
                    painter = painterResource(id = R.drawable.amigostitulo),
                    contentDescription = "Amigos",
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Botón "Añadir amigo" PNG
                Image(
                    painter = painterResource(id = R.drawable.botonaadiramigo),
                    contentDescription = "Añadir amigo",
                    contentScale = ContentScale.FillHeight,
                    modifier = Modifier
                        .height(40.dp)
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
                // Estado de carga
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PrimaryColor)
                }
            } else if (error != null && panel == null) {
                // Estado de error
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
                    friends = emptyList(),
                    requests = emptyList(),
                    gameRequests = emptyList(),
                    pausedGames = emptyList()
                )

                // ═══════════════════════════════════════════════════════
                //  1. TUS AMIGOS – contenido superpuesto al PNG
                // ═══════════════════════════════════════════════════════
                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Fondo: listaamigos.png ocupa todo el ancho
                    Image(
                        painter = painterResource(id = R.drawable.listaamigos),
                        contentDescription = null,
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Contenido superpuesto (solo cuando hay amigos)
                    if (data.friends.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 48.dp, start = 20.dp, end = 20.dp, bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            data.friends.forEach { friend ->
                                FriendRow(
                                    friend = friend,
                                    onProfile = {
                                        val encoded = Uri.encode(friend.name)
                                        onNavigate("profile/${friend.id}/$encoded/friends")
                                    },
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
                                    onInvite1v1 = {
                                        scope.launch {
                                            when (val result = GamesRepository.inviteFriends("1vs1", listOf(friend.id))) {
                                                is UserResult.Success -> {
                                                    showToast("Invitación enviada a ${friend.name}")
                                                    onNavigate("waiting-room/1vs1/${result.data.game_id}/friends/${Uri.encode(friend.name)}")
                                                }
                                                is UserResult.Error -> showToast(result.message)
                                            }
                                        }
                                    },
                                    onInvite4p = {
                                        scope.launch {
                                            when (val result = GamesRepository.inviteFriends("1vs1vs1vs1", listOf(friend.id))) {
                                                is UserResult.Success -> {
                                                    showToast("Invitación enviada a ${friend.name}")
                                                    onNavigate("waiting-room/1vs1vs1vs1/${result.data.game_id}/friends/${Uri.encode(friend.name)}")
                                                }
                                                is UserResult.Error -> showToast(result.message)
                                            }
                                        }
                                    },
                                    onRemove = {
                                        scope.launch {
                                            when (FriendsRepository.removeFriend(friend.id)) {
                                                is UserResult.Success -> {
                                                    showToast("Amigo eliminado")
                                                    loadPanel()
                                                }
                                                is UserResult.Error -> showToast("Error al eliminar")
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                // ═══════════════════════════════════════════════════════
                //  2. SOLICITUDES DE AMISTAD – superpuesto en solicitudes.png
                // ═══════════════════════════════════════════════════════
                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Fondo: solicitudes.png
                    Image(
                        painter = painterResource(id = R.drawable.solicitudes),
                        contentDescription = "Solicitudes",
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Contenido superpuesto
                    if (data.requests.isEmpty()) {
                        // Estado vacío: sinsolicitudesamistad.png superpuesto
                        Image(
                            painter = painterResource(id = R.drawable.sinsolicitudesamistad),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(top = 45.dp, bottom = 12.dp)
                                .fillMaxWidth(0.45f)
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 48.dp, start = 20.dp, end = 20.dp, bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            data.requests.forEach { req ->
                                RequestRow(
                                    friend = req,
                                    onAccept = {
                                        scope.launch {
                                            when (FriendsRepository.acceptFriendRequest(req.id)) {
                                                is UserResult.Success -> {
                                                    showToast("${req.name} añadido como amigo")
                                                    loadPanel()
                                                }
                                                is UserResult.Error -> showToast("Error al aceptar")
                                            }
                                        }
                                    },
                                    onReject = {
                                        scope.launch {
                                            when (FriendsRepository.rejectFriendRequest(req.id)) {
                                                is UserResult.Success -> {
                                                    showToast("Solicitud rechazada")
                                                    loadPanel()
                                                }
                                                is UserResult.Error -> showToast("Error al rechazar")
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ═══════════════════════════════════════════════════════
                //  3. SOLICITUDES DE JUEGO – contenido superpuesto
                // ═══════════════════════════════════════════════════════
                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.solicitudesjuego),
                        contentDescription = null,
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Contenido superpuesto
                    if (data.gameRequests.isEmpty()) {
                        // Estado vacío: sinsolicitudesjuego.png superpuesto
                        Image(
                            painter = painterResource(id = R.drawable.sinsolicitudesjuego),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(top = 45.dp, bottom = 12.dp)
                                .fillMaxWidth(0.45f)
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 48.dp, start = 20.dp, end = 20.dp, bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            data.gameRequests.forEach { invite ->
                                GameInviteRow(
                                    invite = invite,
                                    onAccept = {
                                        scope.launch {
                                            when (val result = GamesRepository.acceptGameInvite(invite.lobby_id)) {
                                                is UserResult.Success -> {
                                                    val gameId = result.data
                                                    val mode = when (invite.gameMode?.lowercase()) {
                                                        "1vs1vs1vs1", "1v1v1v1" -> "1vs1vs1vs1"
                                                        else -> "1vs1"
                                                    }
                                                    onNavigate("waiting-room/$mode/$gameId/friends")
                                                }
                                                is UserResult.Error -> showToast(result.message)
                                            }
                                        }
                                    },
                                    onReject = {
                                        scope.launch {
                                            when (GamesRepository.rejectGameInvite(invite.lobby_id)) {
                                                is UserResult.Success -> {
                                                    showToast("Invitación rechazada")
                                                    loadPanel()
                                                }
                                                is UserResult.Error -> showToast("Error al rechazar")
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ═══════════════════════════════════════════════════════
                //  4. PARTIDAS PAUSADAS – contenido superpuesto
                // ═══════════════════════════════════════════════════════
                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.partidaspausadas),
                        contentDescription = null,
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Contenido superpuesto
                    if (data.pausedGames.isEmpty()) {
                        // Estado vacío: sinpausadas.png superpuesto
                        Image(
                            painter = painterResource(id = R.drawable.sinpausadas),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(top = 40.dp)
                                .fillMaxWidth(0.6f)
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 48.dp, start = 20.dp, end = 20.dp, bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            data.pausedGames.forEach { paused ->
                                PausedGameRow(
                                    game = paused,
                                    onResume = {
                                        val mode = when (paused.mode.lowercase()) {
                                            "1vs1vs1vs1", "1v1v1v1" -> "1vs1vs1vs1"
                                            else -> "1vs1"
                                        }
                                        onNavigate("waiting-room/$mode/${paused.game_id}/friends")
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Botón volver al menú (PNG) ───────────────────────────
            Image(
                painter = painterResource(id = R.drawable.botonvolvermenu),
                contentDescription = "Volver al menú",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxWidth(0.72f)
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

    // ── Diálogo: Añadir amigo ────────────────────────────────────────
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

    // ── Diálogo: Chat ────────────────────────────────────────────────
    if (chatFriend != null) {
        ChatDialog(
            friend = chatFriend!!,
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
    onProfile: () -> Unit,
    onChat: () -> Unit,
    onInvite1v1: () -> Unit,
    onInvite4p: () -> Unit,
    onRemove: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Avatar
            AvatarSmall(friend.avatar_url, friend.name)
            Spacer(modifier = Modifier.width(8.dp))

            // Nombre
            Text(
                friend.name,
                color = TextColor,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(0.6f)
            )

            // RR (Estilo Píldora)
            Surface(
                color = Color(0xFFFBBF24), // Amarillo del mockup
                shape = RoundedCornerShape(12.dp)
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

            // Indicador de mensajes
            val unread = friend.unread_count ?: 0
            if (unread > 0) {
                Surface(
                    color = PrimaryColor,
                    shape = CircleShape
                ) {
                    Text(
                        text = if (unread > 9) "9+" else "$unread",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            } else {
                // Status simple si no hay notificaciones
                val statusColor = when (friend.status?.lowercase()) {
                    "online" -> Color(0xFF4ADE80)
                    "playing" -> Color(0xFFFBBF24)
                    else -> Color(0xFF9CA3AF)
                }
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
            }
        }

        // Acciones expandibles
        AnimatedVisibility(visible = expanded) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ActionButton("Perfil", AccentGreen, Modifier.weight(1f)) { onProfile() }
                ActionButton("Chat", Color(0xFF3B82F6), Modifier.weight(1f)) { onChat() }
                ActionButton("1v1", SecondaryColor, Modifier.weight(1f)) { onInvite1v1() }
                ActionButton("4P", Color(0xFF8B5CF6), Modifier.weight(1f)) { onInvite4p() }
                ActionButton("✕", Color(0xFFF87171), Modifier.weight(0.5f)) { onRemove() }
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
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AvatarSmall(friend.avatar_url, friend.name)
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
            shape = RoundedCornerShape(12.dp)
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
        
        ActionButton("✓", AccentGreen, Modifier.width(36.dp)) { onAccept() }
        Spacer(modifier = Modifier.width(4.dp))
        ActionButton("✕", Color(0xFFF87171), Modifier.width(36.dp)) { onReject() }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Fila de invitación de juego
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun GameInviteRow(
    invite: GameInviteInfo,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    val modeLabel = when (invite.gameMode?.lowercase()) {
        "1vs1vs1vs1", "1v1v1v1" -> "1vs1vs1vs1"
        else -> "1vs1"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AvatarSmall(invite.avatar_url, invite.name ?: "?")
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
        ActionButton("Unirse", AccentGreen) { onAccept() }
        Spacer(modifier = Modifier.width(4.dp))
        ActionButton("✕", Color(0xFFF87171), Modifier.width(36.dp)) { onReject() }
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
    val modeLabel = when (game.mode.lowercase()) {
        "1vs1vs1vs1", "1v1v1v1" -> "1vs1vs1vs1"
        else -> "1vs1"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
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
        ActionButton("Reanudar", AccentGreen) { onResume() }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Diálogo Chat
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun ChatDialog(
    friend: FriendInfo,
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
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AvatarSmall(friend.avatar_url, friend.name)
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

                // Mensajes
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

                // Input
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

// ══════════════════════════════════════════════════════════════════════
//  Componentes auxiliares
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun AvatarSmall(avatarUrl: String?, name: String) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(Color.White)
            .border(1.5.dp, Color.Black, CircleShape)
    ) {
        val presetRes = AvatarPresets.drawableForId(avatarUrl)
        when {
            presetRes != null -> Image(
                painter = painterResource(presetRes),
                contentDescription = name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            !avatarUrl.isNullOrBlank() -> AsyncImage(
                model = avatarUrl,
                contentDescription = name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            else -> Box(
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
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp)
    ) {
        Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

// ── Fila auxiliar para stats (reutilizada del ProfileScreen) ──────────
@Composable
private fun StatRow(label: String, value: String, valueColor: Color = TextColor) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = TextMutedColor, fontSize = 12.sp)
        Text(value, color = valueColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

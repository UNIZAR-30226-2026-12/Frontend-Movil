package com.example.random_reversi.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.random_reversi.data.FriendsRepository
import com.example.random_reversi.data.GamesRepository
import com.example.random_reversi.data.UserResult
import com.example.random_reversi.data.remote.FriendInfo
import com.example.random_reversi.data.remote.GameInviteInfo
import com.example.random_reversi.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class ToastData(
    val message: String,
    val type: String,
    val visible: Boolean = false
)

@Composable
fun FriendsScreen(onNavigate: (String) -> Unit) {
    var friends by remember { mutableStateOf<List<FriendInfo>>(emptyList()) }
    var requests by remember { mutableStateOf<List<FriendInfo>>(emptyList()) }
    var gameInvites by remember { mutableStateOf<List<GameInviteInfo>>(emptyList()) }
    var toast by remember { mutableStateOf(ToastData("", "info")) }
    var isLoading by remember { mutableStateOf(true) }

    var showAddFriendDialog by remember { mutableStateOf(false) }
    var newFriendName by remember { mutableStateOf("") }
    var isSendingRequest by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    fun showToast(message: String, type: String = "info") {
        scope.launch {
            toast = ToastData(message, type, true)
            delay(3000)
            toast = toast.copy(visible = false)
        }
    }

    fun loadSocialPanel() {
        scope.launch {
            isLoading = true
            when (val result = FriendsRepository.getSocialPanel()) {
                is UserResult.Success -> {
                    friends = result.data.friends
                    requests = result.data.pending_requests
                    gameInvites = result.data.game_invitations
                }
                is UserResult.Error -> {
                    showToast(result.message, "error")
                }
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadSocialPanel()
    }

    val onAddFriendSubmit: () -> Unit = {
        val trimmedName = newFriendName.trim()
        if (trimmedName.isEmpty()) {
            showToast("Escribe un nombre de usuario", "error")
        } else {
            scope.launch {
                isSendingRequest = true
                when (val result = FriendsRepository.sendFriendRequest(trimmedName)) {
                    is UserResult.Success -> {
                        showToast("Solicitud enviada a $trimmedName", "success")
                        showAddFriendDialog = false
                        newFriendName = ""
                    }
                    is UserResult.Error -> {
                        showToast(result.message, "error")
                    }
                }
                isSendingRequest = false
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(BgColor)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Amigos", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = TextColor)
                    Text("Conecta y juega", fontSize = 14.sp, color = TextMutedColor)
                }
                Button(
                    onClick = { showAddFriendDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("+ Añadir", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard("Total", friends.size.toString(), Modifier.weight(1f))
                StatCard(
                    "En línea",
                    friends.count { it.status == "online" || it.status == "en linea" }.toString(),
                    Modifier.weight(1f),
                    Color(0xFF4ade80)
                )
                StatCard(
                    "Jugando",
                    friends.count { it.status == "jugando" || it.status == "playing" }.toString(),
                    Modifier.weight(1f),
                    Color(0xFFfbbf24)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (isLoading) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryColor)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 20.dp)
                ) {
                    // Solicitudes de amistad pendientes
                    if (requests.isNotEmpty()) {
                        item { SectionHeader("Solicitudes de amistad (${requests.size})") }
                        items(requests, key = { it.id }) { request ->
                            FriendItem(
                                friend = request,
                                isRequest = true,
                                onAccept = {
                                    scope.launch {
                                        when (FriendsRepository.acceptFriendRequest(request.id)) {
                                            is UserResult.Success -> {
                                                showToast("¡Amigo aceptado!", "success")
                                                loadSocialPanel()
                                            }
                                            is UserResult.Error -> showToast("Error al aceptar", "error")
                                        }
                                    }
                                },
                                onReject = {
                                    scope.launch {
                                        when (FriendsRepository.rejectFriendRequest(request.id)) {
                                            is UserResult.Success -> {
                                                showToast("Solicitud rechazada", "error")
                                                loadSocialPanel()
                                            }
                                            is UserResult.Error -> showToast("Error al rechazar", "error")
                                        }
                                    }
                                }
                            )
                        }
                    }

                    // Lista de amigos
                    item { SectionHeader("Tus Amigos (${friends.size})") }
                    if (friends.isEmpty() && requests.isEmpty()) {
                        item {
                            Text(
                                "Aún no tienes amigos. ¡Añade uno!",
                                color = TextMutedColor,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(vertical = 20.dp)
                            )
                        }
                    }
                    items(friends, key = { it.id }) { friend ->
                        FriendItem(
                            friend = friend,
                            onInvite = { showToast("Retando a ${friend.name}...") },
                            onRemove = {
                                scope.launch {
                                    when (FriendsRepository.removeFriend(friend.id)) {
                                        is UserResult.Success -> {
                                            showToast("Eliminado de amigos", "error")
                                            loadSocialPanel()
                                        }
                                        is UserResult.Error -> showToast("Error al eliminar", "error")
                                    }
                                }
                            }
                        )
                    }

                    // Invitaciones de juego
                    if (gameInvites.isNotEmpty()) {
                        item { SectionHeader("Invitaciones de juego (${gameInvites.size})") }
                        items(gameInvites, key = { it.game_id }) { invite ->
                            GameInviteItem(
                                invite = invite,
                                onAccept = {
                                    scope.launch {
                                        when (GamesRepository.acceptGameInvite(invite.game_id)) {
                                            is UserResult.Success -> {
                                                showToast("¡Aceptando partida!", "success")
                                                onNavigate("waiting-room/${invite.mode}/${invite.game_id}")
                                            }
                                            is UserResult.Error -> showToast("Error al aceptar", "error")
                                        }
                                    }
                                },
                                onReject = {
                                    scope.launch {
                                        when (GamesRepository.rejectGameInvite(invite.game_id)) {
                                            is UserResult.Success -> {
                                                showToast("Invitación rechazada", "error")
                                                loadSocialPanel()
                                            }
                                            is UserResult.Error -> showToast("Error al rechazar", "error")
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { onNavigate("menu") },
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Volver al menú", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Dialog Añadir Amigo
        if (showAddFriendDialog) {
            Dialog(onDismissRequest = { showAddFriendDialog = false }) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = SurfaceColor,
                    border = BorderStroke(1.dp, BorderColor),
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Añadir nuevo amigo",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextColor,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Escribe su nombre de usuario para enviarle una solicitud de amistad",
                            fontSize = 14.sp,
                            color = TextMutedColor,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))

                        TextField(
                            value = newFriendName,
                            onValueChange = { newFriendName = it },
                            placeholder = { Text("Nombre de usuario", color = TextMutedColor) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Black.copy(alpha = 0.2f),
                                unfocusedContainerColor = Color.Black.copy(alpha = 0.2f),
                                focusedTextColor = TextColor,
                                unfocusedTextColor = TextColor,
                                focusedIndicatorColor = PrimaryColor,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            enabled = !isSendingRequest
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = onAddFriendSubmit,
                            enabled = !isSendingRequest,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isSendingRequest) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    "Enviar solicitud",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        TextButton(
                            onClick = { showAddFriendDialog = false },
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text("Cancelar", color = TextMutedColor)
                        }
                    }
                }
            }
        }

        // Toast
        if (toast.visible) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 60.dp),
                color = SurfaceColor,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (toast.type == "error") Color.Red else PrimaryColor)
            ) {
                Text(toast.message, modifier = Modifier.padding(16.dp), color = TextColor)
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier, color: Color = TextColor) {
    Surface(
        modifier = modifier,
        color = SurfaceColor.copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, BorderColor)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(label.uppercase(), fontSize = 10.sp, color = TextMutedColor, fontWeight = FontWeight.Bold)
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = color)
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextMutedColor, modifier = Modifier.padding(vertical = 8.dp))
}

@Composable
private fun FriendItem(
    friend: FriendInfo,
    isRequest: Boolean = false,
    onAccept: () -> Unit = {},
    onReject: () -> Unit = {},
    onInvite: () -> Unit = {},
    onRemove: () -> Unit = {}
) {
    val isOffline = friend.status == "offline" || friend.status == null

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SurfaceColor,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (isRequest) PrimaryColor.copy(0.6f) else BorderColor)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(45.dp).background(SurfaceLightColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(friend.name.firstOrNull()?.uppercase() ?: "?", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextColor)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(friend.name, fontWeight = FontWeight.Bold, color = TextColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if ((friend.unread_count ?: 0) > 0) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = PrimaryColor,
                            shape = CircleShape,
                            modifier = Modifier.size(18.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("${friend.unread_count}", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(6.dp).background(
                            when (friend.status) {
                                "online", "en linea" -> Color.Green
                                "playing", "jugando" -> Color.Yellow
                                else -> Color.Gray
                            }, CircleShape
                        )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = when (friend.status) {
                            "online" -> "En línea"
                            "playing" -> "Jugando"
                            "offline" -> "Desconectado"
                            else -> friend.status?.replaceFirstChar { it.uppercase() } ?: "Desconectado"
                        },
                        fontSize = 12.sp,
                        color = TextMutedColor
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("${friend.rr} RR", fontSize = 11.sp, color = Color(0xFFfbbf24), fontWeight = FontWeight.Bold)
                }
            }

            if (isRequest) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconButton(
                        onClick = onAccept,
                        modifier = Modifier.size(32.dp).background(Color(0xFF4ade80).copy(0.2f), CircleShape)
                    ) { Text("✓", color = Color(0xFF4ade80)) }
                    IconButton(
                        onClick = onReject,
                        modifier = Modifier.size(32.dp).background(Color(0xFFf87171).copy(0.2f), CircleShape)
                    ) { Text("✕", color = Color(0xFFf87171)) }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onInvite,
                        enabled = !isOffline,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryColor.copy(if (isOffline) 0.05f else 0.15f),
                            disabledContainerColor = Color.White.copy(0.05f)
                        ),
                        modifier = Modifier.height(32.dp).alpha(if (isOffline) 0.2f else 1f),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Text("Duelo", color = if (isOffline) TextMutedColor else PrimaryColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) {
                        Text("X", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFf87171))
                    }
                }
            }
        }
    }
}

@Composable
private fun GameInviteItem(
    invite: GameInviteInfo,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SurfaceColor,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, PrimaryColor.copy(0.6f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(45.dp).background(PrimaryColor.copy(0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(invite.creator.firstOrNull()?.uppercase() ?: "?", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PrimaryColor)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(invite.creator, fontWeight = FontWeight.Bold, color = TextColor)
                Text("${invite.mode}", fontSize = 11.sp, color = Color(0xFFc4b5fd))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconButton(
                    onClick = onAccept,
                    modifier = Modifier.size(32.dp).background(Color(0xFF4ade80).copy(0.2f), CircleShape)
                ) { Text("✓", color = Color(0xFF4ade80)) }
                IconButton(
                    onClick = onReject,
                    modifier = Modifier.size(32.dp).background(Color(0xFFf87171).copy(0.2f), CircleShape)
                ) { Text("✕", color = Color(0xFFf87171)) }
            }
        }
    }
}

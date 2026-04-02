package com.example.random_reversi.ui.screens

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.random_reversi.data.FriendsRepository
import com.example.random_reversi.data.GamesRepository
import com.example.random_reversi.data.UserResult
import com.example.random_reversi.data.remote.ChatMessage
import com.example.random_reversi.data.remote.FriendInfo
import com.example.random_reversi.data.remote.GameInviteInfo
import com.example.random_reversi.ui.components.GameModeModal
import com.example.random_reversi.ui.navigation.NavigationMessages
import com.example.random_reversi.ui.theme.BgColor
import com.example.random_reversi.ui.theme.BorderColor
import com.example.random_reversi.ui.theme.PrimaryColor
import com.example.random_reversi.ui.theme.SurfaceColor
import com.example.random_reversi.ui.theme.SurfaceLightColor
import com.example.random_reversi.ui.theme.TextColor
import com.example.random_reversi.ui.theme.TextMutedColor
import com.example.random_reversi.utils.AvatarPresets
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class ToastState(
    val message: String,
    val type: String = "info",
    val visible: Boolean = false
)

private fun normalizeMode(mode: String?): String = when (mode?.trim()?.lowercase()) {
    "1vs1vs1vs1", "1v1v1v1" -> "1vs1vs1vs1"
    "1vs1", "1v1" -> "1vs1"
    else -> "1vs1"
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier, tint: Color = PrimaryColor) {
    Surface(
        modifier = modifier,
        color = SurfaceColor,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, BorderColor)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, color = tint, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
            Text(label, color = TextMutedColor, fontSize = 11.sp)
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SurfaceColor,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, BorderColor)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, color = TextColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            content()
        }
    }
}

@Composable
private fun EmptyState(text: String) {
    Text(text = text, color = TextMutedColor, fontSize = 13.sp)
}

@Composable
private fun FriendRow(
    friend: FriendInfo,
    onOpenProfile: () -> Unit,
    onInvite: () -> Unit,
    onChat: () -> Unit,
    onRemove: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SurfaceLightColor,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, BorderColor)
    ) {
        Row(
            modifier = Modifier
                .clickable { onOpenProfile() }
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AvatarBadge(friend.name, friend.avatar_url)
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(friend.name, color = TextColor, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${friend.rr} RR", color = Color(0xFFFBBF24), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SmallActionButton(
                    text = "Duelo",
                    color = PrimaryColor,
                    enabled = (friend.status ?: "online").lowercase() != "offline",
                    onClick = onInvite
                )
                SmallActionButton(text = "Chat", color = Color(0xFF38BDF8), onClick = onChat, badge = friend.unread_count ?: 0)
                SmallActionButton(text = "X", color = Color(0xFFF87171), onClick = onRemove)
            }
        }
    }
}

@Composable
private fun RequestRow(request: FriendInfo, onAccept: () -> Unit, onReject: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SurfaceLightColor,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, BorderColor)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AvatarBadge(request.name, request.avatar_url)
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(request.name, color = TextColor, fontWeight = FontWeight.Bold)
                Text("${request.rr} RR", color = Color(0xFFFBBF24), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            SmallActionButton(text = "OK", color = Color(0xFF4ADE80), onClick = onAccept)
            Spacer(modifier = Modifier.width(6.dp))
            SmallActionButton(text = "X", color = Color(0xFFF87171), onClick = onReject)
        }
    }
}

@Composable
private fun InviteRow(invite: GameInviteInfo, onAccept: () -> Unit, onReject: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SurfaceLightColor,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, BorderColor)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AvatarBadge(invite.name ?: "Jugador", invite.avatar_url)
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(invite.name ?: "Jugador", color = TextColor, fontWeight = FontWeight.Bold)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${invite.rr ?: 0} RR",
                        color = Color(0xFFFBBF24),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = "  -  ",
                        color = TextMutedColor,
                        fontSize = 12.sp
                    )

                    Text(
                        text = normalizeMode(invite.gameMode),
                        color = Color(0xFFA78BFA),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            SmallActionButton(text = "OK", color = Color(0xFF4ADE80), onClick = onAccept)
            Spacer(modifier = Modifier.width(6.dp))
            SmallActionButton(text = "X", color = Color(0xFFF87171), onClick = onReject)
        }
    }
}

@Composable
private fun SmallActionButton(
    text: String,
    color: Color,
    enabled: Boolean = true,
    badge: Int = 0,
    onClick: () -> Unit
) {
    Box {
        Button(
            onClick = onClick,
            enabled = enabled,
            colors = ButtonDefaults.buttonColors(containerColor = color, disabledContainerColor = color.copy(alpha = 0.35f)),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.height(32.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }

        if (badge > 0) {
            Surface(
                modifier = Modifier.align(Alignment.TopEnd).size(18.dp),
                shape = CircleShape,
                color = Color(0xFFEF4444),
                border = BorderStroke(1.dp, BgColor)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = if (badge > 99) "99+" else badge.toString(),
                        color = Color.White,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun AvatarBadge(name: String, avatarUrl: String?) {
    Surface(
        modifier = Modifier.size(44.dp).clip(CircleShape),
        shape = CircleShape,
        color = SurfaceColor,
        border = BorderStroke(1.dp, BorderColor)
    ) {
        val presetRes = AvatarPresets.drawableForId(avatarUrl)
        when {
            presetRes != null -> {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = presetRes),
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
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(name.firstOrNull()?.uppercaseChar()?.toString() ?: "J", color = TextColor, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ToastBanner(toast: ToastState) {
    val bg = when (toast.type) {
        "success" -> Color(0xFF15803D)
        "error" -> Color(0xFFB91C1C)
        else -> Color(0xFF1D4ED8)
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 22.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Surface(
            color = bg,
            shape = RoundedCornerShape(999.dp),
            shadowElevation = 6.dp
        ) {
            Text(
                text = toast.message,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun AddFriendDialog(
    open: Boolean,
    value: String,
    sending: Boolean,
    onValueChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSubmit: () -> Unit
) {
    if (!open) return
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = SurfaceColor,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, BorderColor)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Anadir nuevo amigo", color = TextColor, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("Escribe su nombre de usuario para enviar una solicitud", color = TextMutedColor, fontSize = 12.sp)

                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    label = { Text("Nombre de usuario") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryColor,
                        unfocusedBorderColor = BorderColor,
                        focusedTextColor = TextColor,
                        unfocusedTextColor = TextColor,
                        focusedLabelColor = PrimaryColor,
                        unfocusedLabelColor = TextMutedColor
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155))
                    ) {
                        Text("Cancelar", color = Color.White)
                    }
                    Button(
                        onClick = onSubmit,
                        enabled = !sending,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)
                    ) {
                        Text(if (sending) "Enviando..." else "Enviar", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupInviteDialog(
    open: Boolean,
    ownerFriend: FriendInfo?,
    candidates: List<FriendInfo>,
    selected: Set<Int>,
    onDismiss: () -> Unit,
    onToggle: (Int) -> Unit,
    onConfirm: () -> Unit
) {
    if (!open) return
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = SurfaceColor,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, BorderColor)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Selecciona 2 amigos extra", color = TextColor, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(
                    "Para iniciar 1vs1vs1vs1 con ${ownerFriend?.name ?: "tu amigo"}, elige dos jugadores mas.",
                    color = TextMutedColor,
                    fontSize = 12.sp
                )

                if (candidates.isEmpty()) {
                    EmptyState("No hay amigos disponibles para completar la sala.")
                } else {
                    candidates.forEach { friend ->
                        val isSelected = selected.contains(friend.id)
                        val disabled = !isSelected && selected.size >= 2
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(enabled = !disabled) { onToggle(friend.id) },
                            color = if (isSelected) PrimaryColor.copy(alpha = 0.16f) else SurfaceLightColor,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, if (isSelected) PrimaryColor else BorderColor)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AvatarBadge(friend.name, friend.avatar_url)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(friend.name, color = TextColor, fontWeight = FontWeight.Bold)
                                    Text("${friend.rr} RR", color = TextMutedColor, fontSize = 12.sp)
                                }
                                Text(if (isSelected) "Seleccionado" else "", color = PrimaryColor, fontSize = 11.sp)
                            }
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155))
                    ) { Text("Cancelar", color = Color.White) }

                    Button(
                        onClick = onConfirm,
                        enabled = selected.size == 2,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)
                    ) { Text("Invitar", color = Color.White, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

@Composable
private fun ChatDialog(
    open: Boolean,
    friend: FriendInfo?,
    loading: Boolean,
    sending: Boolean,
    messages: List<ChatMessage>,
    input: String,
    onInputChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSend: () -> Unit
) {
    if (!open || friend == null) return

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = SurfaceColor,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, BorderColor)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AvatarBadge(friend.name, friend.avatar_url)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Chat con ${friend.name}", color = TextColor, fontWeight = FontWeight.Bold)
                        Text("Mensajes privados entre amigos", color = TextMutedColor, fontSize = 12.sp)
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth().height(280.dp),
                    color = SurfaceLightColor,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, BorderColor)
                ) {
                    if (loading) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = PrimaryColor)
                        }
                    } else if (messages.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            EmptyState("No hay mensajes aun. Empieza la conversacion.")
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(messages) { msg ->
                                val isMine = msg.sender_id != friend.id
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
                                ) {
                                    Surface(
                                        color = if (isMine) PrimaryColor.copy(alpha = 0.22f) else Color(0xFF334155),
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, BorderColor)
                                    ) {
                                        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                                            Text(msg.message, color = TextColor, fontSize = 13.sp)
                                            Text(
                                                formatChatTime(msg.timestamp),
                                                color = TextMutedColor,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = input,
                    onValueChange = onInputChange,
                    label = { Text("Escribe un mensaje...") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryColor,
                        unfocusedBorderColor = BorderColor,
                        focusedTextColor = TextColor,
                        unfocusedTextColor = TextColor,
                        focusedLabelColor = PrimaryColor,
                        unfocusedLabelColor = TextMutedColor
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155))
                    ) { Text("Cerrar", color = Color.White) }

                    Button(
                        onClick = onSend,
                        enabled = !sending && input.trim().isNotBlank(),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)
                    ) {
                        Text(if (sending) "Enviando..." else "Enviar", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun formatChatTime(raw: String): String {
    return try {
        val input = when {
            raw.contains("T") -> SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            else -> SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        }
        val parsed: Date = input.parse(raw.substring(0, 19)) ?: return raw.take(5)
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(parsed)
    } catch (_: Exception) {
        raw.take(5)
    }
}

@Composable
fun FriendsScreen(onNavigate: (String) -> Unit) {
    val scope = rememberCoroutineScope()

    var friends by remember { mutableStateOf<List<FriendInfo>>(emptyList()) }
    var requests by remember { mutableStateOf<List<FriendInfo>>(emptyList()) }
    var invites by remember { mutableStateOf<List<GameInviteInfo>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    var showAddDialog by remember { mutableStateOf(false) }
    var addUsername by remember { mutableStateOf("") }
    var sendingRequest by remember { mutableStateOf(false) }

    var selectedFriend by remember { mutableStateOf<FriendInfo?>(null) }
    var showModeDialog by remember { mutableStateOf(false) }
    var showGroupDialog by remember { mutableStateOf(false) }
    var selectedExtraFriendIds by remember { mutableStateOf<Set<Int>>(emptySet()) }

    var showChatDialog by remember { mutableStateOf(false) }
    var chatFriend by remember { mutableStateOf<FriendInfo?>(null) }
    var chatMessages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var chatInput by remember { mutableStateOf("") }
    var chatLoading by remember { mutableStateOf(false) }
    var chatSending by remember { mutableStateOf(false) }

    var toast by remember { mutableStateOf(ToastState("")) }
    var hasLoadedPanel by remember { mutableStateOf(false) }
    var seenInviteIds by remember { mutableStateOf<Set<Int>>(emptySet()) }

    fun showToast(message: String, type: String = "info") {
        scope.launch {
            toast = ToastState(message = message, type = type, visible = true)
            delay(2600)
            toast = toast.copy(visible = false)
        }
    }

    fun setUnreadCount(friendId: Int, unread: Int) {
        friends = friends.map { if (it.id == friendId) it.copy(unread_count = unread) else it }
    }

    fun loadPanel(showSpinner: Boolean) {
        scope.launch {
            if (showSpinner) loading = true
            when (val result = FriendsRepository.getSocialPanel()) {
                is UserResult.Success -> {
                    friends = result.data.friends
                    requests = result.data.requests
                    invites = result.data.gameRequests
                    val currentIds = result.data.gameRequests.map { it.lobby_id }.toSet()
                    val newInvites = result.data.gameRequests.filter { !seenInviteIds.contains(it.lobby_id) }
                    if (hasLoadedPanel && newInvites.isNotEmpty()) {
                        val firstInvite = newInvites.first()
                        val sender = firstInvite.name ?: "Un amigo"
                        val modeLabel = normalizeMode(firstInvite.gameMode)
                        showToast(
                            "$sender te ha invitado a jugar una partida $modeLabel, puedes aceptarla desde la pestana de amigos.",
                            "info"
                        )
                    }
                    seenInviteIds = currentIds
                    hasLoadedPanel = true
                }
                is UserResult.Error -> showToast(result.message, "error")
            }
            if (showSpinner) loading = false
        }
    }

    fun openChat(friend: FriendInfo) {
        chatFriend = friend
        chatInput = ""
        showChatDialog = true
        scope.launch {
            chatLoading = true
            when (val result = FriendsRepository.getChatHistory(friend.id)) {
                is UserResult.Success -> {
                    chatMessages = result.data
                    FriendsRepository.markChatRead(friend.id)
                    setUnreadCount(friend.id, 0)
                }
                is UserResult.Error -> showToast(result.message, "error")
            }
            chatLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadPanel(showSpinner = true)
    }

    LaunchedEffect(Unit) {
        NavigationMessages.consumeFriendsToast()?.let { message ->
            showToast(message, "info")
        }
    }

    LaunchedEffect(Unit) {
        while (isActive) {
            delay(5000)
            loadPanel(showSpinner = false)
        }
    }

    LaunchedEffect(showChatDialog, chatFriend?.id) {
        if (!showChatDialog || chatFriend == null) return@LaunchedEffect
        while (isActive && showChatDialog && chatFriend != null) {
            delay(3000)
            when (val result = FriendsRepository.getChatHistory(chatFriend!!.id)) {
                is UserResult.Success -> {
                    chatMessages = result.data
                    FriendsRepository.markChatRead(chatFriend!!.id)
                    setUnreadCount(chatFriend!!.id, 0)
                }
                is UserResult.Error -> {}
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Amigos", color = TextColor, fontWeight = FontWeight.ExtraBold, fontSize = 30.sp)
                    Text("Conecta y juega con tus amigos", color = TextMutedColor, fontSize = 13.sp)
                }
                Button(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Anadir", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Spacer(modifier = Modifier.height(6.dp))

            if (loading) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = SurfaceColor,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, BorderColor)
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PrimaryColor)
                    }
                }
            } else {
                SectionCard("Tus amigos (${friends.size})") {
                    if (friends.isEmpty()) {
                        EmptyState("No tienes amigos agregados todavia.")
                    } else {
                        friends.forEach { friend ->
                            FriendRow(
                                friend = friend,
                                onOpenProfile = {
                                    val encodedName = Uri.encode(friend.name)
                                    onNavigate("profile/${friend.id}/$encodedName")
                                },
                                onInvite = {
                                    selectedFriend = friend
                                    showModeDialog = true
                                },
                                onChat = { openChat(friend) },
                                onRemove = {
                                    scope.launch {
                                        when (val result = FriendsRepository.removeFriend(friend.id)) {
                                            is UserResult.Success -> {
                                                friends = friends.filter { it.id != friend.id }
                                                loadPanel(showSpinner = false)
                                                showToast("${friend.name} eliminado de tus amigos", "info")
                                            }
                                            is UserResult.Error -> showToast(result.message, "error")
                                        }
                                    }
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                SectionCard("Solicitudes de amistad (${requests.size})") {
                    if (requests.isEmpty()) {
                        EmptyState("Sin solicitudes de amistad")
                    } else {
                        requests.forEach { request ->
                            RequestRow(
                                request = request,
                                onAccept = {
                                    scope.launch {
                                        when (val result = FriendsRepository.acceptFriendRequest(request.id)) {
                                            is UserResult.Success -> {
                                                requests = requests.filter { it.id != request.id }
                                                loadPanel(showSpinner = false)
                                                showToast("Ahora eres amigo de ${request.name}", "success")
                                            }
                                            is UserResult.Error -> showToast(result.message, "error")
                                        }
                                    }
                                },
                                onReject = {
                                    scope.launch {
                                        when (val result = FriendsRepository.rejectFriendRequest(request.id)) {
                                            is UserResult.Success -> {
                                                requests = requests.filter { it.id != request.id }
                                                showToast("Solicitud de ${request.name} rechazada", "info")
                                            }
                                            is UserResult.Error -> showToast(result.message, "error")
                                        }
                                    }
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                SectionCard("Solicitudes de juego (${invites.size})") {
                    if (invites.isEmpty()) {
                        EmptyState("Sin solicitudes de juego")
                    } else {
                        invites.forEach { invite ->
                            InviteRow(
                                invite = invite,
                                onAccept = {
                                    scope.launch {
                                        when (val result = GamesRepository.acceptGameInvite(invite.lobby_id)) {
                                            is UserResult.Success -> {
                                                invites = invites.filter { it.lobby_id != invite.lobby_id }
                                                showToast("Aceptando partida...", "success")
                                                val encodedName = Uri.encode(invite.name ?: "Un jugador")
                                                onNavigate("waiting-room/${normalizeMode(invite.gameMode)}/${result.data}/friends/$encodedName")
                                            }
                                            is UserResult.Error -> showToast(result.message, "error")
                                        }
                                    }
                                },
                                onReject = {
                                    scope.launch {
                                        when (val result = GamesRepository.rejectGameInvite(invite.lobby_id)) {
                                            is UserResult.Success -> {
                                                invites = invites.filter { it.lobby_id != invite.lobby_id }
                                                showToast("Invitacion rechazada", "info")
                                            }
                                            is UserResult.Error -> showToast(result.message, "error")
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
                modifier = Modifier.fillMaxWidth(0.72f).height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Volver al menu", color = Color.White, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        if (toast.visible) {
            ToastBanner(toast)
        }
    }

    AddFriendDialog(
        open = showAddDialog,
        value = addUsername,
        sending = sendingRequest,
        onValueChange = { addUsername = it },
        onDismiss = {
            showAddDialog = false
            addUsername = ""
        },
        onSubmit = {
            val username = addUsername.trim()
            if (username.isBlank()) {
                showToast("Escribe un nombre de usuario", "error")
                return@AddFriendDialog
            }
            scope.launch {
                sendingRequest = true
                when (val result = FriendsRepository.sendFriendRequest(username)) {
                    is UserResult.Success -> {
                        showToast("Solicitud enviada a $username", "success")
                        addUsername = ""
                        showAddDialog = false
                    }
                    is UserResult.Error -> showToast(result.message, "error")
                }
                sendingRequest = false
            }
        }
    )

    GameModeModal(
        isOpen = showModeDialog,
        onClose = { showModeDialog = false },
        title = "Retar a un duelo",
        subtitle = "Que modo de juego quieres jugar contra ${selectedFriend?.name ?: "tu amigo"}?",
        onSelectMode = { rawMode ->
            val mode = normalizeMode(rawMode)
            val friend = selectedFriend ?: return@GameModeModal
            if (mode == "1vs1vs1vs1") {
                showModeDialog = false
                selectedExtraFriendIds = emptySet()
                showGroupDialog = true
                return@GameModeModal
            }
            scope.launch {
                showModeDialog = false
                when (val result = GamesRepository.inviteFriends(mode, listOf(friend.id))) {
                    is UserResult.Success -> {
                        showToast("Invitacion enviada", "success")
                        val encodedName = Uri.encode(friend.name)
                        onNavigate("waiting-room/$mode/${result.data.game_id}/friends/$encodedName")
                    }
                    is UserResult.Error -> showToast(result.message, "error")
                }
            }
        }
    )

    GroupInviteDialog(
        open = showGroupDialog,
        ownerFriend = selectedFriend,
        candidates = friends.filter { it.id != selectedFriend?.id },
        selected = selectedExtraFriendIds,
        onDismiss = {
            showGroupDialog = false
            selectedExtraFriendIds = emptySet()
        },
        onToggle = { friendId ->
            selectedExtraFriendIds = if (selectedExtraFriendIds.contains(friendId)) {
                selectedExtraFriendIds - friendId
            } else {
                if (selectedExtraFriendIds.size >= 2) selectedExtraFriendIds else selectedExtraFriendIds + friendId
            }
        },
        onConfirm = {
            val owner = selectedFriend ?: return@GroupInviteDialog
            if (selectedExtraFriendIds.size != 2) {
                showToast("Selecciona exactamente 2 amigos mas", "error")
                return@GroupInviteDialog
            }
            scope.launch {
                val ids = listOf(owner.id) + selectedExtraFriendIds.toList()
                when (val result = GamesRepository.inviteFriends("1vs1vs1vs1", ids)) {
                    is UserResult.Success -> {
                        showGroupDialog = false
                        selectedExtraFriendIds = emptySet()
                        showToast("Invitaciones enviadas", "success")
                        onNavigate("waiting-room/1vs1vs1vs1/${result.data.game_id}/friends")
                    }
                    is UserResult.Error -> showToast(result.message, "error")
                }
            }
        }
    )

    ChatDialog(
        open = showChatDialog,
        friend = chatFriend,
        loading = chatLoading,
        sending = chatSending,
        messages = chatMessages,
        input = chatInput,
        onInputChange = { chatInput = it },
        onDismiss = {
            showChatDialog = false
            chatFriend = null
            chatMessages = emptyList()
            chatInput = ""
        },
        onSend = {
            val friend = chatFriend ?: return@ChatDialog
            val text = chatInput.trim()
            if (text.isBlank() || chatSending) return@ChatDialog
            scope.launch {
                chatSending = true
                when (val result = FriendsRepository.sendChatMessage(friend.id, text)) {
                    is UserResult.Success -> {
                        chatInput = ""
                        when (val refresh = FriendsRepository.getChatHistory(friend.id)) {
                            is UserResult.Success -> chatMessages = refresh.data
                            is UserResult.Error -> {}
                        }
                    }
                    is UserResult.Error -> showToast(result.message, "error")
                }
                chatSending = false
            }
        }
    )
}



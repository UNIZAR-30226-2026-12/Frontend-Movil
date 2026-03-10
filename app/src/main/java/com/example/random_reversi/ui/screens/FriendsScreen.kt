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
import com.example.random_reversi.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// --- Modelos de Datos ---
private data class Friend(
    val id: Int,
    val name: String,
    val status: String, // "en linea", "offline", "jugando"
    val rr: Int,
    val gameMode: String? = null,
    val playersCount: String? = null
)

private data class ToastData(
    val message: String,
    val type: String, // "success", "info", "error"
    val visible: Boolean = false
)

// --- Datos Mock ---
private val MOCK_FRIENDS = listOf(
    Friend(1, "CyberNinja", "en linea", 1420),
    Friend(2, "ReversiMaster", "jugando", 2150),
    Friend(3, "StarPlayer99", "offline", 1100),
    Friend(4, "RoboTactics", "en linea", 1575)
)

private val MOCK_REQUESTS = listOf(
    Friend(101, "GamerX", "offline", 845),
    Friend(102, "PixelArtist", "offline", 1320)
)

private val MOCK_GAME_REQUESTS = listOf(
    Friend(4, "RoboTactics", "en linea", 1575, "1vs1", "1/2"),
    Friend(1, "CyberNinja", "en linea", 1420, "1vs1vs1vs1", "3/4")
)

private val AVATAR_EMOJIS = listOf("🟣", "🔵", "⚪", "⚫")

private fun getAvatarFromSeed(seed: String): String {
    var hash = 0
    seed.forEach { char ->
        hash = (hash * 31 + char.code)
    }
    val index = Math.abs(hash) % AVATAR_EMOJIS.size
    return AVATAR_EMOJIS[index]
}

@Composable
fun FriendsScreen(onNavigate: (String) -> Unit) {
    // Estados principales
    var friends by remember { mutableStateOf(MOCK_FRIENDS) }
    var requests by remember { mutableStateOf(MOCK_REQUESTS) }
    var gameRequests by remember { mutableStateOf(MOCK_GAME_REQUESTS) }
    var toast by remember { mutableStateOf(ToastData("", "info")) }

    // Estado del Diálogo "Añadir Amigo"
    var showAddFriendDialog by remember { mutableStateOf(false) }
    var newFriendName by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()

    fun showToast(message: String, type: String = "info") {
        scope.launch {
            toast = ToastData(message, type, true)
            delay(3000)
            toast = toast.copy(visible = false)
        }
    }

    // Lógica para añadir amigo
    val onAddFriendSubmit = {
        val trimmedName = newFriendName.trim()
        if (trimmedName.isEmpty()) {
            showToast("Escribe un nombre de usuario", "error")
        } else {
            val alreadyFriend = friends.any { it.name.equals(trimmedName, ignoreCase = true) }
            if (alreadyFriend) {
                showToast("$trimmedName ya está en tu lista", "error")
            } else {
                showToast("Solicitud enviada a $trimmedName", "info")
                showAddFriendDialog = false
                newFriendName = ""
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
                StatCard("En línea", friends.count { it.status == "en linea" }.toString(), Modifier.weight(1f), Color(0xFF4ade80))
                StatCard("Jugando", friends.count { it.status == "jugando" }.toString(), Modifier.weight(1f), Color(0xFFfbbf24))
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Listas
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                if (requests.isNotEmpty()) {
                    item { SectionHeader("Solicitudes de amistad (${requests.size})") }
                    items(requests) { request ->
                        FriendItem(
                            friend = request,
                            isRequest = true,
                            onAccept = {
                                requests = requests.filter { it.id != request.id }
                                friends = friends + request.copy(status = "en linea")
                                showToast("¡Amigo aceptado!", "success")
                            },
                            onReject = {
                                requests = requests.filter { it.id != request.id }
                                showToast("Solicitud rechazada", "error")
                            }
                        )
                    }
                }

                item { SectionHeader("Tus Amigos (${friends.size})") }
                items(friends) { friend ->
                    FriendItem(
                        friend = friend,
                        onInvite = { showToast("Retando a ${friend.name}...") },
                        onRemove = {
                            friends = friends.filter { it.id != friend.id }
                            gameRequests = gameRequests.filter { it.name != friend.name }
                            showToast("Eliminado de amigos", "error")
                        }
                    )
                }

                val activeGameRequests = gameRequests.filter { req ->
                    friends.any { f -> f.name == req.name && f.status == "en linea" }
                }

                if (activeGameRequests.isNotEmpty()) {
                    item { SectionHeader("Solicitudes de juego (${activeGameRequests.size})") }
                    items(activeGameRequests) { gRequest ->
                        FriendItem(
                            friend = gRequest,
                            isGameRequest = true,
                            onAccept = {
                                gameRequests = gameRequests.filter { it.id != gRequest.id }
                                showToast("Aceptando duelo de ${gRequest.name}...", "success")
                            },
                            onReject = {
                                gameRequests = gameRequests.filter { it.id != gRequest.id }
                                showToast("Duelo rechazado", "error")
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Botón Volver
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

        // --- DIÁLOGO AÑADIR AMIGO ---
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
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Botón Enviar Solicitud con estilo estandarizado (Igual que Volver al menú)
                        Button(
                            onClick = onAddFriendSubmit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp), // Misma altura que el botón Volver
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
                            shape = RoundedCornerShape(12.dp) // Mismo redondeado
                        ) {
                            Text(
                                "Enviar solicitud",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
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

        // Toast Overlay
        if (toast.visible) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 60.dp),
                color = SurfaceColor,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if(toast.type == "error") Color.Red else PrimaryColor)
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
    friend: Friend,
    isRequest: Boolean = false,
    isGameRequest: Boolean = false,
    onAccept: () -> Unit = {},
    onReject: () -> Unit = {},
    onInvite: () -> Unit = {},
    onRemove: () -> Unit = {}
) {
    val isOffline = friend.status == "offline"

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SurfaceColor,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            1.dp,
            if(isRequest || isGameRequest) PrimaryColor.copy(0.6f) else BorderColor
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(45.dp).background(SurfaceLightColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(getAvatarFromSeed(friend.name), fontSize = 24.sp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(friend.name, fontWeight = FontWeight.Bold, color = TextColor, maxLines = 1, overflow = TextOverflow.Ellipsis)

                if (isGameRequest) {
                    Text("${friend.gameMode} • ${friend.playersCount}", fontSize = 11.sp, color = Color(0xFFc4b5fd))
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(6.dp).background(
                            when(friend.status) {
                                "en linea" -> Color.Green
                                "jugando" -> Color.Yellow
                                else -> Color.Gray
                            }, CircleShape
                        ))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = friend.status.replaceFirstChar { it.uppercase() },
                            fontSize = 12.sp,
                            color = TextMutedColor
                        )
                    }
                }
            }

            if (isRequest || isGameRequest) {
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
                        Text("🗑️", fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

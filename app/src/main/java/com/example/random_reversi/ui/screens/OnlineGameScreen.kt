package com.example.random_reversi.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.random_reversi.data.GamesRepository
import com.example.random_reversi.data.UserProfileStore
import com.example.random_reversi.data.UserRepository
import com.example.random_reversi.data.UserResult
import com.example.random_reversi.data.remote.HistoryEntry
import com.example.random_reversi.data.remote.PublicLobby
import com.example.random_reversi.data.remote.UserStatsResponse
import com.example.random_reversi.ui.components.GameModeModal
import com.example.random_reversi.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun OnlineGameScreen(onNavigate: (String) -> Unit) {
    var publicGames by remember { mutableStateOf<List<PublicLobby>>(emptyList()) }
    var userElo by remember { mutableStateOf(0) }
    var history by remember { mutableStateOf<List<HistoryEntry>>(emptyList()) }
    var isRefreshing by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var isCreating by remember { mutableStateOf(false) }
    var showCreateModal by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun loadData() {
        scope.launch {
            isRefreshing = true
            // Cargar perfil para ELO
            when (val meResult = UserRepository.getMe()) {
                is UserResult.Success -> userElo = meResult.data.elo
                is UserResult.Error -> {}
            }
            // Cargar lobbies públicos
            when (val result = GamesRepository.getPublicLobbies()) {
                is UserResult.Success -> publicGames = result.data
                is UserResult.Error -> errorMsg = result.message
            }
            // Cargar historial
            when (val histResult = GamesRepository.getMyHistory()) {
                is UserResult.Success -> history = histResult.data
                is UserResult.Error -> {}
            }
            isRefreshing = false
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadData()
    }

    Box(modifier = Modifier.fillMaxSize().background(BgColor)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(50.dp))

            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Jugar Online", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = TextColor)
                    Text("Compite contra el mundo", fontSize = 13.sp, color = TextMutedColor)
                }

                Button(
                    onClick = { showCreateModal = true },
                    enabled = !isCreating,
                    modifier = Modifier.height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Crear Partida", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ELO + Historial
            Surface(
                color = SurfaceColor,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, BorderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("ELO ACTUAL", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PrimaryColor)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("$userElo RR", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = TextColor)
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("HISTORIAL", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMutedColor)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            history.take(5).forEach { match ->
                                val letter = when (match.result.lowercase()) {
                                    "win", "ganada", "victoria" -> "V"
                                    "loss", "perdida", "derrota" -> "D"
                                    else -> "E"
                                }
                                val textColor = when (match.result.lowercase()) {
                                    "win", "ganada", "victoria" -> Color(0xFF4ADE80)
                                    "loss", "perdida", "derrota" -> Color(0xFFF87171)
                                    else -> Color.Gray
                                }
                                Text(
                                    text = letter,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    color = textColor
                                )
                            }
                            if (history.isEmpty()) {
                                Text("Sin partidas", fontSize = 12.sp, color = TextMutedColor)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Título + Refresh
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Partidas Públicas", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextColor)

                TextButton(
                    onClick = { loadData() },
                    enabled = !isRefreshing
                ) {
                    if (isRefreshing) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = PrimaryColor, strokeWidth = 2.dp)
                    } else {
                        Text("Actualizar", color = PrimaryColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryColor)
                }
            } else if (publicGames.isEmpty()) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No hay partidas públicas", fontSize = 16.sp, color = TextMutedColor)
                        Text("¡Crea una nueva!", fontSize = 14.sp, color = TextMutedColor.copy(0.6f))
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(publicGames, key = { it.game_id }) { game ->
                        GameSessionCard(
                            game = game,
                            onJoin = {
                                scope.launch {
                                    when (val joinResult = GamesRepository.joinLobby(game.game_id)) {
                                        is UserResult.Success -> {
                                            onNavigate("waiting-room/${game.mode}/${game.game_id}")
                                        }
                                        is UserResult.Error -> {
                                            errorMsg = joinResult.message
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }

            // Error toast
            errorMsg?.let { msg ->
                LaunchedEffect(msg) {
                    kotlinx.coroutines.delay(3000)
                    errorMsg = null
                }
                Surface(
                    color = SurfaceColor,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color.Red),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text(msg, modifier = Modifier.padding(12.dp), color = Color(0xFFF87171), fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

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
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Modal crear partida
    GameModeModal(
        isOpen = showCreateModal,
        onClose = { showCreateModal = false },
        onSelectMode = { mode ->
            showCreateModal = false
            isCreating = true
            scope.launch {
                when (val result = GamesRepository.createLobby(mode)) {
                    is UserResult.Success -> {
                        onNavigate("waiting-room/$mode/${result.data.game_id}")
                    }
                    is UserResult.Error -> {
                        errorMsg = result.message
                    }
                }
                isCreating = false
            }
        }
    )
}

@Composable
private fun GameSessionCard(game: PublicLobby, onJoin: () -> Unit) {
    Surface(
        color = SurfaceColor,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, BorderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(30.dp).background(SurfaceLightColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(game.creator.first().toString(), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(game.creator, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(game.mode, fontSize = 10.sp, color = PrimaryColor, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${game.creator_elo} RR", fontSize = 11.sp, color = Color(0xFFfbbf24), fontWeight = FontWeight.Bold)
                Text("${game.players}/${game.max_players}", fontSize = 11.sp, color = TextMutedColor)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onJoin,
                modifier = Modifier.fillMaxWidth().height(32.dp),
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4ade80).copy(0.1f)),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFF4ade80).copy(0.3f))
            ) {
                Text("Unirse", color = Color(0xFF4ade80), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

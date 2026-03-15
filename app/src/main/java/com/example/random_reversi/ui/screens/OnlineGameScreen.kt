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
import com.example.random_reversi.ui.components.GameModeModal
import com.example.random_reversi.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// --- Modelos de Datos ---
private data class GameSession(
    val id: Int,
    val creator: String,
    val creatorRR: Int,
    val mode: String,
    val players: Int,
    val maxPlayers: Int,
    val status: String
)

private data class GameHistory(
    val id: Int,
    val date: String,
    val mode: String,
    val result: String, // "Ganada", "Perdida", "Empate"
    val score: String,
    val rankChange: String
)

// --- Mocks ---
private val MOCK_PUBLIC_GAMES = listOf(
    GameSession(1, "CyberNinja", 1420, "1vs1", 1, 2, "waiting"),
    GameSession(2, "ReversiExpert", 1850, "1vs1vs1vs1", 3, 4, "waiting"),
    GameSession(4, "DarkMaster", 1680, "1vs1", 1, 2, "waiting"),
    GameSession(5, "LighSaber", 1350, "1vs1vs1vs1", 2, 4, "waiting")
)

private val MOCK_HISTORY = listOf(
    GameHistory(1, "12 May", "1vs1", "Ganada", "42-22", "+25 RR"),
    GameHistory(2, "11 May", "1vs1vs1vs1", "Perdida", "10-30", "-15 RR"),
    GameHistory(3, "10 May", "1vs1", "Empate", "32-32", "+0 RR"),
    GameHistory(4, "09 May", "1vs1", "Ganada", "50-14", "+30 RR"),
    GameHistory(5, "08 May", "1vs1", "Perdida", "20-44", "-10 RR")
)

@Composable
fun OnlineGameScreen(onNavigate: (String) -> Unit) {
    var publicGames by remember { mutableStateOf(MOCK_PUBLIC_GAMES) }
    var userElo by remember { mutableStateOf(1500) }
    var isRefreshing by remember { mutableStateOf(false) }
    var showCreateModal by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

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

                // Botón Crear Partida (Estilo estandarizado idéntico al botón volver)
                Button(
                    onClick = { showCreateModal = true },
                    modifier = Modifier.height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Crear Partida", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Sección ELO e Historial
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
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { // Espaciado entre letras
                            MOCK_HISTORY.take(5).forEach { match ->
                                // Asignamos la letra según el resultado
                                val letter = when(match.result) {
                                    "Ganada" -> "V"
                                    "Perdida" -> "D"
                                    else -> "E"
                                }
                                // Asignamos el color según el resultado
                                val textColor = when(match.result) {
                                    "Ganada" -> Color(0xFF4ADE80) // Verde
                                    "Perdida" -> Color(0xFFF87171) // Rojo
                                    else -> Color.Gray // Gris para empate
                                }

                                Text(
                                    text = letter,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    color = textColor
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Título de lista y Refresh
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Partidas Públicas", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextColor)

                TextButton(
                    onClick = {
                        scope.launch {
                            isRefreshing = true
                            delay(1000)
                            isRefreshing = false
                        }
                    }
                ) {
                    Text("Actualizar", color = PrimaryColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            // Grid de Partidas
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(publicGames) { game ->
                    GameSessionCard(game = game, onJoin = { onNavigate("waiting-room/${game.mode}") })
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Botón Volver al menú
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

    // Modal para seleccionar modo de Juego Online (Humano vs Humano)
    GameModeModal(
        isOpen = showCreateModal,
        onClose = { showCreateModal = false },
        onSelectMode = { mode ->
            showCreateModal = false
            onNavigate("waiting-room/$mode")
        }
    )
}

@Composable
private fun GameSessionCard(game: GameSession, onJoin: () -> Unit) {
    Surface(
        color = SurfaceColor,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, BorderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(30.dp).background(SurfaceLightColor, CircleShape), contentAlignment = Alignment.Center) {
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
                Text("${game.creatorRR} RR", fontSize = 11.sp, color = Color(0xFFfbbf24), fontWeight = FontWeight.Bold)
                Text("👥 ${game.players}/${game.maxPlayers}", fontSize = 11.sp, color = TextMutedColor)
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
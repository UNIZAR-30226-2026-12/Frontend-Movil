package com.example.random_reversi

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.random_reversi.data.FriendsRepository
import com.example.random_reversi.data.UserResult
import com.example.random_reversi.ui.screens.HomeScreen
import com.example.random_reversi.ui.screens.MainScreen
import com.example.random_reversi.ui.screens.CustomizationScreen
import com.example.random_reversi.ui.screens.FriendsScreen
import com.example.random_reversi.ui.screens.GameBoard1v1Screen
import com.example.random_reversi.ui.screens.GameBoard1v1v1v1Screen
import com.example.random_reversi.ui.screens.RulesScreen
import com.example.random_reversi.ui.screens.OnlineGameScreen
import com.example.random_reversi.ui.screens.ProfileScreen
import com.example.random_reversi.ui.screens.WaitingRoomScreen
import com.example.random_reversi.ui.screens.RankingScreen
import com.example.random_reversi.ui.theme.ReversiTheme

private fun normalizeInviteMode(mode: String?): String = when (mode?.trim()?.lowercase()) {
    "1vs1vs1vs1", "1v1v1v1" -> "1vs1vs1vs1"
    "1vs1", "1v1" -> "1vs1"
    else -> "1vs1"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ReversiTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    var currentScreen by remember { mutableStateOf("home") }
    var hasLoadedInvites by remember { mutableStateOf(false) }
    var seenInviteIds by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var globalInviteToast by remember { mutableStateOf<String?>(null) }

    // Parsear la ruta: "waiting-room/1vs1/42" -> route="waiting-room", parts=["1vs1","42"]
    val parts = currentScreen.split("/")
    val route = parts[0]

    LaunchedEffect(route) {
        if (route == "home") return@LaunchedEffect
        while (true) {
            when (val result = FriendsRepository.getSocialPanel()) {
                is UserResult.Success -> {
                    val currentInviteIds = result.data.gameRequests.map { it.lobby_id }.toSet()
                    val newInvites = result.data.gameRequests.filter { !seenInviteIds.contains(it.lobby_id) }
                    if (hasLoadedInvites && newInvites.isNotEmpty()) {
                        val firstInvite = newInvites.first()
                        val sender = firstInvite.name ?: "Un amigo"
                        val modeLabel = normalizeInviteMode(firstInvite.gameMode)
                        globalInviteToast = "$sender te ha invitado a jugar una partida $modeLabel, puedes aceptarla desde la pestana de amigos."
                    }
                    seenInviteIds = currentInviteIds
                    hasLoadedInvites = true
                }
                is UserResult.Error -> Unit
            }
            kotlinx.coroutines.delay(4000)
        }
    }

    LaunchedEffect(globalInviteToast) {
        if (globalInviteToast == null) return@LaunchedEffect
        kotlinx.coroutines.delay(2600)
        globalInviteToast = null
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (route) {
            "home" -> {
                HomeScreen(onNavigate = { currentScreen = it })
            }
            "menu" -> {
                MainScreen(onNavigate = { currentScreen = it })
            }
            "customization" -> {
                CustomizationScreen(onNavigate = { currentScreen = it })
            }
            "friends" -> {
                FriendsScreen(onNavigate = { currentScreen = it })
            }
            "rules" -> {
                RulesScreen(onNavigate = { currentScreen = it })
            }
            "ranking" -> {
                RankingScreen(onNavigate = { currentScreen = it })
            }
            "online-game" -> {
                OnlineGameScreen(onNavigate = { currentScreen = it })
            }
            "profile" -> {
                val userId = parts.getOrNull(1)?.toIntOrNull()
                val friendName = parts.getOrNull(2)?.let { Uri.decode(it) }
                ProfileScreen(
                    onNavigate = { currentScreen = it },
                    userId = userId,
                    targetUsername = friendName
                )
            }
            "waiting-room" -> {
                val mode = parts.getOrElse(1) { "1vs1" }
                val gameId = parts.getOrElse(2) { "-1" }.toIntOrNull() ?: -1
                val returnTo = parts.getOrElse(3) { "online-game" }
                val opponentName = Uri.decode(parts.getOrElse(4) { "" }).ifBlank { null }
                WaitingRoomScreen(
                    gameMode = mode,
                    gameId = gameId,
                    returnTo = returnTo,
                    opponentName = opponentName,
                    onNavigate = { currentScreen = it }
                )
            }
            "game-1vs1" -> {
                val gameId = parts.getOrElse(1) { "-1" }.toIntOrNull() ?: -1
                val returnTo = parts.getOrElse(2) { "online-game" }
                GameBoard1v1Screen(
                    gameId = gameId,
                    returnTo = returnTo,
                    onNavigate = { currentScreen = it }
                )
            }
            "game-1vs1vs1vs1" -> {
                val gameId = parts.getOrElse(1) { "-1" }.toIntOrNull() ?: -1
                val returnTo = parts.getOrElse(2) { "online-game" }
                GameBoard1v1v1v1Screen(
                    gameId = gameId,
                    returnTo = returnTo,
                    onNavigate = { currentScreen = it }
                )
            }
        }

        if (!globalInviteToast.isNullOrBlank()) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 56.dp),
                color = Color(0xFF1D4ED8),
                shape = RoundedCornerShape(999.dp)
            ) {
                Text(
                    text = globalInviteToast!!,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

package com.example.random_reversi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.random_reversi.ui.screens.HomeScreen
import com.example.random_reversi.ui.screens.MainScreen
import com.example.random_reversi.ui.screens.CustomizationScreen
import com.example.random_reversi.ui.screens.FriendsScreen
import com.example.random_reversi.ui.screens.GameBoard1v1Screen
import com.example.random_reversi.ui.screens.GameBoard1v1v1v1Screen
import com.example.random_reversi.ui.screens.RulesScreen
import com.example.random_reversi.ui.screens.OnlineGameScreen
import com.example.random_reversi.ui.screens.WaitingRoomScreen
import com.example.random_reversi.ui.screens.RankingScreen
import com.example.random_reversi.ui.theme.ReversiTheme

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

    // Parsear la ruta: "waiting-room/1vs1/42" -> route="waiting-room", parts=["1vs1","42"]
    val parts = currentScreen.split("/")
    val route = parts[0]

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
        "online-game" -> {
            OnlineGameScreen(onNavigate = { currentScreen = it })
        }
        "ranking" -> {
            RankingScreen(onNavigate = { currentScreen = it })
        }
        "waiting-room" -> {
            val mode = parts.getOrElse(1) { "1vs1" }
            val gameId = parts.getOrElse(2) { "-1" }.toIntOrNull() ?: -1
            WaitingRoomScreen(
                gameMode = mode,
                gameId = gameId,
                onNavigate = { currentScreen = it }
            )
        }
        "game-1vs1" -> {
            val gameId = parts.getOrElse(1) { "-1" }.toIntOrNull() ?: -1
            GameBoard1v1Screen(
                gameId = gameId,
                onNavigate = { currentScreen = it }
            )
        }
        "game-1vs1vs1vs1" -> {
            val gameId = parts.getOrElse(1) { "-1" }.toIntOrNull() ?: -1
            GameBoard1v1v1v1Screen(
                gameId = gameId,
                onNavigate = { currentScreen = it }
            )
        }
    }
}

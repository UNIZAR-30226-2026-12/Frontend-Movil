package com.example.random_reversi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
// Importa las pantallas
import com.example.random_reversi.ui.screens.HomeScreen
import com.example.random_reversi.ui.screens.MainScreen
import com.example.random_reversi.ui.screens.CustomizationScreen
import com.example.random_reversi.ui.screens.FriendsScreen
import com.example.random_reversi.ui.screens.GameBoard1v1Screen
import com.example.random_reversi.ui.screens.RulesScreen
import com.example.random_reversi.ui.screens.OnlineGameScreen
import com.example.random_reversi.ui.screens.WaitingRoomScreen
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
    // Estado para controlar la navegación simple
    var currentScreen by remember { mutableStateOf("home") }

    when {
        currentScreen == "home" -> {
            HomeScreen(
                onNavigate = { screen ->
                    currentScreen = screen
                }
            )
        }
        currentScreen == "menu" -> {
            MainScreen(
                onNavigate = { screen ->
                    currentScreen = screen
                }
            )
        }
        currentScreen == "customization" -> {
            CustomizationScreen(
                onNavigate = { screen ->
                    currentScreen = screen
                }
            )
        }
        currentScreen == "friends" -> {
            FriendsScreen(
                onNavigate = { screen ->
                    currentScreen = screen
                }
            )
        }
        currentScreen == "rules" -> {
            RulesScreen(
                onNavigate = { screen ->
                    currentScreen = screen
                }
            )
        }
        currentScreen == "online-game" -> {
            OnlineGameScreen(
                onNavigate = { screen ->
                    currentScreen = screen
                }
            )
        }
        currentScreen == "waiting-room" -> {
            WaitingRoomScreen(
                onNavigate = {screen ->
                    currentScreen = screen
                }
            )
        }
        currentScreen.startsWith("waiting-room") -> {
            val mode = if (currentScreen.contains("/")) currentScreen.substringAfter("/") else "1vs1"
            WaitingRoomScreen(
                gameMode = mode,
                onNavigate = { currentScreen = it }
            )
        }

        currentScreen == "game-1vs1" -> {
            GameBoard1v1Screen(
                onNavigate = { currentScreen = it }
            )
        }
    }
}
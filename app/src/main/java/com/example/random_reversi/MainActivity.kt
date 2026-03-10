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
import com.example.random_reversi.ui.screens.RulesScreen
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

    when (currentScreen) {
        "home" -> {
            HomeScreen(
                onNavigate = { screen ->
                    currentScreen = screen
                }
            )
        }
        "menu" -> {
            MainScreen(
                onNavigate = { screen ->
                    currentScreen = screen
                }
            )
        }
        "customization" -> {
            CustomizationScreen(
                onNavigate = { screen ->
                    currentScreen = screen
                }
            )
        }
        "friends" -> {
            FriendsScreen(
                onNavigate = { screen ->
                    currentScreen = screen
                }
            )
        }
        "rules" -> {
            RulesScreen(
                onNavigate = { screen ->
                    currentScreen = screen
                }
            )
        }
    }
}
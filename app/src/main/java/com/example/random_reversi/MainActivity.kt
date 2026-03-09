package com.example.random_reversi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import com.example.random_reversi.ui.screens.LandingScreen
import com.example.random_reversi.ui.screens.LoginScreen
import com.example.random_reversi.ui.screens.MainScreen
import com.example.random_reversi.ui.screens.RegisterScreen
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
    var currentScreen by remember { mutableStateOf("landing") }
    var showLoginModal by remember { mutableStateOf(false) }
    var showRegisterModal by remember { mutableStateOf(false) }

    when (currentScreen) {
        "landing" -> {
            LandingScreen(
                onLoginClick = { showLoginModal = true },
                onRegisterClick = { showRegisterModal = true }
            )
        }
        "menu" -> {
            MainScreen(
                onNavigate = { screen ->
                    currentScreen = screen
                }
            )
        }
    }

    LoginScreen(
        isOpen = showLoginModal,
        onClose = { showLoginModal = false },
        onNavigate = { screen ->
            currentScreen = screen
            showLoginModal = false
        }
    )

    RegisterScreen(
        isOpen = showRegisterModal,
        onClose = { showRegisterModal = false },
        onNavigate = { screen ->
            currentScreen = screen
            showRegisterModal = false
        },
        onRegisterSuccess = {
            // Aquí puedes manejar la lógica después del registro exitoso
        }
    )
}
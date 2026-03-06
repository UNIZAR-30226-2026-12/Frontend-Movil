package com.example.random_reversi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.random_reversi.ui.screens.LandingScreen
import com.example.random_reversi.ui.theme.ReversiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ReversiTheme {
                LandingScreen()
            }
        }
    }
}
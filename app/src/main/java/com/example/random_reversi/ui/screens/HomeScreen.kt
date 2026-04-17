package com.example.random_reversi.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.random_reversi.R
import com.example.random_reversi.ui.theme.*

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun HomeScreen(
    onNavigate: (screen: String) -> Unit
) {
    var showLoginModal by remember { mutableStateOf(false) }
    var showRegisterModal by remember { mutableStateOf(false) }
    var showForgotPasswordModal by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {

        // ── Fondo ────────────────────────────────────────────────────
        Image(
            painter = painterResource(id = R.drawable.nuevofondomovil),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // ── Contenido con scroll ──────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(56.dp))

            // ── Logo "Random Reversi" ─────────────────────────────────
            Image(
                painter = painterResource(id = R.drawable.logoreversi),
                contentDescription = "Random Reversi",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxWidth(0.72f)
                    .padding(bottom = 4.dp)
            )

            // ── Robot mascota ─────────────────────────────────────────
            Image(
                painter = painterResource(id = R.drawable.robotsentado),
                contentDescription = "Mascota Random Reversi",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxWidth(0.52f)
                    .padding(vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Botón: Iniciar Sesión (ficha púrpura) ─────────────────
            Image(
                painter = painterResource(id = R.drawable.botoniniciarsesion),
                contentDescription = "Iniciar Sesión",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxWidth(0.72f)
                    .padding(vertical = 6.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { showLoginModal = true }
                    )
            )

            // ── Botón: Crear Cuenta (ficha beige) ─────────────────────
            Image(
                painter = painterResource(id = R.drawable.botonregistrobeige),
                contentDescription = "Crear Cuenta",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxWidth(0.72f)
                    .padding(vertical = 6.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { showRegisterModal = true }
                    )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ── Enlace: ¿Olvidaste tu contraseña? ────────────────────
            Text(
                text = "¿Ha olvidado su contraseña?",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = androidx.compose.ui.graphics.Color.Black,
                textDecoration = TextDecoration.Underline,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { showForgotPasswordModal = true }
                    )
                    .padding(vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ── Footer ────────────────────────────────────────────────
            Text(
                text = "HuQ Games Studio · Universidad de Zaragoza",
                color = TextOnDark.copy(alpha = 0.65f),
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 28.dp)
            )
        }
    }

    // ── Modales de autenticación (sin cambios de lógica) ─────────────
    LoginScreen(
        isOpen = showLoginModal,
        onClose = { showLoginModal = false },
        onNavigate = { screen ->
            onNavigate(screen)
            showLoginModal = false
        }
    )

    RegisterScreen(
        isOpen = showRegisterModal,
        onClose = { showRegisterModal = false },
        onNavigate = { screen ->
            onNavigate(screen)
            showRegisterModal = false
        },
        onRegisterSuccess = {
            showRegisterModal = false
            showLoginModal = true
        }
    )

    ForgotPasswordScreen(
        isOpen = showForgotPasswordModal,
        onClose = { showForgotPasswordModal = false }
    )
}

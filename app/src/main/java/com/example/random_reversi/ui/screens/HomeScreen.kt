package com.example.random_reversi.ui.screens

import android.annotation.SuppressLint
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.random_reversi.ui.theme.*

private data class HomeChip(
    val emoji: String,
    val startXFraction: Float,
    val startYFraction: Float,
    val durationMs: Int,
    val delayMs: Int,
    val isQuestion: Boolean = false
)

private val homeChips = listOf(
    HomeChip("⚫", 0.1f, 0.1f, 3000, 0),
    HomeChip("⚪", 0.85f, 0.15f, 3200, 200),
    HomeChip("🔴", 0.2f, 0.45f, 2800, 400),
    HomeChip("🔵", 0.8f, 0.5f, 3100, 600),
    HomeChip("🟢", 0.15f, 0.75f, 2900, 800),
    HomeChip("🟡", 0.9f, 0.8f, 3300, 1000),
    HomeChip("🟣", 0.3f, 0.9f, 3000, 1200),
    HomeChip("🟠", 0.75f, 0.3f, 2700, 1400),
    HomeChip("❓", 0.5f, 0.2f, 3100, 0, true),
    HomeChip("❓", 0.6f, 0.7f, 2900, 500, true),
)

@Composable
private fun AnimatedHomeChip(
    chip: HomeChip,
    screenHeight: Dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "home_chip_${chip.emoji}")
    val yOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(
            animation = tween(chip.durationMs, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(chip.delayMs)
        ),
        label = "yOffset"
    )

    val yPosition = screenHeight * chip.startYFraction

    Text(
        text = chip.emoji,
        fontSize = if (chip.isQuestion) 28.sp else 32.sp,
        modifier = Modifier
            .offset(
                x = (280.dp) * chip.startXFraction,
                y = yPosition + yOffset.dp
            )
            .alpha(0.3f)
    )
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun HomeScreen(
    onNavigate: (screen: String) -> Unit
) {
    var showLoginModal by remember { mutableStateOf(false) }
    var showRegisterModal by remember { mutableStateOf(false) }
    var showForgotPasswordModal by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
    ) {
        val screenHeight = maxHeight

        // Fichas animadas de fondo
        repeat(homeChips.size) { index ->
            AnimatedHomeChip(
                chip = homeChips[index],
                screenHeight = screenHeight
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(80.dp))

            // Título y subtítulo
            Column(
                modifier = Modifier
                    .padding(horizontal = 32.dp)
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Random",
                    fontSize = 52.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = PrimaryColor,
                    textAlign = TextAlign.Center,
                    lineHeight = 58.sp
                )
                Text(
                    text = "Reversi",
                    fontSize = 52.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextColor,
                    textAlign = TextAlign.Center,
                    lineHeight = 58.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "El clásico Reversi reinventado con habilidades especiales, " +
                            "casillas sorpresa y partidas de hasta 4 jugadores.",
                    fontSize = 15.sp,
                    color = TextMutedColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(0.75f),
                    lineHeight = 22.sp
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Botones de acción
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.68f),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Button(
                    onClick = { showLoginModal = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)
                ) {
                    Text(
                        text = "Iniciar Sesión",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                }

                OutlinedButton(
                    onClick = { showRegisterModal = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(50),
                    border = androidx.compose.foundation.BorderStroke(2.dp, PrimaryColor),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryColor)
                ) {
                    Text(
                        text = "Crear Cuenta",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "¿Ha olvidado su contraseña?",
                fontSize = 14.sp,
                color = PrimaryColor,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clickable { showForgotPasswordModal = true }
                    .padding(vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Footer
            Text(
                text = "HuQ Games Studio · Universidad de Zaragoza",
                color = TextMutedColor,
                fontSize = 13.sp,
                modifier = Modifier.alpha(0.7f)
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Modales de autenticación
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

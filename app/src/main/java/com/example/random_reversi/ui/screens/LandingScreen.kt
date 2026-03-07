package com.example.random_reversi.ui.screens

import android.annotation.SuppressLint
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.random_reversi.ui.theme.*

private data class Particle(
    val xFraction: Float,
    val yFraction: Float,
    val size: Dp,
    val color: Color,
    val isQuestion: Boolean,
    val durationMs: Int,
    val delayMs: Int
)

private val particles = listOf(
    // Left side
    Particle(0.06f, 0.05f, 46.dp, Color(0xFF4A4A6A), false, 3000,   0),
    Particle(0.24f, 0.18f, 0.dp,  Color.Transparent, true,  2600, 200),
    Particle(0.03f, 0.38f, 52.dp, Color(0xFF2A6A4A), false, 3200, 400),
    Particle(0.18f, 0.58f, 42.dp, Color(0xFF7A4A28), false, 2800, 800),
    Particle(0.06f, 0.74f, 44.dp, Color(0xFF7A2828), false, 3100, 600),
    Particle(0.20f, 0.91f, 0.dp,  Color.Transparent, true,  2700, 300),
    // Right side
    Particle(0.80f, 0.06f, 42.dp, Color(0xFF5A5A7A), false, 2900, 100),
    Particle(0.93f, 0.20f, 34.dp, Color(0xFF7A7A9A), false, 3300, 700),
    Particle(0.74f, 0.34f, 40.dp, Color(0xFF7A7A3A), false, 2700, 500),
    Particle(0.90f, 0.50f, 0.dp,  Color.Transparent, true,  3000, 350),
    Particle(0.76f, 0.65f, 34.dp, Color(0xFF2A3A5A), false, 3200, 900),
    Particle(0.92f, 0.78f, 30.dp, Color(0xFF5A5A7A), false, 2800, 150),
    Particle(0.80f, 0.90f, 36.dp, Color(0xFF2A6A7A), false, 3000, 550),
    Particle(0.68f, 0.82f, 0.dp,  Color.Transparent, true,  2600, 750),
)

@Composable
private fun FloatingParticle(
    particle: Particle,
    screenWidth: Dp,
    screenHeight: Dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "float_${particle.xFraction}")
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -9f,
        targetValue = 9f,
        animationSpec = infiniteRepeatable(
            animation = tween(particle.durationMs, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(particle.delayMs)
        ),
        label = "y"
    )

    val x = screenWidth * particle.xFraction
    val y = screenHeight * particle.yFraction

    if (particle.isQuestion) {
        Text(
            text = "?",
            color = Color(0xFFCC3333),
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.offset(x = x, y = y + floatOffset.dp)
        )
    } else {
        Box(
            modifier = Modifier
                .offset(x = x, y = y + floatOffset.dp)
                .size(particle.size)
                .clip(CircleShape)
                .background(particle.color.copy(alpha = 0.65f))
        )
    }
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun LandingScreen(
    onLoginClick: () -> Unit = {},
    onRegisterClick: () -> Unit = {}
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
    ) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight

        // Floating background particles
        particles.forEach { particle ->
            FloatingParticle(
                particle = particle,
                screenWidth = screenWidth,
                screenHeight = screenHeight
            )
        }

        // Main content centered
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
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

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = onLoginClick,
                modifier = Modifier
                    .fillMaxWidth(0.68f)
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

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedButton(
                onClick = onRegisterClick,
                modifier = Modifier
                    .fillMaxWidth(0.68f)
                    .height(52.dp),
                shape = RoundedCornerShape(50),
                border = BorderStroke(2.dp, PrimaryColor),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryColor)
            ) {
                Text(
                    text = "Crear Cuenta",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(52.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF3A3A5A))
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "HuQ Games Studio · Universidad de Zaragoza",
                    color = TextMutedColor,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun LandingScreenPreview() {
    ReversiTheme {
        LandingScreen()
    }
}

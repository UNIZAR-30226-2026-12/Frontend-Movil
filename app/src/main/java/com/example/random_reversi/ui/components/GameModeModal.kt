package com.example.random_reversi.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.random_reversi.ui.theme.*

@Composable
fun GameModeModal(
    isOpen: Boolean,
    onClose: () -> Unit,
    onSelectMode: (String) -> Unit
) {
    AppModal(
        isOpen = isOpen,
        onClose = onClose,
        maxWidth = 400.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Jugar contra la IA",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextColor,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "Elige el modo de juego",
                fontSize = 14.sp,
                color = TextMutedColor,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Modo fácil
            GameModeOption(
                title = "Fácil",
                description = "IA básica con movimientos aleatorios",
                emoji = "😊",
                onClick = {
                    onSelectMode("easy")
                }
            )

            // Modo normal
            GameModeOption(
                title = "Normal",
                description = "IA con estrategia moderada",
                emoji = "🤖",
                onClick = {
                    onSelectMode("normal")
                }
            )

            // Modo difícil
            GameModeOption(
                title = "Difícil",
                description = "IA experta con estrategia avanzada",
                emoji = "🧠",
                onClick = {
                    onSelectMode("hard")
                }
            )
        }
    }
}

@Composable
private fun GameModeOption(
    title: String,
    description: String,
    emoji: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = SurfaceLightColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = emoji,
                fontSize = 28.sp,
                modifier = Modifier.width(40.dp),
                textAlign = TextAlign.Center
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextColor
                )
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = TextMutedColor
                )
            }
        }
    }
}

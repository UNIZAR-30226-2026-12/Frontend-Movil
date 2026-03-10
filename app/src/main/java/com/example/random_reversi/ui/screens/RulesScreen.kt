package com.example.random_reversi.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.random_reversi.ui.theme.*

private data class RuleChip(
    val emoji: String,
    val startXFraction: Float,
    val startYFraction: Float,
    val durationMs: Int,
    val delayMs: Int,
    val isQuestion: Boolean = false
)

private val ruleChips = listOf(
    RuleChip("⚫", 0.1f, 0.1f, 3000, 0),
    RuleChip("⚪", 0.85f, 0.15f, 3200, 200),
    RuleChip("🔴", 0.2f, 0.45f, 2800, 400),
    RuleChip("🔵", 0.8f, 0.5f, 3100, 600),
    RuleChip("❓", 0.5f, 0.2f, 3100, 0, true),
    RuleChip("❓", 0.6f, 0.7f, 2900, 500, true),
)

@Composable
fun RulesScreen(onNavigate: (String) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
    ) {
        // Fondo Animado
        BoxWithConstraints {
            ruleChips.forEach { chip ->
                AnimatedRuleChip(chip, maxHeight)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            // Header
            Text(
                text = "Reglas de Random Reversi",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextColor,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Panel de reglas (Glassmorphism)
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                color = Color(0xFF0F1231).copy(alpha = 0.55f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, BorderColor)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    RuleSectionTitle("📜 Cómo jugar")
                    RuleTextBold("🎯 Objetivo:", " terminar con más puntos que el resto de jugadores.")
                    RuleTextBold("🧩 Diferencia clave frente a Reversi normal:", " hay casillas especiales (❓) y, al colocar fichas sobre ellas, consigues habilidades especiales que puedes usar durante la partida.")
                    RuleTextBold("🕹️ Modos:", " 1v1 (tablero 8x8, 2 jugadores) y 1v1v1v1 (tablero 16x16, 4 jugadores).")

                    Spacer(modifier = Modifier.height(12.dp))
                    RuleSectionTitle("▶️ Cómo se juega (paso a paso):")

                    BulletPointBold("🏁 Inicio:", " el tablero inicia con las fichas iniciales de los jugadores y casillas especiales (❓) repartidas aleatoriamente.")
                    BulletPointBold("🔁 Tu turno:", " eliges una única acción: hacer un movimiento normal o usar una habilidad.")
                    BulletPointBold("♟️ Movimiento normal:", " pones una ficha en una casilla vacía que encierra al menos una ficha rival entre tu ficha nueva y otra tuya, en línea recta (horizontal, vertical o diagonal).")
                    BulletPointBold("🔄 Las fichas ", "rivales encerradas", " se voltean a ", "tu color", ".")
                    BulletPointBold("❓ Si colocas una ficha sobre una ", "casilla especial (❓)", ", ganas una ", "habilidad aleatoria", " y la casilla deja de ser especial.")
                    BulletPointBold("⛔ Si no puedes mover ni usar habilidad, ", "pierdes tu turno", ".")
                    BulletPointBold("🏁 La partida acaba si nadie puede jugar en una ", "vuelta completa", " o si no quedan ", "casillas para jugar", ".")
                    BulletPointBold("🧮 Puntuación final:", " fichas propias en tablero - (2 × habilidades no utilizadas).")
                    BulletPointBold("🏆 Gana quien tenga ", "mayor puntuación final", "; si hay empate, se considera un empate.")

                    Spacer(modifier = Modifier.height(12.dp))
                    RuleSectionTitle("🧠 Reglas del uso de habilidades:")
                    BulletPointBold("Usar una ", "habilidad consume tu turno", ".")
                    BulletPoint("Si una habilidad necesita objetivo y no existe, no se puede usar.")
                    BulletPoint("En 1v1v1v1 puedes elegir a qué rival aplicarla cuando corresponda.")
                    BulletPointBold("Las habilidades que no uses al final ", "te penalizan puntos", ", por lo que conviene utilizarlas independientemente de que sean buenas o malas.")

                    Spacer(modifier = Modifier.height(12.dp))
                    RuleSectionTitle("💥 Habilidades (abre cada una para ver descripción):")

                    SkillExpandable("🧲 Gravedad (⬆️⬇️⬅️➡️)", "Desplaza todas las fichas del tablero hacia la dirección elegida, excepto las fichas fijas.")
                    SkillExpandable("💣 Bomba 3x3", "En el área 3x3 seleccionada, todas las fichas se voltean sin importar su dueño.")
                    SkillExpandable("🔒 Poner ficha fija", "Convierte una ficha propia colocada en el tablero en fija (no puede moverse ni voltearse).")
                    SkillExpandable("🔓 Quitar ficha fija", "Libera una ficha fija para que vuelva a ser una ficha normal. Si no hay fichas fijas en el tablero, no se puede usar.")
                    SkillExpandable("✨ Poner ficha libre", "Permite colocar una ficha propia en cualquier casilla vacía sin necesidad de capturar fichas rivales.")
                    SkillExpandable("⏭️ Saltar turno del rival", "El siguiente rival pierde su turno.")
                    SkillExpandable("🚫 Pierdes tu turno", "Al utilizarla, pierdes tu turno actual.")
                    SkillExpandable("🔄 Voltear una ficha del rival", "Convierte una ficha rival elegida a tu color.")
                    SkillExpandable("🔁 Cambiar colores con otro jugador", "Intercambia todas tus fichas del tablero con las del rival seleccionado.")
                    SkillExpandable("🥷 Robar habilidad", "Robas una habilidad aleatoria de un rival. Si nadie tiene habilidades, no se puede usar.")
                    SkillExpandable("🔃 Intercambiar habilidad", "Das una habilidad tuya y recibes una del rival seleccionado. Si no hay habilidades en el jugador origen y/o destino, no se puede usar.")
                    SkillExpandable("🎁 Dar habilidad", "Entregas una habilidad que tengas en tu posesión a otro jugador. Si no tienes ninguna, no se puede usar.")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Botón Volver con estilo estandarizado (PrimaryColor)
            Button(
                onClick = { onNavigate("menu") },
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Volver al menú",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SkillExpandable(title: String, description: String) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { expanded = !expanded },
        color = Color(0xFF1F244F).copy(alpha = 0.55f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, PrimaryColor.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color.White
                )
            }
            AnimatedVisibility(visible = expanded) {
                Text(
                    text = description,
                    color = Color(0xFFD3D7FF),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 8.dp),
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun RuleSectionTitle(text: String) {
    Text(
        text = text,
        color = Color.White,
        fontSize = 17.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 8.dp, top = 8.dp)
    )
}

@Composable
private fun RuleText(text: String) {
    Text(
        text = text,
        color = Color(0xFFD3D7FF),
        fontSize = 14.sp,
        lineHeight = 20.sp,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

// Nueva función para textos con negrita parcial
@Composable
private fun RuleTextBold(boldPart: String, normalPart: String) {
    Text(
        text = buildAnnotatedString {
            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                append(boldPart)
            }
            append(normalPart)
        },
        color = Color(0xFFD3D7FF),
        fontSize = 14.sp,
        lineHeight = 20.sp,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun BulletPoint(text: String) {
    Row(modifier = Modifier.padding(bottom = 8.dp)) {
        Text("• ", color = PrimaryColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text(text = text, color = Color(0xFFD3D7FF), fontSize = 14.sp, lineHeight = 20.sp)
    }
}

// Nueva función para BulletPoints con negritas parciales (acepta múltiples partes)
@Composable
private fun BulletPointBold(vararg parts: String) {
    Row(modifier = Modifier.padding(bottom = 8.dp)) {
        Text("• ", color = PrimaryColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text(
            text = buildAnnotatedString {
                parts.forEachIndexed { index, s ->
                    // En la web sueles poner en negrita la primera parte o partes específicas
                    // Aquí asumimos que si hay más de 2 partes, las pares (0, 2...) son negrita o según el contexto
                    // Para ser exactos con tu web, aplicamos negrita a las partes que coincidan con tus etiquetas <strong>
                    if (s.contains(" Objetivo:") || s.contains("Diferencia clave") || s.contains("Modos:") ||
                        s.contains("Inicio:") || s.contains("Tu turno:") || s.contains("Movimiento normal:") ||
                        s.contains("rivales encerradas") || s.contains("tu color") || s.contains("casilla especial") ||
                        s.contains("habilidad aleatoria") || s.contains("pierdes tu turno") || s.contains("vuelta completa") ||
                        s.contains("casillas para jugar") || s.contains("Puntuación final:") || s.contains("mayor puntuación final") ||
                        s.contains("habilidad consume tu turno") || s.contains("te penalizan puntos")) {
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(s)
                        }
                    } else {
                        append(s)
                    }
                }
            },
            color = Color(0xFFD3D7FF),
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
    }
}

@Composable
private fun AnimatedRuleChip(chip: RuleChip, screenHeight: Dp) {
    val infiniteTransition = rememberInfiniteTransition(label = "")
    val yOffset by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 20f,
        animationSpec = infiniteRepeatable(
            animation = tween(chip.durationMs, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(chip.delayMs)
        ), label = ""
    )
    Text(
        text = chip.emoji,
        fontSize = if (chip.isQuestion) 28.sp else 32.sp,
        modifier = Modifier
            .offset(x = (280.dp) * chip.startXFraction, y = (screenHeight * chip.startYFraction) + yOffset.dp)
            .alpha(0.2f)
    )
}
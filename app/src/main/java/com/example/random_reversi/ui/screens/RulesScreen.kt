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
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.border
import androidx.compose.ui.zIndex
import com.example.random_reversi.R
import com.example.random_reversi.ui.theme.*

@Composable
fun RulesScreen(onNavigate: (String) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Image(
            painter = painterResource(id = R.drawable.nuevofondomovil),
            contentDescription = "Fondo",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Header
            Image(
                painter = painterResource(id = R.drawable.tituloreglas),
                contentDescription = "Reglas de Random Reversi",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(115.dp)
                    .zIndex(1f) // Para dibujar encima de la libreta
                    .offset(y = 25.dp) // Baja el gráfico de manera que solapa
            )

            // Se elimina el Spacer intermedio para maximizar el factor de solapamiento

            // Panel de reglas (Libreta)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Image(
                    painter = painterResource(id = R.drawable.libretareglas2),
                    contentDescription = "Libreta",
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.fillMaxSize()
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 32.dp, end = 32.dp, top = 40.dp, bottom = 20.dp)
                        .verticalScroll(rememberScrollState())
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

            // Spacer(modifier = Modifier.height(24.dp)) -- removido para el solapamiento

            // Botón Volver con la gráfica retro
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-10).dp) // Lo bajamos un poco (antes era -25)
                    .zIndex(1f),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.botonvolvermenu),
                    contentDescription = "Volver al menú",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(100.dp)
                        .clickable { onNavigate("menu") }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SkillExpandable(title: String, description: String) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { expanded = !expanded }
            .background(Color.Black.copy(0.05f), RoundedCornerShape(8.dp))
            .border(1.dp, Color.Black.copy(0.2f), RoundedCornerShape(8.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(title, color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color.Black
                )
            }
            AnimatedVisibility(visible = expanded) {
                Text(
                    text = description,
                    color = Color.DarkGray,
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
        color = Color.Black,
        fontSize = 17.sp,
        fontWeight = FontWeight.Black,
        modifier = Modifier.padding(bottom = 8.dp, top = 8.dp)
    )
}

@Composable
private fun RuleText(text: String) {
    Text(
        text = text,
        color = Color.DarkGray,
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
            withStyle(style = SpanStyle(fontWeight = FontWeight.Black)) {
                append(boldPart)
            }
            append(normalPart)
        },
        color = Color.DarkGray,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun BulletPoint(text: String) {
    Row(modifier = Modifier.padding(bottom = 8.dp)) {
        Text("• ", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 14.sp)
        Text(text = text, color = Color.DarkGray, fontSize = 14.sp, lineHeight = 20.sp)
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
            color = Color.DarkGray,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
    }
}

// Removed AnimatedRuleChip
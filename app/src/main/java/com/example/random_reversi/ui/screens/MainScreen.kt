package com.example.random_reversi.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.random_reversi.R
import com.example.random_reversi.data.UserProfileStore
import com.example.random_reversi.ui.components.GameModeModal
import com.example.random_reversi.ui.theme.*
import com.example.random_reversi.utils.AvatarPresets

@Composable
fun MainScreen(onNavigate: (screen: String) -> Unit) {
    var showGameModeModal by remember { mutableStateOf(false) }

    val profile by UserProfileStore.state.collectAsState()

    val userName = profile.username.ifBlank { "Jugador" }
    val userElo = profile.rr
    val userAvatar = profile.avatarUrl

    LaunchedEffect(Unit) { UserProfileStore.refreshFromBackend() }

    Box(modifier = Modifier.fillMaxSize()) {

        // ── 1. Fondo de la App ────────────────────────────────────────
        Image(
            painter = painterResource(id = R.drawable.nuevofondomovil),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // ── 2. Contenido Principal ────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // FILA SUPERIOR (Reglas con un poco más de espacio arriba)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 20.dp), // Aumentado para ese "pequeñísimo espacio"
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                MenuImageButton(
                    drawableRes = R.drawable.botonreglas,
                    contentDescription = "Reglas",
                    modifier = Modifier.width(85.dp),
                    onClick = { onNavigate("rules") }
                )

                UserProfileAndLogout(
                    userName = userName,
                    elo = userElo,
                    avatarUrl = userAvatar,
                    onProfile = { onNavigate("profile") },
                    onLogout  = { onNavigate("home") }
                )
            }

            Spacer(modifier = Modifier.weight(0.7f))

            // LOGO
            Image(
                painter = painterResource(id = R.drawable.logoreversi),
                contentDescription = "Random Reversi",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth(0.78f)
                    .heightIn(max = 100.dp)
            )

            // SUBTÍTULO
            Text(
                text = "ELIGE TU JUGADA",
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                textAlign = TextAlign.Center,
                style = TextStyle(
                    shadow = Shadow(
                        color = Color.Black,
                        offset = Offset(3f, 4f),
                        blurRadius = 6f
                    )
                ),
                modifier = Modifier.padding(top = 10.dp, bottom = 6.dp)
            )

            Spacer(modifier = Modifier.weight(0.4f))

            // ── MODOS DE JUEGO (Online e IA +10% y IA movido a la izq) ──
            Row(
                modifier = Modifier
                    .fillMaxWidth(1f)
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MenuImageButton(
                    drawableRes = R.drawable.botonjugaronline,
                    contentDescription = "Jugar Online",
                    modifier = Modifier
                        .weight(1.25f) // Aumentado tamaño relativo
                        .padding(end = 0.dp),
                    onClick = { onNavigate("online-game") }
                )

                MenuImageButton(
                    drawableRes = R.drawable.botonjugaria,
                    contentDescription = "Jugar contra la IA",
                    modifier = Modifier
                        .weight(1.0f) // Aumentado tamaño relativo
                        .padding(start = 0.dp, end = 16.dp), // Empujado a la izquierda mediante padding derecho
                    onClick = { showGameModeModal = true }
                )
            }

            Spacer(modifier = Modifier.weight(0.25f))

            // ── PERSONALIZACIÓN Y AMIGOS (Personalización -10%) ──
            Row(
                modifier = Modifier.fillMaxWidth(0.82f), // Fila más estrecha para reducir botones
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MenuImageButton(
                    drawableRes = R.drawable.botonpersonalizacion,
                    contentDescription = "Personalización",
                    modifier = Modifier
                        .weight(0.85f) // Peso reducido para hacerlo más pequeño
                        .padding(end = 6.dp),
                    onClick = { onNavigate("customization") }
                )

                MenuImageButton(
                    drawableRes = R.drawable.botonamigos,
                    contentDescription = "Amigos",
                    modifier = Modifier
                        .weight(0.85f)
                        .padding(start = 6.dp)
                        .heightIn(max = 90.dp),
                    onClick = { onNavigate("friends") }
                )
            }

            Spacer(modifier = Modifier.weight(0.5f))

            // RANKING GLOBAL
            MenuImageButton(
                drawableRes = R.drawable.botonranking,
                contentDescription = "Ranking Global",
                modifier = Modifier.fillMaxWidth(0.48f),
                onClick = { onNavigate("ranking") }
            )

            Spacer(modifier = Modifier.weight(1f))

            // FOOTER
            Text(
                text = "HuQ Games Studio · Universidad de Zaragoza",
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }
    }

    GameModeModal(
        isOpen  = showGameModeModal,
        onClose = { showGameModeModal = false },
        onSelectMode = { showGameModeModal = false }
    )
}

@Composable
private fun MenuImageButton(
    drawableRes: Int,
    contentDescription: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Image(
        painter = painterResource(id = drawableRes),
        contentDescription = contentDescription,
        contentScale = ContentScale.Fit,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    )
}

@Composable
private fun UserProfileAndLogout(
    userName: String,
    elo: Int,
    avatarUrl: String?,
    onProfile: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Perfil
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onProfile)
                .padding(4.dp)
        ) {
            Text(
                text = userName,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                style = TextStyle(shadow = Shadow(color = Color.Black, blurRadius = 4f))
            )

            Spacer(modifier = Modifier.height(2.dp))

            val presetRes = AvatarPresets.drawableForId(avatarUrl)
            Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp))) {
                if (presetRes != null) {
                    Image(painter = painterResource(id = presetRes), contentDescription = null, contentScale = ContentScale.Crop)
                } else if (!avatarUrl.isNullOrBlank()) {
                    AsyncImage(model = avatarUrl, contentDescription = null, contentScale = ContentScale.Crop)
                } else {
                    Box(Modifier.fillMaxSize().background(PrimaryColor), contentAlignment = Alignment.Center) {
                        Text(userName.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "$elo RR",
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 12.sp,
                style = TextStyle(shadow = Shadow(color = Color.Black, blurRadius = 4f))
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Cerrar Sesión
        Image(
            painter = painterResource(id = R.drawable.cerrarsesion),
            contentDescription = "Cerrar sesión",
            modifier = Modifier
                .width(85.dp)
                .clip(RoundedCornerShape(10.dp))
                .clickable(onClick = onLogout)
        )
    }
}
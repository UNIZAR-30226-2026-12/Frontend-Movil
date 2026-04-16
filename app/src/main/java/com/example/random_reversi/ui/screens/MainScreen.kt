package com.example.random_reversi.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
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

    LaunchedEffect(Unit) { UserProfileStore.refreshFromBackend() }

    Box(modifier = Modifier.fillMaxSize()) {

        // ── Fondo mosaico ──────────────────────────────────────────
        Image(
            painter = painterResource(id = R.drawable.nuevofondomovil),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // ── Contenido con scroll ────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ── TOP BAR ─────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 44.dp, start = 16.dp, end = 16.dp)
            ) {
                // Logo / título (izquierda)
                Image(
                    painter = painterResource(id = R.drawable.logoreversi),
                    contentDescription = "Random Reversi",
                    modifier = Modifier
                        .height(56.dp)
                        .align(Alignment.CenterStart)
                )
                // Avatar + cerrar sesión (derecha)
                UserPolaroid(
                    userName = userName,
                    avatarUrl = profile.avatarUrl,
                    onProfile = { onNavigate("profile") },
                    onLogout  = { onNavigate("home") },
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
            }

            // ── Subtítulo ────────────────────────────────────────────
            Text(
                text = "Elige tu jugada",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextOnDark,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 18.dp, bottom = 8.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ── Botón: Jugar Online ──────────────────────────────────
            MenuImageButton(
                drawableRes = R.drawable.botonjugaronline,
                contentDescription = "Jugar Online",
                modifier = Modifier
                    .fillMaxWidth(0.78f)
                    .padding(vertical = 6.dp),
                onClick = { onNavigate("online-game") }
            )

            // ── Botón: Jugar contra la IA ────────────────────────────
            MenuImageButton(
                drawableRes = R.drawable.botonjugaria,
                contentDescription = "Jugar contra la IA",
                modifier = Modifier
                    .fillMaxWidth(0.68f)
                    .padding(vertical = 6.dp),
                onClick = { showGameModeModal = true }
            )

            // ── Fila: Personalización | Amigos ───────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MenuImageButton(
                    drawableRes = R.drawable.botonpersonalizacion,
                    contentDescription = "Personalización",
                    modifier = Modifier.weight(1f).padding(end = 6.dp),
                    onClick = { onNavigate("customization") }
                )
                MenuImageButton(
                    drawableRes = R.drawable.botonamigos,
                    contentDescription = "Amigos",
                    modifier = Modifier.weight(1f).padding(start = 6.dp),
                    onClick = { onNavigate("friends") }
                )
            }

            // ── Fila: Ranking | Reglas ───────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MenuImageButton(
                    drawableRes = R.drawable.botonranking,
                    contentDescription = "Ranking",
                    modifier = Modifier.weight(1f).padding(end = 6.dp),
                    onClick = { onNavigate("ranking") }
                )
                MenuImageButton(
                    drawableRes = R.drawable.botonreglas,
                    contentDescription = "Reglas del juego",
                    modifier = Modifier.weight(1f).padding(start = 6.dp),
                    onClick = { onNavigate("rules") }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Footer ───────────────────────────────────────────────
            Text(
                text = "HuQ Games Studio · Universidad de Zaragoza",
                fontSize = 11.sp,
                color = TextOnDark.copy(alpha = 0.65f),
                modifier = Modifier.padding(bottom = 28.dp)
            )
        }
    }

    GameModeModal(
        isOpen  = showGameModeModal,
        onClose = { showGameModeModal = false },
        onSelectMode = { showGameModeModal = false }
    )
}

// ── Componente: botón que muestra un PNG a pantalla completa ─────────
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
        contentScale = ContentScale.FillWidth,
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    )
}

// ── Componente: avatar estilo polaroid + botón cerrar sesión ─────────
@Composable
private fun UserPolaroid(
    userName: String,
    avatarUrl: String?,
    onProfile: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End
    ) {
        // Tarjeta polaroid clickable → perfil
        Box(
            modifier = Modifier
                .background(Color.White, RoundedCornerShape(6.dp))
                .padding(3.dp)
                .clickable(onClick = onProfile)
        ) {
            val presetRes = AvatarPresets.drawableForId(avatarUrl)
            when {
                presetRes != null ->
                    Image(
                        painter = painterResource(id = presetRes),
                        contentDescription = userName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                !avatarUrl.isNullOrBlank() ->
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = userName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                else ->
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(PrimaryColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = userName.firstOrNull()?.uppercaseChar()?.toString() ?: "J",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
            }
        }

        // Botón cerrar sesión usando PNG
        Image(
            painter = painterResource(id = R.drawable.cerrarsesion),
            contentDescription = "Cerrar sesión",
            contentScale = ContentScale.FillWidth,
            modifier = Modifier
                .width(90.dp)
                .padding(top = 4.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onLogout
                )
        )
    }
}
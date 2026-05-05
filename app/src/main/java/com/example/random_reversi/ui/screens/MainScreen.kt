package com.example.random_reversi.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import com.example.random_reversi.data.GamesRepository
import com.example.random_reversi.data.UserResult
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
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
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.example.random_reversi.R
import com.example.random_reversi.data.SessionManager
import com.example.random_reversi.data.UserProfileStore
import com.example.random_reversi.ui.components.ConfirmModal
import com.example.random_reversi.ui.components.GameModeModal
import com.example.random_reversi.ui.theme.*
import com.example.random_reversi.utils.AvatarPresets

@Composable
fun MainScreen(onNavigate: (screen: String) -> Unit) {
    var showGameModeModal by remember { mutableStateOf(false) }
    var showLogoutConfirm by remember { mutableStateOf(false) }
    var isCreatingAIGame by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

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

            // FILA SUPERIOR (Reglas a la izq, Perfil a la der)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                MenuImageButton(
                    drawableRes = R.drawable.botonreglas,
                    contentDescription = "Reglas del juego",
                    modifier = Modifier.width(105.dp),
                    onClick = { onNavigate("rules") }
                )

                UserProfileAndLogout(
                    userName = userName,
                    elo = userElo,
                    avatarUrl = userAvatar,
                    onProfile = { onNavigate("profile") },
                    onLogout  = { showLogoutConfirm = true }
                )
            }

            Spacer(modifier = Modifier.weight(0.2f))

            // LOGO TITULO
            Image(
                painter = painterResource(id = R.drawable.logoreversi),
                contentDescription = "Random Reversi",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .heightIn(max = 130.dp)
                    .padding(bottom = 0.dp)
            )

            // SUBTÍTULO
            Text(
                text = "Elige tu jugada",
                fontSize = 24.sp,
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
                modifier = Modifier.padding(bottom = 5.dp).zIndex(10f)
            )

            // ── PILA DE BOTONES DE ACCIÓN SUPERPUESTOS ──

            // 1. Jugar Online (Arriba del todo, prioridad Z)
            MenuImageButton(
                drawableRes = R.drawable.botonjugaronline,
                contentDescription = "Jugar Online",
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .zIndex(5f),
                onClick = { onNavigate("online-game") }
            )

            // 2. Jugar contra la IA (Debajo, sube ligeramente)
            MenuImageButton(
                drawableRes = R.drawable.botonjugaria,
                contentDescription = "Jugar contra la IA",
                modifier = Modifier
                    .fillMaxWidth(0.55f)
                    .offset(y = (-35).dp)
                    .zIndex(4f),
                onClick = { showGameModeModal = true }
            )

            // 3. Fila Mixta: Personalización y Amigos juntas
            Row(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .offset(y = (-60).dp)
                    .zIndex(3f),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MenuImageButton(
                    drawableRes = R.drawable.botonpersonalizacion2,
                    contentDescription = "Personalización",
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp, end = 6.dp, top = 8.dp, bottom = 8.dp)
                        .offset(y = 15.dp) // desequilibrio visual intencional
                        .rotate(-3f),
                    onClick = { onNavigate("customization") }
                )

                MenuImageButton(
                    drawableRes = R.drawable.botonamigos,
                    contentDescription = "Amigos",
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp)
                        .rotate(4f),
                    onClick = { onNavigate("friends") }
                )
            }

            // 4. Ranking (Debajo de personalización y amigos)
            MenuImageButton(
                drawableRes = R.drawable.botonranking,
                contentDescription = "Ranking",
                modifier = Modifier
                    .fillMaxWidth(0.45f)
                    .offset(x = 15.dp, y = (-65).dp)
                    .zIndex(2f),
                onClick = { onNavigate("ranking") }
            )

            Spacer(modifier = Modifier.weight(1f))
        }
    }

    GameModeModal(
        isOpen  = showGameModeModal,
        onClose = { showGameModeModal = false },
        onSelectMode = { mode ->
            if (isCreatingAIGame) return@GameModeModal
            showGameModeModal = false
            val hasSkills = mode.endsWith("_skills")
            val variant = if (hasSkills) "skills" else "classic"
            if (mode.startsWith("1vs1vs1vs1")) {
                isCreatingAIGame = true
                scope.launch {
                    val result = GamesRepository.createLobby(mode)
                    if (result is UserResult.Success) {
                        val gameId = result.data.game_id
                        GamesRepository.addBot(gameId)
                        GamesRepository.addBot(gameId)
                        GamesRepository.addBot(gameId)
                        isCreatingAIGame = false
                        onNavigate("game-1vs1vs1vs1/$gameId/menu/$variant")
                    } else {
                        isCreatingAIGame = false
                        onNavigate("game-1vs1vs1vs1/-1/menu/$variant")
                    }
                }
            } else {
                val backendMode = if (hasSkills) "vs_ai_skills" else "vs_ai"
                isCreatingAIGame = true
                scope.launch {
                    val result = GamesRepository.createLobby(backendMode)
                    isCreatingAIGame = false
                    when (result) {
                        is UserResult.Success -> {
                            val gameId = result.data.game_id
                            onNavigate("game-1vs1/$gameId/menu/$variant")
                        }
                        is UserResult.Error -> {
                            onNavigate("game-1vs1/-1/menu/$variant")
                        }
                    }
                }
            }
        }
    )

    ConfirmModal(
        isOpen = showLogoutConfirm,
        onClose = { showLogoutConfirm = false },
        onConfirm = {
            showLogoutConfirm = false
            SessionManager.clear()
            UserProfileStore.clear()
            onNavigate("home")
        },
        title = "Cerrar sesión",
        message = "¿Seguro que quieres cerrar sesión?",
        confirmLabel = "Cerrar sesión"
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
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.End
    ) {
        // Marco de Foto
        Box(
            modifier = Modifier
                .width(85.dp)
                .height(85.dp)
                .rotate(-6f)
                .clickable(onClick = onProfile),
            contentAlignment = Alignment.Center
        ) {
            // Fondo / Foto (Avatar interno)
            Box(
                modifier = Modifier
                    .offset(y = (-8).dp)
                    .size(53.dp)
                    .rotate(-9f) // Rotation changed by user
                    .background(Color(0xFF264653)),
                contentAlignment = Alignment.Center
            ) {
                val presetRes = AvatarPresets.drawableForId(avatarUrl)
                if (presetRes != null) {
                    Image(
                        painter = painterResource(id = presetRes),
                        contentDescription = "Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (!avatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = "Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = userName.firstOrNull()?.toString()?.lowercase() ?: "?",
                        color = Color(0xFFE9C46A),
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            // Capa Superior (Marco)
            Image(
                painter = painterResource(id = R.drawable.marcofotoperfil),
                contentDescription = "Marco de perfil",
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize()
            )

            // Texto de ELO superpuesto sobre la parte beige
            Text(
                text = "$elo RR",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier
                    .offset(x = 7.dp, y = 28.dp)
                    .rotate(-7f)
            )
        }

        // Botón Cerrar Sesión (Etiqueta recortada rota a la derecha)
        Image(
            painter = painterResource(id = R.drawable.cerrarsesion),
            contentDescription = "Cerrar sesión",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .padding(start = 2.dp, top = 8.dp)
                .height(42.dp)
                .rotate(4f)
                .clickable(onClick = onLogout)
        )
    }
}

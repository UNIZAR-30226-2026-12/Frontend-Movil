package com.example.random_reversi.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.random_reversi.R
import com.example.random_reversi.data.GamesRepository
import com.example.random_reversi.data.UserProfileStore
import com.example.random_reversi.data.UserRepository
import com.example.random_reversi.data.UserResult
import com.example.random_reversi.data.remote.HeadToHeadResponse
import com.example.random_reversi.data.remote.ModeStatsResponse
import com.example.random_reversi.data.remote.UserStatsResponse
import com.example.random_reversi.ui.theme.*
import com.example.random_reversi.utils.AvatarPresets
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private enum class Tab(val label: String) {
    Summary("Tus estadisticas"),
    Settings("Ajustes")
}

private enum class StatsMode(val label: String) {
    OneVsOne("Estadisticas 1vs1"),
    FourPlayers("Estadisticas 1vs1vs1vs1")
}

@Composable
fun ProfileScreen(
    onNavigate: (String) -> Unit,
    userId: Int? = null,
    targetUsername: String? = null,
    returnTo: String = "menu"
) {
    val scope = rememberCoroutineScope()
    val profile by UserProfileStore.state.collectAsState()
    val isOwnProfile = userId == null

    var stats by remember { mutableStateOf<UserStatsResponse?>(null) }
    var h2h by remember { mutableStateOf<HeadToHeadResponse?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    var activeTab by remember { mutableStateOf(Tab.Summary) }
    var activeMode by remember { mutableStateOf(StatsMode.OneVsOne) }

    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var originalUsername by remember { mutableStateOf("") }
    var originalEmail by remember { mutableStateOf("") }
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var settingsError by remember { mutableStateOf<String?>(null) }
    var settingsSuccess by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }

    fun loadProfile() {
        scope.launch {
            loading = true
            error = null
            if (isOwnProfile) {
                when (val statsResult = GamesRepository.getMyStats()) {
                    is UserResult.Success -> stats = statsResult.data
                    is UserResult.Error -> error = statsResult.message
                }
                when (val meResult = UserRepository.getMe()) {
                    is UserResult.Success -> {
                        username = meResult.data.username
                        email = meResult.data.email
                        originalUsername = meResult.data.username
                        originalEmail = meResult.data.email
                    }
                    is UserResult.Error -> if (error == null) error = meResult.message
                }
            } else {
                val targetUserId = userId ?: -1
                when (val statsResult = GamesRepository.getUserStats(targetUserId)) {
                    is UserResult.Success -> stats = statsResult.data
                    is UserResult.Error -> error = statsResult.message
                }
                when (val h2hResult = GamesRepository.getHeadToHead(targetUserId)) {
                    is UserResult.Success -> h2h = h2hResult.data
                    is UserResult.Error -> h2h = null
                }
            }
            loading = false
        }
    }

    fun saveSettings() {
        scope.launch {
            settingsError = null
            settingsSuccess = null

            if (newPassword.isNotBlank() && newPassword != confirmPassword) {
                settingsError = "Las contrasenas nuevas no coinciden"
                return@launch
            }
            if (newPassword.isNotBlank() && currentPassword.isBlank()) {
                settingsError = "Debes indicar tu contrasena actual para cambiarla"
                return@launch
            }

            val usernameChanged = username.trim() != originalUsername.trim()
            val emailChanged = email.trim() != originalEmail.trim()
            val passwordChanged = newPassword.isNotBlank()
            if (!usernameChanged && !emailChanged && !passwordChanged) {
                settingsSuccess = "No hay cambios que guardar"
                return@launch
            }

            saving = true
            when (
                val result = UserRepository.updateMe(
                    username = if (usernameChanged) username.trim() else null,
                    email = if (emailChanged) email.trim() else null,
                    currentPassword = if (passwordChanged) currentPassword else null,
                    newPassword = if (passwordChanged) newPassword else null
                )
            ) {
                is UserResult.Success -> {
                    val updated = result.data
                    settingsSuccess = "Ajustes guardados correctamente"
                    username = updated.username
                    email = updated.email
                    originalUsername = updated.username
                    originalEmail = updated.email
                    currentPassword = ""
                    newPassword = ""
                    confirmPassword = ""
                    stats = stats?.copy(username = updated.username, elo = updated.elo, avatar_url = updated.avatar_url)
                    UserProfileStore.refreshFromBackend()
                }
                is UserResult.Error -> settingsError = result.message
            }
            saving = false
        }
    }

    LaunchedEffect(Unit) {
        UserProfileStore.refreshFromBackend()
        loadProfile()
    }

    val modeStats = when (activeMode) {
        StatsMode.OneVsOne -> stats?.stats_1v1 ?: ModeStatsResponse(
            total_games = stats?.total_games ?: 0,
            wins = stats?.wins ?: 0,
            losses = stats?.losses ?: 0,
            draws = stats?.draws ?: 0,
            winrate = stats?.winrate ?: 0.0,
            win_streak = stats?.win_streak ?: 0,
            peak_elo = stats?.peak_elo,
            nemesis_name = stats?.nemesis_name,
            nemesis_losses = stats?.nemesis_losses,
            victim_name = stats?.victim_name,
            victim_wins = stats?.victim_wins
        )
        StatsMode.FourPlayers -> stats?.stats_4p ?: ModeStatsResponse()
    }

    val name = stats?.username ?: targetUsername ?: profile.username
    val elo = stats?.elo ?: 1000
    val avatar = stats?.avatar_url ?: profile.avatarUrl

    // ── Layout principal con fondo decorativo ────────────────────────
    Box(modifier = Modifier.fillMaxSize()) {

        // ── Fondo ────────────────────────────────────────────────────
        Image(
            painter = painterResource(id = R.drawable.nuevofondomovil),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // ── Contenido con scroll ─────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))
            Header(name, elo, avatar, isOwnProfile)
            Spacer(modifier = Modifier.height(12.dp))

            if (isOwnProfile) {
                SegmentedTabs(activeTab, { activeTab = it }, Tab.values().toList())
                Spacer(modifier = Modifier.height(10.dp))
            }

            when {
                loading -> Box(
                    Modifier.fillMaxWidth().height(220.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PrimaryColor)
                }
                error != null -> ErrorCard(error ?: "No se pudo cargar el perfil") { loadProfile() }
                activeTab == Tab.Summary -> {
                    SegmentedTabs(activeMode, { activeMode = it }, StatsMode.values().toList())
                    Spacer(modifier = Modifier.height(12.dp))

                    // ── Fila 1: Win Rate + Estadísticas ──────────────
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        WinRateCard(modeStats, activeMode == StatsMode.FourPlayers, Modifier.weight(1f))
                        StatsCard(modeStats, activeMode == StatsMode.FourPlayers, Modifier.weight(1f))
                    }

                    if (isOwnProfile) {
                        Spacer(modifier = Modifier.height(12.dp))
                        // ── Fila 2: Pico RR + Némesis ────────────────
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            PicoCard(modeStats.peak_elo ?: elo, Modifier.weight(1f))
                            NemesisCard(
                                name = modeStats.nemesis_name ?: "-",
                                count = modeStats.nemesis_losses ?: 0,
                                isFourPlayer = activeMode == StatsMode.FourPlayers,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        // ── Fila 3: Racha + Víctima ──────────────────
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            RachaCard(
                                streak = modeStats.win_streak,
                                isFourPlayer = activeMode == StatsMode.FourPlayers,
                                modifier = Modifier.weight(1f)
                            )
                            VictimaCard(
                                name = modeStats.victim_name ?: "-",
                                count = modeStats.victim_wins ?: 0,
                                isFourPlayer = activeMode == StatsMode.FourPlayers,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    if (!isOwnProfile) {
                        Spacer(modifier = Modifier.height(12.dp))
                        HeadToHeadCard(h2h = h2h, isFourPlayer = activeMode == StatsMode.FourPlayers)
                    }
                }
                else -> SettingsCard(
                    username = username,
                    email = email,
                    currentPassword = currentPassword,
                    newPassword = newPassword,
                    confirmPassword = confirmPassword,
                    onUsername = { username = it },
                    onEmail = { email = it },
                    onCurrent = { currentPassword = it },
                    onNew = { newPassword = it },
                    onConfirm = { confirmPassword = it },
                    onSave = { saveSettings() },
                    saving = saving,
                    error = settingsError,
                    success = settingsSuccess
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Botón volver (PNG) ───────────────────────────────────
            Image(
                painter = painterResource(id = R.drawable.botonvolvermenu),
                contentDescription = "Volver",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxWidth(0.72f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onNavigate(if (isOwnProfile) "menu" else returnTo) }
                    )
            )
            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

// ── Header: Avatar a la izquierda + tarjeta ELO a la derecha ─────────
@Composable
private fun Header(username: String, elo: Int, avatarUrl: String?, isOwnProfile: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        // Avatar estilo polaroid
        Box(
            modifier = Modifier
                .background(Color.White, RoundedCornerShape(6.dp))
                .padding(3.dp)
        ) {
            val presetRes = AvatarPresets.drawableForId(avatarUrl)
            when {
                presetRes != null -> Image(
                    painter = painterResource(presetRes),
                    contentDescription = "Avatar",
                    modifier = Modifier
                        .size(62.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    contentScale = ContentScale.Crop
                )
                !avatarUrl.isNullOrBlank() -> AsyncImage(
                    model = avatarUrl,
                    contentDescription = "Avatar",
                    modifier = Modifier
                        .size(62.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    contentScale = ContentScale.Crop
                )
                else -> Box(
                    modifier = Modifier
                        .size(62.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(PrimaryColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        username.firstOrNull()?.uppercaseChar()?.toString() ?: "J",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Placa ELO ACTUAL (PNG con valor superpuesto)
        Box(
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.eloactual),
                contentDescription = "ELO Actual",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.width(180.dp)
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 18.dp)
            ) {
                Text(
                    "$elo RR",
                    color = TextColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ── Tarjeta ilustrada reutilizable (PNG arriba + datos abajo) ────────
@Composable
private fun IllustratedCard(
    imageRes: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        color = SurfaceColor.copy(alpha = 0.95f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, BorderColor)
    ) {
        Column {
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxWidth()
            )
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                content()
            }
        }
    }
}

// ── Win Rate (texto superpuesto sobre el PNG) ────────────────────────
@Composable
private fun WinRateCard(modeStats: ModeStatsResponse, isFourPlayer: Boolean, modifier: Modifier = Modifier) {
    val total = modeStats.total_games.coerceAtLeast(0)
    val wins = if (isFourPlayer) (modeStats.first_place ?: 0) else modeStats.wins
    val losses = if (isFourPlayer) (total - wins).coerceAtLeast(0) else modeStats.losses
    val ratio = if (total > 0) wins.toFloat() / total.toFloat() else 0f
    val percent = (ratio * 100).roundToInt()

    Box(
        modifier = modifier.clip(RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.BottomCenter
    ) {
        // PNG como fondo completo
        Image(
            painter = painterResource(id = R.drawable.winrate),
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier.fillMaxWidth()
        )
        // Datos superpuestos sobre el PNG
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                "$percent%",
                color = TextColor,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                textAlign = TextAlign.Center
            )
            Text(
                if (isFourPlayer) "1º: $wins" else "$wins V - $losses D",
                color = TextMutedColor,
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ── Estadísticas (números superpuestos sobre el PNG) ─────────────────
@Composable
private fun StatsCard(modeStats: ModeStatsResponse, isFourPlayer: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.clip(RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.BottomCenter
    ) {
        // PNG como fondo completo
        Image(
            painter = painterResource(id = R.drawable.estadisticas),
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier.fillMaxWidth()
        )
        // Solo los números superpuestos
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                modeStats.total_games.toString(),
                color = TextColor,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            if (isFourPlayer) {
                Text((modeStats.first_place ?: 0).toString(), color = Color(0xFF4ADE80), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text((modeStats.second_place ?: 0).toString(), color = TextColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text((modeStats.third_place ?: 0).toString(), color = TextColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text((modeStats.fourth_place ?: 0).toString(), color = Color(0xFFF87171), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            } else {
                Text(modeStats.wins.toString(), color = Color(0xFF4ADE80), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(modeStats.losses.toString(), color = Color(0xFFF87171), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(modeStats.draws.toString(), color = Color(0xFFFBBF24), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

// ── Pico de RR (PNG + valor) ─────────────────────────────────────────
@Composable
private fun PicoCard(peakElo: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.clip(RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.BottomCenter
    ) {
        Image(
            painter = painterResource(id = R.drawable.picorr),
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            "$peakElo RR",
            color = TextColor,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(10.dp)
        )
    }
}

// ── Tu Némesis (datos superpuestos sobre el PNG) ─────────────────────
@Composable
private fun NemesisCard(name: String, count: Int, isFourPlayer: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.clip(RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.BottomCenter
    ) {
        Image(
            painter = painterResource(id = R.drawable.nemesis),
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier.fillMaxWidth()
        )
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                name,
                color = TextColor,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (count > 0) {
                Text(
                    "${if (isFourPlayer) "Superado" else "Derrotas"}: $count",
                    color = TextMutedColor,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ── Mejor Racha (datos superpuestos sobre el PNG) ────────────────────
@Composable
private fun RachaCard(streak: Int, isFourPlayer: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.clip(RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.BottomCenter
    ) {
        Image(
            painter = painterResource(id = R.drawable.racha),
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier.fillMaxWidth()
        )
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                streak.toString(),
                color = TextColor,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )
            Text(
                if (isFourPlayer) "1º puestos seguidos" else "Victorias seguidas",
                color = TextMutedColor,
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ── Tu Víctima (datos superpuestos sobre el PNG) ─────────────────────
@Composable
private fun VictimaCard(name: String, count: Int, isFourPlayer: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.clip(RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.BottomCenter
    ) {
        Image(
            painter = painterResource(id = R.drawable.victima),
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier.fillMaxWidth()
        )
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                name,
                color = TextColor,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (count > 0) {
                Text(
                    "${if (isFourPlayer) "Superado" else "Victorias"}: $count",
                    color = TextMutedColor,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ── Cara a cara (perfil ajeno) ───────────────────────────────────────
@Composable
private fun HeadToHeadCard(h2h: HeadToHeadResponse?, isFourPlayer: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SurfaceColor.copy(alpha = 0.92f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, BorderColor)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Cara a cara", color = TextColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            if (h2h == null) {
                Text("Sin datos disponibles.", color = TextMutedColor, fontSize = 12.sp)
            } else if (isFourPlayer) {
                StatRow("Partidas juntos (4P)", (h2h.total_matches_4p ?: 0).toString())
                StatRow("Tus 1º puestos", (h2h.first_places_4p ?: 0).toString(), Color(0xFF4ADE80))
                StatRow("Resto de puestos", (h2h.other_places_4p ?: 0).toString(), Color(0xFFF87171))
            } else {
                StatRow("Partidas jugadas", h2h.total_matches.toString())
                StatRow("Tus victorias", h2h.wins.toString(), Color(0xFF4ADE80))
                StatRow("Tus derrotas", h2h.losses.toString(), Color(0xFFF87171))
                StatRow("Empates", h2h.draws.toString(), Color(0xFFFBBF24))
            }
        }
    }
}

// ── Ajustes de cuenta ────────────────────────────────────────────────
@Composable
private fun SettingsCard(
    username: String,
    email: String,
    currentPassword: String,
    newPassword: String,
    confirmPassword: String,
    onUsername: (String) -> Unit,
    onEmail: (String) -> Unit,
    onCurrent: (String) -> Unit,
    onNew: (String) -> Unit,
    onConfirm: (String) -> Unit,
    onSave: () -> Unit,
    saving: Boolean,
    error: String?,
    success: String?
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SurfaceColor.copy(alpha = 0.95f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, BorderColor)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Cambiar los datos de tu cuenta", color = TextColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            StyledField(username, onUsername, "Nombre de usuario")
            StyledField(email, onEmail, "Email")
            StyledField(currentPassword, onCurrent, "Contraseña actual", true)
            StyledField(newPassword, onNew, "Nueva contraseña", true)
            StyledField(confirmPassword, onConfirm, "Confirmar contraseña", true)
            if (!error.isNullOrBlank()) Text(error, color = Color(0xFFF87171), fontSize = 12.sp)
            if (!success.isNullOrBlank()) Text(success, color = Color(0xFF4ADE80), fontSize = 12.sp)
            Button(
                onClick = onSave,
                enabled = !saving,
                modifier = Modifier.fillMaxWidth().height(46.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(if (saving) "Guardando..." else "Guardar cambios", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ── Campo de texto con estilo ────────────────────────────────────────
@Composable
private fun StyledField(value: String, onChange: (String) -> Unit, label: String, isPassword: Boolean = false) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = PrimaryColor,
            unfocusedBorderColor = BorderColor,
            focusedLabelColor = PrimaryColor,
            unfocusedLabelColor = TextMutedColor,
            focusedTextColor = TextColor,
            unfocusedTextColor = TextColor,
            cursorColor = PrimaryColor
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

// ── Pestañas segmentadas ─────────────────────────────────────────────
@Composable
private fun <T> SegmentedTabs(active: T, onSelect: (T) -> Unit, values: List<T>) where T : Enum<T> {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SurfaceColor.copy(alpha = 0.92f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, BorderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            values.forEach { value ->
                val label = (value as? Tab)?.label ?: (value as? StatsMode)?.label ?: value.name
                val selected = value == active

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelect(value) },
                    color = if (selected) PrimaryColor.copy(alpha = 0.18f) else Color.Transparent,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(
                        1.dp,
                        if (selected) PrimaryColor.copy(alpha = 0.3f) else BorderColor
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (selected) PrimaryColor else TextMutedColor,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

// ── Fila de estadística ──────────────────────────────────────────────
@Composable private fun StatRow(label: String, value: String, color: Color = TextColor) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = TextMutedColor, fontSize = 11.sp, maxLines = 1)
        Text(value, color = color, fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
    }
}

// ── Tarjeta de error ─────────────────────────────────────────────────
@Composable
private fun ErrorCard(message: String, onRetry: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SurfaceColor.copy(alpha = 0.92f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFF87171))
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(message, color = Color(0xFFF87171))
            Spacer(modifier = Modifier.height(10.dp))
            Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)) {
                Text("Reintentar", color = Color.White)
            }
        }
    }
}

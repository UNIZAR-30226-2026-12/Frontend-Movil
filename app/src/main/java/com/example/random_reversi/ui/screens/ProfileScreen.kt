package com.example.random_reversi.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.random_reversi.data.GamesRepository
import com.example.random_reversi.data.UserProfileStore
import com.example.random_reversi.data.UserRepository
import com.example.random_reversi.data.UserResult
import com.example.random_reversi.data.remote.ModeStatsResponse
import com.example.random_reversi.data.remote.UserStatsResponse
import com.example.random_reversi.ui.theme.BgColor
import com.example.random_reversi.ui.theme.BorderColor
import com.example.random_reversi.ui.theme.PrimaryColor
import com.example.random_reversi.ui.theme.SurfaceColor
import com.example.random_reversi.ui.theme.SurfaceLightColor
import com.example.random_reversi.ui.theme.TextColor
import com.example.random_reversi.ui.theme.TextMutedColor
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
fun ProfileScreen(onNavigate: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    val profile by UserProfileStore.state.collectAsState()

    var stats by remember { mutableStateOf<UserStatsResponse?>(null) }
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

    val name = stats?.username ?: profile.username
    val elo = stats?.elo ?: 1000
    val avatar = stats?.avatar_url ?: profile.avatarUrl

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        Header(name, elo, avatar)
        Spacer(modifier = Modifier.height(10.dp))
        SegmentedTabs(activeTab, { activeTab = it }, Tab.values().toList())
        Spacer(modifier = Modifier.height(10.dp))

        when {
            loading -> Box(Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryColor)
            }
            error != null -> ErrorCard(error ?: "No se pudo cargar el perfil") { loadProfile() }
            activeTab == Tab.Summary -> {
                SegmentedTabs(activeMode, { activeMode = it }, StatsMode.values().toList())
                Spacer(modifier = Modifier.height(10.dp))
                WinRateCard(modeStats, activeMode == StatsMode.FourPlayers)
                Spacer(modifier = Modifier.height(10.dp))
                StatsCard(modeStats, activeMode == StatsMode.FourPlayers)
                Spacer(modifier = Modifier.height(10.dp))
                Highlights(modeStats, elo, activeMode == StatsMode.FourPlayers)
                Spacer(modifier = Modifier.height(10.dp))
                Rivals(modeStats, activeMode == StatsMode.FourPlayers)
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
        Button(
            onClick = { onNavigate("menu") },
            modifier = Modifier.fillMaxWidth(0.72f).height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
            shape = RoundedCornerShape(12.dp)
        ) { Text("Volver al menu", color = Color.White, fontWeight = FontWeight.Bold) }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun Header(username: String, elo: Int, avatarUrl: String?) {
    Surface(modifier = Modifier.fillMaxWidth(), color = SurfaceColor, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, BorderColor)) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.size(62.dp).clip(CircleShape), shape = CircleShape, border = BorderStroke(1.dp, BorderColor), color = SurfaceLightColor) {
                val presetRes = AvatarPresets.drawableForId(avatarUrl)
                when {
                    presetRes != null -> androidx.compose.foundation.Image(painter = painterResource(presetRes), contentDescription = "Avatar", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    !avatarUrl.isNullOrBlank() -> AsyncImage(model = avatarUrl, contentDescription = "Avatar", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(username.firstOrNull()?.uppercaseChar()?.toString() ?: "J", color = TextColor, fontWeight = FontWeight.Bold, fontSize = 22.sp) }
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(username, color = TextColor, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Perfil y estadisticas", color = TextMutedColor, fontSize = 12.sp)
            }
            Surface(color = PrimaryColor.copy(alpha = 0.18f), shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, PrimaryColor.copy(alpha = 0.35f))) {
                Text("$elo RR", modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = Color(0xFFFBBF24), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun WinRateCard(modeStats: ModeStatsResponse, isFourPlayer: Boolean) {
    val total = modeStats.total_games.coerceAtLeast(0)
    val wins = if (isFourPlayer) (modeStats.first_place ?: 0) else modeStats.wins
    val losses = if (isFourPlayer) (total - wins).coerceAtLeast(0) else modeStats.losses
    val ratio = if (total > 0) wins.toFloat() / total.toFloat() else 0f
    val percent = (ratio * 100).roundToInt()

    Surface(modifier = Modifier.fillMaxWidth(), color = SurfaceColor, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, BorderColor)) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(86.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = { ratio },
                    color = Color(0xFF4ADE80),
                    trackColor = Color(0xFFF87171),
                    strokeWidth = 8.dp,
                    strokeCap = StrokeCap.Butt,
                    modifier = Modifier.fillMaxSize()
                )
                Text("$percent%", color = TextColor, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(if (isFourPlayer) "1º puesto: $wins" else "Victorias: $wins", color = Color(0xFF4ADE80), fontWeight = FontWeight.SemiBold)
                Text(if (isFourPlayer) "2º, 3º o 4º puesto: $losses" else "Derrotas: $losses", color = Color(0xFFF87171), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun StatsCard(modeStats: ModeStatsResponse, isFourPlayer: Boolean) {
    Surface(modifier = Modifier.fillMaxWidth(), color = SurfaceColor, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, BorderColor)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Estadisticas", color = TextColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            StatRow("Partidas jugadas", modeStats.total_games.toString())
            if (isFourPlayer) {
                StatRow("1º puesto", (modeStats.first_place ?: 0).toString(), Color(0xFF4ADE80))
                StatRow("2º puesto", (modeStats.second_place ?: 0).toString())
                StatRow("3º puesto", (modeStats.third_place ?: 0).toString())
                StatRow("4º puesto", (modeStats.fourth_place ?: 0).toString(), Color(0xFFF87171))
            } else {
                StatRow("Victorias", modeStats.wins.toString(), Color(0xFF4ADE80))
                StatRow("Derrotas", modeStats.losses.toString(), Color(0xFFF87171))
                StatRow("Empates", modeStats.draws.toString(), Color(0xFFFBBF24))
            }
        }
    }
}

@Composable
private fun Highlights(
    modeStats: ModeStatsResponse,
    elo: Int,
    isFourPlayer: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        MiniCard(
            "👑",
            "Pico RR",
            (modeStats.peak_elo ?: elo).toString(),
            "Maximo historico",
            Modifier.weight(1f)
        )
        MiniCard(
            "🔥",
            "Mejor racha",
            modeStats.win_streak.toString(),
            if (isFourPlayer) "1º puestos seguidos" else "Victorias seguidas",
            Modifier.weight(1f)
        )
    }
}

@Composable private fun Rivals(modeStats: ModeStatsResponse, isFourPlayer: Boolean) {
    val n = modeStats.nemesis_losses ?: 0
    val v = modeStats.victim_wins ?: 0
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        MiniCard("😈", "Nemesis", modeStats.nemesis_name ?: "-", if (n > 0) "${if (isFourPlayer) "Te ha superado" else "Te ha ganado"} $n veces" else "Sin rival destacado", Modifier.weight(1f))
        MiniCard("😇", "Victima", modeStats.victim_name ?: "-", if (v > 0) "${if (isFourPlayer) "Lo has superado" else "Has ganado"} $v veces" else "Sin victima destacada", Modifier.weight(1f))
    }
}

@Composable
private fun MiniCard(icon: String, title: String, value: String, hint: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, color = SurfaceColor, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, BorderColor)) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(icon, fontSize = 18.sp)
            Text(title, color = TextMutedColor, fontSize = 12.sp)
            Text(value, color = TextColor, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(hint, color = TextMutedColor, fontSize = 11.sp)
        }
    }
}

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
    Surface(modifier = Modifier.fillMaxWidth(), color = SurfaceColor, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, BorderColor)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Cambiar los datos de tu cuenta", color = TextColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            StyledField(username, onUsername, "Nombre de usuario")
            StyledField(email, onEmail, "Email")
            StyledField(currentPassword, onCurrent, "Contraseña actual", true)
            StyledField(newPassword, onNew, "Nueva contraseña", true)
            StyledField(confirmPassword, onConfirm, "Confirmar contraseña", true)
            if (!error.isNullOrBlank()) Text(error, color = Color(0xFFF87171), fontSize = 12.sp)
            if (!success.isNullOrBlank()) Text(success, color = Color(0xFF4ADE80), fontSize = 12.sp)
            Button(onClick = onSave, enabled = !saving, modifier = Modifier.fillMaxWidth().height(46.dp), colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor), shape = RoundedCornerShape(10.dp)) {
                Text(if (saving) "Guardando..." else "Guardar cambios", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

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

@Composable
private fun <T> SegmentedTabs(active: T, onSelect: (T) -> Unit, values: List<T>) where T : Enum<T> {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SurfaceColor,
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

@Composable private fun StatRow(label: String, value: String, color: Color = TextColor) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = TextMutedColor, fontSize = 13.sp)
        Text(value, color = color, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
private fun ErrorCard(message: String, onRetry: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), color = SurfaceColor, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Color(0xFFF87171))) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(message, color = Color(0xFFF87171))
            Spacer(modifier = Modifier.height(10.dp))
            Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)) {
                Text("Reintentar", color = Color.White)
            }
        }
    }
}

package com.example.random_reversi.ui.screens

import android.net.Uri
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.random_reversi.R
import com.example.random_reversi.data.GamesRepository
import com.example.random_reversi.data.UserRepository
import com.example.random_reversi.data.UserResult
import com.example.random_reversi.data.remote.HistoryEntry
import com.example.random_reversi.data.remote.PublicLobby
import com.example.random_reversi.ui.components.GameModeModal
import com.example.random_reversi.ui.theme.*
import com.example.random_reversi.utils.AvatarPresets
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
fun OnlineGameScreen(onNavigate: (String) -> Unit) {
    var publicGames by remember { mutableStateOf<List<PublicLobby>>(emptyList()) }
    var userElo by remember { mutableStateOf(0) }
    var history by remember { mutableStateOf<List<HistoryEntry>>(emptyList()) }
    var isRefreshing by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var isCreating by remember { mutableStateOf(false) }
    var showCreateModal by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun loadData() {
        scope.launch {
            isRefreshing = true
            when (val meResult = UserRepository.getMe()) {
                is UserResult.Success -> userElo = meResult.data.elo
                is UserResult.Error -> {}
            }
            when (val result = GamesRepository.getPublicLobbies()) {
                is UserResult.Success -> publicGames = result.data
                is UserResult.Error -> errorMsg = result.message
            }
            when (val histResult = GamesRepository.getMyHistory()) {
                is UserResult.Success -> history = histResult.data
                is UserResult.Error -> {}
            }
            isRefreshing = false
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadData()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Fondo General
        Image(
            painter = painterResource(id = R.drawable.nuevofondomovil),
            contentDescription = "Fondo",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(28.dp)) // Ajuste para el status bar

            // 1. Cabecera: Título y Botones
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Título a la izquierda
                Image(
                    painter = painterResource(id = R.drawable.titulojugaronline),
                    contentDescription = "Jugar Online",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .weight(1f)
                        .height(80.dp)
                        .padding(end = 16.dp)
                )

                // Botones a la derecha
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.actualizar),
                        contentDescription = "Actualizar",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .height(35.dp)
                            .clickable(enabled = !isRefreshing) { loadData() }
                            .alpha(if (isRefreshing) 0.5f else 1f)
                    )
                    Image(
                        painter = painterResource(id = R.drawable.crearpartida),
                        contentDescription = "Crear Partida",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .height(35.dp)
                            .clickable(enabled = !isCreating) { showCreateModal = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 2. Módulo Estatus / Historial (Libreta Superior)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.tuestatusmovil),
                    contentDescription = null,
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier.fillMaxWidth()
                )

                // Contenido dinámico superpuesto en la libreta
                Row(
                    modifier = Modifier
                        .matchParentSize()
                        .padding(top = 40.dp, bottom = 16.dp, start = 16.dp, end = 16.dp)
                ) {
                    // Bloque Izquierdo: ELO
                    Box(
                        modifier = Modifier
                            .weight(0.45f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Image(
                                painter = painterResource(id = R.drawable.eloactual),
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxWidth(0.9f)
                            )
                            Text(
                                text = "$userElo RR",
                                color = Color.Black,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(top = 10.dp) // Reducimos el padding de 18 a 10 para subirlo
                            )
                        }
                    }

                    // Bloque Derecho: Historial (máximo 4 partidas para encajar)
                    Column(
                        modifier = Modifier
                            .weight(0.55f)
                            .fillMaxHeight()
                            .padding(start = 12.dp, top = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val historyTop4 = history.take(4)
                        if (historyTop4.isEmpty()) {
                            Text(
                                "No hay partidas recientes",
                                fontSize = 12.sp,
                                color = TextMutedColor,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                        } else {
                            historyTop4.forEach { entry ->
                                val tone = historyTone(entry)
                                val (icon, color) = when (tone) {
                                    "win" -> "✅" to Color(0xFF15803d)
                                    "loss" -> "❌" to Color(0xFFb91c1c)
                                    else -> "➖" to Color.DarkGray
                                }
                                val modeLabel = when (entry.mode.lowercase()) {
                                    "1vs1vs1vs1", "1v1v1v1" -> "4P"
                                    else -> "1v1"
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("${entry.result.uppercase().take(7)} $icon", color = color, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    Text(entry.date ?: "-", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                                    Text(modeLabel, color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    Text(entry.rankChange, color = color, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Corcho Central (Partidas Públicas)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                // Fondo: Corcho
                Image(
                    painter = painterResource(id = R.drawable.tablerocorchomovil),
                    contentDescription = "Corcho de Partidas",
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.fillMaxSize()
                )

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PrimaryColor)
                    }
                } else if (publicGames.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "No hay partidas públicas clavadas.",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            modifier = Modifier
                                .background(Color.Black.copy(0.4f), RoundedCornerShape(8.dp))
                                .padding(16.dp)
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(publicGames, key = { it.game_id }) { game ->
                            // Rotación pseudoaleatoria consistente basada en el ID
                            val rotationList = listOf(-4f, 4f, -2f, 2f, 0f)
                            val rotIndex = kotlin.math.abs(game.game_id.hashCode()) % rotationList.size
                            val deg = rotationList[rotIndex]

                            GamePostIt(
                                game = game,
                                modifier = Modifier.rotate(deg),
                                onJoin = {
                                    scope.launch {
                                        when (val joinResult = GamesRepository.joinLobby(game.game_id)) {
                                            is UserResult.Success -> {
                                                val safeMode = game.mode ?: "1vs1"
                                                onNavigate("waiting-room/$safeMode/${game.game_id}/online-game")
                                            }
                                            is UserResult.Error -> errorMsg = joinResult.message
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Botón de Volver al Menú
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.botonvolvermenu),
                    contentDescription = "Volver al menú",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .height(100.dp)
                        .clickable { onNavigate("menu") }
                )
            }
        }

        // Popups
        GameModeModal(
            isOpen = showCreateModal,
            onClose = { showCreateModal = false },
            onSelectMode = { mode ->
                showCreateModal = false
                isCreating = true
                scope.launch {
                    when (val result = GamesRepository.createLobby(mode)) {
                        is UserResult.Success -> {
                            onNavigate("waiting-room/$mode/${result.data.game_id}/online-game")
                        }
                        is UserResult.Error -> errorMsg = result.message
                    }
                    isCreating = false
                }
            }
        )
        
        errorMsg?.let { msg ->
            LaunchedEffect(msg) {
                kotlinx.coroutines.delay(3000)
                errorMsg = null
            }
            Box(modifier = Modifier.fillMaxSize().padding(top = 100.dp), contentAlignment = Alignment.TopCenter) {
                Surface(
                    color = SurfaceColor,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color.Red)
                ) {
                    Text(msg, modifier = Modifier.padding(12.dp), color = Color(0xFFF87171), fontSize = 13.sp)
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════
// Componente de "Post-It" dinámico
// ════════════════════════════════════════════════════════════════════

@Composable
private fun GamePostIt(game: PublicLobby, modifier: Modifier = Modifier, onJoin: () -> Unit) {
    val creator = game.creator ?: "Desconocido"
    val mode = when (game.mode?.lowercase()) {
        "1vs1vs1vs1", "1v1v1v1" -> "4P"
        else -> "1VS1"
    }
    val rr = game.creator_elo ?: 0
    val players = game.players ?: 1
    val maxPlayers = game.max_players ?: 2
    val status = game.status?.lowercase() ?: "waiting"
    val isFull = players >= maxPlayers
    val isJoinEnabled = !isFull && status == "waiting"
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        contentAlignment = Alignment.Center
    ) {
        // Fondo Post It
        Image(
            painter = painterResource(id = R.drawable.publicacionpartida),
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier.fillMaxWidth()
        )
        
        // Contenidos (Posición Relativa sobre el Post-It)
        Column(
            modifier = Modifier
                .matchParentSize()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            
            // Fila superior: Host e Info
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                // Host Column
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(0.3f)) {
                    Text("HOST", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.Black)
                    Spacer(modifier = Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(1.dp, Color.Black, CircleShape)
                    ) {
                        val presetAvatar = AvatarPresets.drawableForId(game.avatar_url)
                        when {
                            presetAvatar != null -> {
                                Image(
                                    painter = painterResource(id = presetAvatar),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            !game.avatar_url.isNullOrBlank() -> {
                                AsyncImage(
                                    model = game.avatar_url,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            else -> {
                                Box(modifier = Modifier.fillMaxSize().background(PrimaryColor), contentAlignment = Alignment.Center) {
                                    Text(creator.firstOrNull()?.uppercase() ?: "?", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
                
                // Info Box (Nombre y RR)
                Box(
                    modifier = Modifier
                        .weight(0.7f)
                        .padding(start = 8.dp)
                        .border(2.dp, Color.Black, androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                        .background(Color.LightGray.copy(0.3f))
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(creator, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("$rr RR", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.Black)
                    }
                }
            }
            
            // Fila Inferior: Modo, Status, Botón
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Info pequeña: 1v1 y 1/2
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        color = Color.White,
                        border = BorderStroke(1.dp, Color.Black),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                    ) {
                        Text(mode, fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.Black, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("👤", fontSize = 10.sp)
                        Text("$players/$maxPlayers", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.Black)
                    }
                }
                
                // Botón Unirse
                Box(
                    modifier = Modifier.weight(1f).padding(start = 8.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.botonunirse),
                        contentDescription = "Unirse",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .height(36.dp)
                            .clickable(enabled = isJoinEnabled) { onJoin() }
                            .alpha(if (isJoinEnabled) 1f else 0.5f)
                    )
                }
            }
        }
    }
}

private fun historyTone(entry: HistoryEntry): String {
    val mode = entry.mode.lowercase()
    val normalizedResult = entry.result.lowercase().replace("º", "").trim()
    val is4p = mode == "1vs1vs1vs1" || mode == "1v1v1v1"

    if (is4p) {
        return when {
            normalizedResult.startsWith("1") -> "win"
            normalizedResult.startsWith("4") -> "loss"
            else -> "draw"
        }
    }

    return when (normalizedResult) {
        "ganada", "win", "victoria" -> "win"
        "perdida", "loss", "derrota" -> "loss"
        else -> "draw"
    }
}

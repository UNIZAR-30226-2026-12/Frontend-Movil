package com.example.random_reversi.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.random_reversi.data.RankingRepository
import com.example.random_reversi.data.UserProfileStore
import com.example.random_reversi.data.UserResult
import com.example.random_reversi.data.remote.RankingEntry
import com.example.random_reversi.ui.theme.*
import com.example.random_reversi.utils.AvatarPresets
import kotlinx.coroutines.launch

@Composable
fun RankingScreen(onNavigate: (String) -> Unit) {
    val profile by UserProfileStore.state.collectAsState()
    var ranking by remember { mutableStateOf<List<RankingEntry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun fetchRankingData(isRefresh: Boolean = false) {
        if (isRefresh) isRefreshing = true else isLoading = true
        scope.launch {
            when (val result = RankingRepository.getRanking()) {
                is UserResult.Success -> {
                    ranking = result.data
                    errorMsg = null
                }
                is UserResult.Error -> errorMsg = result.message
            }
            isLoading = false
            isRefreshing = false
        }
    }

    // Carga inicial
    LaunchedEffect(Unit) {
        fetchRankingData()
    }

    Box(modifier = Modifier.fillMaxSize().background(BgColor)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(50.dp))

            // --- Header con Título y Botón de Actualizar ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Ranking Global", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = TextColor)
                    Text("Top jugadores por ELO (RR)", fontSize = 13.sp, color = TextMutedColor)
                }

                Button(
                    onClick = { fetchRankingData(isRefresh = true) },
                    enabled = !isRefreshing && !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.06f),
                        disabledContainerColor = Color.White.copy(alpha = 0.03f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier.height(40.dp)
                ) {
                    Text(
                        text = if (isRefreshing) "Actualizando..." else "Actualizar",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- Contenido ---
            if (isLoading && !isRefreshing) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryColor)
                }
            } else if (errorMsg != null && ranking.isEmpty()) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(errorMsg!!, color = Color(0xFFF87171), fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { fetchRankingData() },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)
                        ) {
                            Text("Reintentar", color = Color.White)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    itemsIndexed(ranking, key = { _, entry -> entry.id }) { index, entry ->
                        RankingItem(
                            position = index + 1,
                            entry = entry,
                            isCurrentUser = entry.username == profile.username,
                            onClick = { onNavigate("profile/${entry.id}/${entry.username}/ranking") }
                        )
                    }
                    if (ranking.isEmpty() && !isLoading) {
                        item {
                            Text("No hay datos de ranking", color = TextMutedColor, modifier = Modifier.padding(20.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- Botón de Volver ---
            Button(
                onClick = { onNavigate("menu") },
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Volver al menú", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun RankingItem(position: Int, entry: RankingEntry, isCurrentUser: Boolean, onClick: () -> Unit) {
    val medalColor = when (position) {
        1 -> Color(0xFFFFD700) // Oro
        2 -> Color(0xFFC0C0C0) // Plata
        3 -> Color(0xFFCD7F32) // Bronce
        else -> null
    }

    val bgColor = if (isCurrentUser) Color(0xFFFBBF24).copy(alpha = 0.12f) else SurfaceColor
    val strokeColor = if (isCurrentUser) Color(0xFFFBBF24).copy(alpha = 0.55f) else (medalColor?.copy(0.5f) ?: BorderColor)

    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        color = bgColor,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, strokeColor)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Posición
            Box(
                modifier = Modifier.size(36.dp).background(
                    medalColor?.copy(0.2f) ?: SurfaceLightColor, CircleShape
                ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "#$position",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = medalColor ?: TextMutedColor
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Avatar (Lógica completa)
            Box(
                modifier = Modifier.size(40.dp).background(SurfaceLightColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                val presetRes = AvatarPresets.drawableForId(entry.avatar_url)
                when {
                    presetRes != null -> {
                        Image(
                            painter = painterResource(id = presetRes),
                            contentDescription = "Avatar de ${entry.username}",
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }
                    !entry.avatar_url.isNullOrBlank() -> {
                        AsyncImage(
                            model = entry.avatar_url,
                            contentDescription = "Avatar de ${entry.username}",
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }
                    else -> {
                        Text(
                            entry.username.firstOrNull()?.uppercase() ?: "?",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Nombre
            Text(
                entry.username,
                modifier = Modifier.weight(1f),
                fontWeight = FontWeight.Bold,
                color = TextColor,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // ELO
            Surface(
                color = Color(0xFFFBBF24).copy(alpha = 0.12f),
                shape = RoundedCornerShape(99.dp),
                border = BorderStroke(1.dp, Color(0xFFFBBF24).copy(alpha = 0.35f))
            ) {
                Text(
                    "${entry.elo} RR",
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFFBBF24),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}
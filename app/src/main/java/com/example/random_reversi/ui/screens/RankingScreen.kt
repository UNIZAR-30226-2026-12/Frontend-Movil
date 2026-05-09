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
import com.example.random_reversi.data.RankingRepository
import com.example.random_reversi.data.UserProfileStore
import com.example.random_reversi.data.UserResult
import com.example.random_reversi.data.remote.RankingEntry
import com.example.random_reversi.ui.theme.*
import com.example.random_reversi.utils.AvatarImage
import kotlinx.coroutines.launch
import androidx.compose.foundation.Image
import androidx.compose.ui.zIndex
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.draw.alpha
import com.example.random_reversi.R

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

    Box(modifier = Modifier.fillMaxSize()) {
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
            Spacer(modifier = Modifier.height(20.dp))

            // --- Header con Título y Botón de Actualizar ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(1f)
                    .offset(y = 25.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Image(
                    painter = painterResource(id = R.drawable.rankingglobal),
                    contentDescription = "Ranking Global",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth(0.65f)
                        .height(105.dp)
                        .padding(start = 16.dp)
                )

                Image(
                    painter = painterResource(id = R.drawable.actualizar),
                    contentDescription = "Actualizar",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 16.dp)
                        .size(112.dp)
                        .alpha(if (isRefreshing || isLoading) 0.5f else 1f)
                        .clickable(enabled = !isRefreshing && !isLoading) {
                            fetchRankingData(isRefresh = true)
                        }
                )
            }

            // --- Contenido ---
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Image(
                    painter = painterResource(id = R.drawable.libretareglas2),
                    contentDescription = "Muro de Ranking",
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                )
                if (isLoading && !isRefreshing) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryColor)
                }
            } else if (errorMsg != null && ranking.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
                    modifier = Modifier.fillMaxSize().padding(horizontal = 48.dp, vertical = 40.dp),
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
                            Text("No hay datos de ranking", color = Color.DarkGray, modifier = Modifier.padding(20.dp))
                        }
                    }
                }
            }
            }

            // --- Botón de Volver ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-25).dp)
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
        }
    }
}

@Composable
private fun RankingItem(position: Int, entry: RankingEntry, isCurrentUser: Boolean, onClick: () -> Unit) {
    val medalColor = when (position) {
        1 -> Color(0xFFD4AF37) // Oro oscuro para contrastar con papel
        2 -> Color(0xFFA0A0A0) // Plata
        3 -> Color(0xFFCD7F32) // Bronce
        else -> null
    }

    val bgColor = if (isCurrentUser) Color.Black.copy(alpha = 0.08f) else Color.Transparent
    val strokeColor = if (isCurrentUser) Color.Black.copy(alpha = 0.4f) else (medalColor?.copy(0.6f) ?: Color.Black.copy(0.15f))

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
                    medalColor?.copy(0.15f) ?: Color.Black.copy(0.05f), CircleShape
                ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "#$position",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = medalColor ?: Color.DarkGray
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Avatar (Lógica completa)
            Box(
                modifier = Modifier.size(40.dp).background(SurfaceLightColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                AvatarImage(
                    avatarUrl = entry.avatar_url,
                    contentDescription = "Avatar de ${entry.username}",
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                    contentScale = ContentScale.Crop,
                    fallback = {
                        Text(
                            entry.username.firstOrNull()?.uppercase() ?: "?",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextColor
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Nombre
            Text(
                entry.username,
                modifier = Modifier.weight(1f),
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // ELO
            Surface(
                color = medalColor?.copy(0.1f) ?: Color.Black.copy(0.05f),
                shape = RoundedCornerShape(99.dp),
                border = BorderStroke(1.dp, medalColor?.copy(0.3f) ?: Color.Black.copy(0.15f))
            ) {
                Text(
                    "${entry.elo} RR",
                    fontWeight = FontWeight.ExtraBold,
                    color = medalColor ?: Color.DarkGray,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}
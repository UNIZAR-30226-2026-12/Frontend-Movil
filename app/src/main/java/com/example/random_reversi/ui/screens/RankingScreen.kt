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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.random_reversi.data.RankingRepository
import com.example.random_reversi.data.UserResult
import com.example.random_reversi.data.remote.RankingEntry
import com.example.random_reversi.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun RankingScreen(onNavigate: (String) -> Unit) {
    var ranking by remember { mutableStateOf<List<RankingEntry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        when (val result = RankingRepository.getRanking()) {
            is UserResult.Success -> ranking = result.data
            is UserResult.Error -> errorMsg = result.message
        }
        isLoading = false
    }

    Box(modifier = Modifier.fillMaxSize().background(BgColor)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(50.dp))

            // Header
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Ranking Global", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = TextColor)
                Text("Top 50 jugadores", fontSize = 14.sp, color = TextMutedColor)
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (isLoading) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryColor)
                }
            } else if (errorMsg != null) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(errorMsg!!, color = Color(0xFFF87171), fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                isLoading = true
                                scope.launch {
                                    when (val result = RankingRepository.getRanking()) {
                                        is UserResult.Success -> { ranking = result.data; errorMsg = null }
                                        is UserResult.Error -> errorMsg = result.message
                                    }
                                    isLoading = false
                                }
                            },
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
                        RankingItem(position = index + 1, entry = entry)
                    }
                    if (ranking.isEmpty()) {
                        item {
                            Text("No hay datos de ranking", color = TextMutedColor, modifier = Modifier.padding(20.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

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
private fun RankingItem(position: Int, entry: RankingEntry) {
    val medalColor = when (position) {
        1 -> Color(0xFFFFD700) // Gold
        2 -> Color(0xFFC0C0C0) // Silver
        3 -> Color(0xFFCD7F32) // Bronze
        else -> null
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SurfaceColor,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, medalColor?.copy(0.5f) ?: BorderColor)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Position
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

            // Avatar
            Box(
                modifier = Modifier.size(40.dp).background(SurfaceLightColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    entry.username.firstOrNull()?.uppercase() ?: "?",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextColor
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Name
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
            Text(
                "${entry.elo} RR",
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFFFBBF24),
                fontSize = 14.sp
            )
        }
    }
}

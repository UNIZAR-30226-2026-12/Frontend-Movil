package com.example.random_reversi.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.random_reversi.R
import com.example.random_reversi.data.UserProfileStore
import com.example.random_reversi.data.UserRepository
import com.example.random_reversi.data.UserResult
import com.example.random_reversi.ui.theme.*
import com.example.random_reversi.utils.AvatarImage
import com.example.random_reversi.utils.AvatarPresets
import kotlinx.coroutines.launch

private data class PieceStyle1v1(
    val sideA: Color,
    val sideB: Color,
    val label: String
)

private data class PieceStyle4P(
    val p1: Color,
    val p2: Color,
    val p3: Color,
    val p4: Color,
    val label: String
)

private data class AvatarOption(
    val id: String,
    val label: String,
    val drawableRes: Int
)

private val pieceStyles1v1 = listOf(
    PieceStyle1v1(Color(0xFF222222), Color(0xFFEEEEEE), "Clasico"),
    PieceStyle1v1(Color(0xFFE74C3C), Color(0xFF3498DB), "Fuego y Hielo"),
    PieceStyle1v1(Color(0xFF2ECC71), Color(0xFFF1C40F), "Selva"),
    PieceStyle1v1(Color(0xFF9B59B6), Color(0xFFE67E22), "Atardecer"),
    PieceStyle1v1(Color(0xFF1ABC9C), Color(0xFFE84393), "Neon"),
    PieceStyle1v1(Color(0xFF34495E), Color(0xFFBDC3C7), "Metal"),
)

private val pieceStyles4P = listOf(
    PieceStyle4P(Color(0xFF18181B), Color(0xFFF8FAFC), Color(0xFFEF4444), Color(0xFF3B82F6), "Clasico 4P"),
    PieceStyle4P(Color(0xFF22C55E), Color(0xFFFDE047), Color(0xFFA855F7), Color(0xFFF97316), "Jungla Solar"),
    PieceStyle4P(Color(0xFF06B6D4), Color(0xFFF43F5E), Color(0xFF84CC16), Color(0xFFFB7185), "Cyber Pop"),
    PieceStyle4P(Color(0xFFF59E0B), Color(0xFF14B8A6), Color(0xFF8B5CF6), Color(0xFFEF4444), "Magma Frio"),
    PieceStyle4P(Color(0xFF0EA5E9), Color(0xFFFACC15), Color(0xFFEC4899), Color(0xFF10B981), "Tropical RGB"),
    PieceStyle4P(Color(0xFF3498DB), Color(0xFFF5F5DC), Color(0xFFE74C3C), Color(0xFF2ECC71), "Nebulosa"), // Azul, Beige, Rojo, Verde
)

private val avatarOptions = AvatarPresets.options.map { AvatarOption(it.id, it.label, it.drawableRes) }

private fun encodePiecePreference(duelIndex: Int, quadIndex: Int): String = "d${duelIndex}-q${quadIndex}"

private fun decodePiecePreference(value: String?): Pair<Int, Int> {
    if (value.isNullOrBlank()) return 0 to 0
    val compact = Regex("^d(\\d+)-q(\\d+)$").matchEntire(value)
    if (compact != null) {
        val duel = compact.groupValues[1].toIntOrNull() ?: 0
        val quad = compact.groupValues[2].toIntOrNull() ?: 0
        val safeDuel = if (duel in pieceStyles1v1.indices) duel else 0
        val safeQuad = if (quad in pieceStyles4P.indices) quad else 0
        return safeDuel to safeQuad
    }
    return 0 to 0
}

@Composable
fun CustomizationScreen(onNavigate: (screen: String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedPiece1v1 by remember { mutableStateOf(0) }
    var selectedPiece4p by remember { mutableStateOf(0) }
    var selectedAvatarId by remember { mutableStateOf(avatarOptions.first().id) }
    var customAvatarUrl by remember { mutableStateOf<String?>(null) }
    var localImageUri by remember { mutableStateOf<Uri?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    fun savePieceSelection(duel: Int, quad: Int) {
        scope.launch {
            isSaving = true
            val preference = encodePiecePreference(duel, quad)
            when (val saveResult = UserRepository.updateCustomization(preferredPieceColor = preference)) {
                is UserResult.Success -> {
                    UserProfileStore.setFromUser(saveResult.data)
                    errorText = null
                }
                is UserResult.Error -> errorText = saveResult.message
            }
            isSaving = false
        }
    }

    fun saveAvatarSelection(avatarIdOrUrl: String) {
        scope.launch {
            isSaving = true
            when (val saveResult = UserRepository.updateCustomization(avatarUrl = avatarIdOrUrl)) {
                is UserResult.Success -> {
                    UserProfileStore.setFromUser(saveResult.data)
                    errorText = null
                }
                is UserResult.Error -> errorText = saveResult.message
            }
            isSaving = false
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult

        // 1. Mostrar la foto de la galería al instante
        localImageUri = uri
        selectedAvatarId = "custom"

        // 2. Subir al servidor y GUARDAR en el perfil del usuario
        scope.launch {
            isSaving = true
            when (val uploadResult = UserRepository.uploadAvatar(context, uri)) {
                is UserResult.Success -> {
                    val nuevaUrl = uploadResult.data
                    customAvatarUrl = nuevaUrl
                    errorText = null

                    // ¡AQUÍ ESTÁ EL ARREGLO!
                    // Vinculamos la nueva imagen al perfil de forma definitiva
                    saveAvatarSelection(nuevaUrl)
                }
                is UserResult.Error -> {
                    errorText = uploadResult.message
                    isSaving = false // Lo quitamos si falla, si va bien lo maneja saveAvatarSelection
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        when (val profileResult = UserRepository.getMe()) {
            is UserResult.Success -> {
                val user = profileResult.data
                UserProfileStore.setFromUser(user)
                val (duel, quad) = decodePiecePreference(user.preferred_piece_color)
                selectedPiece1v1 = duel
                selectedPiece4p = quad

                val preset = avatarOptions.firstOrNull { it.id == user.avatar_url }
                if (preset != null) {
                    selectedAvatarId = preset.id
                } else if (!user.avatar_url.isNullOrBlank()) {
                    selectedAvatarId = "custom"
                    customAvatarUrl = user.avatar_url
                }
                errorText = null
            }
            is UserResult.Error -> {
                errorText = profileResult.message
            }
        }
        isLoading = false
    }

    // ── Layout principal ──
    Box(modifier = Modifier.fillMaxSize()) {

        Image(
            painter = painterResource(id = R.drawable.nuevofondomovil),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryColor)
            }
            return@Box
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(0.25f))

            // ══════════════════════════════════════════════════════════
            // SECCIÓN: FOTO DE PERFIL
            // ══════════════════════════════════════════════════════════

            Image(
                painter = painterResource(id = R.drawable.fotoperfil),
                contentDescription = "Foto de Perfil",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxWidth(0.7f)
            )

            Spacer(modifier = Modifier.weight(0.15f))

            // Avatar actual
            Box(
                modifier = Modifier.size(110.dp),
                contentAlignment = Alignment.Center
            ) {
                val preset = avatarOptions.firstOrNull { it.id == selectedAvatarId }

                when {
                    preset != null -> Image(
                        painter = painterResource(id = preset.drawableRes),
                        contentDescription = preset.label,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(86.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                    localImageUri != null -> AsyncImage(
                        model = localImageUri,
                        contentDescription = "Custom avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(86.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                    !customAvatarUrl.isNullOrBlank() -> AvatarImage(
                        avatarUrl = customAvatarUrl,
                        contentDescription = "Custom avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(86.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        fallback = {
                            Box(
                                modifier = Modifier
                                    .size(86.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(PrimaryColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("?", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                    else -> Box(
                        modifier = Modifier
                            .size(86.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(PrimaryColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("?", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Image(
                    painter = painterResource(id = R.drawable.marcofoto3),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(110.dp)
                )
            }

            Spacer(modifier = Modifier.weight(0.15f))

            // Fila de avatares
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val customImageModel = localImageUri ?: customAvatarUrl
                val maxPresets = if (customImageModel != null) 3 else 4
                val visiblePresets = avatarOptions.takeLast(maxPresets)

                visiblePresets.forEach { option ->
                    AvatarOptionButton(
                        imageRes = option.drawableRes,
                        isSelected = selectedAvatarId == option.id,
                        onClick = {
                            selectedAvatarId = option.id
                            saveAvatarSelection(option.id)
                        }
                    )
                }

                if (customImageModel != null) {
                    CustomAvatarOptionButton(
                        imageModel = customImageModel,
                        isSelected = selectedAvatarId == "custom",
                        onClick = {
                            selectedAvatarId = "custom"
                            if (customAvatarUrl != null) saveAvatarSelection(customAvatarUrl!!)
                        }
                    )
                }

                UploadAvatarButton(
                    onClick = { imagePickerLauncher.launch("image/*") }
                )
            }

            Spacer(modifier = Modifier.weight(0.15f))

            // ══════════════════════════════════════════════════════════
            // SECCIÓN: FICHAS
            // ══════════════════════════════════════════════════════════

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .height(280.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Image(
                    painter = painterResource(id = R.drawable.eleccionfichas2),
                    contentDescription = "Elección de Fichas",
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.fillMaxSize()
                )

                // ── Tableros ──
                Row(
                    modifier = Modifier
                        .fillMaxWidth(0.70f)
                        .offset(y = (-75).dp), // Menos negativo hace que baje
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val style1v1 = pieceStyles1v1.getOrElse(selectedPiece1v1) { pieceStyles1v1[0] }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .offset(x = (-10).dp)
                            .padding(end = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.tablero1v1),
                            contentDescription = "Tablero 1v1",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Column(
                            modifier = Modifier.fillMaxWidth(0.6f).aspectRatio(1f),
                            verticalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(style1v1.sideA).border(1.dp, Color.White.copy(alpha=0.5f), CircleShape))
                                Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(style1v1.sideB).border(1.dp, Color.White.copy(alpha=0.5f), CircleShape))
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(style1v1.sideB).border(1.dp, Color.White.copy(alpha=0.5f), CircleShape))
                                Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(style1v1.sideA).border(1.dp, Color.White.copy(alpha=0.5f), CircleShape))
                            }
                        }
                    }

                    val style4P = pieceStyles4P.getOrElse(selectedPiece4p) { pieceStyles4P[0] }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.tablero1v1),
                            contentDescription = "Tablero 4P",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Column(
                            modifier = Modifier.fillMaxWidth(0.6f).aspectRatio(1f),
                            verticalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(style4P.p1).border(1.dp, Color.White.copy(alpha=0.5f), CircleShape))
                                Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(style4P.p2).border(1.dp, Color.White.copy(alpha=0.5f), CircleShape))
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(style4P.p3).border(1.dp, Color.White.copy(alpha=0.5f), CircleShape))
                                Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(style4P.p4).border(1.dp, Color.White.copy(alpha=0.5f), CircleShape))
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .offset(x = (-10).dp, y = (-30).dp), // Agregado offset X negativo para moverlo a la izquierda
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // ── Selector 1v1 ──
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.seleccioncolores1v1),
                            contentDescription = "Selección colores 1v1",
                            contentScale = ContentScale.FillWidth,
                            modifier = Modifier.fillMaxWidth(0.95f)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(0.9f),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            pieceStyles1v1.forEachIndexed { index, _ ->
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .border(
                                            width = if (selectedPiece1v1 == index) 2.dp else 0.dp,
                                            color = if (selectedPiece1v1 == index) Color.White else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            selectedPiece1v1 = index
                                            savePieceSelection(index, selectedPiece4p)
                                        }
                                )
                            }
                        }
                    }

                    // ── Selector 4P ──
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.seleccioncolores4p),
                            contentDescription = "Selección colores 4P",
                            contentScale = ContentScale.FillWidth,
                            modifier = Modifier.fillMaxWidth(0.95f)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .offset(x = 2.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            pieceStyles4P.forEachIndexed { index, _ ->
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .border(
                                            width = if (selectedPiece4p == index) 2.dp else 0.dp,
                                            color = if (selectedPiece4p == index) Color.White else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            selectedPiece4p = index
                                            savePieceSelection(selectedPiece1v1, index)
                                        }
                                )
                            }
                        }
                    }
                }
            }

            if (isSaving || !errorText.isNullOrBlank()) {
                Spacer(modifier = Modifier.weight(0.1f))
                if (isSaving) {
                    Text("Guardando...", color = Color.White.copy(alpha=0.7f), fontSize = 12.sp)
                }
                if (!errorText.isNullOrBlank()) {
                    Text(errorText ?: "", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.weight(0.5f))

            Image(
                painter = painterResource(id = R.drawable.botonvolvermenu),
                contentDescription = "Volver al menú",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth(0.65f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onNavigate("menu") }
            )

            Spacer(modifier = Modifier.weight(0.35f))
        }
    }
}

// ── Botones de Avatar ───────────────────────────────────────────
@Composable
private fun AvatarOptionButton(imageRes: Int, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = imageRes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
        )
        Image(
            painter = painterResource(id = R.drawable.marcofoto3),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(52.dp)
        )
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(PrimaryColor.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
            )
        }
    }
}

@Composable
private fun CustomAvatarOptionButton(imageModel: Any, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (imageModel is String) {
            AvatarImage(
                avatarUrl = imageModel,
                contentDescription = "Custom avatar option",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
        } else {
            AsyncImage(
                model = imageModel,
                contentDescription = "Custom avatar option",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
        }
        Image(
            painter = painterResource(id = R.drawable.marcofoto3),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(52.dp)
        )
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(PrimaryColor.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
            )
        }
    }
}

@Composable
private fun UploadAvatarButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.marcofoto3),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(52.dp)
        )
        Text(
            text = "+",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
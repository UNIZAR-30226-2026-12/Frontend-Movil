package com.example.random_reversi.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
)

private val pieceStyles4P = listOf(
    PieceStyle4P(Color(0xFF18181B), Color(0xFFF8FAFC), Color(0xFFEF4444), Color(0xFF3B82F6), "Clasico 4P"),
    PieceStyle4P(Color(0xFF22C55E), Color(0xFFFDE047), Color(0xFFA855F7), Color(0xFFF97316), "Jungla Solar"),
    PieceStyle4P(Color(0xFF06B6D4), Color(0xFFF43F5E), Color(0xFF84CC16), Color(0xFFFB7185), "Cyber Pop"),
    PieceStyle4P(Color(0xFFF59E0B), Color(0xFF14B8A6), Color(0xFF8B5CF6), Color(0xFFEF4444), "Magma Frio"),
    PieceStyle4P(Color(0xFF0EA5E9), Color(0xFFFACC15), Color(0xFFEC4899), Color(0xFF10B981), "Tropical RGB"),
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
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            isSaving = true
            when (val uploadResult = UserRepository.uploadAvatar(context, uri)) {
                is UserResult.Success -> {
                    customAvatarUrl = uploadResult.data
                    selectedAvatarId = "custom"
                    errorText = null
                }

                is UserResult.Error -> {
                    errorText = uploadResult.message
                }
            }
            isSaving = false
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

    // ── Layout principal con fondo decorativo ────────────────────────
    Box(modifier = Modifier.fillMaxSize()) {

        // Fondo
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

        // ── Contenido con scroll ─────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // ══════════════════════════════════════════════════════════
            // SECCIÓN: FOTO DE PERFIL
            // ══════════════════════════════════════════════════════════

            // Etiqueta "Foto de Perfil" (PNG)
            Image(
                painter = painterResource(id = R.drawable.fotoperfil),
                contentDescription = "Foto de Perfil",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxWidth(0.7f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Avatar actual dentro del marco decorativo
            Box(
                modifier = Modifier.size(130.dp),
                contentAlignment = Alignment.Center
            ) {
                // Marco decorativo (marcofoto.png)
                Image(
                    painter = painterResource(id = R.drawable.marcofoto),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
                // Avatar del usuario dentro del marco
                val preset = avatarOptions.firstOrNull { it.id == selectedAvatarId }
                when {
                    preset != null -> Image(
                        painter = painterResource(id = preset.drawableRes),
                        contentDescription = preset.label,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(95.dp)
                            .clip(RoundedCornerShape(16.dp))
                    )
                    !customAvatarUrl.isNullOrBlank() -> AsyncImage(
                        model = customAvatarUrl,
                        contentDescription = "Custom avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(95.dp)
                            .clip(RoundedCornerShape(16.dp))
                    )
                    else -> Box(
                        modifier = Modifier
                            .size(95.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(PrimaryColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("?", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Fila de avatares seleccionables
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                avatarOptions.forEach { option ->
                    AvatarOptionButton(
                        imageRes = option.drawableRes,
                        isSelected = selectedAvatarId == option.id,
                        onClick = {
                            selectedAvatarId = option.id
                            saveAvatarSelection(option.id)
                        }
                    )
                }

                if (!customAvatarUrl.isNullOrBlank()) {
                    CustomAvatarOptionButton(
                        imageUrl = customAvatarUrl ?: "",
                        isSelected = selectedAvatarId == "custom",
                        onClick = {
                            selectedAvatarId = "custom"
                            saveAvatarSelection(customAvatarUrl ?: "")
                        }
                    )
                }

                UploadAvatarButton(
                    onClick = { imagePickerLauncher.launch("image/*") }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ══════════════════════════════════════════════════════════
            // SECCIÓN: FICHAS
            // ══════════════════════════════════════════════════════════

            // Etiqueta "Fichas" (PNG)
            Image(
                painter = painterResource(id = R.drawable.fichas),
                contentDescription = "Fichas",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxWidth(0.55f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ── Fichas 1v1 y 4 Jugadores lado a lado ─────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Top
            ) {
                // Columna 1v1
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    // Label "1v1" (PNG)
                    Image(
                        painter = painterResource(id = R.drawable.modo1v1),
                        contentDescription = "1v1",
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier.width(50.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    // Tablero 1v1 (PNG)
                    Image(
                        painter = painterResource(id = R.drawable.tablero1v1),
                        contentDescription = "Tablero 1v1",
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier.size(120.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Columna 4 Jugadores
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    // Label "4 Jugadores" (PNG)
                    Image(
                        painter = painterResource(id = R.drawable.modo4jugadores),
                        contentDescription = "4 Jugadores",
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier.width(130.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    // Tablero 4P (PNG)
                    Image(
                        painter = painterResource(id = R.drawable.tablero4v),
                        contentDescription = "Tablero 4 Jugadores",
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier.size(120.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ── Selector de colores 1v1 (PNG como fondo + botones superpuestos) ──
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.seleccioncolores1v1),
                    contentDescription = "Selección colores 1v1",
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier.fillMaxWidth(0.92f)
                )
                // Botones invisibles superpuestos sobre cada círculo del PNG
                Row(
                    modifier = Modifier.fillMaxWidth(0.82f),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    pieceStyles1v1.forEachIndexed { index, _ ->
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = {
                                        selectedPiece1v1 = index
                                        savePieceSelection(index, selectedPiece4p)
                                    }
                                )
                                .then(
                                    if (selectedPiece1v1 == index) Modifier.background(
                                        PrimaryColor.copy(alpha = 0.3f), CircleShape
                                    ) else Modifier
                                )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ── Selector de colores 4P (PNG como fondo + botones superpuestos) ──
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.seleccioncolores4p),
                    contentDescription = "Selección colores 4P",
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier.fillMaxWidth(0.92f)
                )
                // Botones invisibles superpuestos
                Row(
                    modifier = Modifier.fillMaxWidth(0.82f),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    pieceStyles4P.forEachIndexed { index, _ ->
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = {
                                        selectedPiece4p = index
                                        savePieceSelection(selectedPiece1v1, index)
                                    }
                                )
                                .then(
                                    if (selectedPiece4p == index) Modifier.background(
                                        PrimaryColor.copy(alpha = 0.3f), CircleShape
                                    ) else Modifier
                                )
                        )
                    }
                }
            }

            // ── Estado de guardado / errores ─────────────────────────
            if (isSaving) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = "Guardando cambios...", color = TextMutedColor, fontSize = 12.sp)
            }

            if (!errorText.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorText ?: "",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Botón volver (PNG) ───────────────────────────────────
            Image(
                painter = painterResource(id = R.drawable.botonvolvermenu),
                contentDescription = "Volver al menú",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxWidth(0.55f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onNavigate("menu") }
                    )
            )

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

// ── Botón de avatar preset (marco decorativo estilo polaroid) ────────
@Composable
private fun AvatarOptionButton(imageRes: Int, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // Marco decorativo (marcofoto.png) pequeño
        Image(
            painter = painterResource(id = R.drawable.marcofoto),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )
        // Avatar
        Image(
            painter = painterResource(id = imageRes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
        )
        // Indicador de selección
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(PrimaryColor.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
            )
        }
    }
}

// ── Botón de avatar custom (subido por el usuario) ───────────────────
@Composable
private fun CustomAvatarOptionButton(imageUrl: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.marcofoto),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )
        AsyncImage(
            model = imageUrl,
            contentDescription = "Custom avatar option",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
        )
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(PrimaryColor.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
            )
        }
    }
}

// ── Botón de subir avatar (+) ────────────────────────────────────────
@Composable
private fun UploadAvatarButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.marcofoto),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )
        Text(
            text = "+",
            color = TextColor,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

package com.example.random_reversi.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.random_reversi.R
import com.example.random_reversi.data.UserProfileStore
import com.example.random_reversi.data.UserRepository
import com.example.random_reversi.data.UserResult
import com.example.random_reversi.ui.theme.BgColor
import com.example.random_reversi.ui.theme.BorderColor
import com.example.random_reversi.ui.theme.PrimaryColor
import com.example.random_reversi.ui.theme.SurfaceColor
import com.example.random_reversi.ui.theme.SurfaceLightColor
import com.example.random_reversi.ui.theme.TextColor
import com.example.random_reversi.ui.theme.TextMutedColor
import com.example.random_reversi.utils.AvatarPresets
import kotlinx.coroutines.launch

private data class CustomizationFloatingChip(
    val text: String,
    val startXFraction: Float,
    val startYFraction: Float,
    val durationMs: Int,
    val delayMs: Int
)

private val customizationFloatingChips = listOf(
    CustomizationFloatingChip("\u26AB", 0.10f, 0.10f, 3000, 0),
    CustomizationFloatingChip("\u26AA", 0.85f, 0.15f, 3200, 250),
    CustomizationFloatingChip("\uD83D\uDD34", 0.22f, 0.45f, 2800, 400),
    CustomizationFloatingChip("\uD83D\uDD35", 0.80f, 0.50f, 3100, 600),
    CustomizationFloatingChip("\uD83D\uDFE2", 0.15f, 0.75f, 2900, 800),
    CustomizationFloatingChip("\uD83D\uDFE1", 0.90f, 0.80f, 3300, 1000),
    CustomizationFloatingChip("\uD83D\uDFE3", 0.30f, 0.90f, 3000, 1200),
    CustomizationFloatingChip("\uD83D\uDFE0", 0.75f, 0.30f, 2700, 1400),
    CustomizationFloatingChip("\u2753", 0.55f, 0.20f, 3000, 600),
    CustomizationFloatingChip("\u2753", 0.68f, 0.62f, 3200, 900),
)

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
private fun AnimatedChip(chip: CustomizationFloatingChip, screenHeight: Dp) {
    val transition = rememberInfiniteTransition(label = "floating_chip_${chip.text}")
    val yOffset by transition.animateFloat(
        initialValue = 0f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(
            animation = tween(chip.durationMs, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(chip.delayMs)
        ),
        label = "floating_chip_offset"
    )

    Text(
        text = chip.text,
        fontSize = 28.sp,
        color = TextColor,
        modifier = Modifier
            .offset(x = 280.dp * chip.startXFraction, y = screenHeight * chip.startYFraction + yOffset.dp)
            .alpha(0.28f)
    )
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

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
    ) {
        val screenHeight = maxHeight
        customizationFloatingChips.forEach { chip -> AnimatedChip(chip = chip, screenHeight = screenHeight) }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryColor)
            }
            return@BoxWithConstraints
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Personalizacion",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextColor
            )
            Text(
                text = "Haz que tu estilo sea unico",
                fontSize = 13.sp,
                color = TextMutedColor,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SectionCard(title = "Foto de Perfil") {
                    Text(
                        text = "Vista previa",
                        fontSize = 12.sp,
                        color = TextMutedColor,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    AvatarPreview(
                        selectedAvatarId = selectedAvatarId,
                        customAvatarUrl = customAvatarUrl
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Fotos de perfil para elegir",
                        fontSize = 12.sp,
                        color = TextMutedColor,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

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
                }

                SectionCard(title = "Fichas") {
                    Text(
                        text = "Estilo de fichas (1v1)",
                        fontSize = 12.sp,
                        color = TextMutedColor,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    DuelBoardPreview(styleIndex = selectedPiece1v1)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        pieceStyles1v1.forEachIndexed { index, style ->
                            PieceOptionButtonTwoColors(
                                left = style.sideA,
                                right = style.sideB,
                                isSelected = selectedPiece1v1 == index,
                                onClick = {
                                    selectedPiece1v1 = index
                                    savePieceSelection(index, selectedPiece4p)
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Estilo de fichas (1v1v1v1)",
                        fontSize = 12.sp,
                        color = TextMutedColor,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    QuadBoardPreview(styleIndex = selectedPiece4p)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        pieceStyles4P.forEachIndexed { index, style ->
                            PieceOptionButtonFourColors(
                                c1 = style.p1,
                                c2 = style.p2,
                                c3 = style.p3,
                                c4 = style.p4,
                                isSelected = selectedPiece4p == index,
                                onClick = {
                                    selectedPiece4p = index
                                    savePieceSelection(selectedPiece1v1, index)
                                }
                            )
                        }
                    }
                }
            }

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

            Button(
                onClick = { onNavigate("menu") },
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Volver al menu", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = SurfaceColor,
        border = BorderStroke(1.dp, BorderColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            content = {
                Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextColor)
                Spacer(modifier = Modifier.height(4.dp))
                content()
            }
        )
    }
}

@Composable
private fun AvatarPreview(selectedAvatarId: String, customAvatarUrl: String?) {
    Surface(
        modifier = Modifier.size(84.dp),
        shape = CircleShape,
        color = SurfaceLightColor,
        border = BorderStroke(2.dp, BorderColor)
    ) {
        Box(contentAlignment = Alignment.Center) {
            val preset = avatarOptions.firstOrNull { it.id == selectedAvatarId }
            when {
                preset != null -> {
                    androidx.compose.foundation.Image(
                        painter = painterResource(id = preset.drawableRes),
                        contentDescription = preset.label,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                }

                !customAvatarUrl.isNullOrBlank() -> {
                    AsyncImage(
                        model = customAvatarUrl,
                        contentDescription = "Custom avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                }
            }
        }
    }
}

@Composable
private fun AvatarOptionButton(imageRes: Int, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .size(52.dp)
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = SurfaceLightColor,
        border = BorderStroke(2.dp, if (isSelected) PrimaryColor else BorderColor)
    ) {
        androidx.compose.foundation.Image(
            painter = painterResource(id = imageRes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
        )
    }
}

@Composable
private fun CustomAvatarOptionButton(imageUrl: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .size(52.dp)
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = SurfaceLightColor,
        border = BorderStroke(2.dp, if (isSelected) PrimaryColor else BorderColor)
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = "Custom avatar option",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
        )
    }
}

@Composable
private fun UploadAvatarButton(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .size(52.dp)
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = SurfaceLightColor,
        border = BorderStroke(2.dp, BorderColor)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text = "+", color = TextColor, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun DuelBoardPreview(styleIndex: Int) {
    val style = pieceStyles1v1.getOrElse(styleIndex) { pieceStyles1v1.first() }
    Surface(
        modifier = Modifier.size(112.dp),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF2D6A4F)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            repeat(4) { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    repeat(4) { col ->
                        val isSideA = (row == 1 && col == 1) || (row == 2 && col == 2)
                        val isSideB = (row == 1 && col == 2) || (row == 2 && col == 1)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.12f), RoundedCornerShape(2.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            when {
                                isSideA -> PieceCircle(color = style.sideA)
                                isSideB -> PieceCircle(color = style.sideB)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuadBoardPreview(styleIndex: Int) {
    val style = pieceStyles4P.getOrElse(styleIndex) { pieceStyles4P.first() }
    Surface(
        modifier = Modifier.size(112.dp),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF2D6A4F)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            repeat(4) { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    repeat(4) { col ->
                        val p1 = row == 1 && col == 1
                        val p2 = row == 1 && col == 2
                        val p3 = row == 2 && col == 1
                        val p4 = row == 2 && col == 2
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.12f), RoundedCornerShape(2.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            when {
                                p1 -> PieceCircle(color = style.p1)
                                p2 -> PieceCircle(color = style.p2)
                                p3 -> PieceCircle(color = style.p3)
                                p4 -> PieceCircle(color = style.p4)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PieceCircle(color: Color) {
    Box(
        modifier = Modifier
            .size(14.dp)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
private fun PieceOptionButtonTwoColors(
    left: Color,
    right: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .size(44.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(2.dp, if (isSelected) PrimaryColor else BorderColor),
        shadowElevation = if (isSelected) 8.dp else 0.dp
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f).fillMaxSize().background(left))
            Box(modifier = Modifier.weight(1f).fillMaxSize().background(right))
        }
    }
}

@Composable
private fun PieceOptionButtonFourColors(
    c1: Color,
    c2: Color,
    c3: Color,
    c4: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .size(44.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(2.dp, if (isSelected) PrimaryColor else BorderColor),
        shadowElevation = if (isSelected) 8.dp else 0.dp
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                Box(modifier = Modifier.weight(1f).fillMaxSize().background(c1))
                Box(modifier = Modifier.weight(1f).fillMaxSize().background(c2))
            }
            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                Box(modifier = Modifier.weight(1f).fillMaxSize().background(c3))
                Box(modifier = Modifier.weight(1f).fillMaxSize().background(c4))
            }
        }
    }
}

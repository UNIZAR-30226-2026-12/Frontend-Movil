package com.example.random_reversi.utils

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage

object AvatarUtils {

    private const val DATA_URI_PREFIX = "data:"

    fun isDataUri(value: String?): Boolean =
        !value.isNullOrBlank() && value.startsWith(DATA_URI_PREFIX)

    fun decodeDataUriToBytes(dataUri: String): ByteArray? {
        val commaIndex = dataUri.indexOf(',')
        if (commaIndex < 0) return null
        val header = dataUri.substring(0, commaIndex)
        val payload = dataUri.substring(commaIndex + 1)
        if (!header.contains(";base64")) return null
        return runCatching { Base64.decode(payload, Base64.DEFAULT) }.getOrNull()
    }
}

@Composable
fun AvatarImage(
    avatarUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    fallback: @Composable () -> Unit = {}
) {
    val presetRes = AvatarPresets.drawableForId(avatarUrl)
    when {
        presetRes != null -> Image(
            painter = painterResource(id = presetRes),
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier
        )
        AvatarUtils.isDataUri(avatarUrl) -> {
            val bitmap = remember(avatarUrl) {
                AvatarUtils.decodeDataUriToBytes(avatarUrl!!)?.let { bytes ->
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                }
            }
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = contentDescription,
                    contentScale = contentScale,
                    modifier = modifier
                )
            } else {
                fallback()
            }
        }
        !avatarUrl.isNullOrBlank() -> {
            val fullUrl = if (avatarUrl.startsWith("http")) {
                avatarUrl
            } else {
                val baseUrl = com.example.random_reversi.BuildConfig.API_BASE_URL.trimEnd('/')
                val path = if (avatarUrl.startsWith("/")) avatarUrl else "/$avatarUrl"
                "$baseUrl$path"
            }
            AsyncImage(
                model = fullUrl,
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = modifier
            )
        }
        else -> fallback()
    }
}

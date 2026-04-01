package com.example.random_reversi.utils

import com.example.random_reversi.R

data class AvatarPreset(
    val id: String,
    val label: String,
    val drawableRes: Int
)

object AvatarPresets {
    val options = listOf(
        AvatarPreset("blackice", "Black Ice", R.drawable.blackice),
        AvatarPreset("bluefire", "Blue Fire", R.drawable.bluefire),
        AvatarPreset("whitegrass", "White Grass", R.drawable.whitegrass),
        AvatarPreset("purplesun", "Purple Sun", R.drawable.purplesun),
    )

    fun drawableForId(id: String?): Int? {
        if (id.isNullOrBlank()) return null
        return options.firstOrNull { it.id == id }?.drawableRes
    }
}

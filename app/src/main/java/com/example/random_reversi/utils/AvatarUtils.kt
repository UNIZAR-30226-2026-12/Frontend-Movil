package com.example.random_reversi.utils

//import com.example.random_reversi.R

object AvatarUtils {
    /**
     * Lista de IDs de recursos correspondientes a las imágenes de avatar.
     * Asegúrate de copiar tus archivos (purplesun.png, bluefire.png, etc.) 
     * a app/src/main/res/drawable/
     */
    /*
    private val AVATARS = listOf(
        R.drawable.purplesun,
        R.drawable.bluefire,
        R.drawable.whitegrass,
        R.drawable.blackice
    ) */

    /**
     * Traduce la lógica de JS: (hash * 31 + charCode) >>> 0
     * En Kotlin/Java, String.hashCode() ya implementa (hash * 31 + char),
     * y usamos .toUInt() para simular el desplazamiento sin signo (>>> 0).
     */
    /*fun getAvatarFromSeed(seed: Any): Int {
        val value = seed.toString()
        val hash = value.hashCode().toUInt()
        val index = (hash % AVATARS.size.toUInt()).toInt()
        return AVATARS[index]
    }*/
}

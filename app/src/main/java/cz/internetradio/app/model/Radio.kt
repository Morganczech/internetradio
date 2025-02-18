package cz.internetradio.app.model

import androidx.compose.ui.graphics.Color

data class Radio(
    val id: String,
    val name: String,
    val streamUrl: String,
    val imageUrl: String,
    val description: String,
    val category: RadioCategory,
    val originalCategory: RadioCategory? = null,
    val startColor: Color,
    val endColor: Color,
    val isFavorite: Boolean = false,
    val bitrate: Int? = null
) 
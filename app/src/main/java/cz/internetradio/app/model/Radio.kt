package cz.internetradio.app.model

import androidx.compose.ui.graphics.Color

data class Radio(
    val id: String,
    val name: String,
    val streamUrl: String,
    val imageUrl: String,
    val description: String? = null,
    val startColor: Color = Color(0xFF1A1A1A),
    val endColor: Color = Color(0xFF2D2D2D)
) 
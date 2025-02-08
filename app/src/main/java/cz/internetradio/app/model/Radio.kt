package cz.internetradio.app.model

import androidx.annotation.DrawableRes

data class Radio(
    val id: String,
    val name: String,
    val streamUrl: String,
    @DrawableRes val imageResId: Int,
    val description: String? = null
) 
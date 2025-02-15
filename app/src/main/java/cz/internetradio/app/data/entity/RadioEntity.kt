package cz.internetradio.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import cz.internetradio.app.model.Radio
import androidx.compose.ui.graphics.Color
import cz.internetradio.app.model.RadioCategory
import androidx.compose.ui.graphics.toArgb

@Entity(tableName = "radios")
data class RadioEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val streamUrl: String,
    val imageUrl: String,
    val description: String,
    val category: RadioCategory,
    val originalCategory: RadioCategory?,
    val startColor: Int,
    val endColor: Int,
    val isFavorite: Boolean
) {
    fun toRadio(): Radio = Radio(
        id = id,
        name = name,
        streamUrl = streamUrl,
        imageUrl = imageUrl,
        description = description,
        category = category,
        originalCategory = originalCategory,
        startColor = Color(startColor),
        endColor = Color(endColor),
        isFavorite = isFavorite
    )

    companion object {
        fun fromRadio(radio: Radio): RadioEntity = RadioEntity(
            id = radio.id,
            name = radio.name,
            streamUrl = radio.streamUrl,
            imageUrl = radio.imageUrl,
            description = radio.description,
            category = radio.category,
            originalCategory = radio.originalCategory,
            startColor = radio.startColor.toArgb(),
            endColor = radio.endColor.toArgb(),
            isFavorite = radio.isFavorite
        )
    }
} 
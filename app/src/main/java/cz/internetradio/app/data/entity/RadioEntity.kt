package cz.internetradio.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import cz.internetradio.app.model.Radio
import androidx.compose.ui.graphics.Color
import cz.internetradio.app.model.RadioCategory

@Entity(tableName = "radio_stations")
data class RadioEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val streamUrl: String,
    val imageUrl: String,
    val description: String?,
    val startColorValue: Long,
    val endColorValue: Long,
    val isFavorite: Boolean = false,
    val category: String = RadioCategory.MISTNI.name
) {
    fun toRadio(): Radio = Radio(
        id = id,
        name = name,
        streamUrl = streamUrl,
        imageUrl = imageUrl,
        description = description,
        startColor = Color(startColorValue.toULong()),
        endColor = Color(endColorValue.toULong()),
        isFavorite = isFavorite,
        category = RadioCategory.valueOf(category)
    )

    companion object {
        fun fromRadio(radio: Radio): RadioEntity = RadioEntity(
            id = radio.id,
            name = radio.name,
            streamUrl = radio.streamUrl,
            imageUrl = radio.imageUrl,
            description = radio.description,
            startColorValue = radio.startColor.value.toLong(),
            endColorValue = radio.endColor.value.toLong(),
            isFavorite = radio.isFavorite,
            category = radio.category.name
        )
    }
} 
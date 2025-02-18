package cz.internetradio.app.model

import kotlinx.serialization.Serializable
import cz.internetradio.app.data.entity.RadioEntity

@Serializable
data class AppSettings(
    val maxFavorites: Int,
    val equalizerEnabled: Boolean,
    val fadeOutDuration: Int,
    val favoriteStations: List<SerializableRadio>,
    val favoriteSongs: List<SerializableSong>
)

@Serializable
data class SerializableRadio(
    val id: String,
    val name: String,
    val streamUrl: String,
    val imageUrl: String,
    val description: String,
    val category: String,
    val originalCategory: String?,
    val startColor: Int,
    val endColor: Int,
    val isFavorite: Boolean,
    val bitrate: Int?
) {
    companion object {
        fun fromRadioEntity(entity: RadioEntity): SerializableRadio = SerializableRadio(
            id = entity.id,
            name = entity.name,
            streamUrl = entity.streamUrl,
            imageUrl = entity.imageUrl,
            description = entity.description,
            category = entity.category.name,
            originalCategory = entity.originalCategory?.name,
            startColor = entity.startColor,
            endColor = entity.endColor,
            isFavorite = entity.isFavorite,
            bitrate = entity.bitrate
        )

        fun toRadioEntity(serializable: SerializableRadio): RadioEntity = RadioEntity(
            id = serializable.id,
            name = serializable.name,
            streamUrl = serializable.streamUrl,
            imageUrl = serializable.imageUrl,
            description = serializable.description,
            category = RadioCategory.valueOf(serializable.category),
            originalCategory = serializable.originalCategory?.let { RadioCategory.valueOf(it) },
            startColor = serializable.startColor,
            endColor = serializable.endColor,
            isFavorite = serializable.isFavorite,
            bitrate = serializable.bitrate
        )
    }
}

@Serializable
data class SerializableSong(
    val title: String,
    val artist: String?,
    val radioName: String,
    val radioId: String
) 
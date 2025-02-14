package cz.internetradio.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "favorite_songs")
data class FavoriteSong(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val title: String,
    
    val artist: String?,
    
    @ColumnInfo(name = "radio_name")
    val radioName: String,
    
    @ColumnInfo(name = "radio_id")
    val radioId: String,
    
    @ColumnInfo(name = "added_at")
    val addedAt: Long = System.currentTimeMillis(),
    
    val category: String? = null
) {
    fun toModel() = cz.internetradio.app.model.FavoriteSong(
        id = id,
        title = title,
        artist = artist,
        radioName = radioName,
        radioId = radioId,
        addedAt = addedAt,
        category = category
    )

    companion object {
        fun fromModel(model: cz.internetradio.app.model.FavoriteSong) = FavoriteSong(
            id = model.id,
            title = model.title,
            artist = model.artist,
            radioName = model.radioName,
            radioId = model.radioId,
            addedAt = model.addedAt,
            category = model.category
        )
    }
} 
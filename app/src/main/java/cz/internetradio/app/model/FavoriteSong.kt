package cz.internetradio.app.model

data class FavoriteSong(
    val id: Long = 0,
    val title: String,
    val artist: String?,
    val radioName: String,
    val radioId: String,
    val addedAt: Long = System.currentTimeMillis()
) 
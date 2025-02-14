package cz.internetradio.app.model

data class SongData(
    val title: String,
    val artist: String,
    var youtubeVideoId: String? = null,
    var spotifyTrackId: String? = null
) 
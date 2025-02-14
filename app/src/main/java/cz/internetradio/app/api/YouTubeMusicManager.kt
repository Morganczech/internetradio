package cz.internetradio.app.api

import android.content.Context
import android.content.Intent
import android.net.Uri
import cz.internetradio.app.model.FavoriteSong
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YouTubeMusicManager @Inject constructor(
    private val youTubeMusicApi: YouTubeMusicApi
) {
    suspend fun exportToPlaylist(songs: List<FavoriteSong>): Boolean {
        try {
            // Vytvoření nového playlistu
            val playlistName = "Internet Radio - Oblíbené skladby"
            val playlistDescription = "Exportováno z aplikace Internet Radio"
            val playlistId = youTubeMusicApi.createPlaylist(playlistName, playlistDescription) ?: return false

            // Přidání skladeb do playlistu
            songs.forEach { song ->
                val videoId = youTubeMusicApi.searchSong(
                    artist = song.artist ?: "",
                    title = song.title
                ) ?: return@forEach

                youTubeMusicApi.addToPlaylist(playlistId, videoId)
            }

            return true
        } catch (e: Exception) {
            return false
        }
    }

    suspend fun playOnYouTube(song: FavoriteSong, context: Context) {
        val videoId = youTubeMusicApi.searchSong(
            artist = song.artist ?: "",
            title = song.title
        ) ?: return

        // Otevření videa v YouTube aplikaci nebo prohlížeči
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=$videoId"))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
} 
package cz.internetradio.app.api

import android.content.Context
import android.content.Intent
import android.net.Uri
import cz.internetradio.app.config.ApiConfig
import cz.internetradio.app.model.SongData
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpotifyApi @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    suspend fun findSongOnSpotify(songData: SongData): String? {
        // Pokud už máme uložené trackId, použijeme ho
        if (songData.spotifyTrackId != null) {
            return getSpotifyUrl(songData.spotifyTrackId!!)
        }

        return try {
            val query = "${songData.title} artist:${songData.artist}"
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            
            val request = Request.Builder()
                .url("https://api.spotify.com/v1/search?q=$encodedQuery&type=track&limit=1")
                .header("Authorization", "Bearer ${ApiConfig.SPOTIFY_API_TOKEN}")
                .build()

            val response = okHttpClient.newCall(request).execute()
            val jsonResponse = JSONObject(response.body?.string() ?: return null)

            val tracks = jsonResponse.getJSONObject("tracks")
            val items = tracks.getJSONArray("items")
            
            if (items.length() > 0) {
                val track = items.getJSONObject(0)
                val trackId = track.getString("id")
                songData.spotifyTrackId = trackId
                getSpotifyUrl(trackId)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun getSpotifyUrl(trackId: String): String {
        return "https://open.spotify.com/track/$trackId"
    }

    fun openInSpotify(context: Context, url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
} 
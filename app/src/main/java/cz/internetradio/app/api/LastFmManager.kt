package cz.internetradio.app.api

import cz.internetradio.app.config.ApiConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LastFmManager @Inject constructor(
    private val lastFmApi: LastFmApi
) {
    suspend fun getAlbumArtUrl(artist: String?, track: String): String? {
        if (artist == null) return null
        
        return try {
            val response = lastFmApi.getTrackInfo(
                apiKey = ApiConfig.LASTFM_API_KEY,
                artist = artist,
                track = track
            )
            
            response.track?.album?.image?.lastOrNull { it.size == "large" }?.text
        } catch (e: Exception) {
            null
        }
    }
} 
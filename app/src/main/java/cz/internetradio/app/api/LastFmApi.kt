package cz.internetradio.app.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Query

interface LastFmApi {
    @GET("2.0/")
    suspend fun getTrackInfo(
        @Query("method") method: String = "track.getInfo",
        @Query("api_key") apiKey: String,
        @Query("artist") artist: String,
        @Query("track") track: String,
        @Query("format") format: String = "json"
    ): LastFmResponse
}

@JsonClass(generateAdapter = true)
data class LastFmResponse(
    val track: Track?
)

@JsonClass(generateAdapter = true)
data class Track(
    val album: Album?
)

@JsonClass(generateAdapter = true)
data class Album(
    val image: List<Image>?
)

@JsonClass(generateAdapter = true)
data class Image(
    val size: String,
    @Json(name = "#text") val text: String
) 
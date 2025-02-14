package cz.internetradio.app.api

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.youtube.YouTube
import com.google.api.services.youtube.model.SearchListResponse
import cz.internetradio.app.config.ApiConfig
import cz.internetradio.app.model.SongData
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YouTubeMusicApi @Inject constructor() {
    private val youtube: YouTube by lazy {
        YouTube.Builder(
            GoogleNetHttpTransport.newTrustedTransport(),
            GsonFactory.getDefaultInstance(),
            null
        )
        .setApplicationName("InternetRadio")
        .build()
    }

    suspend fun findSongOnYouTubeMusic(songData: SongData): String? {
        // Pokud už máme uložené videoId, použijeme ho
        if (songData.youtubeVideoId != null) {
            return getYoutubeMusicUrl(songData.youtubeVideoId!!)
        }

        // Jinak vyhledáme skladbu
        val videoId = searchSong(songData.artist, songData.title)
        songData.youtubeVideoId = videoId
        return videoId?.let { getYoutubeMusicUrl(it) }
    }

    private suspend fun searchSong(artist: String, title: String): String? {
        return try {
            // Upřesnění dotazu pro YouTube Music
            val query = if (artist.isNotBlank()) {
                "$artist - $title official music video"
            } else {
                "$title song"
            }

            val search = youtube.search().list(listOf("id", "snippet"))
                .setKey(ApiConfig.YOUTUBE_API_KEY)
                .setQ(query)
                .setType(listOf("video"))
                .setVideoCategoryId("10") // Hudební kategorie
                .setRelevanceLanguage("cs") // Nastavíme jazyk na češtinu
                .setRegionCode("CZ") // Nastavíme region na Českou republiku
                .setMaxResults(1)
                .execute()

            search.items?.firstOrNull()?.id?.videoId
        } catch (e: Exception) {
            null
        }
    }

    private fun getYoutubeMusicUrl(videoId: String): String {
        return "https://music.youtube.com/watch?v=$videoId"
    }

    fun openInYouTubeMusic(context: Context, url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    suspend fun createPlaylist(name: String, description: String): String? {
        return try {
            val playlist = youtube.playlists().insert(
                listOf("snippet", "status"),
                com.google.api.services.youtube.model.Playlist().apply {
                    setSnippet(
                        com.google.api.services.youtube.model.PlaylistSnippet().apply {
                            setTitle(name)
                            setDescription(description)
                        }
                    )
                    setStatus(
                        com.google.api.services.youtube.model.PlaylistStatus().apply {
                            setPrivacyStatus("private")
                        }
                    )
                }
            ).execute()

            playlist.id
        } catch (e: Exception) {
            null
        }
    }

    suspend fun addToPlaylist(playlistId: String, videoId: String): Boolean {
        return try {
            youtube.playlistItems().insert(
                listOf("snippet"),
                com.google.api.services.youtube.model.PlaylistItem().apply {
                    setSnippet(
                        com.google.api.services.youtube.model.PlaylistItemSnippet().apply {
                            setPlaylistId(playlistId)
                            setResourceId(
                                com.google.api.services.youtube.model.ResourceId().apply {
                                    setKind("youtube#video")
                                    setVideoId(videoId)
                                }
                            )
                        }
                    )
                }
            ).execute()
            true
        } catch (e: Exception) {
            false
        }
    }
} 
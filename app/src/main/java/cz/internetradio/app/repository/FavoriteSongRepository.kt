package cz.internetradio.app.repository

import cz.internetradio.app.data.dao.FavoriteSongDao
import cz.internetradio.app.data.entity.FavoriteSong
import cz.internetradio.app.model.FavoriteSong as FavoriteSongModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavoriteSongRepository @Inject constructor(
    private val favoriteSongDao: FavoriteSongDao
) {
    fun getAllSongs(): Flow<List<FavoriteSongModel>> {
        return favoriteSongDao.getAllSongs().map { songs ->
            songs.map { it.toModel() }
        }
    }

    fun getSongsByRadio(radioId: String): Flow<List<FavoriteSongModel>> {
        return favoriteSongDao.getSongsByRadio(radioId).map { songs ->
            songs.map { it.toModel() }
        }
    }

    suspend fun addSong(song: FavoriteSongModel): Long {
        return favoriteSongDao.insertSong(FavoriteSong.fromModel(song))
    }

    suspend fun updateSong(song: FavoriteSongModel) {
        favoriteSongDao.updateSong(FavoriteSong.fromModel(song))
    }

    suspend fun deleteSong(song: FavoriteSongModel) {
        favoriteSongDao.deleteSong(FavoriteSong.fromModel(song))
    }

    suspend fun deleteSongById(songId: Long) {
        favoriteSongDao.deleteSongById(songId)
    }

    suspend fun songExists(title: String, artist: String?, radioId: String): Boolean {
        return favoriteSongDao.songExists(title, artist, radioId)
    }

    suspend fun deleteAllSongs() {
        favoriteSongDao.deleteAllSongs()
    }
} 
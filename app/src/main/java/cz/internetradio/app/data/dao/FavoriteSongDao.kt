package cz.internetradio.app.data.dao

import androidx.room.*
import cz.internetradio.app.data.entity.FavoriteSong
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteSongDao {
    @Query("SELECT * FROM favorite_songs ORDER BY added_at DESC")
    fun getAllSongs(): Flow<List<FavoriteSong>>

    @Query("SELECT * FROM favorite_songs WHERE radio_id = :radioId ORDER BY added_at DESC")
    fun getSongsByRadio(radioId: String): Flow<List<FavoriteSong>>

    @Query("SELECT * FROM favorite_songs WHERE id = :songId")
    suspend fun getSongById(songId: Long): FavoriteSong?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: FavoriteSong): Long

    @Update
    suspend fun updateSong(song: FavoriteSong)

    @Delete
    suspend fun deleteSong(song: FavoriteSong)

    @Query("DELETE FROM favorite_songs WHERE id = :songId")
    suspend fun deleteSongById(songId: Long)

    @Query("""
        SELECT EXISTS(
            SELECT 1 FROM favorite_songs 
            WHERE title = :title 
            AND (artist IS NULL OR artist = :artist)
            AND radio_id = :radioId
        )
    """)
    suspend fun songExists(title: String, artist: String?, radioId: String): Boolean
} 
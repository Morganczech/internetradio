package cz.internetradio.app.data.dao

import androidx.room.*
import cz.internetradio.app.data.entity.RadioEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RadioDao {
    @Query("SELECT * FROM radio_stations")
    fun getAllRadios(): Flow<List<RadioEntity>>

    @Query("SELECT * FROM radio_stations WHERE isFavorite = 1")
    fun getFavoriteRadios(): Flow<List<RadioEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRadio(radio: RadioEntity)

    @Update
    suspend fun updateRadio(radio: RadioEntity)

    @Query("UPDATE radio_stations SET isFavorite = :isFavorite WHERE id = :radioId")
    suspend fun updateFavoriteStatus(radioId: String, isFavorite: Boolean)

    @Query("SELECT COUNT(*) FROM radio_stations")
    suspend fun getStationCount(): Int

    @Delete
    suspend fun deleteRadio(radio: RadioEntity)
} 
package cz.internetradio.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RadioDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRadio(radio: Radio)

    @Query("SELECT * FROM radios")
    fun getAllRadios(): Flow<List<Radio>>

    @Delete
    suspend fun deleteRadio(radio: Radio)
} 
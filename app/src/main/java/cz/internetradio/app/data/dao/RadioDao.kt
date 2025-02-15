package cz.internetradio.app.data.dao

import androidx.room.*
import cz.internetradio.app.data.entity.RadioEntity
import kotlinx.coroutines.flow.Flow
import cz.internetradio.app.model.RadioCategory

@Dao
interface RadioDao {
    @Query("SELECT * FROM radios")
    fun getAllRadios(): Flow<List<RadioEntity>>

    @Query("SELECT * FROM radios WHERE isFavorite = 1")
    fun getFavoriteRadios(): Flow<List<RadioEntity>>

    @Query("SELECT * FROM radios WHERE category = :category")
    fun getRadiosByCategory(category: RadioCategory): Flow<List<RadioEntity>>

    @Query("SELECT * FROM radios WHERE id = :radioId")
    suspend fun getRadioById(radioId: String): RadioEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRadio(radio: RadioEntity)

    @Delete
    suspend fun deleteRadio(radio: RadioEntity)

    @Query("UPDATE radios SET isFavorite = :isFavorite WHERE id = :radioId")
    suspend fun updateFavoriteStatus(radioId: String, isFavorite: Boolean)

    @Query("UPDATE radios SET isFavorite = :isFavorite, category = :category WHERE id = :radioId")
    suspend fun updateFavoriteStatusAndCategory(radioId: String, isFavorite: Boolean, category: RadioCategory)

    @Query("SELECT EXISTS(SELECT 1 FROM radios WHERE streamUrl = :streamUrl)")
    suspend fun existsByStreamUrl(streamUrl: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM radios WHERE name = :name)")
    suspend fun existsByName(name: String): Boolean
} 
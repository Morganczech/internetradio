package cz.internetradio.app.repository

import javax.inject.Inject
import javax.inject.Singleton
import cz.internetradio.app.data.dao.RadioDao
import cz.internetradio.app.data.entity.RadioEntity
import cz.internetradio.app.api.RadioBrowserApi
import cz.internetradio.app.api.SearchParams
import cz.internetradio.app.model.Radio
import cz.internetradio.app.model.RadioStation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import cz.internetradio.app.model.RadioCategory
import androidx.compose.ui.graphics.toArgb
import android.util.Log

@Singleton
class RadioRepository @Inject constructor(
    private val radioDao: RadioDao,
    private val radioBrowserApi: RadioBrowserApi
) {
    fun getAllRadios(): Flow<List<Radio>> {
        return radioDao.getAllRadios().map { entities ->
            entities.map { it.toRadio() }
        }
    }

    suspend fun searchStations(params: SearchParams): List<RadioStation>? {
        return radioBrowserApi.searchStations(params)
    }

    suspend fun getStationsByTag(tag: String): List<RadioStation>? {
        return radioBrowserApi.getStationsByTag(tag)
    }

    suspend fun getStationsByCountry(country: String): List<RadioStation>? {
        // Načteme stanice z databáze v kategorii MISTNI
        val entities = radioDao.getRadiosByCategory(RadioCategory.MISTNI).first()
        return entities.map { entity ->
            RadioStation(
                stationuuid = entity.id,
                name = entity.name,
                url = entity.streamUrl,
                url_resolved = entity.streamUrl,
                favicon = entity.imageUrl,
                tags = entity.description,
                category = entity.category,
                isFromRadioBrowser = false
            )
        }
    }

    suspend fun getTags(): List<String>? {
        return radioBrowserApi.getTags()?.map { it.name }
    }

    fun getFavoriteRadios(): Flow<List<Radio>> {
        return radioDao.getFavoriteRadios().map { entities ->
            entities.map { it.toRadio() }
        }
    }

    fun getRadiosByCategory(category: RadioCategory): Flow<List<Radio>> {
        return radioDao.getRadiosByCategory(category).map { entities ->
            entities.map { it.toRadio() }
        }
    }

    suspend fun toggleFavorite(radioId: String) {
        val radio = radioDao.getRadioById(radioId)?.toRadio()
        radio?.let {
            if (!it.isFavorite) {
                // Při přidání do oblíbených zachováme původní kategorii
                val updatedRadio = it.copy(
                    isFavorite = true
                )
                radioDao.insertRadio(RadioEntity.fromRadio(updatedRadio))
            } else {
                // Při odebrání z oblíbených pouze zrušíme příznak oblíbené
                val updatedRadio = it.copy(
                    isFavorite = false
                )
                radioDao.insertRadio(RadioEntity.fromRadio(updatedRadio))
            }
        }
    }

    suspend fun removeFavorite(radioId: String) {
        val radio = radioDao.getRadioById(radioId)?.toRadio()
        radio?.let {
            // Při odebrání z oblíbených pouze zrušíme příznak oblíbené
            val updatedRadio = it.copy(
                isFavorite = false
            )
            radioDao.insertRadio(RadioEntity.fromRadio(updatedRadio))
        }
    }

    suspend fun getRadioById(radioId: String): Radio? {
        return radioDao.getRadioById(radioId)?.toRadio()
    }

    suspend fun addRadioStationToFavorites(radioStation: RadioStation, category: RadioCategory) {
        val streamUrl = radioStation.url_resolved ?: radioStation.url
        
        // Kontrola, zda již existuje stanice se stejnou URL (kontrolujeme obě možné URL)
        val existingRadio = radioDao.getAllRadios().first().find { entity -> 
            val entityRadio = entity.toRadio()
            entityRadio.streamUrl == streamUrl || 
            entityRadio.streamUrl == radioStation.url ||
            (radioStation.url_resolved != null && entityRadio.streamUrl == radioStation.url_resolved)
        }?.toRadio()

        if (existingRadio != null) {
            // Pokud stanice existuje, zachováme její nastavení oblíbenosti a ID
            val updatedRadio = existingRadio.copy(
                name = radioStation.name,
                imageUrl = radioStation.favicon ?: existingRadio.imageUrl,
                description = radioStation.tags ?: existingRadio.description,
                category = category,
                originalCategory = if (existingRadio.isFavorite) existingRadio.originalCategory else category,
                startColor = category.startColor,
                endColor = category.endColor,
                bitrate = try {
                    radioStation.bitrate?.toInt()
                } catch (e: NumberFormatException) {
                    Log.w("RadioRepository", "Nepodařilo se převést bitrate na číslo: ${radioStation.bitrate}")
                    null
                }
            )
            radioDao.insertRadio(RadioEntity.fromRadio(updatedRadio))
        } else {
            // Pokud stanice neexistuje, vytvoříme nový záznam
            val radio = Radio(
                id = radioStation.stationuuid ?: radioStation.url,
                name = radioStation.name,
                streamUrl = streamUrl,
                imageUrl = radioStation.favicon ?: "android.resource://cz.internetradio.app/drawable/ic_radio_default",
                description = radioStation.tags ?: "",
                category = category,
                originalCategory = category,
                startColor = category.startColor,
                endColor = category.endColor,
                isFavorite = false,
                bitrate = try {
                    radioStation.bitrate?.toInt()
                } catch (e: NumberFormatException) {
                    Log.w("RadioRepository", "Nepodařilo se převést bitrate na číslo: ${radioStation.bitrate}")
                    null
                }
            )
            radioDao.insertRadio(RadioEntity.fromRadio(radio))
        }
    }

    suspend fun removeStation(radioId: String) {
        radioDao.getRadioById(radioId)?.let { radioEntity ->
            radioDao.deleteRadio(radioEntity)
        }
    }

    suspend fun insertRadio(radio: Radio) {
        radioDao.insertRadio(RadioEntity.fromRadio(radio))
    }

    suspend fun deleteRadio(radio: Radio) {
        radioDao.deleteRadio(RadioEntity.fromRadio(radio))
    }

    suspend fun existsByStreamUrl(streamUrl: String): Boolean {
        return radioDao.existsByStreamUrl(streamUrl)
    }

    suspend fun existsByName(name: String): Boolean {
        return radioDao.existsByName(name)
    }
} 
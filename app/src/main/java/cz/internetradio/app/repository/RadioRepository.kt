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
import com.google.gson.Gson
import cz.internetradio.app.ui.theme.Gradients
import cz.internetradio.app.BuildConfig

@Singleton
class RadioRepository @Inject constructor(
    private val radioDao: RadioDao,
    private val radioBrowserApi: RadioBrowserApi
) {
    suspend fun existsByName(name: String): Boolean {
        return radioDao.existsByName(name)
    }

    suspend fun existsByStreamUrl(url: String): Boolean {
        return radioDao.existsByStreamUrl(url)
    }

    suspend fun initializeFromApi(countryCode: String): Boolean {
        return try {
            val stations = radioBrowserApi.searchStations(
                SearchParams(
                    name = "", // Search by country only
                    country = countryCode,
                    limit = 10,
                    orderBy = "clickcount"
                )
            )

            if (stations.isNullOrEmpty()) return false

            val currentMaxOrder = getNextOrderIndex(RadioCategory.MISTNI)
            stations.forEachIndexed { index, station ->
                val streamUrl = station.url_resolved ?: station.url
                val radio = Radio(
                    id = station.stationuuid ?: station.url,
                    name = station.name,
                    streamUrl = streamUrl,
                    imageUrl = station.favicon ?: "android.resource://cz.internetradio.app/drawable/ic_radio_default",
                    description = station.tags ?: "",
                    category = RadioCategory.MISTNI,
                    originalCategory = RadioCategory.MISTNI,
                    startColor = Gradients.getGradientForCategory(RadioCategory.MISTNI).first,
                    endColor = Gradients.getGradientForCategory(RadioCategory.MISTNI).second,
                    isFavorite = false,
                    bitrate = station.bitrate?.toString()?.toIntOrNull()
                )
                val entity = RadioEntity.fromRadio(radio).copy(orderIndex = currentMaxOrder + index)
                radioDao.insertRadio(entity)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getAllRadios(): Flow<List<Radio>> {
        return radioDao.getAllRadios().map { entities ->
            entities.map { it.toRadio() }
        }
    }

    suspend fun searchStations(params: SearchParams): List<RadioStation>? {
        return radioBrowserApi.searchStations(params)
    }

    suspend fun getStationsByCountry(country: String): List<RadioStation>? {
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
            val updatedRadio = it.copy(isFavorite = !it.isFavorite)
            radioDao.insertRadio(RadioEntity.fromRadio(updatedRadio))
        }
    }

    suspend fun removeFavorite(radioId: String) {
        radioDao.getRadioById(radioId)?.let { entity ->
            val updated = entity.copy(isFavorite = false)
            radioDao.insertRadio(updated)
        }
    }

    suspend fun getRadioById(radioId: String): Radio? {
        return radioDao.getRadioById(radioId)?.toRadio()
    }

    suspend fun addStationToFavorites(radioStation: RadioStation, category: RadioCategory, isFavorite: Boolean = true) {
        val streamUrl = radioStation.url_resolved ?: radioStation.url
        val existingRadio = radioDao.getAllRadios().first().find { it.streamUrl == streamUrl }
        
        if (existingRadio != null) {
            val updated = existingRadio.copy(category = category, isFavorite = isFavorite)
            radioDao.insertRadio(updated)
        } else {
            val gradient = Gradients.getGradientForCategory(category)
            val nextOrder = getNextOrderIndex(category)
            val radio = Radio(
                id = radioStation.stationuuid ?: radioStation.url,
                name = radioStation.name,
                streamUrl = streamUrl,
                imageUrl = radioStation.favicon ?: "android.resource://cz.internetradio.app/drawable/ic_radio_default",
                description = radioStation.tags ?: "",
                category = category,
                originalCategory = category,
                startColor = gradient.first,
                endColor = gradient.second,
                isFavorite = isFavorite,
                bitrate = radioStation.bitrate?.toString()?.toIntOrNull()
            )
            val entity = RadioEntity.fromRadio(radio).copy(orderIndex = nextOrder)
            radioDao.insertRadio(entity)
        }
    }

    suspend fun removeStation(radioId: String) {
        radioDao.getRadioById(radioId)?.let { radioDao.deleteRadio(it) }
    }

    suspend fun insertRadio(radio: Radio) {
        val nextOrder = getNextOrderIndex(radio.category)
        val entity = RadioEntity.fromRadio(radio).copy(orderIndex = nextOrder)
        radioDao.insertRadio(entity)
    }

    suspend fun insertRadioEntity(entity: RadioEntity) {
        radioDao.insertRadio(entity)
    }

    suspend fun deleteRadio(radio: Radio) {
        radioDao.deleteRadio(RadioEntity.fromRadio(radio))
    }

    suspend fun deleteAllStations() {
        radioDao.deleteAllRadios()
    }

    suspend fun updateStationOrder(category: RadioCategory, fromPosition: Int, toPosition: Int) {
        radioDao.reorderStations(category, fromPosition, toPosition)
    }

    suspend fun getNextOrderIndex(category: RadioCategory): Int {
        return radioDao.getNextOrderIndex(category) ?: 0
    }

    suspend fun updateStationOrderIndex(radioId: String, newOrder: Int) {
        radioDao.updateOrder(radioId, newOrder)
    }
}

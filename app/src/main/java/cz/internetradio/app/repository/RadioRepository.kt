package cz.internetradio.app.repository

import javax.inject.Inject
import javax.inject.Singleton
import cz.internetradio.app.data.dao.RadioDao
import cz.internetradio.app.data.entity.RadioEntity
import cz.internetradio.app.api.RadioBrowserApi
import cz.internetradio.app.model.Radio
import cz.internetradio.app.model.RadioStation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import cz.internetradio.app.model.RadioCategory

@Singleton
class RadioRepository @Inject constructor(
    private val radioDao: RadioDao,
    private val radioBrowserApi: RadioBrowserApi
) {
    suspend fun searchStationsByName(name: String): List<RadioStation>? {
        return radioBrowserApi.searchStationsByName(name)
    }

    suspend fun getStationsByTag(tag: String): List<RadioStation>? {
        return radioBrowserApi.getStationsByTag(tag)
    }

    suspend fun getStationsByCountry(country: String): List<RadioStation>? {
        return radioBrowserApi.getStationsByCountry(country)
    }

    suspend fun getTags(): List<String>? {
        return radioBrowserApi.getTags()?.map { it.name }
    }

    fun getFavoriteRadios(): Flow<List<Radio>> {
        return radioDao.getFavoriteRadios().map { entities ->
            entities.map { it.toRadio() }
        }
    }

    suspend fun toggleFavorite(radioId: String) {
        radioDao.updateFavoriteStatus(radioId, true)
    }

    suspend fun removeFavorite(radioId: String) {
        radioDao.updateFavoriteStatus(radioId, false)
    }

    suspend fun getRadioById(radioId: String): Radio? {
        return radioDao.getRadioById(radioId)?.toRadio()
    }

    suspend fun addRadioStationToFavorites(radioStation: RadioStation, category: RadioCategory) {
        val radio = Radio(
            id = radioStation.stationuuid ?: radioStation.url,
            name = radioStation.name,
            streamUrl = radioStation.url_resolved ?: radioStation.url,
            imageUrl = radioStation.favicon ?: "android.resource://cz.internetradio.app/drawable/ic_radio_default",
            description = radioStation.tags ?: "",
            category = category
        )
        radioDao.insertRadio(RadioEntity.fromRadio(radio))
    }
} 
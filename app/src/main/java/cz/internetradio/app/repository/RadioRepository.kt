package cz.internetradio.app.repository

import javax.inject.Inject
import javax.inject.Singleton
import cz.internetradio.app.model.Radio
import androidx.compose.ui.graphics.Color
import cz.internetradio.app.data.dao.RadioDao
import cz.internetradio.app.data.entity.RadioEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class RadioRepository @Inject constructor(
    private val radioDao: RadioDao
) {
    private val defaultRadioStations = listOf(
        Radio(
            id = "evropa2",
            name = "Evropa 2",
            streamUrl = "https://ice.actve.net/fm-evropa2-128",
            imageUrl = "https://m.actve.net/e2/favicon/apple-touch-icon.png",
            description = "MaXXimum muziky",
            // Modrá podle loga Evropa 2
            startColor = Color(0xFF00B7FF), // Světle modrá
            endColor = Color(0xFF0066CC)    // Tmavě modrá
        ),
        Radio(
            id = "frekvence1",
            name = "Frekvence 1",
            streamUrl = "https://playerservices.streamtheworld.com/api/livestream-redirect/FREKVENCE1.mp3",
            imageUrl = "https://www.frekvence1.cz/img/favicons/apple-touch-icon.png?v138",
            description = "Rádio pro celou rodinu",
            // Zelená podle loga Frekvence 1
            startColor = Color(0xFF00D100), // Světle zelená
            endColor = Color(0xFF008800)    // Tmavě zelená
        ),
        Radio(
            id = "impuls",
            name = "Impuls Ráááádio",
            streamUrl = "http://icecast5.play.cz/impuls128.mp3",
            imageUrl = "https://1gr.cz/u/favicon/apple-touch-icon/impuls.png",
            description = "Největší česká rádiová stanice",
            // Oranžová podle loga Impulsu
            startColor = Color(0xFFFF8C00), // Světle oranžová
            endColor = Color(0xFFFF4500)    // Tmavě oranžová
        ),
        Radio(
            id = "kiss",
            name = "Kiss Radio",
            streamUrl = "http://icecast4.play.cz/kiss128.mp3",
            imageUrl = "https://www.kiss.cz/assets/favicon/apple-touch-icon.png",
            description = "Dance & Nineties",
            // Růžová podle loga Kiss Rádia
            startColor = Color(0xFFFF69B4), // Světle růžová
            endColor = Color(0xFFFF1493)    // Tmavě růžová
        ),
        Radio(
            id = "rockradio",
            name = "Rock Radio",
            streamUrl = "https://playerservices.streamtheworld.com/api/livestream-redirect/ROCK_RADIO_128.mp3",
            imageUrl = "https://www.radiohouse.cz/wp-content/uploads/2022/06/Rock-radio.png",
            description = "Rockové rádio",
            // Červená podle loga Rock Rádia
            startColor = Color(0xFFDC143C), // Jasně červená
            endColor = Color(0xFF8B0000)    // Tmavě červená
        ),
        Radio(
            id = "beat",
            name = "Rádio Beat",
            streamUrl = "https://icecast2.play.cz/beat128aac",
            imageUrl = "https://www.radiobeat.cz/img/logo@2x.png",
            description = "Classic rock",
            // Barvy podle loga Rádia Beat
            startColor = Color(0xFFD6032C), // Červená z loga
            endColor = Color(0xFF000000)    // Černá z loga
        )
    )

    suspend fun initializeDefaultRadios() {
        if (radioDao.getStationCount() == 0) {
            defaultRadioStations.forEach { radio ->
                radioDao.insertRadio(RadioEntity.fromRadio(radio))
            }
        }
    }

    fun getAllRadios(): Flow<List<Radio>> {
        return radioDao.getAllRadios().map { entities ->
            entities.map { it.toRadio() }
        }
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

    suspend fun resetDatabase() {
        radioDao.deleteAllRadios()
        initializeDefaultRadios()
    }
} 
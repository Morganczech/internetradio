package cz.internetradio.app.repository

import javax.inject.Inject
import javax.inject.Singleton
import cz.internetradio.app.model.Radio
import androidx.compose.ui.graphics.Color

@Singleton
class RadioRepository @Inject constructor() {
    
    fun getRadioStations(): List<Radio> = listOf(
        Radio(
            id = "evropa2",
            name = "Evropa 2",
            streamUrl = "https://ice.actve.net/fm-evropa2-128",
            imageUrl = "https://m.actve.net/e2/favicon/apple-touch-icon.png",
            description = "Nejlepší hudba - 88.2 FM",
            startColor = Color(0xFF0066CC),
            endColor = Color(0xFF003366)
        ),
        Radio(
            id = "frekvence1",
            name = "Frekvence 1",
            streamUrl = "https://playerservices.streamtheworld.com/api/livestream-redirect/FREKVENCE1.mp3",
            imageUrl = "https://www.frekvence1.cz/img/favicons/apple-touch-icon.png?v138",
            description = "Rádio pro celou rodinu",
            startColor = Color(0xFF009933),
            endColor = Color(0xFF006622)
        ),
        Radio(
            id = "impuls",
            name = "Impuls Ráááádio",
            streamUrl = "http://icecast5.play.cz/impuls128.mp3",
            imageUrl = "https://1gr.cz/u/favicon/apple-touch-icon/impuls.png",
            description = "Největší česká rádiová stanice",
            startColor = Color(0xFFFF6600),
            endColor = Color(0xFFCC5200)
        ),
        Radio(
            id = "kiss",
            name = "Kiss Radio",
            streamUrl = "http://icecast4.play.cz/kiss128.mp3",
            imageUrl = "https://www.kiss.cz/assets/favicon/apple-touch-icon.png",
            description = "Dance & Nineties",
            startColor = Color(0xFFFF1493),
            endColor = Color(0xFFCC1177)
        ),
        Radio(
            id = "rockradio",
            name = "Rock Radio",
            streamUrl = "https://playerservices.streamtheworld.com/api/livestream-redirect/ROCK_RADIO_128.mp3",
            imageUrl = "https://www.radiohouse.cz/wp-content/uploads/2022/06/Rock-radio.png",
            description = "Rockové rádio",
            startColor = Color(0xFF8B0000),
            endColor = Color(0xFF660000)
        ),
        Radio(
            id = "beat",
            name = "Rádio Beat",
            streamUrl = "https://icecast2.play.cz/beat128aac",
            imageUrl = "https://www.radiobeat.cz/img/logo@2x.png",
            description = "Classic rock",
            startColor = Color(0xFF4B0082),
            endColor = Color(0xFF2E0051)
        )
    )
} 
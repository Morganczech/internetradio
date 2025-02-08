package cz.internetradio.app.repository

import cz.internetradio.app.R
import cz.internetradio.app.model.Radio
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RadioRepository @Inject constructor() {
    
    fun getRadioStations(): List<Radio> = listOf(
        Radio(
            id = "radiozet",
            name = "Rádio ZET",
            streamUrl = "https://icecast8.play.cz/zet-128.mp3",
            imageResId = R.drawable.logo_radiozet,
            description = "Zpravodajské rádio"
        ),
        Radio(
            id = "evropa2",
            name = "Evropa 2",
            streamUrl = "https://ice.actve.net/fm-evropa2-128",
            imageResId = R.drawable.logo_evropa2,
            description = "Nejlepší hudba"
        ),
        Radio(
            id = "impuls",
            name = "Rádio Impuls",
            streamUrl = "https://icecast8.play.cz/impuls128.mp3",
            imageResId = R.drawable.logo_impuls,
            description = "Největší česká rádiová stanice"
        ),
        Radio(
            id = "frekvence1",
            name = "Frekvence 1",
            streamUrl = "https://ice.actve.net/fm-frekvence1-128",
            imageResId = R.drawable.logo_frekvence1,
            description = "Rádio pro celou rodinu"
        )
    )
} 
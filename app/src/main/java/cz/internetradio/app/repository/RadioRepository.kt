package cz.internetradio.app.repository

import javax.inject.Inject
import javax.inject.Singleton
import cz.internetradio.app.model.Radio
import androidx.compose.ui.graphics.Color
import cz.internetradio.app.data.dao.RadioDao
import cz.internetradio.app.data.entity.RadioEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import cz.internetradio.app.model.RadioCategory

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
            endColor = Color(0xFF0066CC),
            category = RadioCategory.CESKE
        ),
        Radio(
            id = "expresfm",
            name = "Expres FM",
            streamUrl = "http://icecast8.play.cz/expres128mp3",
            imageUrl = "https://www.expresfm.cz/app/mu-plugins/expresfm/assets/favicons/apple-touch-icon.png",
            description = "Hits, Pop, Top 40",
            startColor = Color(0xFFE91E63),
            endColor = Color(0xFFC2185B),
            category = RadioCategory.CESKE
        ),
        Radio(
            id = "bonton",
            name = "Rádio Bonton",
            streamUrl = "http://ice.actve.net/fm-bonton-128",
            imageUrl = "https://www.radiobonton.cz/favicons/apple-touch-icon.png",
            description = "Klídek, pohoda, relax",
            startColor = Color(0xFF00BCD4),
            endColor = Color(0xFF0097A7),
            category = RadioCategory.CESKE
        ),
        Radio(
            id = "rockmax",
            name = "ROCK MAX",
            streamUrl = "http://ice.abradio.cz/rockmax256.mp3",
            imageUrl = "android.resource://cz.internetradio.app/drawable/ic_radio_default",
            description = "Rock naplno",
            startColor = Color(0xFF424242),
            endColor = Color(0xFF212121),
            category = RadioCategory.CESKE
        ),
        Radio(
            id = "croplus",
            name = "ČRo Plus",
            streamUrl = "https://rozhlas.stream/plus_mp3_128.mp3",
            imageUrl = "https://plus.rozhlas.cz/sites/default/files/favicon_plus.ico",
            description = "Zpravodajství a analýzy",
            startColor = Color(0xFF1976D2),
            endColor = Color(0xFF0D47A1),
            category = RadioCategory.CESKE
        ),
        Radio(
            id = "danceradio",
            name = "Dance Radio",
            streamUrl = "http://ice.actve.net/dance-radio320.mp3",
            imageUrl = "https://www.danceradio.cz/favicons/apple-icon-120x120.png",
            description = "Elektronická taneční hudba",
            startColor = Color(0xFFE040FB),
            endColor = Color(0xFFAA00FF),
            category = RadioCategory.CESKE
        ),
        Radio(
            id = "oldiesradio",
            name = "Oldies Rádio",
            streamUrl = "http://ice.abradio.cz/oldiesradio128.mp3",
            imageUrl = "http://www.oldiesradio.cz/design/favicons/apple-icon-120x120.png",
            description = "Nestárnoucí hity",
            startColor = Color(0xFFFFD700),
            endColor = Color(0xFFDAA520),
            category = RadioCategory.CESKE
        ),
        Radio(
            id = "pohadka",
            name = "Rádio pohádka",
            streamUrl = "http://ice3.abradio.cz/pohadka128.mp3",
            imageUrl = "android.resource://cz.internetradio.app/drawable/ic_radio_default",
            description = "Pohádky pro děti",
            startColor = Color(0xFF4CAF50),
            endColor = Color(0xFF2E7D32),
            category = RadioCategory.CESKE
        ),
        Radio(
            id = "fajnradio",
            name = "Fajn Rádio",
            streamUrl = "http://ice.abradio.cz/fajn128.mp3",
            imageUrl = "https://www.fajnradio.cz/favicon.ico",
            description = "Moderní hudba",
            startColor = Color(0xFFFF4081),
            endColor = Color(0xFFF50057),
            category = RadioCategory.CESKE
        ),
        Radio(
            id = "colormusic",
            name = "Color Music Radio",
            streamUrl = "http://sc.ipip.cz:8206/;stream",
            imageUrl = "https://radiocolor.cz/image/color_logo500x500.jpg",
            description = "Funk, Hip Hop, RnB, Soul",
            startColor = Color(0xFF7B1FA2),
            endColor = Color(0xFF4A148C),
            category = RadioCategory.CESKE
        ),
        Radio(
            id = "radiozurnalsport",
            name = "ČRO Radiožurnál Sport",
            streamUrl = "https://rozhlas.stream/radiozurnal_sport_high.aac",
            imageUrl = "https://portal.rozhlas.cz/sites/default/files/favicon_portal.ico",
            description = "Sportovní zpravodajství",
            startColor = Color(0xFF43A047),
            endColor = Color(0xFF2E7D32),
            category = RadioCategory.CESKE
        ),
        Radio(
            id = "crojazz",
            name = "ČRO Jazz",
            streamUrl = "https://rozhlas.stream/jazz_mp3_128.mp3",
            imageUrl = "https://portal.rozhlas.cz/sites/default/files/favicon_portal.ico",
            description = "Jazzová hudba",
            startColor = Color(0xFF3949AB),
            endColor = Color(0xFF1A237E),
            category = RadioCategory.CESKE
        ),
        Radio(
            id = "cropohoda",
            name = "ČRo Pohoda",
            streamUrl = "https://rozhlas.stream/pohoda_mp3_128.mp3",
            imageUrl = "https://pohoda.rozhlas.cz/sites/default/files/favicon.ico",
            description = "Pohodová hudba a oldies",
            startColor = Color(0xFFFFA000),
            endColor = Color(0xFFFF6F00),
            category = RadioCategory.CESKE
        ),
        Radio(
            id = "radio1",
            name = "RADIO 1",
            streamUrl = "http://icecast2.play.cz/radio1.mp3",
            imageUrl = "http://www.radio1.cz/favicon.ico",
            description = "Alternativní hudba",
            startColor = Color(0xFF212121),
            endColor = Color(0xFF000000),
            category = RadioCategory.CESKE
        ),
        Radio(
            id = "orion",
            name = "HIT Rádio Orion",
            streamUrl = "http://ice.abradio.cz/orion128.mp3",
            imageUrl = "http://www.hitradioorion.cz/favicon.ico",
            description = "Hitové rádio",
            startColor = Color(0xFFFF3D00),
            endColor = Color(0xFFDD2C00),
            category = RadioCategory.CESKE
        ),
        Radio(
            id = "blanik",
            name = "Rádio Blaník živé",
            streamUrl = "http://ice.abradio.cz/blanikfm128.mp3",
            imageUrl = "http://www.radioblanik.cz/design/favicons/apple-icon-120x120.png",
            description = "České písničky pro každého",
            // Zeleno-žlutý gradient pro příjemné lidové rádio
            startColor = Color(0xFF4CAF50),
            endColor = Color(0xFF388E3C),
            category = RadioCategory.CESKE
        ),
        Radio(
            id = "diana",
            name = "Comedy Club R@dio DIANA",
            streamUrl = "https://westradio.cz/radio/8010/radio.mp3",
            imageUrl = "android.resource://cz.internetradio.app/drawable/ic_radio_default",
            description = "Zábava a humor",
            startColor = Color(0xFF9C27B0),
            endColor = Color(0xFF7B1FA2),
            category = RadioCategory.CESKE
        ),
        Radio(
            id = "cro2",
            name = "ČRo Dvojka",
            streamUrl = "http://icecast6.play.cz/cro2-128.mp3",
            imageUrl = "https://dvojka.rozhlas.cz/sites/default/files/favicon_dvojka.ico",
            description = "Český rozhlas - mluvené slovo a zábava",
            // Teplý oranžový gradient pro přátelské rádio
            startColor = Color(0xFFFF9800),
            endColor = Color(0xFFF57C00),
            category = RadioCategory.CESKE
        ),
        Radio(
            id = "radiozurnal",
            name = "ČRo Radiožurnál",
            streamUrl = "http://icecast8.play.cz/cro1-128.mp3",
            imageUrl = "https://radiozurnal.rozhlas.cz/sites/default/files/favicon_radiozurnal.ico",
            description = "Zpravodajství a publicistika",
            // Seriózní modro-šedý gradient pro zpravodajské rádio
            startColor = Color(0xFF2C5282),
            endColor = Color(0xFF1A365D),
            category = RadioCategory.CESKE
        ),
        Radio(
            id = "povidka",
            name = "Povídka",
            streamUrl = "https://ice3.abradio.cz/povidka128.mp3",
            imageUrl = "android.resource://cz.internetradio.app/drawable/ic_radio_default",
            description = "České povídky a příběhy",
            startColor = Color(0xFFB7791F),
            endColor = Color(0xFF975A16),
            category = RadioCategory.CESKE
        ),
        Radio(
            id = "countryradio",
            name = "Country Radio",
            streamUrl = "http://icecast2.play.cz:8000/country128aac",
            imageUrl = "http://www.countryradio.cz/assets/favicon/apple-touch-icon.png",
            description = "Country hudba",
            // Zemitý, hnědý gradient pro country rádio
            startColor = Color(0xFF8B4513),
            endColor = Color(0xFF654321),
            category = RadioCategory.CESKE
        ),
        Radio(
            id = "frekvence1",
            name = "Frekvence 1",
            streamUrl = "https://playerservices.streamtheworld.com/api/livestream-redirect/FREKVENCE1.mp3",
            imageUrl = "https://www.frekvence1.cz/img/favicons/apple-touch-icon.png?v138",
            description = "Rádio pro celou rodinu",
            // Zelená podle loga Frekvence 1
            startColor = Color(0xFF00D100), // Světle zelená
            endColor = Color(0xFF008800),
            category = RadioCategory.CESKE
        ),
        Radio(
            id = "impuls",
            name = "Impuls Ráááádio",
            streamUrl = "http://icecast5.play.cz/impuls128.mp3",
            imageUrl = "https://1gr.cz/u/favicon/apple-touch-icon/impuls.png",
            description = "Největší česká rádiová stanice",
            // Oranžová podle loga Impulsu
            startColor = Color(0xFFFF8C00), // Světle oranžová
            endColor = Color(0xFFFF4500),
            category = RadioCategory.CESKE
        ),
        Radio(
            id = "kiss",
            name = "Kiss Radio",
            streamUrl = "http://icecast4.play.cz/kiss128.mp3",
            imageUrl = "https://www.kiss.cz/assets/favicon/apple-touch-icon.png",
            description = "Dance & Nineties",
            // Růžová podle loga Kiss Rádia
            startColor = Color(0xFFFF69B4), // Světle růžová
            endColor = Color(0xFFFF1493),
            category = RadioCategory.CESKE
        ),
        Radio(
            id = "rockradio",
            name = "Rock Radio",
            streamUrl = "https://playerservices.streamtheworld.com/api/livestream-redirect/ROCK_RADIO_128.mp3",
            imageUrl = "https://www.radiohouse.cz/wp-content/uploads/2022/06/Rock-radio.png",
            description = "Rockové rádio",
            // Červená podle loga Rock Rádia
            startColor = Color(0xFFDC143C), // Jasně červená
            endColor = Color(0xFF8B0000),
            category = RadioCategory.CESKE
        ),
        Radio(
            id = "beat",
            name = "Rádio Beat",
            streamUrl = "https://icecast2.play.cz/beat128aac",
            imageUrl = "https://www.radiobeat.cz/img/logo@2x.png",
            description = "Classic rock",
            // Barvy podle loga Rádia Beat
            startColor = Color(0xFFD6032C), // Červená z loga
            endColor = Color(0xFF000000),
            category = RadioCategory.CESKE
        ),
        Radio(
            id = "classicvinylhd",
            name = "Classic Vinyl HD",
            streamUrl = "https://icecast.walmradio.com:8443/classic",
            imageUrl = "https://icecast.walmradio.com:8443/classic.jpg",
            description = "Jazz, Big Band, Swing & Classic Hits",
            startColor = Color(0xFF4A148C), // Tmavě fialová
            endColor = Color(0xFF311B92),
            category = RadioCategory.JAZZ
        ),
        Radio(
            id = "101smoothjazz",
            name = "101 SMOOTH JAZZ",
            streamUrl = "https://streaming.live365.com/b48071_128mp3",
            imageUrl = "http://101smoothjazz.com/favicon.ico",
            description = "Smooth Jazz & Easy Listening",
            startColor = Color(0xFF1A237E),
            endColor = Color(0xFF0D47A1),
            category = RadioCategory.JAZZ
        ),
        Radio(
            id = "adroitjazz",
            name = "Adroit Jazz Underground",
            streamUrl = "https://icecast.walmradio.com:8443/jazz",
            imageUrl = "https://icecast.walmradio.com:8443/jazz.jpg",
            description = "Contemporary Jazz, Bebop & Modern Jazz",
            startColor = Color(0xFF006064), // Tmavě tyrkysová
            endColor = Color(0xFF00838F),
            category = RadioCategory.JAZZ
        ),
        Radio(
            id = "radioswissjazz",
            name = "Radio Swiss Jazz",
            streamUrl = "http://stream.srg-ssr.ch/m/rsj/mp3_128",
            imageUrl = "http://www.radioswissjazz.ch/favicon.ico",
            description = "Swiss Public Jazz Radio",
            startColor = Color(0xFF880E4F), // Vínová
            endColor = Color(0xFFC2185B),
            category = RadioCategory.JAZZ
        ),
        Radio(
            id = "jazzradioblues",
            name = "Jazz Radio Blues",
            streamUrl = "http://jazzblues.ice.infomaniak.ch/jazzblues-high.mp3",
            imageUrl = "https://www.jazzradio.fr/apple-touch-icon-120x120.png",
            description = "Jazz & Blues from France",
            startColor = Color(0xFF1B5E20), // Tmavě zelená
            endColor = Color(0xFF2E7D32),
            category = RadioCategory.JAZZ
        ),
        Radio(
            id = "bossajazz",
            name = "Bossa Jazz Brasil",
            streamUrl = "https://centova5.transmissaodigital.com:20104/live",
            imageUrl = "https://bossajazzbrasil.com/wp-content/uploads/2020/12/cropped-bjs-app-180x180.png",
            description = "Bossa Nova & Brazilian Jazz",
            startColor = Color(0xFFE65100), // Oranžová
            endColor = Color(0xFFF57C00),
            category = RadioCategory.JAZZ
        ),
        Radio(
            id = "jazzloungebargreat",
            name = "100 GREATEST JAZZ LOUNGE BAR",
            streamUrl = "https://cast1.torontocast.com:4640/stream",
            imageUrl = "https://static.wixstatic.com/media/f361b3_2722a2a63db342f0901576a0168f2571~mv2.jpg",
            description = "Jazz Lounge & Smooth Jazz",
            startColor = Color(0xFF5D4037), // Tmavě hnědá
            endColor = Color(0xFF3E2723),
            category = RadioCategory.JAZZ
        ),
        Radio(
            id = "011fmsmoothjazz",
            name = "011.FM - Smooth Jazz",
            streamUrl = "https://listen.011fm.com/stream23",
            imageUrl = "https://cdn-profiles.tunein.com/s300606/images/logod.jpg?t=637903061830000000",
            description = "Relaxační Smooth Jazz",
            startColor = Color(0xFF00695C), // Tmavě tyrkysová
            endColor = Color(0xFF004D40),
            category = RadioCategory.JAZZ
        ),
        Radio(
            id = "jazzgrooveeast",
            name = "The Jazz Groove - East",
            streamUrl = "http://east-mp3-128.streamthejazzgroove.com/stream",
            imageUrl = "android.resource://cz.internetradio.app/drawable/ic_radio_default",
            description = "Cool Jazz & Fusion",
            startColor = Color(0xFF6A1B9A), // Tmavě fialová
            endColor = Color(0xFF4A148C),
            category = RadioCategory.JAZZ
        ),
        Radio(
            id = "jazzlondonradio",
            name = "Jazz London Radio",
            streamUrl = "http://radio.canstream.co.uk:8075/live.mp3",
            imageUrl = "https://www.jazzlondonradio.com/images/favicon.ico",
            description = "Jazz z Londýna",
            startColor = Color(0xFF283593), // Tmavě modrá
            endColor = Color(0xFF1A237E),
            category = RadioCategory.JAZZ
        ),
        Radio(
            id = "1fmbaysmoothjazz",
            name = "1.FM - Bay Smooth Jazz Radio",
            streamUrl = "http://strm112.1.fm/smoothjazz_mobile_mp3",
            imageUrl = "android.resource://cz.internetradio.app/drawable/ic_radio_default",
            description = "Smooth Jazz ze Švýcarska",
            startColor = Color(0xFF455A64), // Tmavě modrošedá
            endColor = Color(0xFF263238),
            category = RadioCategory.JAZZ
        ),
        Radio(
            id = "jazzlounge",
            name = "Jazz Lounge",
            streamUrl = "http://eu8.fastcast4u.com:5068/;",
            imageUrl = "android.resource://cz.internetradio.app/drawable/ic_radio_default",
            description = "Classic & Smooth Jazz",
            startColor = Color(0xFF795548), // Tmavě hnědá
            endColor = Color(0xFF5D4037),
            category = RadioCategory.JAZZ
        ),
        Radio(
            id = "977smoothjazz",
            name = ".977 Smooth Jazz",
            streamUrl = "https://playerservices.streamtheworld.com/api/livestream-redirect/977_SMOOJAZZ.mp3",
            imageUrl = "android.resource://cz.internetradio.app/drawable/ic_radio_default",
            description = "Smooth Jazz z USA",
            startColor = Color(0xFF512DA8),
            endColor = Color(0xFF4527A0),
            category = RadioCategory.JAZZ
        ),
        Radio(
            id = "loungefmaustria",
            name = "Lounge.FM - 100% Austria",
            streamUrl = "https://s35.derstream.net/100austria.mp3",
            imageUrl = "https://www.lounge.fm/wp-content/uploads/2014/06/loungefm_logo_colour2-300x300.jpg",
            description = "Lounge & Smooth Jazz z Vídně",
            startColor = Color(0xFF00796B),
            endColor = Color(0xFF00695C),
            category = RadioCategory.JAZZ
        ),
        Radio(
            id = "rdmixclassicrock",
            name = "RdMix Classic Rock 70s 80s 90s",
            streamUrl = "https://cast1.torontocast.com:4610/stream",
            imageUrl = "https://static.wixstatic.com/media/f361b3_1bad7e5fb9a54196b2ac2c2fe71d6e4f~mv2.jpg",
            description = "Classic Rock, Blues Rock, Hard Rock",
            startColor = Color(0xFF8B0000), // Tmavě červená
            endColor = Color(0xFF4B0082), // Indigová
            category = RadioCategory.ROCK
        ),
        Radio(
            id = "rockantenne",
            name = "Rock Antenne",
            streamUrl = "http://mp3channels.webradio.rockantenne.de/rockantenne",
            imageUrl = "http://www.rockantenne.de/logos/rock-antenne/apple-touch-icon.png",
            description = "Německé rockové rádio",
            startColor = Color(0xFF4A148C), // Tmavě fialová
            endColor = Color(0xFF311B92), // Tmavě indigová
            category = RadioCategory.ROCK
        ),
        Radio(
            id = "megarockradio",
            name = "Megarock Radio 320k",
            streamUrl = "https://stream3.megarockradio.net:80/",
            imageUrl = "https://megarockradio.net/favicon.ico",
            description = "Classic Rock, Hard Rock, Metal",
            startColor = Color(0xFF1A237E),
            endColor = Color(0xFF000051),
            category = RadioCategory.ROCK
        ),
        Radio(
            id = "90s90srock",
            name = "90s90s Rock",
            streamUrl = "https://streams.90s90s.de/rock/mp3-192/",
            imageUrl = "https://www.radio.de/images/broadcasts/8d/ce/185038/1/c300.png",
            description = "Alternative Rock & Hard Rock z 90. let",
            startColor = Color(0xFF880E4F),
            endColor = Color(0xFF560027),
            category = RadioCategory.ROCK
        ),
        Radio(
            id = "nostalgiepoprock80",
            name = "Nostalgie Pop Rock 80",
            streamUrl = "https://scdn.nrjaudio.fm/adwz1/fr/56718/aac_64.mp3",
            imageUrl = "https://www.nostalgie.fr/uploads/assets/nostalgie/icons/apple-icon-120x120.png",
            description = "Pop Rock 80s z Francie",
            startColor = Color(0xFFD84315),
            endColor = Color(0xFFBF360C),
            category = RadioCategory.ROCK
        ),
        Radio(
            id = "slowrock90",
            name = "Slow Rock 90'",
            streamUrl = "https://stream.zeno.fm/hiut6cwfoneuv",
            imageUrl = "android.resource://cz.internetradio.app/drawable/ic_radio_default",
            description = "Slow Rock z 90. let",
            startColor = Color(0xFF37474F),
            endColor = Color(0xFF263238),
            category = RadioCategory.ROCK
        ),
        Radio(
            id = "radioparadiserock",
            name = "Radio Paradise - Rock Mix",
            streamUrl = "http://stream.radioparadise.com/rock-320",
            imageUrl = "https://radioparadise.com/apple-touch-icon.png",
            description = "Rock Mix z Radio Paradise",
            startColor = Color(0xFF006064),
            endColor = Color(0xFF00363A),
            category = RadioCategory.ROCK
        ),
        Radio(
            id = "nonstopoldies",
            name = "Non-Stop Oldies",
            streamUrl = "https://ais-sa2.cdnstream1.com/2383_128.mp3",
            imageUrl = "https://static.wixstatic.com/media/00a61a_a29da417d9ce44d18dc83e35a30c2f43.png/v1/crop/x_24,y_30,w_555,h_544/fill/w_136,h_133,al_c,q_85,usm_0.66_1.00_0.01,enc_auto/00a61a_a29da417d9ce44d18dc83e35a30c2f43.png",
            description = "70's, Motown, Soul & Rock n' Roll",
            startColor = Color(0xFFFF6F00),
            endColor = Color(0xFFC43E00),
            category = RadioCategory.ROCK
        ),
        Radio(
            id = "monstersofrock",
            name = "Monsters of Rock",
            streamUrl = "http://ice55.securenetsystems.net/DASH14",
            imageUrl = "android.resource://cz.internetradio.app/drawable/ic_radio_default",
            description = "Classic Rock, Hair Metal & Hard Rock",
            startColor = Color(0xFF1A1A1A),
            endColor = Color(0xFF000000),
            category = RadioCategory.ROCK
        ),
        Radio(
            id = "bluesrockradio",
            name = "Blues & Rock Radio",
            streamUrl = "http://stream.laut.fm/bluesrock",
            imageUrl = "https://i.ibb.co/7pjwNCm/Blues-Rock.jpg",
            description = "Blues Rock & Hard Rock z Ukrajiny",
            startColor = Color(0xFF0D47A1), // Tmavě modrá pro blues
            endColor = Color(0xFF1A237E), // Ještě tmavší modrá
            category = RadioCategory.ROCK
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
package cz.internetradio.app.model

import androidx.compose.ui.graphics.Color
import androidx.annotation.StringRes
import cz.internetradio.app.R

enum class RadioCategory(
    @StringRes private val titleRes: Int,
    val startColor: Color,
    val endColor: Color
) {
    MISTNI(
        R.string.category_local,
        Color(0xFF4CAF50),
        Color(0xFF388E3C)
    ),
    POP(
        R.string.category_pop,
        Color(0xFFFF4B8A),
        Color(0xFFFF1F5A)
    ),
    ROCK(
        R.string.category_rock,
        Color(0xFF8B0000),
        Color(0xFF5A0000)
    ),
    JAZZ(
        R.string.category_jazz,
        Color(0xFF4B4BE8),
        Color(0xFF2525A0)
    ),
    DANCE(
        R.string.category_dance,
        Color(0xFFFF8C00),
        Color(0xFFFF6B00)
    ),
    ELEKTRONICKA(
        R.string.category_electronic,
        Color(0xFF00FFFF),
        Color(0xFF00B2B2)
    ),
    KLASICKA(
        R.string.category_classical,
        Color(0xFF8B4513),
        Color(0xFF5A2D0C)
    ),
    COUNTRY(
        R.string.category_country,
        Color(0xFFCD853F),
        Color(0xFF8B5A2B)
    ),
    FOLK(
        R.string.category_folk,
        Color(0xFF556B2F),
        Color(0xFF3B4A20)
    ),
    MLUVENE_SLOVO(
        R.string.category_spoken_word,
        Color(0xFF4682B4),
        Color(0xFF2F5A8B)
    ),
    DETSKE(
        R.string.category_children,
        Color(0xFFFF69B4),
        Color(0xFFFF1493)
    ),
    NABOZENSKE(
        R.string.category_religious,
        Color(0xFF9370DB),
        Color(0xFF6A3DB3)
    ),
    ZPRAVODAJSKE(
        R.string.category_news,
        Color(0xFF20B2AA),
        Color(0xFF008B8B)
    ),
    VLASTNI(
        R.string.category_favorites,
        Color(0xFF9C27B0),
        Color(0xFF7B1FA2)
    ),
    OSTATNI(
        R.string.category_other,
        Color(0xFF808080),
        Color(0xFF696969)
    );

    companion object {
        private var currentCountryCode: String? = null

        fun getLocalizedTitle(countryCode: String): String {
            return when (countryCode.uppercase()) {
                "CZ" -> "České stanice"
                "SK" -> "Slovenské stanice"
                "DE" -> "Deutsche Sender"
                "AT" -> "Österreichische Sender"
                "PL" -> "Polskie stacje"
                "GB", "UK" -> "British stations"
                "US" -> "American stations"
                else -> "Local stations"
            }
        }

        fun setCurrentCountryCode(countryCode: String?) {
            currentCountryCode = countryCode
        }
    }

    @StringRes
    fun getTitleRes(): Int {
        return if (this == MISTNI) {
            currentCountryCode?.let {
                // Pro místní stanice vracíme speciální resource ID podle země
                when (it.uppercase()) {
                    "CZ" -> R.string.category_local_cz
                    "SK" -> R.string.category_local_sk
                    "DE" -> R.string.category_local_de
                    "AT" -> R.string.category_local_at
                    "PL" -> R.string.category_local_pl
                    "GB", "UK" -> R.string.category_local_gb
                    "US" -> R.string.category_local_us
                    else -> R.string.category_local
                }
            } ?: titleRes
        } else {
            titleRes
        }
    }
} 
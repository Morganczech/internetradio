package cz.internetradio.app.model

import androidx.annotation.StringRes
import cz.internetradio.app.R

enum class RadioCategory(
    @StringRes private val titleRes: Int
) {
    VSE(R.string.category_all),
    MISTNI(R.string.category_local),
    POP(R.string.category_pop),
    ROCK(R.string.category_rock),
    JAZZ(R.string.category_jazz),
    DANCE(R.string.category_dance),
    ELEKTRONICKA(R.string.category_electronic),
    KLASICKA(R.string.category_classical),
    COUNTRY(R.string.category_country),
    FOLK(R.string.category_folk),
    MLUVENE_SLOVO(R.string.category_spoken_word),
    DETSKE(R.string.category_children),
    NABOZENSKE(R.string.category_religious),
    ZPRAVODAJSKE(R.string.category_news),
    VLASTNI(R.string.category_favorites),
    OSTATNI(R.string.category_other);

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
        return titleRes
    }
} 
package cz.internetradio.app.model

import androidx.compose.ui.graphics.Color

enum class RadioCategory(
    private val defaultTitle: String,
    val startColor: Color,
    val endColor: Color
) {
    MISTNI(
        "Místní stanice",
        Color(0xFF4CAF50),
        Color(0xFF388E3C)
    ),
    POP(
        "Pop",
        Color(0xFFFF4B8A),
        Color(0xFFFF1F5A)
    ),
    ROCK(
        "Rock",
        Color(0xFF8B0000),
        Color(0xFF5A0000)
    ),
    JAZZ(
        "Jazz",
        Color(0xFF4B4BE8),
        Color(0xFF2525A0)
    ),
    DANCE(
        "Dance",
        Color(0xFFFF8C00),
        Color(0xFFFF6B00)
    ),
    ELEKTRONICKA(
        "Elektronická",
        Color(0xFF00FFFF),
        Color(0xFF00B2B2)
    ),
    KLASICKA(
        "Klasická",
        Color(0xFF8B4513),
        Color(0xFF5A2D0C)
    ),
    COUNTRY(
        "Country",
        Color(0xFFCD853F),
        Color(0xFF8B5A2B)
    ),
    FOLK(
        "Folk",
        Color(0xFF556B2F),
        Color(0xFF3B4A20)
    ),
    MLUVENE_SLOVO(
        "Mluvené slovo",
        Color(0xFF4682B4),
        Color(0xFF2F5A8B)
    ),
    DETSKE(
        "Dětské",
        Color(0xFFFF69B4),
        Color(0xFFFF1493)
    ),
    NABOZENSKE(
        "Náboženské",
        Color(0xFF9370DB),
        Color(0xFF6A3DB3)
    ),
    ZPRAVODAJSKE(
        "Zpravodajské",
        Color(0xFF20B2AA),
        Color(0xFF008B8B)
    ),
    VLASTNI(
        "Moje stanice",
        Color(0xFF9C27B0),
        Color(0xFF7B1FA2)
    ),
    OSTATNI(
        "Ostatní",
        Color(0xFF808080),
        Color(0xFF696969)
    );

    val title: String
        get() = if (this == MISTNI) {
            currentCountryCode?.let { getLocalizedTitle(it) } ?: defaultTitle
        } else {
            defaultTitle
        }

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
} 
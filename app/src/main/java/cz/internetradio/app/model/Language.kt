package cz.internetradio.app.model

import androidx.annotation.StringRes
import cz.internetradio.app.R
import java.util.Locale

enum class Language(
    val code: String,
    @StringRes val nameRes: Int
) {
    SYSTEM("system", R.string.settings_language_system),
    ENGLISH("en", R.string.settings_language_en),
    CZECH("cs", R.string.settings_language_cs),
    SLOVAK("sk", R.string.settings_language_sk),
    POLISH("pl", R.string.settings_language_pl),
    GERMAN("de", R.string.settings_language_de),
    FRENCH("fr", R.string.settings_language_fr),
    SPANISH("es", R.string.settings_language_es);

    companion object {
        fun fromCode(code: String): Language {
            return values().find { it.code == code } ?: SYSTEM
        }

        fun getCurrentSystemLanguage(): Language {
            val locale = Locale.getDefault()
            return when (locale.language) {
                "cs" -> CZECH
                "sk" -> SLOVAK
                "pl" -> POLISH
                "de" -> GERMAN
                "fr" -> FRENCH
                "es" -> SPANISH
                else -> ENGLISH
            }
        }
    }

    fun toLocale(): Locale {
        return when (this) {
            SYSTEM -> getCurrentSystemLanguage().toLocale()
            ENGLISH -> Locale("en")
            CZECH -> Locale("cs")
            SLOVAK -> Locale("sk")
            POLISH -> Locale("pl")
            GERMAN -> Locale("de")
            FRENCH -> Locale("fr")
            SPANISH -> Locale("es")
        }
    }
} 
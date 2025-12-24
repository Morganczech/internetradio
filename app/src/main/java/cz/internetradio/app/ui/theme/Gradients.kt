package cz.internetradio.app.ui.theme

import androidx.compose.ui.graphics.Color
import cz.internetradio.app.model.RadioCategory

object Gradients {
    // Předdefinované gradienty pro kategorie
    private val categoryGradients = mapOf(
        RadioCategory.VSE to Pair(
            Color(0xFF607D8B), // Šedá - neutrální pro "Vše"
            Color(0xFF455A64)
        ),
        RadioCategory.MISTNI to Pair(
            Color(0xFF1976D2), // Modrá - stejná jako v PopularStationsScreen
            Color(0xFF0D47A1)  // Tmavší modrá pro gradient
        ),
        RadioCategory.POP to Pair(
            Color(0xFFE91E63), // Růžová - energická, moderní
            Color(0xFFC2185B)
        ),
        RadioCategory.ROCK to Pair(
            Color(0xFF424242), // Tmavě šedá - klasický rock
            Color(0xFF212121)
        ),
        RadioCategory.JAZZ to Pair(
            Color(0xFF6A1B9A), // Fialová - sofistikovaná, jazzová
            Color(0xFF4A148C)
        ),
        RadioCategory.DANCE to Pair(
            Color(0xFFE040FB), // Neonově fialová - taneční, energická
            Color(0xFFAA00FF)
        ),
        RadioCategory.ELEKTRONICKA to Pair(
            Color(0xFF1E88E5), // Tmavší modrá - elektronická, modernější
            Color(0xFF0D47A1)  // Velmi tmavá modrá pro gradient
        ),
        RadioCategory.KLASICKA to Pair(
            Color(0xFF8D6E63), // Hnědá - klasická, vážná
            Color(0xFF6D4C41)
        ),
        RadioCategory.COUNTRY to Pair(
            Color(0xFF8B4513), // Hnědá - saddlebrown - připomíná dřevo
            Color(0xFF5D2A0C)  // Tmavší hnědá
        ),
        RadioCategory.FOLK to Pair(
            Color(0xFF689F38), // Zelená - folk, příroda
            Color(0xFF558B2F)
        ),
        RadioCategory.MLUVENE_SLOVO to Pair(
            Color(0xFF3F51B5), // Indigová - mluvené slovo
            Color(0xFF303F9F)
        ),
        RadioCategory.DETSKE to Pair(
            Color(0xFFFF4081), // Růžová - dětské
            Color(0xFFF50057)
        ),
        RadioCategory.NABOZENSKE to Pair(
            Color(0xFF7E57C2), // Fialová - náboženské
            Color(0xFF5E35B1)
        ),
        RadioCategory.ZPRAVODAJSKE to Pair(
            Color(0xFF2C5282), // Tmavě modrá - seriózní
            Color(0xFF1A365D)
        ),
        RadioCategory.VLASTNI to Pair(
            Color(0xFF9C27B0), // Fialová - vlastní stanice
            Color(0xFF7B1FA2)
        ),
        RadioCategory.OSTATNI to Pair(
            Color(0xFF455A64), // Šedomodrá - neutrální
            Color(0xFF263238)
        )
    )

    // Jednotná neutrální barva pro "Unified Accent Color" režim
    val UNIFIED_COLOR_PAIR = Pair(
        Color(0xFF607D8B), // Blue Grey
        Color(0xFF37474F)
    )

    val availableGradients = listOf(
        GradientOption(0, "Modrá - Česká", Pair(Color(0xFF1976D2), Color(0xFF0D47A1))),
        GradientOption(1, "Růžová - Pop", Pair(Color(0xFFE91E63), Color(0xFFC2185B))),
        GradientOption(2, "Tmavě šedá - Rock", Pair(Color(0xFF424242), Color(0xFF212121))),
        GradientOption(3, "Fialová - Jazz", Pair(Color(0xFF6A1B9A), Color(0xFF4A148C))),
        GradientOption(4, "Neonová - Dance", Pair(Color(0xFFE040FB), Color(0xFFAA00FF))),
        GradientOption(5, "Modrá - Elektronická", Pair(Color(0xFF1E88E5), Color(0xFF0D47A1))),
        GradientOption(6, "Hnědá - Klasická", Pair(Color(0xFF8D6E63), Color(0xFF6D4C41))),
        GradientOption(7, "Hnědá - Country", Pair(Color(0xFF8B4513), Color(0xFF5D2A0C))),
        GradientOption(8, "Zelená - Folk", Pair(Color(0xFF689F38), Color(0xFF558B2F))),
        GradientOption(9, "Indigová - Mluvené slovo", Pair(Color(0xFF3F51B5), Color(0xFF303F9F))),
        GradientOption(10, "Růžová - Dětské", Pair(Color(0xFFFF4081), Color(0xFFF50057))),
        GradientOption(11, "Fialová - Náboženské", Pair(Color(0xFF7E57C2), Color(0xFF5E35B1))),
        GradientOption(12, "Tmavě modrá - Zpravodajské", Pair(Color(0xFF2C5282), Color(0xFF1A365D)))
    )

    data class GradientOption(
        val id: Int,
        val name: String,
        val colors: Pair<Color, Color>
    )

    fun getGradientById(id: Int?): Pair<Color, Color> {
        return id?.let { gradientId ->
            availableGradients.find { it.id == gradientId }?.colors
        } ?: categoryGradients[RadioCategory.OSTATNI]!!
    }

    // Bezpečná metoda pro získání gradientu
    fun getGradientForCategory(category: RadioCategory, selectedGradientId: Int? = null, useUnified: Boolean = false): Pair<Color, Color> {
        if (useUnified) return UNIFIED_COLOR_PAIR
        
        return if (selectedGradientId != null) {
            getGradientById(selectedGradientId)
        } else {
            // Použijeme getOrDefault nebo null-safe přístup s fallbackem na OSTATNI
            categoryGradients[category] ?: categoryGradients[RadioCategory.OSTATNI]!!
        }
    }

    fun createCustomGradient(startColor: Color, endColor: Color): Pair<Color, Color> {
        return Pair(startColor, endColor)
    }
}

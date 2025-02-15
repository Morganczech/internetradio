package cz.internetradio.app.ui.theme

import androidx.compose.ui.graphics.Color
import cz.internetradio.app.model.RadioCategory
import kotlin.random.Random

object Gradients {
    // Předdefinované gradienty pro kategorie
    private val categoryGradients = mapOf(
        RadioCategory.CESKE to Pair(
            Color(0xFF1976D2), // Modrá - symbolizuje českou vlajku
            Color(0xFF0D47A1)
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
        RadioCategory.ZPRAVODAJSKE to Pair(
            Color(0xFF2C5282), // Tmavě modrá - seriózní
            Color(0xFF1A365D)
        ),
        RadioCategory.VLASTNI to Pair(
            Color(0xFF1A1A1A), // Tmavě šedá - neutrální
            Color(0xFF2D2D2D)
        ),
        RadioCategory.OSTATNI to Pair(
            Color(0xFF455A64), // Šedomodrá - neutrální
            Color(0xFF263238)
        )
    )

    // Předpřipravené gradienty pro náhodný výběr
    private val randomGradients = listOf(
        Pair(Color(0xFFFF6B6B), Color(0xFFFF4949)), // Červená
        Pair(Color(0xFF4ECDC4), Color(0xFF45B7AF)), // Tyrkysová
        Pair(Color(0xFFFFBE0B), Color(0xFFFB8500)), // Žlutá/Oranžová
        Pair(Color(0xFF8338EC), Color(0xFF6A26D9)), // Fialová
        Pair(Color(0xFF06D6A0), Color(0xFF05BF8E)), // Zelená
        Pair(Color(0xFF4361EE), Color(0xFF3046D1)), // Modrá
        Pair(Color(0xFFFF006E), Color(0xFFE50063)), // Růžová
        Pair(Color(0xFF9B5DE5), Color(0xFF8046C3)), // Levandulová
        Pair(Color(0xFFF15BB5), Color(0xFFD44D9A)), // Magenta
        Pair(Color(0xFF00BBF9), Color(0xFF00A6DF)), // Světle modrá
        Pair(Color(0xFFFEE440), Color(0xFFE5CD3D)), // Žlutá
        Pair(Color(0xFF00F5D4), Color(0xFF00D9BC)), // Mátová
    )

    // Získá gradient pro danou kategorii
    fun getGradientForCategory(category: RadioCategory): Pair<Color, Color> {
        return categoryGradients[category] ?: getRandomGradient()
    }

    // Získá náhodný gradient pro vlastní stanice
    fun getRandomGradient(): Pair<Color, Color> {
        return randomGradients[Random.nextInt(randomGradients.size)]
    }

    // Vytvoří vlastní gradient z vybraných barev
    fun createCustomGradient(startColor: Color, endColor: Color): Pair<Color, Color> {
        return Pair(startColor, endColor)
    }
} 
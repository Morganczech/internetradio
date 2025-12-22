package cz.internetradio.app.utils

import java.text.Normalizer

/**
 * Převede text na malá písmena a odstraní diakritiku.
 * Příklad: "Český rozhlas" -> "cesky rozhlas"
 */
fun String.normalizeForSearch(): String {
    val normalized = Normalizer.normalize(this, Normalizer.Form.NFD)
    return normalized.replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
        .lowercase()
        .trim()
}

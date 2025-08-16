package cz.internetradio.app.audio

import android.content.Context
import android.media.audiofx.Equalizer
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.locks.ReentrantLock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt
import cz.internetradio.app.model.EqualizerPreset

@Singleton
class EqualizerManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var equalizer: Equalizer? = null
    private var audioSessionId: Int = -1
    private var isEnabled: Boolean = false
    private var currentBandLevels: MutableList<Int> = mutableListOf()
    private val lock = ReentrantLock()

    fun setupEqualizer(sessionId: Int) {
        Log.d(TAG, """🎛️ Požadavek na setup equalizeru:
            |  - session ID: $sessionId
            |  - aktuální session ID: $audioSessionId
            |  - equalizer inicializován: ${equalizer != null}
            |  - isEnabled: $isEnabled
            |  - uložené hodnoty pásem: $currentBandLevels
            """.trimMargin())
        
        if (sessionId == -1) {
            Log.e(TAG, "❌ Neplatné session ID")
            return
        }

        lock.withLock {
            try {
                if (audioSessionId != sessionId || equalizer == null) {
                    Log.d(TAG, "🔄 Nastavuji nový equalizer")
                    
                    // Uložení aktuálních hodnot před uvolněním
                    if (equalizer != null) {
                        currentBandLevels = (0 until equalizer!!.numberOfBands).map { band ->
                            equalizer!!.getBandLevel(band.toShort()) / 100
                        }.toMutableList()
                        Log.d(TAG, "📝 Uloženy hodnoty pásem: $currentBandLevels")
                    }
                    
                    release()
                    audioSessionId = sessionId

                    equalizer = try {
                        Equalizer(0, sessionId).also { eq ->
                            eq.enabled = isEnabled
                            Log.d(TAG, """✅ Equalizer vytvořen:
                                |  - enabled: $isEnabled
                                |  - počet pásem: ${eq.numberOfBands}
                                |  - rozsah: ${eq.bandLevelRange[0]/100}dB až ${eq.bandLevelRange[1]/100}dB
                                |  - frekvence: ${(0 until eq.numberOfBands).map { "${eq.getCenterFreq(it.toShort())/1000}Hz" }}
                                """.trimMargin())
                            
                            // Obnovení uložených hodnot
                            if (currentBandLevels.isNotEmpty() && currentBandLevels.size == eq.numberOfBands) {
                                Log.d(TAG, "🔄 Obnovuji uložené hodnoty pásem")
                                currentBandLevels.forEachIndexed { index, level ->
                                    val milliLevel = (level * 100).toShort()
                                    eq.setBandLevel(index.toShort(), milliLevel)
                                    Log.d(TAG, "✅ Obnoveno pásmo $index na hodnotu ${level}dB")
                                }
                            } else {
                                Log.d(TAG, "⚠️ Žádné uložené hodnoty k obnovení")
                                // Inicializace nových hodnot
                                currentBandLevels = (0 until eq.numberOfBands).map { 0 }.toMutableList()
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Chyba při vytváření equalizeru", e)
                        null
                    }
                } else {
                    Log.d(TAG, "✅ Equalizer již nastaven pro toto audio session ID")
                    equalizer?.let { eq ->
                        eq.enabled = isEnabled
                        Log.d(TAG, """📊 Aktuální stav equalizeru:
                            |  - enabled: ${eq.enabled}
                            |  - počet pásem: ${eq.numberOfBands}
                            |  - hodnoty pásem: ${(0 until eq.numberOfBands).map { "${eq.getBandLevel(it.toShort())/100}dB" }}
                            """.trimMargin())
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Chyba při nastavování equalizeru", e)
                release()
                throw e
            }
        }
    }

    fun setEnabled(enabled: Boolean) {
        Log.d(TAG, """🎛️ Nastavuji enabled stav equalizeru:
            |  - požadovaný stav: $enabled
            |  - aktuální stav: $isEnabled
            |  - equalizer inicializován: ${equalizer != null}
            """.trimMargin())
        
        lock.withLock {
            try {
                isEnabled = enabled
                equalizer?.let { eq ->
                    eq.enabled = enabled
                    if (enabled) {
                        // Při zapnutí obnovíme uložené hodnoty
                        currentBandLevels.forEachIndexed { index, level ->
                            val milliLevel = (level * 100).toShort()
                            eq.setBandLevel(index.toShort(), milliLevel)
                        }
                    }
                    Log.d(TAG, """✅ Stav equalizeru nastaven:
                        |  - enabled: ${eq.enabled}
                        |  - hodnoty pásem: ${(0 until eq.numberOfBands).map { "${eq.getBandLevel(it.toShort())/100}dB" }}
                        """.trimMargin())
                } ?: run {
                    Log.w(TAG, "⚠️ Equalizer není inicializován, ukládám pouze stav isEnabled=$enabled")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Chyba při nastavování enabled stavu", e)
            }
        }
    }

    fun applyPreset(preset: EqualizerPreset) {
        Log.d(TAG, """🎛️ Aplikuji preset:
            |  - název: ${preset.title}
            |  - hodnoty: ${preset.bands}
            |  - equalizer enabled: ${equalizer?.enabled}
            |  - equalizer inicializován: ${equalizer != null}
            """.trimMargin())
        
        try {
            equalizer?.let { eq ->
                if (!eq.enabled) {
                    Log.w(TAG, "⚠️ Equalizer není povolen, povoluji...")
                    eq.enabled = true
                    isEnabled = true
                }

                preset.bands.forEachIndexed { index, gain ->
                    if (index < eq.numberOfBands) {
                        val milliGain = (gain * 100).toInt().coerceIn(
                            eq.bandLevelRange[0].toInt(),
                            eq.bandLevelRange[1].toInt()
                        ).toShort()
                        
                        eq.setBandLevel(index.toShort(), milliGain)
                        // Uložení hodnoty do našeho stavu
                        if (index < currentBandLevels.size) {
                            currentBandLevels[index] = gain.toInt()
                        }
                        
                        Log.d(TAG, """✅ Nastaveno pásmo $index:
                            |  - frekvence: ${eq.getCenterFreq(index.toShort())/1000}Hz
                            |  - hodnota: ${gain}dB (${milliGain}mB)
                            """.trimMargin())
                    }
                }
                
                Log.d(TAG, """📊 Aktuální hodnoty pásem:
                    |${(0 until eq.numberOfBands).joinToString("\n") { index ->
                        "  - ${eq.getCenterFreq(index.toShort())/1000}Hz: ${eq.getBandLevel(index.toShort())/100}dB"
                    }}
                    """.trimMargin())
            } ?: Log.w(TAG, "⚠️ Equalizer není inicializován")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Chyba při aplikování presetu: ${e.message}")
        }
    }

    fun setBandLevel(band: Int, level: Int) {
        lock.withLock {
            try {
                equalizer?.let { eq ->
                    if (!eq.enabled) {
                        Log.w(TAG, "⚠️ Nelze nastavit úroveň pásma - equalizer není enabled")
                        return
                    }

                    val bandRange = eq.bandLevelRange
                    if (bandRange == null) {
                        Log.e(TAG, "❌ Nelze získat rozsah pásma")
                        return
                    }

                    // Převod z dB na miliBely a omezení na platný rozsah
                    val levelInMilliBels = (level * 100).roundToInt().toShort()
                        .coerceIn(bandRange[0], bandRange[1])

                    eq.setBandLevel(band.toShort(), levelInMilliBels)
                    // Uložení hodnoty do našeho stavu
                    if (band < currentBandLevels.size) {
                        currentBandLevels[band] = level
                    }
                    
                    val freq = eq.getCenterFreq(band.toShort())
                    Log.d(TAG, """✅ Nastaveno pásmo $band:
                        |  - Frekvence: ${freq/1000}Hz
                        |  - Požadovaná hodnota: ${level}dB
                        |  - Skutečná hodnota: ${levelInMilliBels/100}dB
                        |  - Rozsah: ${bandRange[0]/100}dB až ${bandRange[1]/100}dB
                        """.trimMargin())
                } ?: Log.w(TAG, "⚠️ Nelze nastavit úroveň pásma - equalizer není inicializován")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Chyba při nastavování úrovně pásma", e)
            }
        }
    }

    fun release() {
        lock.withLock {
            try {
                equalizer?.let { eq ->
                    eq.enabled = false
                    eq.release()
                    Log.d(TAG, "✅ Equalizer uvolněn")
                }
                equalizer = null
                audioSessionId = -1
            } catch (e: Exception) {
                Log.e(TAG, "❌ Chyba při uvolňování equalizeru", e)
            }
        }
    }

    companion object {
        private const val TAG = "EqualizerManager"
    }
} 
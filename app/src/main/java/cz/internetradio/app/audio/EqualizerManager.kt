package cz.internetradio.app.audio

import android.content.Context
import android.media.audiofx.Equalizer
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.locks.ReentrantLock
import javax.inject.Inject
import javax.inject.Singleton
import cz.internetradio.app.model.EqualizerPreset
import kotlin.concurrent.withLock

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
        if (sessionId == -1) {
            return
        }

        lock.withLock {
            try {
                if (audioSessionId != sessionId || equalizer == null) {
                    // Uložení aktuálních hodnot před uvolněním
                    if (equalizer != null) {
                        currentBandLevels = (0 until equalizer!!.numberOfBands.toInt()).map { band ->
                            equalizer!!.getBandLevel(band.toShort()) / 100
                        }.toMutableList()
                    }
                    
                    releaseInternal()
                    audioSessionId = sessionId

                    equalizer = try {
                        Equalizer(0, sessionId).also { eq ->
                            eq.enabled = isEnabled
                            
                            // Obnovení uložených hodnot
                            if (currentBandLevels.isNotEmpty() && currentBandLevels.size == eq.numberOfBands.toInt()) {
                                currentBandLevels.forEachIndexed { index, level ->
                                    val milliLevel = (level * 100).toShort()
                                    eq.setBandLevel(index.toShort(), milliLevel)
                                }
                            } else {
                                // Inicializace nových hodnot
                                currentBandLevels = (0 until eq.numberOfBands.toInt()).map { 0 }.toMutableList()
                            }
                        }
                    } catch (e: Exception) {
                        if (cz.internetradio.app.BuildConfig.DEBUG) Log.e(TAG, "Chyba při vytváření equalizeru", e)
                        null
                    }
                } else {
                    equalizer?.let { eq ->
                        eq.enabled = isEnabled
                    }
                }
            } catch (e: Exception) {
                if (cz.internetradio.app.BuildConfig.DEBUG) Log.e(TAG, "Chyba při nastavování equalizeru", e)
                releaseInternal()
                throw e
            }
        }
    }

    fun setEnabled(enabled: Boolean) {
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
                }
            } catch (e: Exception) {
                if (cz.internetradio.app.BuildConfig.DEBUG) Log.e(TAG, "Chyba při nastavování enabled stavu", e)
                Unit
            }
        }
    }

    fun applyPreset(preset: EqualizerPreset) {
        lock.withLock {
            try {
                equalizer?.let { eq ->
                    if (!eq.enabled) {
                        eq.enabled = true
                        isEnabled = true
                    }

                    preset.bands.forEachIndexed { index, gain ->
                        if (index < eq.numberOfBands.toInt()) {
                            val milliGain = (gain * 100).toInt().coerceIn(
                                eq.bandLevelRange[0].toInt(),
                                eq.bandLevelRange[1].toInt()
                            ).toShort()
                            
                            eq.setBandLevel(index.toShort(), milliGain)
                            // Uložení hodnoty do našeho stavu
                            if (index < currentBandLevels.size) {
                                currentBandLevels[index] = gain.toInt()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                 if (cz.internetradio.app.BuildConfig.DEBUG) Log.e(TAG, "Chyba při aplikování presetu: ${e.message}")
                 Unit
            }
        }
    }

    fun setBandLevel(band: Int, level: Int) {
        lock.withLock {
            try {
                equalizer?.let { eq ->
                    if (!eq.enabled) {
                        return@let
                    }

                    val bandRange = eq.bandLevelRange
                    if (bandRange == null) {
                        return@let
                    }

                    val levelInMilliBels = (level * 100).coerceIn(
                        bandRange[0].toInt(),
                        bandRange[1].toInt()
                    ).toShort()

                    eq.setBandLevel(band.toShort(), levelInMilliBels)
                    // Uložení hodnoty do našeho stavu
                    if (band < currentBandLevels.size) {
                        currentBandLevels[band] = level
                    }
                }
            } catch (e: Exception) {
                if (cz.internetradio.app.BuildConfig.DEBUG) Log.e(TAG, "Chyba při nastavování úrovně pásma", e)
                Unit
            }
        }
    }

    fun release() {
        lock.withLock {
            releaseInternal()
        }
    }

    private fun releaseInternal() {
        try {
            equalizer?.let { eq ->
                eq.enabled = false
                eq.release()
            }
            equalizer = null
            audioSessionId = -1
        } catch (e: Exception) {
            if (cz.internetradio.app.BuildConfig.DEBUG) Log.e(TAG, "Chyba při uvolňování equalizeru", e)
        }
    }

    companion object {
        private const val TAG = "EqualizerManager"
    }
} 

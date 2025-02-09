package cz.internetradio.app.audio

import android.content.Context
import android.media.audiofx.Equalizer
import cz.internetradio.app.model.EqualizerPreset
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext

@Singleton
class EqualizerManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var equalizer: Equalizer? = null
    private var audioSessionId: Int = -1

    fun setupEqualizer(sessionId: Int) {
        if (audioSessionId != sessionId) {
            release()
            audioSessionId = sessionId
            equalizer = Equalizer(0, sessionId).apply {
                enabled = true
            }
        }
    }

    fun setEnabled(enabled: Boolean) {
        equalizer?.enabled = enabled
    }

    fun applyPreset(preset: EqualizerPreset) {
        equalizer?.let { eq ->
            preset.bands.forEachIndexed { index, gain ->
                // Převod z dB na milibely (1 dB = 100 mB)
                val milliGain = (gain * 100).toInt().toShort()
                if (index < eq.numberOfBands) {
                    eq.setBandLevel(index.toShort(), milliGain)
                }
            }
        }
    }

    fun setBandLevel(band: Int, gain: Float) {
        equalizer?.let { eq ->
            if (band < eq.numberOfBands) {
                // Převod z dB na milibely
                val milliGain = (gain * 100).toInt().toShort()
                eq.setBandLevel(band.toShort(), milliGain)
            }
        }
    }

    fun release() {
        equalizer?.release()
        equalizer = null
        audioSessionId = -1
    }
} 
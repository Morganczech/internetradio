package cz.internetradio.app.audio

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.jtransforms.fft.FloatFFT_1D

class AudioSpectrumProcessor : AudioProcessor {
    private var inputEnded = false
    private var inputFormat: AudioFormat? = null
    private var outputFormat: AudioFormat? = null
    private var buffer: ByteBuffer = ByteBuffer.allocate(0)
    private var fft: FloatFFT_1D? = null
    private val fftSize = 2048 // Velikost FFT (musí být mocnina 2)
    private val bandCount = 32 // Počet výsledných frekvenčních pásem
    
    private val _spectrumData = MutableStateFlow(FloatArray(bandCount) { 0f })
    val spectrumData: StateFlow<FloatArray> = _spectrumData

    override fun configure(inputAudioFormat: AudioFormat): AudioFormat {
        // Podporujeme pouze PCM 16bit
        require(inputAudioFormat.encoding == C.ENCODING_PCM_16BIT) {
            "Unsupported encoding: ${inputAudioFormat.encoding}"
        }

        inputFormat = inputAudioFormat
        outputFormat = inputAudioFormat
        buffer = ByteBuffer.allocate(fftSize * 2) // *2 protože máme 16bit vzorky
        buffer.order(ByteOrder.nativeOrder())
        fft = FloatFFT_1D(fftSize.toLong())
        
        return inputAudioFormat
    }

    override fun isActive(): Boolean = true

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!inputBuffer.hasRemaining()) {
            return
        }

        // Načtení dat do bufferu
        while (inputBuffer.hasRemaining() && buffer.hasRemaining()) {
            buffer.put(inputBuffer.get())
        }

        // Když je buffer plný, provedeme FFT
        if (!buffer.hasRemaining()) {
            processBuffer()
        }
    }

    private fun processBuffer() {
        buffer.flip()
        val samples = FloatArray(fftSize)
        
        // Převod na float a aplikace Hammingova okna
        for (i in 0 until fftSize) {
            if (buffer.remaining() >= 2) {
                val sample = buffer.short / 32768f // Normalizace na -1.0 až 1.0
                samples[i] = sample * (0.54f - 0.46f * Math.cos(2.0 * Math.PI * i / (fftSize - 1)).toFloat())
            }
        }

        // FFT transformace
        fft?.realForward(samples)

        // Výpočet magnitud a rozdělení do pásem
        val bands = FloatArray(bandCount)
        val samplesPerBand = (fftSize / 4) / bandCount // Používáme jen první čtvrtinu FFT (užitečné frekvence)
        
        for (i in 0 until bandCount) {
            var magnitude = 0f
            val startBin = i * samplesPerBand
            val endBin = startBin + samplesPerBand
            
            for (j in startBin until endBin) {
                if (j * 2 + 1 < samples.size) {
                    val real = samples[j * 2]
                    val imag = samples[j * 2 + 1]
                    magnitude += Math.sqrt((real * real + imag * imag).toDouble()).toFloat()
                }
            }
            
            bands[i] = magnitude / samplesPerBand
        }

        // Normalizace a vyhlazení
        val maxMagnitude = bands.maxOrNull() ?: 1f
        for (i in bands.indices) {
            bands[i] = (bands[i] / maxMagnitude).coerceIn(0f, 1f)
        }

        _spectrumData.value = bands
        buffer.clear()
    }

    override fun queueEndOfStream() {
        inputEnded = true
    }

    override fun getOutput(): ByteBuffer = ByteBuffer.allocate(0)

    override fun isEnded(): Boolean = inputEnded

    override fun flush() {
        buffer.clear()
        inputEnded = false
    }

    override fun reset() {
        flush()
        inputFormat = null
        outputFormat = null
        buffer = ByteBuffer.allocate(0)
        fft = null
    }
} 
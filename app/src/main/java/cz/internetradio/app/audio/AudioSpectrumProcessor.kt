package cz.internetradio.app.audio

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import java.nio.ByteBuffer

class AudioSpectrumProcessor : AudioProcessor {
    private var inputEnded = false
    private var inputFormat: AudioFormat? = null
    private var outputFormat: AudioFormat? = null

    override fun configure(inputAudioFormat: AudioFormat): AudioFormat {
        inputFormat = inputAudioFormat
        outputFormat = inputAudioFormat
        return inputAudioFormat
    }

    override fun isActive(): Boolean = true

    override fun queueInput(inputBuffer: ByteBuffer) {
        // Prázdná implementace - žádné zpracování
    }

    override fun queueEndOfStream() {
        inputEnded = true
    }

    override fun getOutput(): ByteBuffer = ByteBuffer.allocate(0)

    override fun isEnded(): Boolean = inputEnded

    override fun flush() {
        inputEnded = false
    }

    override fun reset() {
        flush()
        inputFormat = null
        outputFormat = null
    }
} 
package cz.internetradio.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.metadata.Metadata
import cz.internetradio.app.model.Radio
import cz.internetradio.app.repository.RadioRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RadioViewModel @Inject constructor(
    private val radioRepository: RadioRepository,
    private val exoPlayer: ExoPlayer
) : ViewModel() {

    private val _currentRadio = MutableStateFlow<Radio?>(null)
    val currentRadio: StateFlow<Radio?> = _currentRadio

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _currentMetadata = MutableStateFlow<String?>(null)
    val currentMetadata: StateFlow<String?> = _currentMetadata

    private val _volume = MutableStateFlow(1f)
    val volume: StateFlow<Float> = _volume

    private val _sleepTimerMinutes = MutableStateFlow<Int?>(null)
    val sleepTimerMinutes: StateFlow<Int?> = _sleepTimerMinutes

    val radioStations = radioRepository.getRadioStations()

    init {
        setupPlayerListener()
    }

    private fun setupPlayerListener() {
        exoPlayer.addListener(object : Player.Listener {
            override fun onMetadata(metadata: Metadata) {
                if (metadata.length() > 0) {
                    val rawMetadata = metadata.get(0)?.toString() ?: return
                    // Extrahujeme jen název skladby z metadat
                    val cleanMetadata = when {
                        rawMetadata.contains("title=") -> {
                            rawMetadata.substringAfter("title=\"")
                                .substringBefore("\"")
                                .takeIf { it.isNotBlank() && it != "null" }
                        }
                        rawMetadata.contains("StreamTitle=") -> {
                            rawMetadata.substringAfter("StreamTitle='")
                                .substringBefore("'")
                                .takeIf { it.isNotBlank() && it != "null" }
                        }
                        else -> null
                    }
                    _currentMetadata.value = cleanMetadata
                }
            }
        })
    }

    fun playRadio(radio: Radio) {
        _currentRadio.value = radio
        val mediaItem = MediaItem.fromUri(radio.streamUrl)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.play()
        _isPlaying.value = true
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
            _isPlaying.value = false
        } else {
            exoPlayer.play()
            _isPlaying.value = true
        }
    }

    fun setVolume(newVolume: Float) {
        val clampedVolume = newVolume.coerceIn(0f, 1f)
        exoPlayer.volume = clampedVolume
        _volume.value = clampedVolume
    }

    fun setSleepTimer(minutes: Int?) {
        _sleepTimerMinutes.value = minutes
        
        if (minutes != null) {
            viewModelScope.launch {
                delay(minutes * 60 * 1000L)
                if (_sleepTimerMinutes.value == minutes) {
                    exoPlayer.pause()
                    _isPlaying.value = false
                    _sleepTimerMinutes.value = null
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        exoPlayer.release()
    }
} 
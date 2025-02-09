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
import kotlinx.coroutines.flow.*
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

    private val _remainingTimeMinutes = MutableStateFlow<Int?>(null)
    val remainingTimeMinutes: StateFlow<Int?> = _remainingTimeMinutes

    private val _showOnlyFavorites = MutableStateFlow(false)
    val showOnlyFavorites: StateFlow<Boolean> = _showOnlyFavorites

    val radioStations: StateFlow<List<Radio>> = if (_showOnlyFavorites.value) {
        radioRepository.getFavoriteRadios().stateIn(
            viewModelScope,
            SharingStarted.Lazily,
            emptyList()
        )
    } else {
        radioRepository.getAllRadios().stateIn(
            viewModelScope,
            SharingStarted.Lazily,
            emptyList()
        )
    }

    init {
        setupPlayerListener()
        initializeDatabase()
    }

    private fun initializeDatabase() {
        viewModelScope.launch {
            radioRepository.initializeDefaultRadios()
        }
    }

    private fun setupPlayerListener() {
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                _isPlaying.value = state == Player.STATE_READY && exoPlayer.playWhenReady
            }

            override fun onMetadata(metadata: Metadata) {
                if (metadata.length() > 0) {
                    val rawMetadata = metadata.get(0)?.toString() ?: return
                    val cleanMetadata = when {
                        rawMetadata.contains("title=") -> {
                            rawMetadata.substringAfter("title=\"")
                                .substringBefore("\"")
                                .takeIf { it.isNotBlank() && it != "null" }
                                ?.let { java.net.URLDecoder.decode(it, "UTF-8") }
                        }
                        rawMetadata.contains("StreamTitle=") -> {
                            rawMetadata.substringAfter("StreamTitle='")
                                .substringBefore("'")
                                .takeIf { it.isNotBlank() && it != "null" }
                                ?.let { java.net.URLDecoder.decode(it, "UTF-8") }
                        }
                        else -> null
                    }
                    _currentMetadata.value = cleanMetadata
                }
            }

            override fun onPlayerError(error: com.google.android.exoplayer2.PlaybackException) {
                _isPlaying.value = false
            }
        })
    }

    fun getAllRadios(): Flow<List<Radio>> = radioRepository.getAllRadios()

    fun getFavoriteRadios(): Flow<List<Radio>> = radioRepository.getFavoriteRadios()

    fun toggleFavorite(radio: Radio) {
        viewModelScope.launch {
            if (radio.isFavorite) {
                radioRepository.removeFavorite(radio.id)
            } else {
                radioRepository.toggleFavorite(radio.id)
            }
        }
    }

    fun playRadio(radio: Radio) {
        viewModelScope.launch {
            try {
                _currentRadio.value = radio
                exoPlayer.stop()
                exoPlayer.clearMediaItems()
                val mediaItem = MediaItem.fromUri(radio.streamUrl)
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.prepare()
                exoPlayer.playWhenReady = true
                _isPlaying.value = true
            } catch (e: Exception) {
                // Handle error
                _isPlaying.value = false
            }
        }
    }

    fun stopPlayback() {
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        _currentRadio.value = null
        _isPlaying.value = false
        _currentMetadata.value = null
    }

    fun togglePlayPause() {
        if (_isPlaying.value) {
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
        _remainingTimeMinutes.value = minutes
        
        viewModelScope.launch {
            if (minutes != null) {
                val startTime = System.currentTimeMillis()
                val endTime = startTime + (minutes * 60 * 1000L)

                while (System.currentTimeMillis() < endTime && _sleepTimerMinutes.value == minutes) {
                    val remaining = ((endTime - System.currentTimeMillis()) / 1000L / 60L).toInt()
                    _remainingTimeMinutes.value = remaining
                    delay(1000) // Aktualizace každou sekundu
                }

                if (_sleepTimerMinutes.value == minutes) {
                    stopPlayback()
                    _sleepTimerMinutes.value = null
                    _remainingTimeMinutes.value = null
                }
            } else {
                _remainingTimeMinutes.value = null
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        exoPlayer.release()
    }
} 
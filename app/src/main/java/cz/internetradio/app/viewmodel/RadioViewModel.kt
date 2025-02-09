package cz.internetradio.app.viewmodel

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import cz.internetradio.app.model.Radio
import cz.internetradio.app.repository.RadioRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RadioViewModel @Inject constructor(
    private val radioRepository: RadioRepository,
    private val exoPlayer: ExoPlayer,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val prefs: SharedPreferences = context.getSharedPreferences("radio_prefs", Context.MODE_PRIVATE)

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
        loadSavedState()
    }

    private fun initializeDatabase() {
        viewModelScope.launch {
            radioRepository.initializeDefaultRadios()
        }
    }

    private fun setupPlayerListener() {
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                _isPlaying.value = playbackState == Player.STATE_READY && exoPlayer.playWhenReady
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
            }

            override fun onMediaMetadataChanged(mediaMetadata: androidx.media3.common.MediaMetadata) {
                val title = mediaMetadata.title?.toString()?.let { decodeMetadata(it) }
                val artist = mediaMetadata.artist?.toString()?.let { decodeMetadata(it) }
                
                val metadata = when {
                    !title.isNullOrBlank() && !artist.isNullOrBlank() -> "$artist - $title"
                    !title.isNullOrBlank() -> title
                    !artist.isNullOrBlank() -> artist
                    else -> null
                }
                
                _currentMetadata.value = metadata?.let { decodeMetadata(it) }
            }

            private fun decodeMetadata(text: String): String {
                return try {
                    java.net.URLDecoder.decode(
                        text
                            .replace("&amp;", "&")
                            .replace("&lt;", "<")
                            .replace("&gt;", ">")
                            .replace("&quot;", "\"")
                            .replace("&#039;", "'")
                            .replace("&#39;", "'")
                            .replace("&#x27;", "'")
                            .replace("&apos;", "'")
                            .replace("+", " ")
                            .replace("\\", "")
                            .replace("%C3%A1", "á")
                            .replace("%C3%A9", "é")
                            .replace("%C3%AD", "í")
                            .replace("%C3%BD", "ý")
                            .replace("%C3%B3", "ó")
                            .replace("%C3%BA", "ú")
                            .replace("%C4%8D", "č")
                            .replace("%C4%8F", "ď")
                            .replace("%C4%9B", "ě")
                            .replace("%C5%88", "ň")
                            .replace("%C5%99", "ř")
                            .replace("%C5%A1", "š")
                            .replace("%C5%A5", "ť")
                            .replace("%C5%AF", "ů")
                            .replace("%C5%BE", "ž")
                            .replace("%20", " "),
                        "UTF-8"
                    ).trim()
                } catch (e: Exception) {
                    text.trim()
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                _isPlaying.value = false
            }
        })
    }

    private fun loadSavedState() {
        viewModelScope.launch {
            // Načtení uložené hlasitosti
            val savedVolume = prefs.getFloat("volume", 1.0f)
            setVolume(savedVolume)

            // Načtení posledního přehrávaného rádia
            val lastRadioId = prefs.getString("last_radio_id", null)
            if (lastRadioId != null) {
                val lastRadio = radioRepository.getRadioById(lastRadioId)
                lastRadio?.let { radio ->
                    _currentRadio.value = radio
                    preparePlayer(radio.streamUrl)
                    exoPlayer.playWhenReady = false
                    _isPlaying.value = false
                }
            }
        }
    }

    private fun preparePlayer(url: String) {
        val mediaItem = MediaItem.fromUri(url)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
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
                // Uložení ID posledního rádia
                prefs.edit().putString("last_radio_id", radio.id).apply()
            } catch (e: Exception) {
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
        // Uložení hlasitosti
        prefs.edit().putFloat("volume", clampedVolume).apply()
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
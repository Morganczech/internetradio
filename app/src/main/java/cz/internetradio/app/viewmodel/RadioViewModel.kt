package cz.internetradio.app.viewmodel

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.util.UnstableApi
import cz.internetradio.app.model.Radio
import cz.internetradio.app.repository.RadioRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import cz.internetradio.app.model.EqualizerPreset
import cz.internetradio.app.audio.EqualizerManager
import androidx.media3.common.C
import cz.internetradio.app.audio.AudioSpectrumProcessor
import android.util.Log
import com.google.android.gms.wearable.*
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable

@OptIn(UnstableApi::class)
@HiltViewModel
class RadioViewModel @Inject constructor(
    private val radioRepository: RadioRepository,
    private val exoPlayer: ExoPlayer,
    private val equalizerManager: EqualizerManager,
    private val audioSpectrumProcessor: AudioSpectrumProcessor,
    @ApplicationContext private val context: Context
) : ViewModel(), DataClient.OnDataChangedListener {

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

    private val _showMaxFavoritesError = MutableStateFlow(false)
    val showMaxFavoritesError: StateFlow<Boolean> = _showMaxFavoritesError

    private val _maxFavorites = MutableStateFlow(8)
    val maxFavorites: StateFlow<Int> = _maxFavorites

    private val _currentPreset = MutableStateFlow(EqualizerPreset.NORMAL)
    val currentPreset: StateFlow<EqualizerPreset> = _currentPreset

    private val _equalizerEnabled = MutableStateFlow(false)
    val equalizerEnabled: StateFlow<Boolean> = _equalizerEnabled

    private val _bandValues = MutableStateFlow(List(5) { 0f })
    val bandValues: StateFlow<List<Float>> = _bandValues

    private val frequencies = listOf(60f, 230f, 910f, 3600f, 14000f)  // Hz

    companion object {
        const val DEFAULT_MAX_FAVORITES = 8
        const val PREFS_MAX_FAVORITES = "max_favorites"
    }

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
        _maxFavorites.value = prefs.getInt(PREFS_MAX_FAVORITES, DEFAULT_MAX_FAVORITES)
        _bandValues.value = _currentPreset.value.bands
        loadEqualizerState()
        setupWearableListener()
        
        // Nastavení equalizeru pro ExoPlayer
        equalizerManager.setupEqualizer(exoPlayer.audioSessionId)
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

    fun setMaxFavorites(value: Int) {
        _maxFavorites.value = value
        prefs.edit().putInt(PREFS_MAX_FAVORITES, value).apply()
    }

    fun toggleFavorite(radio: Radio) {
        viewModelScope.launch {
            if (radio.isFavorite) {
                radioRepository.removeFavorite(radio.id)
            } else {
                val favoriteCount = radioRepository.getFavoriteRadios().first().size
                if (favoriteCount >= maxFavorites.value) {
                    _showMaxFavoritesError.value = true
                    return@launch
                }
                radioRepository.toggleFavorite(radio.id)
            }
        }
    }

    fun dismissMaxFavoritesError() {
        _showMaxFavoritesError.value = false
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
                prefs.edit().putString("last_radio_id", radio.id).apply()
                updateWearableState()
            } catch (e: Exception) {
                _isPlaying.value = false
                updateWearableState()
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
        updateWearableState()
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

    private fun loadEqualizerState() {
        _equalizerEnabled.value = prefs.getBoolean("equalizer_enabled", false)
        val presetName = prefs.getString("equalizer_preset", EqualizerPreset.NORMAL.name)
        _currentPreset.value = EqualizerPreset.valueOf(presetName ?: EqualizerPreset.NORMAL.name)
        applyEqualizerSettings()
    }

    fun setEqualizerEnabled(enabled: Boolean) {
        _equalizerEnabled.value = enabled
        prefs.edit().putBoolean("equalizer_enabled", enabled).apply()
        equalizerManager.setEnabled(enabled)
        applyEqualizerSettings()
    }

    fun setEqualizerPreset(preset: EqualizerPreset) {
        _currentPreset.value = preset
        _bandValues.value = preset.bands
        prefs.edit().putString("equalizer_preset", preset.name).apply()
        if (_equalizerEnabled.value) {
            equalizerManager.applyPreset(preset)
        }
    }

    fun setBandValue(index: Int, value: Float) {
        val newValues = _bandValues.value.toMutableList()
        newValues[index] = value
        _bandValues.value = newValues
        if (_equalizerEnabled.value) {
            equalizerManager.setBandLevel(index, value)
        }
    }

    private fun applyEqualizerSettings() {
        if (_equalizerEnabled.value) {
            equalizerManager.setEnabled(true)
            equalizerManager.applyPreset(_currentPreset.value)
        } else {
            equalizerManager.setEnabled(false)
        }
    }

    private fun setupWearableListener() {
        Wearable.getDataClient(context).addListener(this)
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents.forEach { event ->
            if (event.type == DataEvent.TYPE_CHANGED) {
                val dataItem = event.dataItem
                when (dataItem.uri.path) {
                    "/command" -> {
                        DataMapItem.fromDataItem(dataItem).dataMap.apply {
                            when (getString("action")) {
                                "play_pause" -> togglePlayPause()
                                "next" -> playNextFavorite()
                                "previous" -> playPreviousFavorite()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun updateWearableState() {
        viewModelScope.launch {
            val request = PutDataMapRequest.create("/radio_state").apply {
                dataMap.putString("radio_name", _currentRadio.value?.name ?: "")
                dataMap.putBoolean("is_playing", _isPlaying.value)
            }.asPutDataRequest()
            
            Wearable.getDataClient(context).putDataItem(request)
        }
    }

    fun playNextFavorite() {
        viewModelScope.launch {
            val favoriteRadios = getFavoriteRadios().first()
            val currentIndex = favoriteRadios.indexOfFirst { it.id == _currentRadio.value?.id }
            if (currentIndex < favoriteRadios.size - 1) {
                playRadio(favoriteRadios[currentIndex + 1])
            }
        }
    }

    fun playPreviousFavorite() {
        viewModelScope.launch {
            val favoriteRadios = getFavoriteRadios().first()
            val currentIndex = favoriteRadios.indexOfFirst { it.id == _currentRadio.value?.id }
            if (currentIndex > 0) {
                playRadio(favoriteRadios[currentIndex - 1])
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        equalizerManager.release()
        exoPlayer.release()
        Wearable.getDataClient(context).removeListener(this)
    }
} 
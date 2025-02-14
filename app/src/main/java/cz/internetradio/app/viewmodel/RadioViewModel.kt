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
import android.content.Intent
import cz.internetradio.app.service.RadioService
import android.content.BroadcastReceiver
import android.content.IntentFilter

@OptIn(UnstableApi::class)
@HiltViewModel
class RadioViewModel @Inject constructor(
    private val radioRepository: RadioRepository,
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

    private val serviceReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d("RadioViewModel", "Přijat broadcast: ${intent.action}")
            when (intent.action) {
                RadioService.ACTION_PLAYBACK_STATE_CHANGED -> {
                    val isPlaying = intent.getBooleanExtra(RadioService.EXTRA_IS_PLAYING, false)
                    val metadata = intent.getStringExtra(RadioService.EXTRA_METADATA)
                    val radioId = intent.getStringExtra(RadioService.EXTRA_CURRENT_RADIO)
                    
                    Log.d("RadioViewModel", "Přijat stav: playing=$isPlaying, metadata=$metadata, radioId=$radioId")
                    
                    viewModelScope.launch {
                        try {
                            // Aktualizace rádia
                            if (radioId != null) {
                                val radio = radioRepository.getRadioById(radioId)
                                if (radio != null) {
                                    Log.d("RadioViewModel", "Aktualizuji rádio na: ${radio.name}")
                                    _currentRadio.value = radio
                                } else {
                                    Log.e("RadioViewModel", "Rádio s ID $radioId nebylo nalezeno")
                                }
                            } else {
                                Log.d("RadioViewModel", "Resetuji aktuální rádio (radioId je null)")
                                _currentRadio.value = null
                            }

                            // Aktualizace stavu přehrávání
                            if (_isPlaying.value != isPlaying) {
                                Log.d("RadioViewModel", "Aktualizuji stav přehrávání na: $isPlaying")
                                _isPlaying.value = isPlaying
                            }

                            // Aktualizace metadat
                            if (_currentMetadata.value != metadata) {
                                Log.d("RadioViewModel", "Aktualizuji metadata na: $metadata")
                                _currentMetadata.value = metadata
                            }

                            // Aktualizace stavu pro wearable
                            updateWearableState()
                        } catch (e: Exception) {
                            Log.e("RadioViewModel", "Chyba při zpracování broadcastu: ${e.message}")
                        }
                    }
                }
            }
        }
    }

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
        initializeDatabase()
        loadSavedState()
        _maxFavorites.value = prefs.getInt(PREFS_MAX_FAVORITES, DEFAULT_MAX_FAVORITES)
        _bandValues.value = _currentPreset.value.bands
        loadEqualizerState()
        setupWearableListener()
        
        try {
            // Registrace broadcast receiveru s explicitním povolením exportu
            val filter = IntentFilter(RadioService.ACTION_PLAYBACK_STATE_CHANGED)
            context.registerReceiver(
                serviceReceiver,
                filter,
                Context.RECEIVER_EXPORTED
            )
            Log.d("RadioViewModel", "Broadcast receiver úspěšně zaregistrován")
        } catch (e: Exception) {
            Log.e("RadioViewModel", "Chyba při registraci receiveru: ${e.message}")
        }
    }

    private fun initializeDatabase() {
        viewModelScope.launch {
            radioRepository.initializeDefaultRadios()
        }
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
                }
            }
        }
    }

    fun playRadio(radio: Radio) {
        viewModelScope.launch {
            try {
                Log.d("RadioViewModel", "Spouštím rádio: ${radio.name}")
                _currentRadio.value = radio
                prefs.edit().putString("last_radio_id", radio.id).apply()
                updateWearableState()
                
                // Spuštění služby
                val intent = Intent(context, RadioService::class.java).apply {
                    action = RadioService.ACTION_PLAY
                    putExtra(RadioService.EXTRA_RADIO_ID, radio.id)
                }
                context.startForegroundService(intent)
            } catch (e: Exception) {
                Log.e("RadioViewModel", "Chyba při spouštění rádia: ${e.message}")
                _isPlaying.value = false
                updateWearableState()
            }
        }
    }

    fun stopPlayback() {
        _currentRadio.value = null
        _isPlaying.value = false
        _currentMetadata.value = null
        
        // Zastavení služby
        val intent = Intent(context, RadioService::class.java)
        context.stopService(intent)
    }

    fun togglePlayPause() {
        if (_isPlaying.value) {
            // Pozastavení přehrávání v službě
            val intent = Intent(context, RadioService::class.java).apply {
                action = RadioService.ACTION_PAUSE
            }
            context.startForegroundService(intent)
        } else {
            // Obnovení přehrávání v službě
            _currentRadio.value?.let { radio ->
                val intent = Intent(context, RadioService::class.java).apply {
                    action = RadioService.ACTION_PLAY
                    putExtra(RadioService.EXTRA_RADIO_ID, radio.id)
                }
                context.startForegroundService(intent)
            }
        }
    }

    fun setVolume(newVolume: Float) {
        val clampedVolume = newVolume.coerceIn(0f, 1f)
        _volume.value = clampedVolume
        // Uložení hlasitosti
        prefs.edit().putFloat("volume", clampedVolume).apply()
        
        // Odeslání změny hlasitosti do služby
        val intent = Intent(context, RadioService::class.java).apply {
            action = RadioService.ACTION_SET_VOLUME
            putExtra(RadioService.EXTRA_VOLUME, clampedVolume)
        }
        context.startForegroundService(intent)
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

    fun deleteRadio(radio: Radio) {
        viewModelScope.launch {
            radioRepository.deleteRadio(radio)
            // Pokud je smazané rádio právě přehrávané, zastav přehrávání
            if (radio.id == currentRadio.value?.id) {
                stopPlayback()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        equalizerManager.release()
        Wearable.getDataClient(context).removeListener(this)
        context.unregisterReceiver(serviceReceiver)
    }
} 
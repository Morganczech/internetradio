package cz.internetradio.app.viewmodel

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.util.UnstableApi
import cz.internetradio.app.model.Radio
import cz.internetradio.app.model.RadioStation
import cz.internetradio.app.repository.RadioRepository
import cz.internetradio.app.repository.FavoriteSongRepository
import cz.internetradio.app.model.FavoriteSong
import cz.internetradio.app.api.SearchParams
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
import cz.internetradio.app.model.RadioCategory
import cz.internetradio.app.location.LocationService
import com.google.gson.Gson
import android.content.Intent
import cz.internetradio.app.service.RadioService
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.content.ClipboardManager
import android.content.ClipData
import android.net.Uri
import kotlinx.serialization.json.Json
import cz.internetradio.app.model.AppSettings
import cz.internetradio.app.model.SerializableRadio
import cz.internetradio.app.model.SerializableSong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.graphics.Color
import cz.internetradio.app.model.Language
import cz.internetradio.app.R
import cz.internetradio.app.data.entity.RadioEntity
import cz.internetradio.app.ui.theme.Gradients

private data class Country(
    val name: String,
    val stations: List<PopularStation>
)

private data class PopularStation(
    val id: String,
    val name: String,
    val streamUrl: String,
    val imageUrl: String,
    val description: String,
    val bitrate: Int? = null
)

@OptIn(UnstableApi::class)
@HiltViewModel
class RadioViewModel @Inject constructor(
    private val radioRepository: RadioRepository,
    private val favoriteSongRepository: FavoriteSongRepository,
    private val exoPlayer: ExoPlayer,
    private val equalizerManager: EqualizerManager,
    private val audioSpectrumProcessor: AudioSpectrumProcessor,
    @ApplicationContext private val context: Context,
    private val locationService: LocationService
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

    private val _remainingTimeSeconds = MutableStateFlow<Int?>(null)
    val remainingTimeSeconds: StateFlow<Int?> = _remainingTimeSeconds

    private val _showOnlyFavorites = MutableStateFlow(false)
    val showOnlyFavorites: StateFlow<Boolean> = _showOnlyFavorites

    private val _showMaxFavoritesError = MutableStateFlow(false)
    val showMaxFavoritesError: StateFlow<Boolean> = _showMaxFavoritesError

    private val _maxFavorites = MutableStateFlow(10)
    val maxFavorites: StateFlow<Int> = _maxFavorites

    private val _currentPreset = MutableStateFlow(EqualizerPreset.NORMAL)
    val currentPreset: StateFlow<EqualizerPreset> = _currentPreset

    private val _currentSongSaved = MutableStateFlow(false)
    val currentSongSaved: StateFlow<Boolean> = _currentSongSaved

    private val _showSongSavedMessage = MutableStateFlow<String?>(null)
    val showSongSavedMessage: StateFlow<String?> = _showSongSavedMessage

    private val _equalizerEnabled = MutableStateFlow(false)
    val equalizerEnabled: StateFlow<Boolean> = _equalizerEnabled

    private val _bandValues = MutableStateFlow(List(5) { 0f })
    val bandValues: StateFlow<List<Float>> = _bandValues

    private val frequencies = listOf(60f, 230f, 910f, 3600f, 14000f)  // Hz

    private val _localStations = MutableStateFlow<List<RadioStation>?>(null)
    val localStations: StateFlow<List<RadioStation>?> = _localStations

    private val _currentCountryCode = MutableStateFlow<String?>(null)
    val currentCountryCode: StateFlow<String?> = _currentCountryCode

    private val _fadeOutDuration = MutableStateFlow(60)
    val fadeOutDuration: StateFlow<Int> = _fadeOutDuration

    private val _currentCategory = MutableStateFlow<RadioCategory?>(null)
    val currentCategory: StateFlow<RadioCategory?> = _currentCategory

    private val _currentLanguage = MutableStateFlow(Language.SYSTEM)
    val currentLanguage: StateFlow<Language> = _currentLanguage

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    private val serviceReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d("RadioViewModel", "📻 Přijat broadcast")
            
            if (context == null || intent == null) {
                Log.e("RadioViewModel", "❌ Kontext nebo intent je null")
                return
            }

            try {
                val isPlaying = intent.getBooleanExtra(RadioService.EXTRA_IS_PLAYING, false)
                val metadata = intent.getStringExtra(RadioService.EXTRA_METADATA)
                val currentRadioId = intent.getStringExtra(RadioService.EXTRA_CURRENT_RADIO)
                val audioSessionId = intent.getIntExtra(RadioService.EXTRA_AUDIO_SESSION_ID, -1)

                Log.d("RadioViewModel", "📻 Přijatá data:")
                Log.d("RadioViewModel", "- isPlaying: $isPlaying")
                Log.d("RadioViewModel", "- metadata: $metadata")
                Log.d("RadioViewModel", "- currentRadioId: $currentRadioId")
                Log.d("RadioViewModel", "- audioSessionId: $audioSessionId")

                _isPlaying.value = isPlaying
                _currentMetadata.value = metadata

                if (currentRadioId != null) {
                    viewModelScope.launch {
                        try {
                            val radio = radioRepository.getRadioById(currentRadioId)
                            _currentRadio.value = radio
                            Log.d("RadioViewModel", "✅ Aktualizováno rádio: ${radio?.name}")
                        } catch (e: Exception) {
                            Log.e("RadioViewModel", "❌ Chyba při načítání rádia: ${e.message}")
                        }
                    }
                }

                if (audioSessionId != -1) {
                    Log.d("RadioViewModel", "🎵 Nastavuji equalizer s audio session ID: $audioSessionId")
                    viewModelScope.launch {
                        try {
                            equalizerManager.setupEqualizer(audioSessionId)
                            Log.d("RadioViewModel", "✅ Equalizer nastaven")
                        } catch (e: Exception) {
                            Log.e("RadioViewModel", "❌ Chyba při nastavování equalizeru: ${e.message}")
                            e.printStackTrace()
                        }
                    }
                } else {
                    Log.w("RadioViewModel", "⚠️ Neplatné audio session ID")
                }
            } catch (e: Exception) {
                Log.e("RadioViewModel", "❌ Chyba při zpracování broadcastu: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    companion object {
        const val DEFAULT_MAX_FAVORITES = 10
        const val PREFS_MAX_FAVORITES = "max_favorites"
        const val PREFS_FADE_OUT_DURATION = "fade_out_duration"
        const val DEFAULT_FADE_OUT_DURATION = 60 // sekund
        const val PREFS_LANGUAGE = "language"
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
        loadSavedState()
        _maxFavorites.value = prefs.getInt(PREFS_MAX_FAVORITES, DEFAULT_MAX_FAVORITES)
        _fadeOutDuration.value = prefs.getInt(PREFS_FADE_OUT_DURATION, DEFAULT_FADE_OUT_DURATION)
        _bandValues.value = _currentPreset.value.bands
        loadEqualizerState()
        loadLanguage()
        setupWearableListener()
        
        try {
            // Registrace broadcast receiveru s explicitním povolením exportu
            val filter = IntentFilter(RadioService.ACTION_PLAYBACK_STATE_CHANGED)
            filter.addCategory(Intent.CATEGORY_DEFAULT)
            
            context.registerReceiver(
                serviceReceiver,
                filter,
                Context.RECEIVER_EXPORTED
            )
            Log.d("RadioViewModel", "DEBUG: Broadcast receiver úspěšně zaregistrován pro akci: ${RadioService.ACTION_PLAYBACK_STATE_CHANGED}")
        } catch (e: Exception) {
            Log.e("RadioViewModel", "Chyba při registraci receiveru: ${e.message}")
            e.printStackTrace()
        }

        loadLocalStations()

        // Inicializace oblíbených stanic při prvním spuštění
        viewModelScope.launch {
            initializeFavoriteStations()
        }
    }

    private fun loadSavedState() {
        viewModelScope.launch {
            // Načtení uložené hlasitosti
            val savedVolume = prefs.getFloat("volume", 1.0f)
            _volume.value = savedVolume

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
                // Kontrola limitu pouze pro kategorii VLASTNI
                if (radio.category == RadioCategory.VLASTNI) {
                    val favoriteCount = radioRepository.getFavoriteRadios().first().size
                    if (favoriteCount >= maxFavorites.value) {
                        _showMaxFavoritesError.value = true
                        return@launch
                    }
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

    private fun loadLocalStations() {
        viewModelScope.launch {
            val countryCode = locationService.getCurrentCountry()
            if (countryCode != null) {
                _currentCountryCode.value = countryCode
                RadioCategory.setCurrentCountryCode(countryCode)
                val stations = radioRepository.getStationsByCountry(countryCode)
                _localStations.value = stations
            }
        }
    }

    fun refreshLocalStations() {
        loadLocalStations()
    }

    private suspend fun initializeFavoriteStations() {
        // Kontrola, zda už byly stanice inicializovány a zda jsou v databázi nějaké stanice
        val isInitialized = prefs.getBoolean("favorites_initialized", false)
        val totalStations = radioRepository.getAllRadios().first().size
        
        if (!isInitialized || totalStations == 0) {
            // Resetujeme příznak inicializace
            prefs.edit().putBoolean("favorites_initialized", false).apply()
            
            // Načtení stanic ze souboru podle jazyka/lokace
            val countryCode = locationService.getCurrentCountry() ?: 
                if (context.resources.configuration.locales[0].language == "cs") "CZ" else "SK"

            try {
                val jsonFileName = if (countryCode == "CZ") "stations_cz.json" else "stations_sk.json"
                val jsonString = context.assets.open(jsonFileName).bufferedReader().use { it.readText() }
                val countryData = Gson().fromJson(jsonString, Country::class.java)
                
                // Přidání prvních 10 stanic do databáze
                countryData.stations.take(10).forEach { station ->
                    val radio = Radio(
                        id = station.id,
                        name = station.name,
                        streamUrl = station.streamUrl,
                        imageUrl = station.imageUrl,
                        description = station.description,
                        category = RadioCategory.MISTNI,
                        originalCategory = RadioCategory.MISTNI,
                        startColor = Gradients.getGradientForCategory(RadioCategory.MISTNI).first,
                        endColor = Gradients.getGradientForCategory(RadioCategory.MISTNI).second,
                        isFavorite = false,
                        bitrate = station.bitrate
                    )
                    radioRepository.insertRadio(radio)
                }
                
                // Označení, že inicializace proběhla
                prefs.edit().putBoolean("favorites_initialized", true).apply()
            } catch (e: Exception) {
                Log.e("RadioViewModel", "Chyba při inicializaci místních stanic", e)
            }
        }
    }

    fun playRadio(radio: Radio) {
        viewModelScope.launch {
            _currentRadio.value = radio
            _currentCategory.value = radio.category
            // Uložení posledního přehrávaného rádia
            prefs.edit().putString("last_radio_id", radio.id).apply()
            // Spuštění služby pro přehrávání
            val intent = Intent(context, RadioService::class.java).apply {
                action = RadioService.ACTION_PLAY
                putExtra(RadioService.EXTRA_RADIO_ID, radio.id)
            }
            context.startForegroundService(intent)
        }
    }

    fun stopPlayback() {
        _currentRadio.value = null
        _isPlaying.value = false
        _currentMetadata.value = null
        
        // Uvolnění equalizeru
        equalizerManager.release()
        
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

    private fun linearToExponential(value: Float): Float {
        return (Math.pow(value.toDouble(), 4.0)).toFloat()
    }

    private fun exponentialToLinear(value: Float): Float {
        return Math.pow(value.toDouble(), 0.25).toFloat()
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

    fun setFadeOutDuration(seconds: Int) {
        _fadeOutDuration.value = seconds
        prefs.edit().putInt(PREFS_FADE_OUT_DURATION, seconds).apply()
    }

    fun setSleepTimer(minutes: Int?) {
        _sleepTimerMinutes.value = minutes
        _remainingTimeMinutes.value = minutes
        _remainingTimeSeconds.value = 0
        
        viewModelScope.launch {
            if (minutes != null) {
                val startTime = System.currentTimeMillis()
                val endTime = startTime + (minutes * 60 * 1000L)
                val fadeOutDurationMs = (_fadeOutDuration.value * 1000L)
                val fadeOutStartTime = endTime - fadeOutDurationMs
                val initialVolume = _volume.value

                while (System.currentTimeMillis() < endTime && _sleepTimerMinutes.value == minutes) {
                    val currentTime = System.currentTimeMillis()
                    val remainingTotal = endTime - currentTime
                    val remainingMinutes = (remainingTotal / 1000L / 60L).toInt()
                    val remainingSeconds = ((remainingTotal / 1000L) % 60L).toInt()
                    
                    _remainingTimeMinutes.value = remainingMinutes
                    _remainingTimeSeconds.value = remainingSeconds

                    // Začít fade out podle nastavené doby
                    if (currentTime >= fadeOutStartTime) {
                        val fadeOutElapsed = currentTime - fadeOutStartTime
                        val fadeOutProgress = fadeOutElapsed.toFloat() / fadeOutDurationMs
                        val newVolume = initialVolume * (1f - fadeOutProgress)
                        setVolume(newVolume.coerceIn(0f, initialVolume))
                    }

                    delay(1000) // Aktualizace každou sekundu
                }

                if (_sleepTimerMinutes.value == minutes) {
                    stopPlayback()
                    _sleepTimerMinutes.value = null
                    _remainingTimeMinutes.value = null
                    _remainingTimeSeconds.value = null
                    // Obnovení původní hlasitosti
                    setVolume(initialVolume)
                }
            } else {
                _remainingTimeMinutes.value = null
                _remainingTimeSeconds.value = null
            }
        }
    }

    private fun loadEqualizerState() {
        Log.d("RadioViewModel", "📱 Načítám stav equalizeru")
        _equalizerEnabled.value = prefs.getBoolean("equalizer_enabled", false)
        val presetName = prefs.getString("equalizer_preset", EqualizerPreset.NORMAL.name)
        _currentPreset.value = EqualizerPreset.valueOf(presetName ?: EqualizerPreset.NORMAL.name)
        _bandValues.value = _currentPreset.value.bands
        
        Log.d("RadioViewModel", """📱 Načtený stav equalizeru:
            |  - Enabled: ${_equalizerEnabled.value}
            |  - Preset: ${_currentPreset.value.title}
            |  - Hodnoty pásem: ${_bandValues.value}
            """.trimMargin())
    }

    fun setEqualizerEnabled(enabled: Boolean) {
        Log.d("RadioViewModel", "🎛️ Nastavuji equalizer enabled: $enabled")
        _equalizerEnabled.value = enabled
        prefs.edit().putBoolean("equalizer_enabled", enabled).apply()
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                equalizerManager.setEnabled(enabled)
                if (enabled) {
                    Log.d("RadioViewModel", "🎛️ Equalizer zapnut, aplikuji nastavení")
                    // Znovu aplikujeme aktuální preset
                    _currentPreset.value.bands.forEachIndexed { index, value ->
                        equalizerManager.setBandLevel(index, value.toInt())
                        Log.d("RadioViewModel", "✅ Nastaveno pásmo $index na hodnotu ${value.toInt()}")
                    }
                }
            } catch (e: Exception) {
                Log.e("RadioViewModel", "❌ Chyba při nastavování stavu equalizeru", e)
            }
        }
    }

    fun setEqualizerPreset(preset: EqualizerPreset) {
        Log.d("RadioViewModel", "🎛️ Nastavuji equalizer preset: ${preset.title}")
        _currentPreset.value = preset
        _bandValues.value = preset.bands
        prefs.edit().putString("equalizer_preset", preset.name).apply()
        
        if (_equalizerEnabled.value) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    equalizerManager.setEnabled(true) // Ujistíme se, že je equalizer zapnutý
                    preset.bands.forEachIndexed { index, value ->
                        equalizerManager.setBandLevel(index, value.toInt())
                        Log.d("RadioViewModel", "✅ Nastaveno pásmo $index na hodnotu ${value.toInt()}")
                    }
                } catch (e: Exception) {
                    Log.e("RadioViewModel", "❌ Chyba při aplikování presetu", e)
                }
            }
        }
    }

    fun setBandValue(index: Int, value: Float) {
        Log.d("RadioViewModel", "🎛️ Nastavuji band $index na hodnotu $value")
        val newValues = _bandValues.value.toMutableList()
        newValues[index] = value
        _bandValues.value = newValues
        
        if (_equalizerEnabled.value) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    equalizerManager.setBandLevel(index, value.toInt())
                    Log.d("RadioViewModel", "✅ Band $index úspěšně nastaven na $value")
                } catch (e: Exception) {
                    Log.e("RadioViewModel", "❌ Chyba při nastavování bandu $index", e)
                }
            }
        }
    }

    private fun applyEqualizerSettings() {
        Log.d("RadioViewModel", """🎛️ Aplikuji nastavení equalizeru:
            |  - Enabled: ${_equalizerEnabled.value}
            |  - Preset: ${_currentPreset.value.title}
            |  - Hodnoty pásem: ${_bandValues.value}
            """.trimMargin())
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                equalizerManager.setEnabled(_equalizerEnabled.value)
                if (_equalizerEnabled.value) {
                    _bandValues.value.forEachIndexed { index, value ->
                        equalizerManager.setBandLevel(index, value.toInt())
                        Log.d("RadioViewModel", "✅ Nastaveno pásmo $index na hodnotu ${value.toInt()}")
                    }
                }
            } catch (e: Exception) {
                Log.e("RadioViewModel", "❌ Chyba při aplikování nastavení equalizeru", e)
            }
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

    fun searchStations(
        query: String,
        country: String? = null,
        minBitrate: Int? = null,
        orderBy: String = "votes",
        onResult: (List<RadioStation>?) -> Unit
    ) {
        viewModelScope.launch {
            Log.d("RadioViewModel", "Začínám vyhledávat stanice pro dotaz: $query")
            val searchParams = SearchParams(
                name = query,
                country = country,
                minBitrate = minBitrate,
                orderBy = orderBy
            )
            val result = radioRepository.searchStations(searchParams)
            Log.d("RadioViewModel", "Výsledek vyhledávání: ${result?.size ?: 0} stanic")
            
            // Získání všech uložených stanic pro kontrolu
            val savedStations = radioRepository.getAllRadios().first()
            
            result?.forEach { station -> 
                // Kontrola, zda je stanice již uložená
                val savedStation = savedStations.find { 
                    it.streamUrl == station.url_resolved || it.streamUrl == station.url 
                }
                if (savedStation != null) {
                    station.isFromRadioBrowser = false
                    station.category = savedStation.category
                } else {
                    station.isFromRadioBrowser = true
                    station.category = determineCategory(station.tags)
                }
            }
            onResult(result)
        }
    }

    private fun determineCategory(tags: String?): RadioCategory {
        if (tags == null) return RadioCategory.OSTATNI
        
        val lowerTags = tags.lowercase()
        return when {
            lowerTags.contains("pop") || lowerTags.contains("top 40") -> RadioCategory.POP
            lowerTags.contains("rock") || lowerTags.contains("metal") || lowerTags.contains("punk") -> RadioCategory.ROCK
            lowerTags.contains("jazz") || lowerTags.contains("blues") || lowerTags.contains("soul") -> RadioCategory.JAZZ
            lowerTags.contains("dance") || lowerTags.contains("techno") || lowerTags.contains("house") || lowerTags.contains("disco") -> RadioCategory.DANCE
            lowerTags.contains("electronic") || lowerTags.contains("electro") || lowerTags.contains("ambient") -> RadioCategory.ELEKTRONICKA
            lowerTags.contains("classic") || lowerTags.contains("classical") || lowerTags.contains("orchestra") || lowerTags.contains("symphony") -> RadioCategory.KLASICKA
            lowerTags.contains("country") || lowerTags.contains("western") -> RadioCategory.COUNTRY
            lowerTags.contains("folk") || lowerTags.contains("folklore") || lowerTags.contains("traditional") -> RadioCategory.FOLK
            lowerTags.contains("talk") || lowerTags.contains("speech") || lowerTags.contains("spoken") -> RadioCategory.MLUVENE_SLOVO
            lowerTags.contains("kids") || lowerTags.contains("children") || lowerTags.contains("junior") -> RadioCategory.DETSKE
            lowerTags.contains("religious") || lowerTags.contains("christian") || lowerTags.contains("gospel") -> RadioCategory.NABOZENSKE
            lowerTags.contains("news") || lowerTags.contains("information") || lowerTags.contains("actualit") -> RadioCategory.ZPRAVODAJSKE
            else -> RadioCategory.OSTATNI
        }
    }

    fun addStationToFavorites(station: RadioStation, category: RadioCategory) {
        viewModelScope.launch {
            // Kontrola limitu pouze pro kategorii VLASTNI
            if (category == RadioCategory.VLASTNI) {
                val favoriteCount = radioRepository.getFavoriteRadios().first().size
                if (favoriteCount >= maxFavorites.value) {
                    _showMaxFavoritesError.value = true
                    return@launch
                }
            }
            
            Log.d("RadioViewModel", "Ukládám stanici: ${station.name}")
            val radio = Radio(
                id = station.stationuuid ?: station.url,
                name = station.name,
                streamUrl = station.url_resolved ?: station.url,
                imageUrl = station.favicon ?: "android.resource://cz.internetradio.app/drawable/ic_radio_default",
                description = station.tags ?: "",
                category = category,
                originalCategory = category,
                startColor = Gradients.getGradientForCategory(category).first,
                endColor = Gradients.getGradientForCategory(category).second,
                isFavorite = category == RadioCategory.VLASTNI,  // Nastavíme isFavorite pouze pro kategorii VLASTNI
                bitrate = station.bitrate?.toString()?.toIntOrNull()
            )
            radioRepository.insertRadio(radio)
        }
    }

    fun removeStation(radioId: String) {
        viewModelScope.launch {
            try {
                // Zjistíme, zda je mazaná stanice právě přehrávána
                val isCurrentStation = currentRadio.value?.id == radioId
                val currentCategory = currentRadio.value?.category
                
                // Získáme seznam stanic v aktuální kategorii před smazáním
                val stationsInCategory = if (currentCategory != null) {
                    radioRepository.getRadiosByCategory(currentCategory).first()
                        .sortedBy { it.name.lowercase() }
                } else {
                    emptyList()
                }
                
                val currentIndex = stationsInCategory.indexOfFirst { it.id == radioId }
                
                // Smažeme stanici
                radioRepository.removeStation(radioId)
                
                // Pokud byla mazaná stanice právě přehrávána
                if (isCurrentStation) {
                    // Zastavíme službu
                    val stopIntent = Intent(context, RadioService::class.java).apply {
                        action = RadioService.ACTION_STOP
                    }
                    context.startForegroundService(stopIntent)
                    
                    // Vyčistíme stav
                    _currentRadio.value = null
                    _currentMetadata.value = null
                    _isPlaying.value = false
                    
                    // Pokud máme více než jednu stanici v kategorii
                    if (stationsInCategory.size > 1) {
                        // Vybereme následující stanici nebo první v seznamu, pokud jsme byli byli na konci
                        val nextStation = if (currentIndex < stationsInCategory.size - 1) {
                            stationsInCategory[currentIndex + 1]
                        } else {
                            stationsInCategory[0]
                        }
                        
                        // Krátká pauza před spuštěním další stanice
                        delay(1000)
                        
                        // Spustíme přehrávání další stanice přes službu
                        val playIntent = Intent(context, RadioService::class.java).apply {
                            action = RadioService.ACTION_PLAY
                            putExtra(RadioService.EXTRA_RADIO_ID, nextStation.id)
                        }
                        context.startForegroundService(playIntent)
                        
                        // Aktualizujeme stav ve ViewModelu
                        _currentRadio.value = nextStation
                        _currentCategory.value = nextStation.category
                        _isPlaying.value = true
                        
                        Log.d("RadioViewModel", "Přepnuto na ${if (currentIndex == stationsInCategory.size - 1) "první" else "další"} stanici v kategorii ${nextStation.category}: ${nextStation.name}")
                    } else {
                        Log.d("RadioViewModel", "Byla smazána poslední stanice v kategorii $currentCategory, přehrávání zastaveno")
                    }
                }
            } catch (e: Exception) {
                Log.e("RadioViewModel", "Chyba při mazání stanice: ${e.message}", e)
            }
        }
    }

    fun saveSongToFavorites() {
        viewModelScope.launch {
            val currentMetadata = _currentMetadata.value ?: return@launch
            val currentRadio = _currentRadio.value ?: return@launch

            // Rozdělení metadat na umělce a název skladby
            val parts = currentMetadata.split(" - ", limit = 2)
            val (artist, title) = when {
                parts.size == 2 -> parts[0] to parts[1]
                else -> null to currentMetadata
            }

            // Kontrola, zda skladba již existuje
            val exists = favoriteSongRepository.songExists(title, artist, currentRadio.id)
            if (exists) {
                _showSongSavedMessage.value = "Skladba je již v oblíbených"
                return@launch
            }

            val favoriteSong = FavoriteSong(
                title = title,
                artist = artist,
                radioName = currentRadio.name,
                radioId = currentRadio.id
            )

            favoriteSongRepository.addSong(favoriteSong)
            _currentSongSaved.value = true
            _showSongSavedMessage.value = "Skladba byla uložena do oblíbených"
        }
    }

    fun dismissSongSavedMessage() {
        _showSongSavedMessage.value = null
    }

    fun getAllFavoriteSongs(): Flow<List<FavoriteSong>> {
        return favoriteSongRepository.getAllSongs()
    }

    fun deleteFavoriteSong(song: FavoriteSong) {
        viewModelScope.launch {
            favoriteSongRepository.deleteSong(song)
        }
    }

    fun copyToClipboard(song: FavoriteSong) {
        val text = buildString {
            append(song.title)
            song.artist?.let { artist ->
                append(" - ")
                append(artist)
            }
        }
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Skladba", text)
        clipboard.setPrimaryClip(clip)
        _showSongSavedMessage.value = "Zkopírováno do schránky"
    }

    fun playNextStation() {
        viewModelScope.launch {
            val currentCategory = _currentCategory.value
            val allRadios = if (currentCategory != null) {
                radioRepository.getRadiosByCategory(currentCategory).first()
            } else {
                radioRepository.getAllRadios().first()
            }
            
            val sortedRadios = allRadios.sortedBy { it.name.lowercase() }
            val currentIndex = sortedRadios.indexOfFirst { it.id == _currentRadio.value?.id }
            
            if (currentIndex < sortedRadios.size - 1) {
                playRadio(sortedRadios[currentIndex + 1])
            }
        }
    }

    fun playPreviousStation() {
        viewModelScope.launch {
            val currentCategory = _currentCategory.value
            val allRadios = if (currentCategory != null) {
                radioRepository.getRadiosByCategory(currentCategory).first()
            } else {
                radioRepository.getAllRadios().first()
            }
            
            val sortedRadios = allRadios.sortedBy { it.name.lowercase() }
            val currentIndex = sortedRadios.indexOfFirst { it.id == _currentRadio.value?.id }
            
            if (currentIndex > 0) {
                playRadio(sortedRadios[currentIndex - 1])
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        equalizerManager.release()
        Wearable.getDataClient(context).removeListener(this)
        context.unregisterReceiver(serviceReceiver)
    }

    fun exportSettings(uri: Uri) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    // Získáme všechny stanice místo pouze oblíbených
                    val allRadios = radioRepository.getAllRadios().first()
                    val stations = allRadios.map { radio ->
                        SerializableRadio(
                            id = radio.id,
                            name = radio.name,
                            streamUrl = radio.streamUrl,
                            imageUrl = radio.imageUrl,
                            description = radio.description,
                            category = radio.category.name,
                            originalCategory = radio.originalCategory?.name,
                            startColor = radio.startColor.value.toInt(),
                            endColor = radio.endColor.value.toInt(),
                            isFavorite = radio.isFavorite,
                            bitrate = radio.bitrate
                        )
                    }

                    // Získáme všechny oblíbené skladby
                    val favoriteSongs = favoriteSongRepository.getAllSongs().first().map { song ->
                        SerializableSong(
                            title = song.title,
                            artist = song.artist,
                            radioName = song.radioName,
                            radioId = song.radioId
                        )
                    }

                    val settings = AppSettings(
                        maxFavorites = maxFavorites.value,
                        equalizerEnabled = equalizerEnabled.value,
                        fadeOutDuration = fadeOutDuration.value,
                        favoriteStations = stations,  // Použijeme všechny stanice
                        favoriteSongs = favoriteSongs
                    )
                    
                    val json = Json { 
                        prettyPrint = true 
                        encodeDefaults = true
                    }.encodeToString(AppSettings.serializer(), settings)
                    
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.write(json.toByteArray())
                    } ?: run {
                        Log.e("RadioViewModel", "Nelze otevřít soubor pro zápis")
                        _message.value = "Nelze otevřít soubor pro zápis"
                        return@withContext
                    }

                    Log.d("RadioViewModel", "Export dokončen, exportováno ${stations.size} stanic a ${favoriteSongs.size} skladeb")
                    _message.value = context.getString(R.string.msg_settings_exported)
                } catch (e: Exception) {
                    Log.e("RadioViewModel", "Chyba při exportu nastavení", e)
                    _message.value = "Chyba při exportu nastavení"
                }
            }
        }
    }

    fun importSettings(uri: Uri) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val settingsFile = context.contentResolver.openInputStream(uri)
                    if (settingsFile == null) {
                        Log.e("RadioViewModel", "Nelze načíst soubor s nastavením")
                        _message.value = "Nelze načíst soubor s nastavením"
                        return@withContext
                    }

                    val jsonString = settingsFile.bufferedReader().use { it.readText() }
                    val settings = try {
                        Json.decodeFromString<AppSettings>(jsonString)
                    } catch (e: Exception) {
                        Log.e("RadioViewModel", "Chyba při parsování souboru s nastavením", e)
                        _message.value = "Chyba při parsování souboru s nastavením"
                        return@withContext
                    }
                    
                    // Aktualizace nastavení
                    setMaxFavorites(settings.maxFavorites)
                    setEqualizerEnabled(settings.equalizerEnabled)
                    setFadeOutDuration(settings.fadeOutDuration)
                    
                    var importedStations = 0
                    var importedSongs = 0
                    
                    // Import stanic
                    settings.favoriteStations.forEach { serializable ->
                        try {
                            val entity = SerializableRadio.toRadioEntity(serializable)
                            val existingById = radioRepository.getRadioById(entity.id)
                            val existingByUrl = radioRepository.getAllRadios().first().find { 
                                it.streamUrl == entity.streamUrl 
                            }
                            
                            when {
                                // Pokud existuje stanice se stejným ID, aktualizujeme ji
                                existingById != null -> {
                                    val updatedRadio = Radio(
                                        id = entity.id,
                                        name = entity.name,
                                        streamUrl = entity.streamUrl,
                                        imageUrl = entity.imageUrl,
                                        description = entity.description,
                                        category = entity.category,
                                        originalCategory = existingById.originalCategory ?: entity.originalCategory,
                                        startColor = Color(entity.startColor),
                                        endColor = Color(entity.endColor),
                                        isFavorite = true,  // Explicitně nastavíme jako oblíbenou
                                        bitrate = entity.bitrate
                                    )
                                    radioRepository.insertRadio(updatedRadio)
                                    Log.d("RadioViewModel", "Aktualizována existující stanice podle ID: ${entity.name}")
                                }
                                // Pokud existuje stanice se stejnou URL, aktualizujeme ji
                                existingByUrl != null -> {
                                    val updatedRadio = Radio(
                                        id = existingByUrl.id,
                                        name = entity.name,
                                        streamUrl = entity.streamUrl,
                                        imageUrl = entity.imageUrl,
                                        description = entity.description,
                                        category = entity.category,
                                        originalCategory = existingByUrl.originalCategory ?: entity.originalCategory,
                                        startColor = Color(entity.startColor),
                                        endColor = Color(entity.endColor),
                                        isFavorite = true,  // Explicitně nastavíme jako oblíbenou
                                        bitrate = entity.bitrate
                                    )
                                    radioRepository.insertRadio(updatedRadio)
                                    Log.d("RadioViewModel", "Aktualizována existující stanice podle URL: ${entity.name}")
                                }
                                // Pokud stanice neexistuje, vložíme ji jako novou
                                else -> {
                                    val newRadio = Radio(
                                        id = entity.id,
                                        name = entity.name,
                                        streamUrl = entity.streamUrl,
                                        imageUrl = entity.imageUrl,
                                        description = entity.description,
                                        category = entity.category,
                                        originalCategory = entity.originalCategory,
                                        startColor = Gradients.getGradientForCategory(entity.category).first,
                                        endColor = Gradients.getGradientForCategory(entity.category).second,
                                        isFavorite = true,
                                        bitrate = entity.bitrate
                                    )
                                    radioRepository.insertRadio(newRadio)
                                    Log.d("RadioViewModel", "Vložena nová stanice: ${entity.name}")
                                }
                            }
                            importedStations++
                        } catch (e: Exception) {
                            Log.e("RadioViewModel", "Chyba při importu stanice ${serializable.name}: ${e.message}")
                            // Pokračujeme s další stanicí
                        }
                    }

                    // Import oblíbených skladeb
                    settings.favoriteSongs.forEach { song ->
                        try {
                            favoriteSongRepository.addSong(FavoriteSong(
                                title = song.title,
                                artist = song.artist,
                                radioName = song.radioName,
                                radioId = song.radioId
                            ))
                            importedSongs++
                            Log.d("RadioViewModel", "Importována skladba: ${song.title}")
                        } catch (e: Exception) {
                            Log.e("RadioViewModel", "Chyba při importu skladby ${song.title}: ${e.message}")
                            // Pokračujeme s další skladbou
                        }
                    }

                    Log.d("RadioViewModel", "Import dokončen, importováno $importedStations stanic a $importedSongs skladeb")
                    _message.value = context.getString(R.string.msg_settings_imported)
                } catch (e: Exception) {
                    Log.e("RadioViewModel", "Chyba při importu nastavení: ${e.message}", e)
                    _message.value = "Chyba při importu nastavení"
                }
            }
        }
    }

    private fun loadLanguage() {
        val savedLanguage = prefs.getString(PREFS_LANGUAGE, Language.SYSTEM.code)
        _currentLanguage.value = Language.values().find { it.code == savedLanguage } ?: Language.SYSTEM
    }

    fun setLanguage(language: Language) {
        _currentLanguage.value = language
        prefs.edit().putString(PREFS_LANGUAGE, language.code).apply()
        // Restartování aktivity pro aplikování změny jazyka
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun clearAllData() {
        viewModelScope.launch {
            try {
                // Zastavení přehrávání
                stopPlayback()

                // Vymazání všech stanic z databáze
                radioRepository.deleteAllStations()
                
                // Vymazání všech oblíbených skladeb
                favoriteSongRepository.deleteAllSongs()

                // Vymazání SharedPreferences
                prefs.edit().clear().apply()

                // Obnovení výchozích hodnot
                _maxFavorites.value = DEFAULT_MAX_FAVORITES
                _fadeOutDuration.value = DEFAULT_FADE_OUT_DURATION
                _currentLanguage.value = Language.SYSTEM
                _equalizerEnabled.value = false
                _currentPreset.value = EqualizerPreset.NORMAL
                _bandValues.value = EqualizerPreset.NORMAL.bands
                _volume.value = 1f

                // Reinicializace stanic
                initializeFavoriteStations()

                // Počkáme na dokončení inicializace
                delay(1000)

                // Restartování aplikace
                val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK) // Přidáno pro úplné vyčištění back stacku
                }
                if (intent != null) {
                    context.startActivity(intent)
                    // Ukončení současné aktivity
                    android.os.Process.killProcess(android.os.Process.myPid())
                }
            } catch (e: Exception) {
                Log.e("RadioViewModel", "Chyba při mazání dat: ${e.message}")
                _message.value = "Chyba při mazání dat aplikace"
            }
        }
    }

    fun loadMoreStations() {
        viewModelScope.launch {
            try {
                // Načtení stanic ze souboru podle jazyka/lokace
                val countryCode = locationService.getCurrentCountry() ?: 
                    if (context.resources.configuration.locales[0].language == "cs") "CZ" else "SK"

                val jsonFileName = if (countryCode == "CZ") "stations_cz.json" else "stations_sk.json"
                val jsonString = context.assets.open(jsonFileName).bufferedReader().use { it.readText() }
                val countryData = Gson().fromJson(jsonString, Country::class.java)
                
                // Získání aktuálních stanic z databáze
                val currentStations = radioRepository.getAllRadios().first()
                
                // Načtení dalších 10 stanic, které ještě nejsou v databázi
                val newStations = countryData.stations
                    .filterNot { station -> currentStations.any { it.id == station.id } }
                    .take(10)
                
                // Přidání nových stanic do databáze
                newStations.forEach { station ->
                    val radio = Radio(
                        id = station.id,
                        name = station.name,
                        streamUrl = station.streamUrl,
                        imageUrl = station.imageUrl,
                        description = station.description,
                        category = RadioCategory.MISTNI,
                        originalCategory = RadioCategory.MISTNI,
                        startColor = Gradients.getGradientForCategory(RadioCategory.MISTNI).first,
                        endColor = Gradients.getGradientForCategory(RadioCategory.MISTNI).second,
                        isFavorite = false,
                        bitrate = station.bitrate
                    )
                    radioRepository.insertRadio(radio)
                }

                // Pokud už nejsou další stanice k načtení, informujeme uživatele
                if (newStations.isEmpty()) {
                    _message.value = "Všechny dostupné stanice jsou již načteny"
                }
            } catch (e: Exception) {
                Log.e("RadioViewModel", "Chyba při načítání dalších stanic", e)
                _message.value = "Chyba při načítání dalších stanic"
            }
        }
    }

    fun updateStationOrder(category: RadioCategory, fromPosition: Int, toPosition: Int) {
        viewModelScope.launch {
            radioRepository.updateStationOrder(category, fromPosition, toPosition)
        }
    }

    fun getNextOrderIndex(category: RadioCategory, onResult: (Int) -> Unit) {
        viewModelScope.launch {
            val nextIndex = radioRepository.getNextOrderIndex(category)
            onResult(nextIndex)
        }
    }

    fun updateStationOrderIndex(radioId: String, newOrder: Int) {
        viewModelScope.launch {
            radioRepository.updateStationOrderIndex(radioId, newOrder)
        }
    }
} 
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

private data class Country(
    val name: String,
    val stations: List<PopularStation>
)

private data class PopularStation(
    val id: String,
    val name: String,
    val streamUrl: String,
    val imageUrl: String,
    val description: String
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
        const val DEFAULT_MAX_FAVORITES = 10
        const val PREFS_MAX_FAVORITES = "max_favorites"
        const val PREFS_FADE_OUT_DURATION = "fade_out_duration"
        const val DEFAULT_FADE_OUT_DURATION = 60 // sekund
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
        // Kontrola, zda už byly stanice inicializovány
        val isInitialized = prefs.getBoolean("favorites_initialized", false)
        if (!isInitialized) {
            val favoriteCount = radioRepository.getFavoriteRadios().first().size
            if (favoriteCount == 0) {
                // Načtení stanic ze souboru podle jazyka/lokace
                val countryCode = locationService.getCurrentCountry() ?: 
                    if (context.resources.configuration.locales[0].language == "cs") "CZ" else "SK"

                try {
                    val jsonFileName = if (countryCode == "CZ") "stations_cz.json" else "stations_sk.json"
                    val jsonString = context.assets.open(jsonFileName).bufferedReader().use { it.readText() }
                    val countryData = Gson().fromJson(jsonString, Country::class.java)
                    
                    // Přidání stanic do databáze
                    countryData.stations.take(10).forEach { station ->
                        val radio = Radio(
                            id = station.id,
                            name = station.name,
                            streamUrl = station.streamUrl,
                            imageUrl = station.imageUrl,
                            description = station.description,
                            category = RadioCategory.MISTNI,
                            originalCategory = RadioCategory.MISTNI,
                            startColor = RadioCategory.MISTNI.startColor,
                            endColor = RadioCategory.MISTNI.endColor,
                            isFavorite = false
                        )
                        radioRepository.insertRadio(radio)
                    }
                } catch (e: Exception) {
                    Log.e("RadioViewModel", "Chyba při inicializaci místních stanic", e)
                }
            }
            // Označení, že inicializace proběhla
            prefs.edit().putBoolean("favorites_initialized", true).apply()
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
            val favoriteCount = radioRepository.getFavoriteRadios().first().size
            if (favoriteCount >= maxFavorites.value) {
                _showMaxFavoritesError.value = true
                return@launch
            }
            
            Log.d("RadioViewModel", "Ukládám stanici: ${station.name}")
            radioRepository.addRadioStationToFavorites(station, category)
        }
    }

    fun removeStation(radioId: String) {
        viewModelScope.launch {
            radioRepository.removeStation(radioId)
            // Pokud je stanice právě přehrávána, zastavíme přehrávání
            if (currentRadio.value?.id == radioId) {
                stopPlayback()
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
                            gradientId = radio.gradientId,
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
                    }

                    Log.d("RadioViewModel", "Export dokončen, exportováno ${stations.size} stanic a ${favoriteSongs.size} skladeb")
                } catch (e: Exception) {
                    Log.e("RadioViewModel", "Chyba při exportu nastavení", e)
                }
            }
        }
    }

    fun importSettings(uri: Uri) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val jsonString = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        inputStream.bufferedReader().use { it.readText() }
                    } ?: return@withContext

                    val settings = Json.decodeFromString<AppSettings>(jsonString)
                    
                    // Aktualizace nastavení
                    setMaxFavorites(settings.maxFavorites)
                    setEqualizerEnabled(settings.equalizerEnabled)
                    setFadeOutDuration(settings.fadeOutDuration)
                    
                    // Import stanic
                    settings.favoriteStations.forEach { serializable ->
                        val entity = SerializableRadio.toRadioEntity(serializable)
                        val category = entity.category // Získáme kategorii
                        radioRepository.insertRadio(Radio(
                            id = entity.id,
                            name = entity.name,
                            streamUrl = entity.streamUrl,
                            imageUrl = entity.imageUrl,
                            description = entity.description,
                            category = category,
                            originalCategory = entity.originalCategory,
                            startColor = category.startColor, // Použijeme barvy z kategorie
                            endColor = category.endColor,     // místo hodnot ze souboru
                            isFavorite = entity.isFavorite,
                            gradientId = entity.gradientId,
                            bitrate = entity.bitrate
                        ))
                    }

                    // Import oblíbených skladeb
                    settings.favoriteSongs.forEach { song ->
                        favoriteSongRepository.addSong(FavoriteSong(
                            title = song.title,
                            artist = song.artist,
                            radioName = song.radioName,
                            radioId = song.radioId
                        ))
                    }

                    Log.d("RadioViewModel", "Import dokončen, importováno ${settings.favoriteStations.size} stanic a ${settings.favoriteSongs.size} skladeb")
                } catch (e: Exception) {
                    Log.e("RadioViewModel", "Chyba při importu nastavení", e)
                }
            }
        }
    }
} 
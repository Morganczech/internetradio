package cz.internetradio.app.viewmodel

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
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
import cz.internetradio.app.utils.normalizeForSearch
import cz.internetradio.app.BuildConfig

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

    private val _isCompactMode = MutableStateFlow(false)
    val isCompactMode: StateFlow<Boolean> = _isCompactMode

    private val _useUnifiedAccentColor = MutableStateFlow(false)
    val useUnifiedAccentColor: StateFlow<Boolean> = _useUnifiedAccentColor

    private val _playbackContext = MutableStateFlow<RadioCategory?>(null)
    val playbackContext: StateFlow<RadioCategory?> = _playbackContext

    private val serviceReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (context == null || intent == null) return

            try {
                // Handling chyb přehrávání
                val errorMessage = intent.getStringExtra(RadioService.EXTRA_ERROR)
                if (errorMessage != null) {
                    _message.value = errorMessage
                    // Pokud nastala chyba, zastavíme UI stav
                    if (_isPlaying.value) _isPlaying.value = false
                    return
                }

                val isPlaying = intent.getBooleanExtra(RadioService.EXTRA_IS_PLAYING, false)
                val metadata = intent.getStringExtra(RadioService.EXTRA_METADATA)
                val currentRadioId = intent.getStringExtra(RadioService.EXTRA_CURRENT_RADIO)
                val audioSessionId = intent.getIntExtra(RadioService.EXTRA_AUDIO_SESSION_ID, -1)

                _isPlaying.value = isPlaying
                _currentMetadata.value = metadata

                if (currentRadioId != null) {
                    viewModelScope.launch {
                        _currentRadio.value = radioRepository.getRadioById(currentRadioId)
                    }
                }

                if (audioSessionId != -1) {
                    viewModelScope.launch {
                        equalizerManager.setupEqualizer(audioSessionId)
                    }
                }
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) Log.e("RadioViewModel", "Error processing broadcast", e)
            }
        }
    }

    companion object {
        const val DEFAULT_MAX_FAVORITES = 10
        const val PREFS_MAX_FAVORITES = "max_favorites"
        const val PREFS_FADE_OUT_DURATION = "fade_out_duration"
        const val DEFAULT_FADE_OUT_DURATION = 60
        const val PREFS_LANGUAGE = "language"
        const val PREFS_USE_UNIFIED_ACCENT_COLOR = "use_unified_accent_color"
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
            val filter = IntentFilter(RadioService.ACTION_PLAYBACK_STATE_CHANGED)
            filter.addCategory(Intent.CATEGORY_DEFAULT)
            context.registerReceiver(serviceReceiver, filter, Context.RECEIVER_EXPORTED)
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.e("RadioViewModel", "Receiver error", e)
        }

        loadLocalStations()
        viewModelScope.launch { ensureDatabaseInitialized() }
        
        // Synchronize state with service
        try {
            val intent = Intent(context, RadioService::class.java).apply {
                action = RadioService.ACTION_REQUEST_STATE
            }
            context.startForegroundService(intent)
        } catch (e: Exception) {
            // Service might not be running or permission issue
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    }

    private fun loadLanguage() {
        val saved = prefs.getString(PREFS_LANGUAGE, Language.SYSTEM.code)
        _currentLanguage.value = Language.values().find { it.code == saved } ?: Language.SYSTEM
    }

    private fun loadSavedState() {
        viewModelScope.launch {
            _volume.value = prefs.getFloat("volume", 1.0f)
            _isCompactMode.value = prefs.getBoolean("compact_mode", false)
            _useUnifiedAccentColor.value = prefs.getBoolean(PREFS_USE_UNIFIED_ACCENT_COLOR, false)
            val lastId = prefs.getString("last_radio_id", null)
            if (lastId != null) {
                _currentRadio.value = radioRepository.getRadioById(lastId)
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
                if (radio.category == RadioCategory.VLASTNI && radioRepository.getFavoriteRadios().first().size >= maxFavorites.value) {
                    _showMaxFavoritesError.value = true
                    return@launch
                }
                radioRepository.toggleFavorite(radio.id)
            }
        }
    }

    fun dismissMaxFavoritesError() { _showMaxFavoritesError.value = false }

    fun deleteRadio(radio: Radio) {
        viewModelScope.launch {
            radioRepository.deleteRadio(radio)
            if (radio.id == currentRadio.value?.id) stopPlayback()
        }
    }


    private fun loadLocalStations() {
        viewModelScope.launch {
            // Přísná strategie: Region pouze podle "Locale.country"
            val countryCode = java.util.Locale.getDefault().country.uppercase()
            
            if (countryCode.isNotEmpty()) {
                _currentCountryCode.value = countryCode
                RadioCategory.setCurrentCountryCode(countryCode)
                radioRepository.getStationsByCountry(countryCode)?.let { _localStations.value = it }
            }
        }
    }

    fun refreshLocalStations() { loadLocalStations() }

    private suspend fun ensureDatabaseInitialized() {
        val isInitialized = prefs.getBoolean("favorites_initialized", false)
        val totalStations = radioRepository.getAllRadios().first().size

        // Pokud je již inicializováno a DB není prázdná, nic neděláme
        if (isInitialized && totalStations > 0) return

        val countryCode = java.util.Locale.getDefault().country.uppercase()
        // Ochrana proti prázdnému locale (např. emulátor), default CZ
        val targetCountry = if (countryCode.isBlank()) "CZ" else countryCode
        
        var success = false

        try {
            // 1. Zkusíme Seed JSON (stations_XX.json)
            val seedFileName = "stations_${targetCountry.lowercase()}.json"
            val assetsList = context.assets.list("") ?: emptyArray()

            if (assetsList.contains(seedFileName)) {
                val json = context.assets.open(seedFileName).bufferedReader().use { it.readText() }
                success = radioRepository.initializeDefaultStations(json)
            } else {
                // 2. Pokud nemáme seed, zkusíme API (pouze pokud jsme online)
                if (isNetworkAvailable()) {
                     success = radioRepository.initializeFromApi(targetCountry)
                }
                // 3. Offline + No Seed -> Zůstane prázdné (správné chování)
            }

            // Flag nastavíme POUZE při úspěchu
            if (success) {
                prefs.edit().putBoolean("favorites_initialized", true).apply()
            }

        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.e("RadioViewModel", "Init DB error", e)
        }
    }

    fun playRadio(radio: Radio, categoryContext: RadioCategory? = null) {
        // Strict Offline Check
        if (!isNetworkAvailable()) {
             _message.value = context.getString(R.string.error_no_internet)
             return
        }
        viewModelScope.launch {
            _currentRadio.value = radio
            if (categoryContext != null) {
                _playbackContext.value = categoryContext
            }
            _currentCategory.value = radio.category
            prefs.edit().putString("last_radio_id", radio.id).apply()
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
        equalizerManager.release()
        context.stopService(Intent(context, RadioService::class.java))
    }

    fun togglePlayPause() {
        // Strict Offline Check
        if (!isNetworkAvailable()) {
            _message.value = context.getString(R.string.error_no_internet)
            return
        }
        // ... zbytek logiky
        val intent = Intent(context, RadioService::class.java).apply {
            action = if (_isPlaying.value) RadioService.ACTION_PAUSE else RadioService.ACTION_PLAY
            if (!_isPlaying.value) putExtra(RadioService.EXTRA_RADIO_ID, _currentRadio.value?.id)
        }
        context.startForegroundService(intent)
    }

    fun setVolume(newVolume: Float) {
        val clamped = newVolume.coerceIn(0f, 1f)
        _volume.value = clamped
        prefs.edit().putFloat("volume", clamped).apply()
        context.startForegroundService(Intent(context, RadioService::class.java).apply {
            action = RadioService.ACTION_SET_VOLUME
            putExtra(RadioService.EXTRA_VOLUME, clamped)
        })
    }

    fun setFadeOutDuration(seconds: Int) {
        _fadeOutDuration.value = seconds
        prefs.edit().putInt(PREFS_FADE_OUT_DURATION, seconds).apply()
    }

    fun setUseUnifiedAccentColor(enabled: Boolean) {
        _useUnifiedAccentColor.value = enabled
        prefs.edit().putBoolean(PREFS_USE_UNIFIED_ACCENT_COLOR, enabled).apply()
    }

    fun setSleepTimer(minutes: Int?) {
        _sleepTimerMinutes.value = minutes
        _remainingTimeMinutes.value = minutes
        _remainingTimeSeconds.value = 0
        viewModelScope.launch {
            if (minutes != null) {
                val endTime = System.currentTimeMillis() + (minutes * 60 * 1000L)
                val initialVol = _volume.value
                val fadeOutMs = (_fadeOutDuration.value * 1000L)
                val fadeStart = endTime - fadeOutMs

                while (System.currentTimeMillis() < endTime && _sleepTimerMinutes.value == minutes) {
                    val current = System.currentTimeMillis()
                    val rem = endTime - current
                    _remainingTimeMinutes.value = (rem / 60000L).toInt()
                    _remainingTimeSeconds.value = ((rem / 1000L) % 60).toInt()
                    if (current >= fadeStart) {
                        setVolume((initialVol * (1f - (current - fadeStart).toFloat() / fadeOutMs)).coerceIn(0f, initialVol))
                    }
                    delay(1000)
                }
                if (_sleepTimerMinutes.value == minutes) {
                    stopPlayback()
                    _sleepTimerMinutes.value = null
                    _remainingTimeMinutes.value = null
                    _remainingTimeSeconds.value = null
                    setVolume(initialVol)
                }
            } else {
                _remainingTimeMinutes.value = null
                _remainingTimeSeconds.value = null
            }
        }
    }

    private fun loadEqualizerState() {
        _equalizerEnabled.value = prefs.getBoolean("equalizer_enabled", false)
        val preset = prefs.getString("equalizer_preset", EqualizerPreset.NORMAL.name)
        _currentPreset.value = EqualizerPreset.valueOf(preset ?: EqualizerPreset.NORMAL.name)
        _bandValues.value = _currentPreset.value.bands
    }

    fun setEqualizerEnabled(enabled: Boolean) {
        _equalizerEnabled.value = enabled
        prefs.edit().putBoolean("equalizer_enabled", enabled).apply()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                equalizerManager.setEnabled(enabled)
                if (enabled) _currentPreset.value.bands.forEachIndexed { i, v -> equalizerManager.setBandLevel(i, v.toInt()) }
            } catch (e: Exception) {}
        }
    }

    fun setEqualizerPreset(preset: EqualizerPreset) {
        _currentPreset.value = preset
        _bandValues.value = preset.bands
        prefs.edit().putString("equalizer_preset", preset.name).apply()
        if (_equalizerEnabled.value) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    equalizerManager.setEnabled(true)
                    preset.bands.forEachIndexed { i, v -> equalizerManager.setBandLevel(i, v.toInt()) }
                } catch (e: Exception) {}
            }
        }
    }

    fun setBandValue(index: Int, value: Float) {
        val vals = _bandValues.value.toMutableList()
        vals[index] = value
        _bandValues.value = vals
        if (_equalizerEnabled.value) {
            viewModelScope.launch(Dispatchers.IO) {
                try { equalizerManager.setBandLevel(index, value.toInt()) } catch (e: Exception) {}
            }
        }
    }

    private fun setupWearableListener() { Wearable.getDataClient(context).addListener(this) }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents.forEach { event ->
            if (event.type == DataEvent.TYPE_CHANGED && event.dataItem.uri.path == "/command") {
                DataMapItem.fromDataItem(event.dataItem).dataMap.apply {
                    when (getString("action")) {
                        "play_pause" -> togglePlayPause()
                        "next" -> playNextFavorite()
                        "previous" -> playPreviousFavorite()
                    }
                }
            }
        }
    }

    fun playNextFavorite() {
        viewModelScope.launch {
            val list = getFavoriteRadios().first()
            val i = list.indexOfFirst { it.id == _currentRadio.value?.id }
            if (i < list.size - 1) playRadio(list[i + 1])
        }
    }

    fun playPreviousFavorite() {
        viewModelScope.launch {
            val list = getFavoriteRadios().first()
            val i = list.indexOfFirst { it.id == _currentRadio.value?.id }
            if (i > 0) playRadio(list[i - 1])
        }
    }

    fun searchStations(query: String, country: String? = null, minBitrate: Int? = null, orderBy: String = "votes", onResult: (List<RadioStation>?) -> Unit) {
        if (!isNetworkAvailable()) {
            _message.value = context.getString(R.string.error_no_internet)
            onResult(null)
            return
        }
        viewModelScope.launch {
            try {
                val res = radioRepository.searchStations(SearchParams(name = query, country = country, minBitrate = minBitrate, orderBy = orderBy))
                val saved = radioRepository.getAllRadios().first()
                val processed = res?.map { s ->
                    val norm = s.name.normalizeForSearch()
                    val existing = saved.find { it.streamUrl == s.url_resolved || it.streamUrl == s.url || it.name.normalizeForSearch() == norm }
                    if (existing != null) s.copy(isFromRadioBrowser = false, category = existing.category)
                    else s.copy(isFromRadioBrowser = true, category = determineCategory(s.tags))
                }
                onResult(processed)
            } catch (e: Exception) { onResult(null) }
        }
    }

    private fun determineCategory(tags: String?): RadioCategory {
        if (tags == null) return RadioCategory.OSTATNI
        val t = tags.lowercase()
        return when {
            t.contains("pop") -> RadioCategory.POP
            t.contains("rock") -> RadioCategory.ROCK
            t.contains("jazz") -> RadioCategory.JAZZ
            t.contains("dance") -> RadioCategory.DANCE
            t.contains("electronic") -> RadioCategory.ELEKTRONICKA
            t.contains("classic") -> RadioCategory.KLASICKA
            t.contains("country") -> RadioCategory.COUNTRY
            t.contains("folk") -> RadioCategory.FOLK
            t.contains("talk") -> RadioCategory.MLUVENE_SLOVO
            t.contains("kids") -> RadioCategory.DETSKE
            t.contains("religious") -> RadioCategory.NABOZENSKE
            t.contains("news") -> RadioCategory.ZPRAVODAJSKE
            else -> RadioCategory.OSTATNI
        }
    }

    fun addStationToFavorites(station: RadioStation, category: RadioCategory) {
        viewModelScope.launch {
            if (category == RadioCategory.VLASTNI && radioRepository.getFavoriteRadios().first().size >= maxFavorites.value) {
                _showMaxFavoritesError.value = true
                return@launch
            }
            val isFavorite = category == RadioCategory.VLASTNI
            radioRepository.addStationToFavorites(station, category, isFavorite)
        }
    }

    fun removeStation(radioId: String) {
        viewModelScope.launch {
            try {
                val isCurrent = currentRadio.value?.id == radioId
                val cat = currentRadio.value?.category
                val list = if (cat != null) radioRepository.getRadiosByCategory(cat).first().sortedBy { it.name.lowercase() } else emptyList()
                val i = list.indexOfFirst { it.id == radioId }
                radioRepository.removeStation(radioId)
                if (isCurrent) {
                    context.startForegroundService(Intent(context, RadioService::class.java).apply { action = RadioService.ACTION_STOP })
                    _currentRadio.value = null
                    _isPlaying.value = false
                    if (list.size > 1) playRadio(if (i < list.size - 1) list[i + 1] else list[0])
                }
            } catch (e: Exception) {}
        }
    }

    fun saveSongToFavorites() {
        viewModelScope.launch {
            val meta = _currentMetadata.value ?: return@launch
            val radio = _currentRadio.value ?: return@launch
            val parts = meta.split(" - ", limit = 2)
            val (a, t) = if (parts.size == 2) parts[0] to parts[1] else null to meta
            if (favoriteSongRepository.songExists(t, a, radio.id)) {
                _showSongSavedMessage.value = "Skladba je již v oblíbených"
                return@launch
            }
            favoriteSongRepository.addSong(FavoriteSong(title = t, artist = a, radioName = radio.name, radioId = radio.id))
            _currentSongSaved.value = true
            _showSongSavedMessage.value = "Skladba byla uložena do oblíbených"
        }
    }

    fun dismissSongSavedMessage() { _showSongSavedMessage.value = null }
    fun getAllFavoriteSongs(): Flow<List<FavoriteSong>> = favoriteSongRepository.getAllSongs()
    fun deleteFavoriteSong(song: FavoriteSong) { viewModelScope.launch { favoriteSongRepository.deleteSong(song) } }

    fun copyToClipboard(song: FavoriteSong) {
        val text = if (song.artist != null) "${song.title} - ${song.artist}" else song.title
        (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Skladba", text))
        _showSongSavedMessage.value = "Zkopírováno do schránky"
    }

    fun playNextStation() {
        viewModelScope.launch {
            val cat = _currentCategory.value
            val list = if (cat != null) radioRepository.getRadiosByCategory(cat).first() else radioRepository.getAllRadios().first()
            val i = list.indexOfFirst { it.id == _currentRadio.value?.id }
            if (i < list.size - 1) playRadio(list[i + 1])
        }
    }

    fun playPreviousStation() {
        viewModelScope.launch {
            val cat = _currentCategory.value
            val list = if (cat != null) radioRepository.getRadiosByCategory(cat).first() else radioRepository.getAllRadios().first()
            val i = list.indexOfFirst { it.id == _currentRadio.value?.id }
            if (i > 0) playRadio(list[i - 1])
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
                    val stations = radioRepository.getAllRadios().first().map { radio ->
                        SerializableRadio(
                            id = radio.id, name = radio.name, streamUrl = radio.streamUrl,
                            imageUrl = radio.imageUrl, description = radio.description,
                            category = radio.category.name, originalCategory = radio.originalCategory?.name,
                            startColor = radio.startColor.value.toInt(), endColor = radio.endColor.value.toInt(),
                            isFavorite = radio.isFavorite, bitrate = radio.bitrate
                        )
                    }

                    val songs = favoriteSongRepository.getAllSongs().first().map { song ->
                        SerializableSong(title = song.title, artist = song.artist, radioName = song.radioName, radioId = song.radioId)
                    }
                    val exportDate = java.time.format.DateTimeFormatter.ISO_LOCAL_DATE.format(java.time.LocalDate.now())
                    val json = Json { prettyPrint = true; encodeDefaults = true }.encodeToString(AppSettings.serializer(), AppSettings(maxFavorites = maxFavorites.value, equalizerEnabled = equalizerEnabled.value, fadeOutDuration = fadeOutDuration.value, favoriteStations = stations, favoriteSongs = songs, exportDate = exportDate))
                    context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
                    _message.value = context.getString(R.string.msg_settings_exported)
                } catch (e: Exception) { _message.value = "Chyba exportu" }
            }
        }
    }

    fun importSettings(uri: Uri) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: return@withContext
                    val settings = Json.decodeFromString<AppSettings>(json)
                    setMaxFavorites(settings.maxFavorites)
                    setEqualizerEnabled(settings.equalizerEnabled)
                    setFadeOutDuration(settings.fadeOutDuration)
                    settings.favoriteStations.forEach { radioRepository.insertRadio(SerializableRadio.toRadioEntity(it).toRadio()) }
                    settings.favoriteSongs.forEach { favoriteSongRepository.addSong(FavoriteSong(title = it.title, artist = it.artist, radioName = it.radioName, radioId = it.radioId)) }
                    _message.value = context.getString(R.string.msg_settings_imported)
                } catch (e: Exception) { _message.value = "Chyba importu" }
            }
        }
    }

    fun setLanguage(language: Language) {
        _currentLanguage.value = language
        prefs.edit().putString(PREFS_LANGUAGE, language.code).apply()
        context.startActivity(context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply { addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK) })
    }

    fun clearAllData() {
        viewModelScope.launch {
            try {
                stopPlayback()
                radioRepository.deleteAllStations()
                favoriteSongRepository.deleteAllSongs()
                prefs.edit().clear().apply()
                _currentLanguage.value = Language.SYSTEM
                ensureDatabaseInitialized()
                delay(1000)
                context.startActivity(context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply { addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK) })
                android.os.Process.killProcess(android.os.Process.myPid())
            } catch (e: Exception) { _message.value = "Chyba mazání dat" }
        }
    }

    fun setCompactMode(enabled: Boolean) {
        _isCompactMode.value = enabled
        prefs.edit().putBoolean("compact_mode", enabled).apply()
    }

    fun loadMoreStations() {
        viewModelScope.launch {
            try {
                val code = locationService.getCurrentCountry() ?: if (context.resources.configuration.locales[0].language == "cs") "CZ" else "SK"
                val json = context.assets.open(if (code == "CZ") "stations_cz.json" else "stations_sk.json").bufferedReader().use { it.readText() }
                radioRepository.initializeDefaultStations(json)
            } catch (e: Exception) { _message.value = "Chyba načítání" }
        }
    }

    fun updateStationOrder(category: RadioCategory, from: Int, to: Int) { viewModelScope.launch { radioRepository.updateStationOrder(category, from, to) } }
    fun getNextOrderIndex(category: RadioCategory, onResult: (Int) -> Unit) { viewModelScope.launch { onResult(radioRepository.getNextOrderIndex(category)) } }
    fun updateStationOrderIndex(id: String, order: Int) { viewModelScope.launch { radioRepository.updateStationOrderIndex(id, order) } }
}

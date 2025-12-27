package cz.internetradio.app.service

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.BatteryManager
import android.os.IBinder
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import cz.internetradio.app.data.RadioDatabase
import cz.internetradio.app.model.Radio
import cz.internetradio.app.model.RadioCategory
import cz.internetradio.app.repository.RadioRepository
import cz.internetradio.app.widget.RadioWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.media3.session.MediaSession
import android.os.PowerManager
import android.content.BroadcastReceiver
import cz.internetradio.app.api.RadioBrowserApi
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import android.util.Log
import androidx.media3.common.Metadata
import androidx.media3.exoplayer.analytics.AnalyticsListener
import cz.internetradio.app.audio.EqualizerManager
import cz.internetradio.app.BuildConfig
import androidx.media3.common.PlaybackException
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import androidx.media3.common.ForwardingPlayer

@AndroidEntryPoint
class RadioService : Service() {

    @Inject
    lateinit var exoPlayer: ExoPlayer

    @Inject
    lateinit var database: RadioDatabase

    @Inject
    lateinit var radioBrowserApi: RadioBrowserApi

    @Inject
    lateinit var equalizerManager: EqualizerManager

    private lateinit var radioRepository: RadioRepository
    private lateinit var mediaSession: MediaSession
    private lateinit var wakeLock: PowerManager.WakeLock
    private lateinit var screenWakeLock: PowerManager.WakeLock
    private lateinit var notificationManager: NotificationManager
    private lateinit var radioNotificationManager: RadioNotificationManager

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private val _currentRadio = MutableStateFlow<Radio?>(null)
    val currentRadio: StateFlow<Radio?> = _currentRadio

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _currentMetadata = MutableStateFlow<String?>(null)
    val currentMetadata: StateFlow<String?> = _currentMetadata

    private var bufferSize = 2000 // ms
    
    companion object {
        private const val TAG = "RadioService"
        const val ACTION_PLAY = "cz.internetradio.app.action.PLAY"
        const val ACTION_PAUSE = "cz.internetradio.app.action.PAUSE"
        const val ACTION_NEXT = "cz.internetradio.app.action.NEXT"
        const val ACTION_PREVIOUS = "cz.internetradio.app.action.PREVIOUS"
        const val ACTION_STOP = "cz.internetradio.app.action.STOP"
        const val ACTION_SET_VOLUME = "cz.internetradio.app.action.SET_VOLUME"
        const val ACTION_REQUEST_STATE = "cz.internetradio.app.action.REQUEST_STATE"
        const val EXTRA_RADIO_ID = "radio_id"
        const val EXTRA_VOLUME = "volume"
        const val EXTRA_IS_PLAYING = "is_playing"
        const val EXTRA_METADATA = "metadata"
        const val EXTRA_ERROR = "error_message"
        const val EXTRA_CURRENT_RADIO = "current_radio"
        const val EXTRA_AUDIO_SESSION_ID = "audio_session_id"
        const val EXTRA_CONTEXT_CATEGORY = "context_category"
        private const val WAKELOCK_TAG = "RadioService::WakeLock"

        const val ACTION_PLAYBACK_STATE_CHANGED = "cz.internetradio.app.action.PLAYBACK_STATE_CHANGED"
        
        // Internal flag to track if foreground is active
        private var isForegroundService = false
    }

    private var playbackContext: RadioCategory? = null

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_POWER_CONNECTED -> if (!screenWakeLock.isHeld) screenWakeLock.acquire()
                Intent.ACTION_POWER_DISCONNECTED -> if (screenWakeLock.isHeld) screenWakeLock.release()
            }
        }
    }

    private val audioOutputReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                if (_isPlaying.value) pausePlayback()
            }
        }
    }

    private var wasPlayingBeforeAudioOutputChange = false
    private lateinit var audioManager: AudioManager
    
    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> exoPlayer.volume = 1.0f
            AudioManager.AUDIOFOCUS_LOSS -> if (_isPlaying.value) pausePlayback()
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                if (_isPlaying.value) {
                    wasPlayingBeforeAudioOutputChange = true
                    pausePlayback()
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> exoPlayer.volume = 0.3f
        }
    }
    
    private fun requestAudioFocus(): Boolean {
        val result = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val audioAttributes = android.media.AudioAttributes.Builder()
                .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            val focusRequest = android.media.AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(audioAttributes)
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .build()
            audioManager.requestAudioFocus(focusRequest)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(audioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
        }
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }
    
    private fun abandonAudioFocus() {
        audioManager.abandonAudioFocus(audioFocusChangeListener)
    }
    
    private fun isAudioOutputConnected(): Boolean {
        val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return am.isBluetoothScoOn || am.isBluetoothA2dpOn || am.isWiredHeadsetOn || am.isSpeakerphoneOn || am.isMusicActive
    }
    
    private fun startAudioOutputMonitoring() {
        serviceScope.launch {
            while (true) {
                try {
                    kotlinx.coroutines.delay(3000)
                    if (_isPlaying.value && !isAudioOutputConnected()) {
                        wasPlayingBeforeAudioOutputChange = true
                        pausePlayback()
                        radioNotificationManager.updateNotificationWithInfo(_currentRadio.value, "Audio výstup odpojen")
                    }
                } catch (e: Exception) { break }
            }
        }
    }

    private val mediaSessionCallback = object : MediaSession.Callback {
        override fun onConnect(session: MediaSession, controller: MediaSession.ControllerInfo): MediaSession.ConnectionResult {
            val playerCommands = MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS.buildUpon()
                .add(Player.COMMAND_SEEK_TO_NEXT)
                .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                .add(Player.COMMAND_PLAY_PAUSE)
                .build()
            return MediaSession.ConnectionResult.accept(MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS, playerCommands)
        }
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        radioRepository = RadioRepository(database.radioDao(), radioBrowserApi)
        
        exoPlayer = ExoPlayer.Builder(this)
            .setLoadControl(DefaultLoadControl.Builder()
                .setBufferDurationsMs(bufferSize, bufferSize * 2, bufferSize / 2, bufferSize / 2)
                .setPrioritizeTimeOverSizeThresholds(true).build())
            .build()
        
        
        setupPlayer()

        // Restore volume
        val prefs = getSharedPreferences("radio_prefs", Context.MODE_PRIVATE)
        val savedVolume = prefs.getFloat("volume", 1.0f)
        exoPlayer.volume = savedVolume
        
        // Inicializace Media3 MediaSession s custom callbackem pro podporu tlačítek
        
        val forwardingPlayer = object : ForwardingPlayer(exoPlayer) {
            override fun seekToNext() { playNextRadio() }
            override fun seekToPrevious() { playPreviousRadio() }
            
            // Force system to think there are items to skip to so buttons appear on Wear OS
            override fun hasNextMediaItem(): Boolean = true
            override fun hasPreviousMediaItem(): Boolean = true

            override fun getAvailableCommands(): Player.Commands {
                return super.getAvailableCommands().buildUpon()
                    .add(Player.COMMAND_SEEK_TO_NEXT)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                    .build()
            }
            override fun isCommandAvailable(command: Int): Boolean {
                 return command == Player.COMMAND_SEEK_TO_NEXT || command == Player.COMMAND_SEEK_TO_PREVIOUS || super.isCommandAvailable(command)
            }
        }

        mediaSession = MediaSession.Builder(this, forwardingPlayer)
            .setCallback(mediaSessionCallback)
            .build()

        radioNotificationManager = RadioNotificationManager(this, mediaSession)
        
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKELOCK_TAG).apply { setReferenceCounted(false) }
        screenWakeLock = powerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ON_AFTER_RELEASE, "RadioService::ScreenWakeLock").apply { setReferenceCounted(false) }
        
        val batteryStatus = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        if ((status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL) && !screenWakeLock.isHeld) {
            screenWakeLock.acquire()
        }
        
        
        // DELAYED START_FOREGROUND: We do NOT call startForeground here.
        // It will be called in onStartCommand (for ACTION_PLAY) or if restoring state.
        // startForeground(RadioNotificationManager.NOTIFICATION_ID, radioNotificationManager.getInitialNotification())
        
        registerReceiver(batteryReceiver, IntentFilter().apply { addAction(Intent.ACTION_POWER_CONNECTED); addAction(Intent.ACTION_POWER_DISCONNECTED) })
        registerReceiver(audioOutputReceiver, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY))
        
        startAudioOutputMonitoring()
        broadcastPlaybackState()
    }

    private fun setupPlayer() {
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                serviceScope.launch {
                    kotlinx.coroutines.delay(100)
                    updateNotification()
                }
                broadcastPlaybackState()
                RadioWidgetProvider.updateWidgets(applicationContext, isPlaying, _currentRadio.value?.id, _currentMetadata.value)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_READY -> {
                        _isPlaying.value = exoPlayer.isPlaying
                        serviceScope.launch { kotlinx.coroutines.delay(150); updateNotification() }
                    }
                    Player.STATE_BUFFERING -> updateNotification(loading = true)
                    Player.STATE_ENDED, Player.STATE_IDLE -> {
                        _isPlaying.value = false
                        serviceScope.launch { kotlinx.coroutines.delay(150); updateNotification() }
                    }
                }
                broadcastPlaybackState()
            }

            override fun onPlayerError(error: PlaybackException) {
                if (BuildConfig.DEBUG) Log.e(TAG, "ExoPlayer Error: ${error.message}")
                broadcastPlaybackError()
                _isPlaying.value = false
                updateNotification()
            }

            override fun onMediaMetadataChanged(mediaMetadata: androidx.media3.common.MediaMetadata) {
                val title = mediaMetadata.title?.toString()?.trim()
                val artist = mediaMetadata.artist?.toString()?.trim()
                val extras = mediaMetadata.extras
                val icyMetadata = extras?.getString("icy_metadata") ?: extras?.getString("icy_title") ?: title
                
                val regexMatch = Regex("""title="([^"]+)"""").find(icyMetadata ?: "")
                var extractedTitle: String? = null
                var extractedArtist: String? = null

                if (regexMatch != null) {
                    val fullTitle = regexMatch.groupValues[1]
                    val parts = fullTitle.split(" - ", limit = 2)
                    if (parts.size == 2) {
                        extractedArtist = decodeHtmlEntities(parts[0].trim())
                        extractedTitle = decodeHtmlEntities(parts[1].trim())
                    } else {
                        extractedTitle = decodeHtmlEntities(fullTitle)
                    }
                }
                
                if (extractedArtist.isNullOrBlank()) extractedArtist = decodeHtmlEntities(extras?.getString("icy_artist") ?: artist)

                _currentMetadata.value = when {
                    !extractedArtist.isNullOrBlank() && !extractedTitle.isNullOrBlank() -> "$extractedArtist - $extractedTitle"
                    !extractedTitle.isNullOrBlank() -> extractedTitle
                    !extractedArtist.isNullOrBlank() -> extractedArtist
                    else -> null
                }

                serviceScope.launch { kotlinx.coroutines.delay(200); updateNotification() }
                RadioWidgetProvider.updateWidgets(applicationContext, _isPlaying.value, _currentRadio.value?.id, _currentMetadata.value)
                broadcastPlaybackState()
            }
        })

        exoPlayer.addAnalyticsListener(object : AnalyticsListener {
            override fun onMetadata(eventTime: AnalyticsListener.EventTime, metadata: Metadata) {
                for (i in 0 until metadata.length()) {
                    val entry = metadata.get(i)
                    val text = entry?.toString()?.trim() ?: continue
                    val regexMatch = Regex("""title="([^"]+)"""").find(text)
                    if (regexMatch != null) {
                        val fullTitle = regexMatch.groupValues[1]
                        _currentMetadata.value = fullTitle
                        updateNotification()
                        RadioWidgetProvider.updateWidgets(applicationContext, _isPlaying.value, _currentRadio.value?.id, _currentMetadata.value)
                        broadcastPlaybackState()
                    }
                }
            }
        })
    }

    private fun broadcastPlaybackState() {
        try {
            val intent = Intent(ACTION_PLAYBACK_STATE_CHANGED).apply {
                addCategory(Intent.CATEGORY_DEFAULT)
                putExtra(EXTRA_IS_PLAYING, _isPlaying.value)
                putExtra(EXTRA_METADATA, _currentMetadata.value)
                putExtra(EXTRA_CURRENT_RADIO, _currentRadio.value?.id)
                putExtra(EXTRA_AUDIO_SESSION_ID, exoPlayer.audioSessionId)
                addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES or Intent.FLAG_RECEIVER_FOREGROUND)
            }
            applicationContext.sendBroadcast(intent)
        } catch (e: Exception) { if (BuildConfig.DEBUG) Log.e(TAG, "Broadcast error: ${e.message}") }
    }

    private fun broadcastPlaybackError() {
        try {
            val intent = Intent(ACTION_PLAYBACK_STATE_CHANGED).apply {
                addCategory(Intent.CATEGORY_DEFAULT)
                putExtra(EXTRA_ERROR, "Nepodařilo se přehrát stanici")
                addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES or Intent.FLAG_RECEIVER_FOREGROUND)
            }
            applicationContext.sendBroadcast(intent)
        } catch (e: Exception) { }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            // Service restarted by system but we don't have enough state to resume playback automatically without intent
            return START_NOT_STICKY
        }

        when (intent.action) {
            ACTION_PLAY -> {
                val radioId = intent.getStringExtra(EXTRA_RADIO_ID)
                val contextName = intent.getStringExtra(EXTRA_CONTEXT_CATEGORY)
                
                // Guard: If we have no radio ID and no currently loaded radio, we cannot play.
                // But if we were started with startForegroundService, we MUST call startForeground or we crash.
                if (radioId == null && _currentRadio.value == null) {
                    Log.e(TAG, "ACTION_PLAY called without radioId and no current radio. Stopping.")
                    // Satisfy foreground promise to avoid crash
                    startForeground(RadioNotificationManager.NOTIFICATION_ID, radioNotificationManager.getInitialNotification())
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    return START_NOT_STICKY
                }

                // Ensure foreground immediately
                if (!isForegroundService) {
                    startForeground(RadioNotificationManager.NOTIFICATION_ID, radioNotificationManager.getInitialNotification())
                    isForegroundService = true
                }

                if (contextName != null) {
                    try {
                        playbackContext = RadioCategory.valueOf(contextName)
                    } catch (e: Exception) {
                        playbackContext = null
                    }
                }

                if (radioId != null) {
                    serviceScope.launch { 
                        val radio = radioRepository.getRadioById(radioId)
                        if (radio != null) {
                            playRadio(radio)
                        } else {
                             // ID exists in intent but radio not found in DB
                             Log.e(TAG, "Radio not found: $radioId")
                             stopSelf()
                        }
                    }
                } else {
                    _currentRadio.value?.let { playRadio(it) }
                }
            }
            ACTION_PAUSE -> pausePlayback()
            ACTION_NEXT -> playNextRadio()
            ACTION_PREVIOUS -> playPreviousRadio()
            // Remove duplicate ACTION_PREVIOUS case if exists
            ACTION_STOP -> stopPlayback()
            ACTION_SET_VOLUME -> exoPlayer.volume = intent.getFloatExtra(EXTRA_VOLUME, 1.0f)
            ACTION_REQUEST_STATE -> {
                broadcastPlaybackState()
                // If not playing and not foreground, we can stop to save resources
                // But only if we are sure we are not needed.
                if (!_isPlaying.value && !isForegroundService) {
                    stopSelf()
                } else if (_isPlaying.value && !isForegroundService) {
                     // If we are playing but somehow lost foreground (unlikely), restore it
                     startForeground(RadioNotificationManager.NOTIFICATION_ID, radioNotificationManager.getInitialNotification())
                     isForegroundService = true
                     updateNotification() // Update with real data
                }
            }
        }
        return START_STICKY
    }

    private fun decodeHtmlEntities(input: String?): String {
        if (input.isNullOrBlank()) return ""
        return android.text.Html.fromHtml(input, android.text.Html.FROM_HTML_MODE_LEGACY).toString()
    }

    private fun updateNotification(loading: Boolean = false) {
        radioNotificationManager.updateNotification(_currentRadio.value, _isPlaying.value, _currentMetadata.value, loading)
    }

    private fun playRadio(radio: Radio) {
        if (!requestAudioFocus()) return
        
        // Stop previous playback immediately to avoid confusion if new load fails
        exoPlayer.stop()
        
        try {
            if (radio.streamUrl.isBlank()) throw IllegalArgumentException("Empty stream URL")
            
            exoPlayer.setMediaItem(MediaItem.fromUri(radio.streamUrl))
            exoPlayer.prepare()
            exoPlayer.play()
            _currentRadio.value = radio
            _isPlaying.value = true
            updateNotification()
            broadcastPlaybackState()
            if (!wakeLock.isHeld) wakeLock.acquire()
            RadioWidgetProvider.updateWidgets(applicationContext, true, radio.id, _currentMetadata.value)
        } catch (e: Exception) { 
            if (BuildConfig.DEBUG) Log.e(TAG, "Play error", e)
            abandonAudioFocus() 
            broadcastPlaybackError()
            _isPlaying.value = false
        }
    }

    private fun pausePlayback() {
        serviceScope.launch(Dispatchers.Main.immediate) {
            exoPlayer.pause()
            _isPlaying.value = false
            updateNotification()
            broadcastPlaybackState()
            if (wakeLock.isHeld) wakeLock.release()
            RadioWidgetProvider.updateWidgets(applicationContext, false, _currentRadio.value?.id, _currentMetadata.value)
        }
    }

    private fun stopPlayback() {
        serviceScope.launch(Dispatchers.Main.immediate) {
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
            abandonAudioFocus()
            _isPlaying.value = false
            _currentRadio.value = null
            _currentMetadata.value = null
            broadcastPlaybackState()
            if (wakeLock.isHeld) wakeLock.release()
            if (screenWakeLock.isHeld) screenWakeLock.release()
            stopForeground(true)
            isForegroundService = false
            stopSelf()
        }
    }

    private fun playNextRadio() {
        serviceScope.launch {
            val radio = _currentRadio.value ?: return@launch
            
            val radiosFlow = when (playbackContext) {
                RadioCategory.VLASTNI -> radioRepository.getFavoriteRadios()
                RadioCategory.VSE -> radioRepository.getAllRadios()
                null -> radioRepository.getRadiosByCategory(radio.category)
                else -> radioRepository.getRadiosByCategory(playbackContext!!)
            }

            val radios = radiosFlow.first().sortedBy { it.name.lowercase() }
            if (radios.isEmpty()) return@launch
            val index = radios.indexOfFirst { it.id == radio.id }
            if (index != -1) {
                val nextRadio = if (index < radios.size - 1) radios[index + 1] else radios.first()
                playRadio(nextRadio)
            } else if (radios.isNotEmpty()) {
                 // Fallback if current not found in context (e.g. removed from favorites while playing)
                 playRadio(radios.first())
            }
        }
    }

    private fun playPreviousRadio() {
        serviceScope.launch {
            val radio = _currentRadio.value ?: return@launch
            
            val radiosFlow = when (playbackContext) {
                RadioCategory.VLASTNI -> radioRepository.getFavoriteRadios()
                RadioCategory.VSE -> radioRepository.getAllRadios()
                null -> radioRepository.getRadiosByCategory(radio.category)
                else -> radioRepository.getRadiosByCategory(playbackContext!!)
            }

            val radios = radiosFlow.first().sortedBy { it.name.lowercase() }
            if (radios.isEmpty()) return@launch
            val index = radios.indexOfFirst { it.id == radio.id }
            if (index != -1) {
                val prevRadio = if (index > 0) radios[index - 1] else radios.last()
                playRadio(prevRadio)
            } else if (radios.isNotEmpty()) {
                 playRadio(radios.last())
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        try {
            // Správné pořadí uvolňování zdrojů: Player -> Session
            exoPlayer.stop()
            exoPlayer.release()
            mediaSession.release()
            
            equalizerManager.release()
            if (wakeLock.isHeld) wakeLock.release()
            if (screenWakeLock.isHeld) screenWakeLock.release()
            unregisterReceiver(batteryReceiver)
            unregisterReceiver(audioOutputReceiver)
            serviceJob.cancel()
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.e(TAG, "Error in onDestroy: ${e.message}")
        }
    }
}

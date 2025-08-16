package cz.internetradio.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.BatteryManager
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.room.Room
import cz.internetradio.app.R
import cz.internetradio.app.data.RadioDatabase
import cz.internetradio.app.model.Radio
import cz.internetradio.app.repository.RadioRepository
import cz.internetradio.app.widget.RadioWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v4.media.MediaMetadataCompat
import android.os.PowerManager
import android.content.BroadcastReceiver
import cz.internetradio.app.api.RadioBrowserApi
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import cz.internetradio.app.MainActivity
import android.util.Log
import coil.ImageLoader
import coil.request.ImageRequest
import android.os.Bundle
import androidx.media3.common.Metadata
import androidx.media3.exoplayer.analytics.AnalyticsListener

@AndroidEntryPoint
class RadioService : Service() {

    @Inject
    lateinit var exoPlayer: ExoPlayer

    @Inject
    lateinit var database: RadioDatabase

    @Inject
    lateinit var radioBrowserApi: RadioBrowserApi

    private lateinit var radioRepository: RadioRepository
    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var wakeLock: PowerManager.WakeLock
    private lateinit var screenWakeLock: PowerManager.WakeLock
    private lateinit var notificationManager: NotificationManager
    private lateinit var imageLoader: ImageLoader

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private val _currentRadio = MutableStateFlow<Radio?>(null)
    val currentRadio: StateFlow<Radio?> = _currentRadio

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _currentMetadata = MutableStateFlow<String?>(null)
    val currentMetadata: StateFlow<String?> = _currentMetadata

    private var bufferSize = 2000 // Výchozí velikost bufferu v ms
    
    companion object {
        private const val TAG = "RadioService"
        const val ACTION_PLAY = "cz.internetradio.app.action.PLAY"
        const val ACTION_PAUSE = "cz.internetradio.app.action.PAUSE"
        const val ACTION_NEXT = "cz.internetradio.app.action.NEXT"
        const val ACTION_PREVIOUS = "cz.internetradio.app.action.PREVIOUS"
        const val ACTION_STOP = "cz.internetradio.app.action.STOP"
        const val ACTION_SET_VOLUME = "cz.internetradio.app.action.SET_VOLUME"
        const val EXTRA_RADIO_ID = "radio_id"
        const val EXTRA_VOLUME = "volume"
        const val EXTRA_IS_PLAYING = "is_playing"
        const val EXTRA_METADATA = "metadata"
        const val EXTRA_CURRENT_RADIO = "current_radio"
        const val EXTRA_AUDIO_SESSION_ID = "audio_session_id"
        private const val NOTIFICATION_CHANNEL_ID = "radio_channel"
        private const val NOTIFICATION_ID = 1
        private const val WAKELOCK_TAG = "RadioService::WakeLock"

        // Broadcast akce pro komunikaci s ViewModel
        const val ACTION_PLAYBACK_STATE_CHANGED = "cz.internetradio.app.action.PLAYBACK_STATE_CHANGED"
    }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_POWER_CONNECTED -> {
                    // Při připojení nabíječky udržujeme obrazovku zapnutou
                    if (!screenWakeLock.isHeld) {
                        screenWakeLock.acquire()
                    }
                }
                Intent.ACTION_POWER_DISCONNECTED -> {
                    // Při odpojení nabíječky uvolníme WakeLock pro obrazovku
                    if (screenWakeLock.isHeld) {
                        screenWakeLock.release()
                    }
                }
            }
        }
    }

    private val audioOutputReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_AUDIO_BECOMING_NOISY -> {
                    // Tato akce se spustí při odpojení wired headsetu
                    Log.d(TAG, "🔊 Audio výstup se stal hlučným - pozastavuji přehrávání")
                    if (_isPlaying.value) {
                        pausePlayback()
                    }
                }
            }
        }
    }

    // Proměnná pro uložení stavu před změnou audio výstupu
    private var wasPlayingBeforeAudioOutputChange = false
    
    // AudioManager pro správu audio focusu a detekci změn
    private lateinit var audioManager: AudioManager
    
    // Audio Focus Change Listener pro správu audio focusu
    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        Log.d(TAG, "🎵 Audio focus změna: $focusChange")
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                Log.d(TAG, "🎵 Audio focus získán")
                // Můžeme pokračovat v přehrávání
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                Log.d(TAG, "🎵 Audio focus ztracen - pozastavuji přehrávání")
                if (_isPlaying.value) {
                    pausePlayback()
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                Log.d(TAG, "🎵 Audio focus dočasně ztracen - pozastavuji přehrávání")
                if (_isPlaying.value) {
                    wasPlayingBeforeAudioOutputChange = true
                    pausePlayback()
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                Log.d(TAG, "🎵 Audio focus dočasně ztracen - snižuji hlasitost")
                // Můžeme snížit hlasitost místo pozastavení
                exoPlayer.volume = 0.3f
            }
        }
    }
    
    // Metoda pro požádání o audio focus
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
            audioManager.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }
        
        val granted = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        Log.d(TAG, "🎵 Audio focus požadavek: ${if (granted) "udělen" else "zamítnut"}")
        return granted
    }
    
    // Metoda pro uvolnění audio focusu
    private fun abandonAudioFocus() {
        audioManager.abandonAudioFocus(audioFocusChangeListener)
        Log.d(TAG, "🎵 Audio focus uvolněn")
    }
    
    // Metoda pro kontrolu změn audio výstupu
    private fun checkAudioOutputChanges() {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val currentAudioOutput = isAudioOutputConnected()
        
        // Kontrola, zda se změnil stav audio výstupu
        if (!currentAudioOutput && _isPlaying.value) {
            Log.d(TAG, "🔍 Žádný audio výstup není připojen - pozastavuji přehrávání")
            
            // Uložení stavu před pozastavením
            wasPlayingBeforeAudioOutputChange = true
            
            // Pozastavení přehrávání
            pausePlayback()
            
            // Zobrazení notifikace uživateli
            showAudioOutputDisconnectedNotification()
            
            // Aktualizace notifikace s informací o pozastavení
            updateNotificationWithAudioOutputInfo("Pozastaveno - audio výstup odpojen")
            
        } else if (currentAudioOutput && !_isPlaying.value && wasPlayingBeforeAudioOutputChange) {
            Log.d(TAG, "🔍 Audio výstup je opět připojen - obnovuji přehrávání")
            wasPlayingBeforeAudioOutputChange = false
            
            // Obnovení přehrávání, pokud bylo předtím pozastaveno kvůli odpojení audio výstupu
            _currentRadio.value?.let { radio ->
                playRadio(radio)
            }
            
            // Skrytí notifikace o odpojení audio výstupu
            notificationManager.cancel(2)
            
            // Obnovení původní notifikace
            updateNotification()
        }
        
        // Kontrola, zda je audio systém stále aktivní
        if (currentAudioOutput && _isPlaying.value) {
            val isAudioStillActive = audioManager.isMusicActive || 
                                   audioManager.mode != AudioManager.MODE_NORMAL
            
            if (!isAudioStillActive) {
                Log.d(TAG, "🔍 Audio systém není aktivní - pozastavuji přehrávání")
                wasPlayingBeforeAudioOutputChange = true
                pausePlayback()
                updateNotificationWithAudioOutputInfo("Pozastaveno - audio systém neaktivní")
            }
        }
    }
    
    // Metoda pro kontrolu, zda je připojen Bluetooth nebo headset
    private fun isAudioOutputConnected(): Boolean {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val isBluetoothConnected = audioManager.isBluetoothScoOn || 
                                 audioManager.isBluetoothA2dpOn ||
                                 audioManager.mode == AudioManager.MODE_IN_COMMUNICATION
        val isWiredHeadsetConnected = audioManager.isWiredHeadsetOn
        val isSpeakerOn = audioManager.isSpeakerphoneOn
        
        // Kontrola, zda je aktivní nějaký audio výstup
        val hasActiveAudioOutput = isBluetoothConnected || isWiredHeadsetConnected || isSpeakerOn
        
        // Kontrola, zda je audio systém aktivní
        val isAudioSystemActive = audioManager.mode != AudioManager.MODE_NORMAL || 
                                 audioManager.isMusicActive
        
        // Kontrola, zda je nějaké audio zařízení připojeno
        val hasConnectedAudioDevice = audioManager.isBluetoothScoOn || 
                                    audioManager.isBluetoothA2dpOn ||
                                    audioManager.isWiredHeadsetOn ||
                                    audioManager.isSpeakerphoneOn
        
        Log.d(TAG, "🔍 Kontrola audio výstupu: Bluetooth SCO=$isBluetoothConnected, A2DP=${audioManager.isBluetoothA2dpOn}, Headset=$isWiredHeadsetConnected, Speaker=$isSpeakerOn, Active=$hasActiveAudioOutput, System=$isAudioSystemActive, Device=$hasConnectedAudioDevice")
        
        return hasActiveAudioOutput || isAudioSystemActive || hasConnectedAudioDevice
    }
    
    // Spuštění periodické kontroly audio výstupu
    private fun startAudioOutputMonitoring() {
        serviceScope.launch {
            while (true) {
                try {
                    kotlinx.coroutines.delay(3000) // Kontrola každé 3 sekundy
                    
                    // Kontrola, zda je audio focus stále aktivní
                    if (_isPlaying.value && !audioManager.isMusicActive) {
                        Log.d(TAG, "🔍 Audio focus není aktivní - pozastavuji přehrávání")
                        wasPlayingBeforeAudioOutputChange = true
                        pausePlayback()
                        updateNotificationWithAudioOutputInfo("Pozastaveno - audio focus ztracen")
                    }
                    
                    // Kontrola Bluetooth stavu
                    val isBluetoothActive = audioManager.isBluetoothScoOn || audioManager.isBluetoothA2dpOn
                    if (!isBluetoothActive && _isPlaying.value && wasPlayingBeforeAudioOutputChange) {
                        Log.d(TAG, "🔍 Bluetooth není aktivní - pozastavuji přehrávání")
                        pausePlayback()
                        updateNotificationWithAudioOutputInfo("Pozastaveno - Bluetooth odpojen")
                    }
                    
                    // Kontrola, zda je nějaký audio výstup připojen
                    if (_isPlaying.value && !isAudioOutputConnected()) {
                        Log.d(TAG, "🔍 Žádný audio výstup není připojen - pozastavuji přehrávání")
                        wasPlayingBeforeAudioOutputChange = true
                        pausePlayback()
                        updateNotificationWithAudioOutputInfo("Pozastaveno - audio výstup odpojen")
                    }
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Chyba při kontrole audio výstupu: ${e.message}")
                    break
                }
            }
        }
    }
    
    // Zobrazení notifikace o odpojení audio výstupu
    private fun showAudioOutputDisconnectedNotification() {
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Audio výstup odpojen")
            .setContentText("Přehrávání bylo pozastaveno - připojte reproduktor nebo headset")
            .setSmallIcon(R.drawable.ic_radio_default)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setTimeoutAfter(5000) // Automatické skrytí po 5 sekundách
            .build()
        
        notificationManager.notify(2, notification)
    }
    
    // Aktualizace hlavní notifikace s informací o audio výstupu
    private fun updateNotificationWithAudioOutputInfo(info: String) {
        val currentRadio = _currentRadio.value
        if (currentRadio != null) {
            val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setContentTitle(currentRadio.name)
                .setContentText(info)
                .setSmallIcon(R.drawable.ic_radio_default)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setOngoing(true)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .build()
            
            notificationManager.notify(NOTIFICATION_ID, notification)
        }
    }

    override fun onCreate() {
        super.onCreate()
        
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        imageLoader = ImageLoader.Builder(this)
            .crossfade(true)
            .build()
        
        // Inicializace AudioManager
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        
        // Inicializace repository s RadioDao a RadioBrowserApi
        radioRepository = RadioRepository(database.radioDao(), radioBrowserApi)
        
        // Nastavení ExoPlayeru
        exoPlayer = ExoPlayer.Builder(this)
            .setLoadControl(
                DefaultLoadControl.Builder()
                    .setBufferDurationsMs(
                        bufferSize,
                        bufferSize * 2,
                        bufferSize / 2,
                        bufferSize / 2
                    )
                    .setPrioritizeTimeOverSizeThresholds(true)
                    .build()
            )
            .build()
        
        setupPlayer()
        
        // Inicializace MediaSession hned na začátku
        mediaSession = MediaSessionCompat(this, "RadioService").apply {
            setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    _currentRadio.value?.let { radio ->
                        // Požádání o audio focus před obnovením přehrávání
                        if (requestAudioFocus()) {
                            playRadio(radio)
                        }
                    }
                }

                override fun onPause() {
                    pausePlayback()
                }

                override fun onSkipToNext() {
                    playNextRadio()
                }

                override fun onSkipToPrevious() {
                    playPreviousRadio()
                }

                override fun onStop() {
                    stopPlayback()
                }
            })
        }
        
        // Inicializace WakeLock pro přehrávání
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            WAKELOCK_TAG
        ).apply {
            setReferenceCounted(false)
        }
        
        // Inicializace WakeLock pro obrazovku
        screenWakeLock = powerManager.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ON_AFTER_RELEASE,
            "RadioService::ScreenWakeLock"
        ).apply {
            setReferenceCounted(false)
        }
        
        // Kontrola stavu nabíjení a nastavení WakeLock pro obrazovku
        val batteryStatus = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || 
                        status == BatteryManager.BATTERY_STATUS_FULL
        
        if (isCharging && !screenWakeLock.isHeld) {
            screenWakeLock.acquire()
        }
        
        createNotificationChannel()
        
        // Vytvoření základní notifikace pro foreground service
        val initialNotification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Internet Radio")
            .setContentText("Připraveno k přehrávání")
            .setSmallIcon(R.drawable.ic_radio_default)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
            
        Log.d("RadioService", "🔔 Vytvářím počáteční notifikaci")
        Log.d("RadioService", "🔔 Notifikace obsahuje:")
        Log.d("RadioService", "   - Title: ${initialNotification.extras.getString("android.title")}")
        Log.d("RadioService", "   - Text: ${initialNotification.extras.getString("android.text")}")
        Log.d("RadioService", "   - Priority: ${initialNotification.extras.getInt("android.priority", 0)}")
        Log.d("RadioService", "   - Category: ${initialNotification.extras.getString("android.category")}")
        Log.d("RadioService", "   - Visibility: ${initialNotification.extras.getInt("android.visibility", 0)}")
        Log.d("RadioService", "   - Ongoing: ${initialNotification.extras.getBoolean("android.ongoing", false)}")
        
        startForeground(NOTIFICATION_ID, initialNotification)
        
        // Kontrola, zda se notifikace zobrazuje při startu
        val activeNotifications = notificationManager.activeNotifications
        val hasOurNotification = activeNotifications.any { it.id == NOTIFICATION_ID }
        Log.d("RadioService", "🔍 Kontrola notifikace při startu: ${if (hasOurNotification) "ZOBRAZUJE SE" else "NEZOBRAZUJE SE"}")
        Log.d("RadioService", "🔍 Počet aktivních notifikací: ${activeNotifications.size}")
        
        // Kontrola notifikace po chvíli
        serviceScope.launch {
            kotlinx.coroutines.delay(1000)
            val delayedNotifications = notificationManager.activeNotifications
            val hasDelayedNotification = delayedNotifications.any { it.id == NOTIFICATION_ID }
            Log.d("RadioService", "🔍 Kontrola notifikace po 1s: ${if (hasDelayedNotification) "ZOBRAZUJE SE" else "NEZOBRAZUJE SE"}")
            Log.d("RadioService", "🔍 Počet aktivních notifikací po 1s: ${delayedNotifications.size}")
        }
        
        // Registrace receiveru pro sledování stavu baterie
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
        registerReceiver(batteryReceiver, filter)
        
        // Registrace receiveru pro sledování změn audio výstupu
        val audioFilter = IntentFilter().apply {
            addAction(Intent.ACTION_AUDIO_BECOMING_NOISY)
        }
        registerReceiver(audioOutputReceiver, audioFilter)
        
        // Spuštění periodické kontroly audio výstupu
        startAudioOutputMonitoring()
        
        // Odeslání počátečního stavu
        broadcastPlaybackState()
    }

    private fun setupPlayer() {
        Log.d(TAG, "🎵 Nastavuji ExoPlayer")
        val initialSessionId = exoPlayer.audioSessionId
        Log.d(TAG, "🎵 Počáteční audio session ID: $initialSessionId")
        
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                val currentSessionId = exoPlayer.audioSessionId
                Log.d(TAG, """🎵 onIsPlayingChanged:
                    |  - isPlaying: $isPlaying
                    |  - audio session ID: $currentSessionId
                    """.trimMargin())
                _isPlaying.value = isPlaying
                updatePlaybackState()
                
                // Explicitní aktualizace notifikace s malým zpožděním
                serviceScope.launch {
                    kotlinx.coroutines.delay(100)
                    updateNotification()
                }
                
                broadcastPlaybackState()
                
                // Aktualizace widgetu
                RadioWidgetProvider.updateWidgets(
                    applicationContext,
                    isPlaying,
                    _currentRadio.value?.id
                )
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                val currentSessionId = exoPlayer.audioSessionId
                Log.d(TAG, """🎵 onPlaybackStateChanged:
                    |  - stav: ${playbackStateToString(playbackState)}
                    |  - audio session ID: $currentSessionId
                    """.trimMargin())
                onPlaybackStateChanged(playbackState)
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                super.onMediaItemTransition(mediaItem, reason)
                val currentSessionId = exoPlayer.audioSessionId
                Log.d(TAG, """🎵 onMediaItemTransition:
                    |  - důvod: $reason
                    |  - audio session ID: $currentSessionId
                    """.trimMargin())
                broadcastPlaybackState()
            }

            override fun onMediaMetadataChanged(mediaMetadata: androidx.media3.common.MediaMetadata) {
                val radio = _currentRadio.value
                val title = mediaMetadata.title?.toString()?.trim()?.takeIf { it.isNotEmpty() }
                val artist = mediaMetadata.artist?.toString()?.trim()?.takeIf { it.isNotEmpty() }

                Log.d("RadioService", "📻 Přijata metadata:")
                Log.d("RadioService", "- title: '${title ?: "null"}'")
                Log.d("RadioService", "- artist: '${artist ?: "null"}'")

                // Hledání v `extras`
                val extras = mediaMetadata.extras
                val icyMetadata = extras?.getString("icy_metadata")?.takeIf { it.isNotEmpty() }
                val icyTitle = extras?.getString("icy_title")?.takeIf { it.isNotEmpty() }
                val icyArtist = extras?.getString("icy_artist")?.takeIf { it.isNotEmpty() }

                Log.d("RadioService", "🔹 icy_metadata: '${icyMetadata ?: "null"}'")
                Log.d("RadioService", "🔹 icy_title: '${icyTitle ?: "null"}'")
                Log.d("RadioService", "🔹 icy_artist: '${icyArtist ?: "null"}'")

                // Regex pro extrakci názvu skladby z ICY metadat
                val icyRawText = icyMetadata ?: icyTitle ?: title
                val regexMatch = Regex("""title="([^"]+)"""").find(icyRawText ?: "")

                var extractedTitle: String? = null
                var extractedArtist: String? = null

                if (regexMatch != null) {
                    val fullTitle = regexMatch.groupValues[1] // Extrahovaný text mezi title="..."
                    Log.d("RadioService", "🎵 Extrahovaná metadata: $fullTitle")

                    // Pokud obsahuje "-", pokusíme se rozdělit na interpreta a název skladby
                    val parts = fullTitle.split(" - ", limit = 2)
                    if (parts.size == 2) {
                        extractedArtist = decodeHtmlEntities(parts[0].trim())
                        extractedTitle = decodeHtmlEntities(parts[1].trim())
                    } else {
                        extractedTitle = decodeHtmlEntities(fullTitle)
                    }
                }

                // Pokud interpret není nalezen, pokusíme se použít `icy_artist`
                if (extractedArtist.isNullOrBlank()) {
                    extractedArtist = decodeHtmlEntities(icyArtist)
                }

                // Ošetření prázdných metadat
                if (extractedTitle.isNullOrBlank() && extractedArtist.isNullOrBlank()) {
                    Log.d("RadioService", "⚠ Metadata neobsahují žádné platné informace.")
                    return
                }

                Log.d("RadioService", "✅ Opravená metadata: '$extractedArtist - $extractedTitle'")

                _currentMetadata.value = when {
                    !extractedArtist.isNullOrBlank() && !extractedTitle.isNullOrBlank() -> "$extractedArtist - $extractedTitle"
                    !extractedTitle.isNullOrBlank() -> extractedTitle
                    !extractedArtist.isNullOrBlank() -> extractedArtist
                    else -> null
                }

                updateMediaMetadata(extractedArtist, extractedTitle)
                
                // Explicitní aktualizace notifikace po změně metadat
                serviceScope.launch {
                    kotlinx.coroutines.delay(200)
                    updateNotification()
                }
                
                broadcastPlaybackState()
            }
        })

        // Přidání AnalyticsListener pro zachycení metadat
        exoPlayer.addAnalyticsListener(object : AnalyticsListener {
            override fun onMetadata(eventTime: AnalyticsListener.EventTime, metadata: Metadata) {
                Log.d("RadioService", "🎵 Přijata metadata")
                
                for (i in 0 until metadata.length()) {
                    val entry = metadata.get(i)
                    Log.d("RadioService", "📻 Metadata entry: ${entry?.javaClass?.simpleName}")
                    
                    val text = when (entry) {
                        is Metadata.Entry -> entry.toString()
                        else -> null
                    }?.trim()
                    
                    if (!text.isNullOrBlank()) {
                        Log.d("RadioService", "📻 Metadata text: '$text'")
                        
                        // Použití stejného regexu jako v onMediaMetadataChanged
                        val regexMatch = Regex("""title="([^"]+)"""").find(text)
                        
                        if (regexMatch != null) {
                            val fullTitle = regexMatch.groupValues[1]
                            Log.d("RadioService", "🎵 Extrahovaná metadata: $fullTitle")
                            
                            // Nastavení kompletních metadat do _currentMetadata
                            _currentMetadata.value = fullTitle
                            
                            val parts = fullTitle.split(" - ", limit = 2)
                            val (extractedArtist, extractedTitle) = if (parts.size == 2) {
                                Pair(parts[0].trim(), parts[1].trim())
                            } else {
                                Pair(null, fullTitle)
                            }
                            
                            Log.d("RadioService", "✅ Opravená metadata: '$extractedArtist - $extractedTitle'")
                            
                            updateMediaMetadata(extractedArtist, extractedTitle)
                            updateNotification()
                            broadcastPlaybackState()
                        }
                    }
                }
            }
        })
    }

    private fun onPlaybackStateChanged(playbackState: Int) {
        Log.d("RadioService", "Stav přehrávání změněn na ${playbackStateToString(playbackState)}, audio session ID: ${exoPlayer.audioSessionId}")

        when (playbackState) {
            Player.STATE_READY -> {
                Log.d("RadioService", "Přehrávač je připraven, audio session ID: ${exoPlayer.audioSessionId}")
                _isPlaying.value = exoPlayer.isPlaying
                
                // Aktualizace notifikace s malým zpožděním
                serviceScope.launch {
                    kotlinx.coroutines.delay(150)
                    updateNotification()
                }
            }
            Player.STATE_BUFFERING -> {
                Log.d("RadioService", "Přehrávač načítá data, audio session ID: ${exoPlayer.audioSessionId}")
                updateNotification(isLoading = true)
            }
            Player.STATE_ENDED -> {
                Log.d("RadioService", "Přehrávání skončilo, audio session ID: ${exoPlayer.audioSessionId}")
                _isPlaying.value = false
                
                // Aktualizace notifikace s malým zpožděním
                serviceScope.launch {
                    kotlinx.coroutines.delay(150)
                    updateNotification()
                }
            }
            Player.STATE_IDLE -> {
                Log.d("RadioService", "Přehrávač je nečinný, audio session ID: ${exoPlayer.audioSessionId}")
                _isPlaying.value = false
                
                // Aktualizace notifikace s malým zpožděním
                serviceScope.launch {
                    kotlinx.coroutines.delay(150)
                    updateNotification()
                }
            }
        }

        broadcastPlaybackState()
    }

    private fun broadcastPlaybackState() {
        try {
            val audioSessionId = exoPlayer.audioSessionId
            Log.d("RadioService", """📢 Odesílám broadcast:
                |  - playing: ${_isPlaying.value}
                |  - metadata: ${_currentMetadata.value}
                |  - radio: ${_currentRadio.value?.name}
                |  - audioSessionId: $audioSessionId
                """.trimMargin())

            val intent = Intent(ACTION_PLAYBACK_STATE_CHANGED).apply {
                addCategory(Intent.CATEGORY_DEFAULT)
                putExtra(EXTRA_IS_PLAYING, _isPlaying.value)
                putExtra(EXTRA_METADATA, _currentMetadata.value)
                putExtra(EXTRA_CURRENT_RADIO, _currentRadio.value?.id)
                putExtra(EXTRA_AUDIO_SESSION_ID, audioSessionId)
                // Přidání flagů pro zajištění doručení
                addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
            }
            
            // Použití applicationContext pro zajištění doručení
            applicationContext.sendBroadcast(intent)
            Log.d("RadioService", "✅ Broadcast odeslán s audio session ID: $audioSessionId")
        } catch (e: Exception) {
            Log.e("RadioService", "❌ Chyba při odesílání broadcastu: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun updatePlaybackState() {
        try {
            val state = if (_isPlaying.value) {
                PlaybackStateCompat.STATE_PLAYING
            } else {
                PlaybackStateCompat.STATE_PAUSED
            }

            val playbackState = PlaybackStateCompat.Builder()
                .setState(state, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1.0f)
                .setActions(
                    PlaybackStateCompat.ACTION_PLAY or
                    PlaybackStateCompat.ACTION_PAUSE or
                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                    PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                    PlaybackStateCompat.ACTION_STOP
                )
                .build()

            mediaSession.setPlaybackState(playbackState)
        } catch (e: Exception) {
            // Ignorujeme chybu při aktualizaci stavu
        }
    }

    private fun updateMediaMetadata(artist: String?, title: String?) {
        try {
            val radio = _currentRadio.value ?: return
            
            Log.d("RadioService", "📢 Aktualizuji metadata v MediaSession:")
            Log.d("RadioService", "- artist: '${artist ?: "null"}'")
            Log.d("RadioService", "- title: '${title ?: "null"}'")

            val displaySubtitle = when {
                !artist.isNullOrBlank() && !title.isNullOrBlank() -> "$artist - $title"
                !title.isNullOrBlank() -> title
                !artist.isNullOrBlank() -> artist
                else -> ""
            }

            val metadataBuilder = MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title ?: radio.name)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist ?: "")
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, radio.name)
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, radio.name)
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, displaySubtitle)
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION, radio.description)
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, radio.id)
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, radio.streamUrl)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, radio.imageUrl)
                .putString("android.media.metadata.ARTIST", artist ?: "")
                .putString("android.media.metadata.TITLE", title ?: radio.name)
                .putString("android.media.metadata.DISPLAY_TITLE", radio.name)
                .putString("android.media.metadata.DISPLAY_SUBTITLE", displaySubtitle)

            mediaSession.setMetadata(metadataBuilder.build())
            Log.d("RadioService", "✅ Metadata v MediaSession úspěšně aktualizována")
            Log.d("RadioService", "🎵 Nastaveno: název='${radio.name}', metadata='$displaySubtitle'")

            // Explicitní aktualizace notifikace po změně metadat
            updateNotification()
        } catch (e: Exception) {
            Log.e("RadioService", "❌ Chyba při aktualizaci metadat: ${e.message}")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("RadioService", "onStartCommand: ${intent?.action}")
        when (intent?.action) {
            ACTION_PLAY -> {
                val radioId = intent.getStringExtra(EXTRA_RADIO_ID)
                if (radioId != null) {
                    serviceScope.launch {
                        val radio = radioRepository.getRadioById(radioId)
                        radio?.let { 
                            Log.d("RadioService", "Spouštím rádio z ID: ${radio.name}")
                            playRadio(it) 
                        }
                    }
                } else {
                    _currentRadio.value?.let { 
                        Log.d("RadioService", "Obnovuji přehrávání: ${it.name}")
                        playRadio(it) 
                    }
                }
            }
            ACTION_PAUSE -> {
                Log.d("RadioService", "Pozastavuji přehrávání")
                pausePlayback()
            }
            ACTION_NEXT -> {
                Log.d("RadioService", "Přepínám na další")
                playNextRadio()
            }
            ACTION_PREVIOUS -> {
                Log.d("RadioService", "Přepínám na předchozí")
                playPreviousRadio()
            }
            ACTION_STOP -> {
                Log.d("RadioService", "Zastavuji přehrávání")
                stopPlayback()
            }
            ACTION_SET_VOLUME -> {
                val volume = intent.getFloatExtra(EXTRA_VOLUME, 1.0f)
                Log.d("RadioService", "Nastavuji hlasitost: $volume")
                exoPlayer.volume = volume
            }
        }
        return START_STICKY
    }

    private fun initMediaSession() {
        if (!::mediaSession.isInitialized) {
            mediaSession = MediaSessionCompat(this, "RadioService").apply {
                setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
                setCallback(object : MediaSessionCompat.Callback() {
                    override fun onPlay() {
                        _currentRadio.value?.let { playRadio(it) }
                    }

                    override fun onPause() {
                        pausePlayback()
                    }

                    override fun onSkipToNext() {
                        playNextRadio()
                    }

                    override fun onSkipToPrevious() {
                        playPreviousRadio()
                    }

                    override fun onStop() {
                        stopPlayback()
                    }
                })
                
                // Nastavení výchozího stavu přehrávání
                val playbackState = PlaybackStateCompat.Builder()
                    .setState(PlaybackStateCompat.STATE_STOPPED, 0, 1.0f)
                    .setActions(PlaybackStateCompat.ACTION_PLAY or 
                               PlaybackStateCompat.ACTION_PAUSE or 
                               PlaybackStateCompat.ACTION_SKIP_TO_NEXT or 
                               PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or 
                               PlaybackStateCompat.ACTION_STOP)
                    .build()
                setPlaybackState(playbackState)
                
                // Nastavení výchozích metadat
                val metadata = MediaMetadataCompat.Builder()
                    .putString(MediaMetadataCompat.METADATA_KEY_TITLE, "Internetové rádio")
                    .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, "")
                    .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, "Internetové rádio")
                    .build()
                setMetadata(metadata)
                
                Log.d("RadioService", "🎵 MediaSession inicializován s session tokenem: ${sessionToken}")
            }
        }
    }

    private fun decodeHtmlEntities(input: String?): String {
        if (input.isNullOrBlank()) return ""
        return android.text.Html.fromHtml(input, android.text.Html.FROM_HTML_MODE_LEGACY).toString()
    }

    private fun createNotification(): NotificationCompat.Builder {
        val radio = _currentRadio.value
        val isPlaying = _isPlaying.value
        val metadata = _currentMetadata.value
        
        // Intent pro otevření aplikace
        val contentIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            0,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Rozdělení metadat na interpreta a název skladby
        val (artist, title) = if (metadata?.contains(" - ") == true) {
            val parts = metadata.split(" - ", limit = 2)
            Pair(parts[0].trim(), parts[1].trim())
        } else {
            Pair(null, metadata)
        }
        
        // Sestavení textu pro notifikaci
        val displayTitle = radio?.name ?: "Internetové rádio"
        val displayText = when {
            !artist.isNullOrBlank() && !title.isNullOrBlank() -> "$artist - $title"
            !title.isNullOrBlank() -> title
            !artist.isNullOrBlank() -> artist
            else -> "Internetové rádio"
        }
        
        Log.d("RadioService", "🔔 Vytvářím notifikaci:")
        Log.d("RadioService", "- název rádia: '$displayTitle'")
        Log.d("RadioService", "- text: '$displayText'")
        Log.d("RadioService", "- artist: '$artist'")
        Log.d("RadioService", "- title: '$title'")
        Log.d("RadioService", "- isPlaying: ${_isPlaying.value}")
        Log.d("RadioService", "- currentRadio: ${_currentRadio.value?.name}")

        // Vytvoření MediaStyle s podporou pro moderní Android
        val mediaStyle = androidx.media.app.NotificationCompat.MediaStyle()
            .setShowActionsInCompactView(0, 1, 2) // Zobrazit play/pause, previous, next v kompaktním zobrazení
            .setMediaSession(mediaSession.sessionToken)
            .setShowCancelButton(true)

        // Vytvoření notifikace s MediaStyle a správnou prioritou
        val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(displayTitle)
            .setContentText(displayText)
            .setSubText(title)
            .setTicker(displayText)
            .setStyle(mediaStyle)
            .setSmallIcon(if (isPlaying) R.drawable.ic_notification_play else R.drawable.ic_pause) // Správná ikona podle stavu
            .setContentIntent(contentPendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW) // Změna na LOW pro MediaStyle
            .setOngoing(true)
            .setOnlyAlertOnce(false)
            .setAutoCancel(false)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT) // Důležité pro MediaStyle

        // Načtení ikony rádia pomocí Coil
        radio?.imageUrl?.let { imageUrl ->
            try {
                val request = ImageRequest.Builder(this)
                    .data(imageUrl)
                    .target { drawable ->
                        val bitmap = (drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
                        bitmap?.let {
                            builder.setLargeIcon(it)
                            // Explicitní aktualizace notifikace s ikonou
                            notificationManager.notify(NOTIFICATION_ID, builder.build())
                        }
                    }
                    .build()
                imageLoader.enqueue(request)
            } catch (e: Exception) {
                Log.e("RadioService", "Chyba při načítání ikony rádia: ${e.message}")
            }
        }

        // Přidáme ovládací tlačítka pouze pokud máme aktivní rádio
        if (radio != null) {
            try {
                // Intenty pro ovládací tlačítka
                val playPauseIntent = Intent(this, RadioService::class.java).apply {
                    action = if (isPlaying) ACTION_PAUSE else ACTION_PLAY
                }
                val playPausePendingIntent = PendingIntent.getService(
                    this,
                    0,
                    playPauseIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val previousIntent = Intent(this, RadioService::class.java).apply {
                    action = ACTION_PREVIOUS
                }
                val previousPendingIntent = PendingIntent.getService(
                    this,
                    1,
                    previousIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val nextIntent = Intent(this, RadioService::class.java).apply {
                    action = ACTION_NEXT
                }
                val nextPendingIntent = PendingIntent.getService(
                    this,
                    2,
                    nextIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val stopIntent = Intent(this, RadioService::class.java).apply {
                    action = ACTION_STOP
                }
                val stopPendingIntent = PendingIntent.getService(
                    this,
                    3,
                    stopIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                builder
                    .addAction(
                        R.drawable.ic_skip_previous,
                        "Předchozí",
                        previousPendingIntent
                    )
                    .addAction(
                        if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play,
                        if (isPlaying) "Pozastavit" else "Přehrát",
                        playPausePendingIntent
                    )
                    .addAction(
                        R.drawable.ic_skip_next,
                        "Další",
                        nextPendingIntent
                    )
                    .addAction(
                        R.drawable.ic_notification_close,
                        "Ukončit",
                        stopPendingIntent
                    )
            } catch (e: Exception) {
                Log.e("RadioService", "Chyba při vytváření ovládacích prvků notifikace: ${e.message}")
            }
        }

        Log.d("RadioService", "🔔 Notifikace vytvořena s:")
        Log.d("RadioService", "   - Style: ${builder.extras.getString("android.mediaStyle")}")
        Log.d("RadioService", "   - Priority: ${builder.extras.getInt("android.priority", 0)}")
        Log.d("RadioService", "   - Category: ${builder.extras.getString("android.category")}")
        Log.d("RadioService", "   - Visibility: ${builder.extras.getInt("android.visibility", 0)}")
        Log.d("RadioService", "   - Ongoing: ${builder.extras.getBoolean("android.ongoing", false)}")
        Log.d("RadioService", "   - SmallIcon: ${if (isPlaying) "ic_notification_play" else "ic_pause"}")
        Log.d("RadioService", "   - MediaSession Token: ${mediaSession.sessionToken}")
        Log.d("RadioService", "   - MediaSession Active: ${mediaSession.isActive}")
        
        return builder
    }

    private fun updateNotification(loading: Boolean = false) {
        try {
            val notificationBuilder = createNotification()
            
            if (loading) {
                notificationBuilder.setContentText("Čekejte chvíli...")
                notificationBuilder.setSubText(null)
            }

            val notification = notificationBuilder.build()
            Log.d("RadioService", "🔄 Aktualizuji notifikaci ${if (loading) "(Loading...)" else ""}")
            Log.d("RadioService", "🔔 Notifikace obsahuje:")
            Log.d("RadioService", "   - Title: ${notification.extras.getString("android.title")}")
            Log.d("RadioService", "   - Text: ${notification.extras.getString("android.text")}")
            Log.d("RadioService", "   - SubText: ${notification.extras.getString("android.subText")}")
            
            notificationManager.notify(NOTIFICATION_ID, notification)
            Log.d("RadioService", "✅ Notifikace úspěšně aktualizována")
            
            // Kontrola, zda se notifikace zobrazuje
            val activeNotifications = notificationManager.activeNotifications
            val hasOurNotification = activeNotifications.any { it.id == NOTIFICATION_ID }
            Log.d("RadioService", "🔍 Kontrola notifikace: ${if (hasOurNotification) "ZOBRAZUJE SE" else "NEZOBRAZUJE SE"}")
            Log.d("RadioService", "🔍 Počet aktivních notifikací: ${activeNotifications.size}")
        } catch (e: Exception) {
            Log.e("RadioService", "❌ Chyba při aktualizaci notifikace: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun playRadio(radio: Radio) {
        Log.d("RadioService", "🎵 Spouštím přehrávání rádia: ${radio.name}")
        
        // Požádání o audio focus před začátkem přehrávání
        if (!requestAudioFocus()) {
            Log.w(TAG, "🎵 Audio focus nebyl udělen - nelze přehrávat")
            return
        }
        
        try {
            // Nastavení MediaItem
            val mediaItem = MediaItem.fromUri(radio.url)
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.play()
            
            // Aktualizace stavů
            _currentRadio.value = radio
            _isPlaying.value = true
            
            // Nastavení MediaSession pro přehrávání
            val playbackState = PlaybackStateCompat.Builder()
                .setState(PlaybackStateCompat.STATE_PLAYING, 0, 1.0f)
                .setActions(PlaybackStateCompat.ACTION_PLAY or 
                           PlaybackStateCompat.ACTION_PAUSE or 
                           PlaybackStateCompat.ACTION_SKIP_TO_NEXT or 
                           PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or 
                           PlaybackStateCompat.ACTION_STOP)
                .build()
            mediaSession.setPlaybackState(playbackState)
            
            // Aktivace MediaSession
            mediaSession.isActive = true
            
            // Aktualizace notifikace
            updateNotification()
            
            // Odeslání broadcastu o změně stavu
            broadcastPlaybackState()
            
            // Získání WakeLock pro přehrávání
            if (!wakeLock.isHeld) {
                wakeLock.acquire()
            }
            
            // Aktualizace widgetu
            RadioWidgetProvider.updateWidgets(
                applicationContext,
                true,
                radio.id
            )
            
            Log.d("RadioService", "✅ Přehrávání rádia úspěšně spuštěno")
            
        } catch (e: Exception) {
            Log.e("RadioService", "Chyba při spouštění přehrávání: ${e.message}", e)
            // Uvolnění audio focusu při chybě
            abandonAudioFocus()
        }
    }

    private fun pausePlayback() {
        Log.d("RadioService", "Pozastavuji přehrávání")
        serviceScope.launch(Dispatchers.Main.immediate) {
            try {
                exoPlayer.pause()
                _isPlaying.value = false
                
                // Nastavení MediaSession pro pozastavení
                val playbackState = PlaybackStateCompat.Builder()
                    .setState(PlaybackStateCompat.STATE_PAUSED, 0, 1.0f)
                    .setActions(PlaybackStateCompat.ACTION_PLAY or 
                               PlaybackStateCompat.ACTION_PAUSE or 
                               PlaybackStateCompat.ACTION_SKIP_TO_NEXT or 
                               PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or 
                               PlaybackStateCompat.ACTION_STOP)
                    .build()
                mediaSession.setPlaybackState(playbackState)
                
                updatePlaybackState()
                
                Log.d("RadioService", "🔄 Aktualizuji notifikaci po pozastavení")
                updateNotification()
                broadcastPlaybackState()
                
                if (wakeLock.isHeld) {
                    wakeLock.release()
                }
                
                // Aktualizace widgetu
                RadioWidgetProvider.updateWidgets(
                    applicationContext,
                    false,
                    _currentRadio.value?.id
                )
                
                // Kontrola notifikace po pozastavení
                serviceScope.launch {
                    kotlinx.coroutines.delay(300)
                    val activeNotifications = notificationManager.activeNotifications
                    val hasOurNotification = activeNotifications.any { it.id == NOTIFICATION_ID }
                    Log.d("RadioService", "🔍 Kontrola notifikace po pozastavení: ${if (hasOurNotification) "ZOBRAZUJE SE" else "NEZOBRAZUJE SE"}")
                }
            } catch (e: Exception) {
                Log.e("RadioService", "Chyba při pozastavování přehrávání: ${e.message}", e)
            }
        }
    }

    private fun stopPlayback() {
        Log.d("RadioService", "Zastavuji přehrávání")
        serviceScope.launch(Dispatchers.Main.immediate) {
            try {
                // Zastavení přehrávání
                exoPlayer.stop()
                exoPlayer.clearMediaItems()
                
                // Uvolnění audio focusu
                abandonAudioFocus()
                
                // Aktualizace stavů
                _isPlaying.value = false
                _currentRadio.value = null
                _currentMetadata.value = null
                
                // Nastavení MediaSession pro zastavení
                val playbackState = PlaybackStateCompat.Builder()
                    .setState(PlaybackStateCompat.STATE_STOPPED, 0, 1.0f)
                    .setActions(PlaybackStateCompat.ACTION_PLAY or 
                               PlaybackStateCompat.ACTION_PAUSE or 
                               PlaybackStateCompat.ACTION_SKIP_TO_NEXT or 
                               PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or 
                               PlaybackStateCompat.ACTION_STOP)
                    .build()
                mediaSession.setPlaybackState(playbackState)
                
                // Deaktivace MediaSession
                mediaSession.isActive = false
                
                // Odeslání broadcastu o změně stavu
                broadcastPlaybackState()
                
                // Uvolnění WakeLock pro přehrávání
                if (wakeLock.isHeld) {
                    wakeLock.release()
                }
                
                // Uvolnění WakeLock pro obrazovku
                if (screenWakeLock.isHeld) {
                    screenWakeLock.release()
                }
                
                // Zastavení služby
                stopForeground(true)
                stopSelf()
                
                Log.d("RadioService", "Přehrávání úspěšně zastaveno")
            } catch (e: Exception) {
                Log.e("RadioService", "Chyba při zastavování přehrávání: ${e.message}", e)
            }
        }
    }

    private fun playNextRadio() {
        serviceScope.launch {
            val currentRadio = _currentRadio.value ?: return@launch
            val allRadios = radioRepository.getRadiosByCategory(currentRadio.category).first()
            val sortedRadios = allRadios.sortedBy { it.name.lowercase() }
            val currentIndex = sortedRadios.indexOfFirst { it.id == currentRadio.id }
            
            if (currentIndex < sortedRadios.size - 1) {
                val nextRadio = sortedRadios[currentIndex + 1]
                Log.d("RadioService", "Přepínám na další rádio: ${nextRadio.name}")
                playRadio(nextRadio)
                broadcastPlaybackState()
            }
        }
    }

    private fun playPreviousRadio() {
        serviceScope.launch {
            val currentRadio = _currentRadio.value ?: return@launch
            val allRadios = radioRepository.getRadiosByCategory(currentRadio.category).first()
            val sortedRadios = allRadios.sortedBy { it.name.lowercase() }
            val currentIndex = sortedRadios.indexOfFirst { it.id == currentRadio.id }
            
            if (currentIndex > 0) {
                val previousRadio = sortedRadios[currentIndex - 1]
                Log.d("RadioService", "Přepínám na předchozí rádio: ${previousRadio.name}")
                playRadio(previousRadio)
                broadcastPlaybackState()
            }
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Přehrávání rádia",
            NotificationManager.IMPORTANCE_LOW // Důležité pro MediaStyle
        ).apply {
            description = "Ovládání přehrávání internetového rádia"
            setShowBadge(false) // Vypnout odznak pro MediaStyle
            enableLights(false) // Vypnout světla pro MediaStyle
            enableVibration(false) // Vypnout vibrace pro MediaStyle
            setSound(null, null) // Vypnout zvuk pro MediaStyle
        }
        notificationManager.createNotificationChannel(channel)
        Log.d("RadioService", "🔔 Notifikační kanál vytvořen s IMPORTANCE_LOW")
    }

    private fun recreatePlayer() {
        val currentPosition = exoPlayer.currentPosition
        val wasPlaying = exoPlayer.isPlaying
        val mediaItem = exoPlayer.currentMediaItem
        
        exoPlayer.release()
        
        exoPlayer = ExoPlayer.Builder(this)
            .setLoadControl(
                DefaultLoadControl.Builder()
                    .setBufferDurationsMs(
                        bufferSize,  // Minimální buffer
                        bufferSize * 2, // Maximální buffer
                        bufferSize / 2, // Buffer pro začátek přehrávání
                        bufferSize / 2  // Buffer pro pokračování po rebufferingu
                    )
                    .setPrioritizeTimeOverSizeThresholds(true)
                    .build()
            )
            .build()
            
        setupPlayer()
        
        mediaItem?.let {
            exoPlayer.setMediaItem(it)
            exoPlayer.prepare()
            if (wasPlaying) {
                exoPlayer.play()
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        try {
            // Zastavení přehrávání
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
            
            // Uvolnění audio focusu
            abandonAudioFocus()
            
            // Uvolnění ExoPlayeru
            exoPlayer.release()
            
            // Uvolnění MediaSession
            mediaSession.release()
            
            // Uvolnění WakeLock pro přehrávání
            if (wakeLock.isHeld) {
                wakeLock.release()
            }
            
            // Uvolnění WakeLock pro obrazovku
            if (screenWakeLock.isHeld) {
                screenWakeLock.release()
            }
            
            // Odregistrace receiveru
            unregisterReceiver(batteryReceiver)
            unregisterReceiver(audioOutputReceiver) // Odregistrace nového receiveru
            
            // Zrušení coroutine scope
            serviceJob.cancel()
            
            // Uvolnění ImageLoaderu
            imageLoader.shutdown()
            
            Log.d("RadioService", "Zdroje úspěšně uvolněny")
        } catch (e: Exception) {
            Log.e("RadioService", "Chyba při uvolňování zdrojů: ${e.message}")
        }
    }

    private fun playbackStateToString(state: Int): String {
        return when (state) {
            Player.STATE_IDLE -> "IDLE"
            Player.STATE_BUFFERING -> "BUFFERING"
            Player.STATE_READY -> "READY"
            Player.STATE_ENDED -> "ENDED"
            else -> "UNKNOWN"
        }
    }
} 
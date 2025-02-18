package cz.internetradio.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
                    bufferSize = 2000
                    recreatePlayer()
                }
                Intent.ACTION_POWER_DISCONNECTED -> {
                    bufferSize = 4000
                    recreatePlayer()
                }
                Intent.ACTION_BATTERY_LOW -> {
                    bufferSize = 6000
                    recreatePlayer()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        imageLoader = ImageLoader.Builder(this)
            .crossfade(true)
            .build()
        
        // Inicializace MediaSession hned na začátku
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
        }
        
        // Detekce připojení k nabíječce
        val batteryStatus = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || 
                        status == BatteryManager.BATTERY_STATUS_FULL
        
        // Nastavení velikosti bufferu podle stavu nabíjení
        bufferSize = if (isCharging) 2000 else 4000
        
        // Inicializace WakeLock s vyšší prioritou
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK or
            PowerManager.ON_AFTER_RELEASE,
            WAKELOCK_TAG
        ).apply {
            setReferenceCounted(false)
        }
        
        // Inicializace repository s RadioDao a RadioBrowserApi
        radioRepository = RadioRepository(database.radioDao(), radioBrowserApi)
        
        setupPlayer()
        createNotificationChannel()
        
        // Vytvoření základní notifikace pro foreground service
        val initialNotification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Internet Radio")
            .setContentText("Připraveno k přehrávání")
            .setSmallIcon(R.drawable.ic_radio_default)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
            
        startForeground(NOTIFICATION_ID, initialNotification)
        
        // Registrace receiveru pro sledování stavu baterie
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_BATTERY_LOW)
        }
        registerReceiver(batteryReceiver, filter)
    }

    private fun setupPlayer() {
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                Log.d("RadioService", "onIsPlayingChanged: $isPlaying")
                _isPlaying.value = isPlaying
                updatePlaybackState()
                updateNotification()
                broadcastPlaybackState()
                
                // Aktualizace widgetu
                RadioWidgetProvider.updateWidgets(
                    applicationContext,
                    isPlaying,
                    _currentRadio.value?.id
                )
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)
                Log.d("RadioService", "onPlaybackStateChanged: $playbackState")
                // Přidáno pro lepší synchronizaci
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
                updateNotification()
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

    private fun broadcastPlaybackState() {
        try {
            Log.d("RadioService", "Odesílám broadcast - playing: ${_isPlaying.value}, radio: ${_currentRadio.value?.name}")
            val intent = Intent(ACTION_PLAYBACK_STATE_CHANGED).apply {
                putExtra(EXTRA_IS_PLAYING, _isPlaying.value)
                putExtra(EXTRA_METADATA, _currentMetadata.value)
                putExtra(EXTRA_CURRENT_RADIO, _currentRadio.value?.id)
                // Přidání flagů pro zajištění doručení
                addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
            }
            // Použití applicationContext pro zajištění doručení
            applicationContext.sendBroadcast(intent)
            Log.d("RadioService", "Broadcast odeslán úspěšně")
        } catch (e: Exception) {
            Log.e("RadioService", "Chyba při odesílání broadcastu: ${e.message}")
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

        // Vytvoření MediaStyle s podporou pro Samsung
        val mediaStyle = androidx.media.app.NotificationCompat.MediaStyle()
            .setShowActionsInCompactView(1, 2)
            .setMediaSession(mediaSession.sessionToken)

        // Vytvoření notifikace s metadaty a vyšší prioritou
        val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(displayTitle)
            .setContentText(displayText)
            .setSubText(title)
            .setTicker(displayText)
            .setStyle(mediaStyle)
            .setSmallIcon(R.drawable.ic_notification_play)
            .setContentIntent(contentPendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setAutoCancel(false)
            .addExtras(Bundle().apply {
                putString("android.media.metadata.ARTIST", artist)
                putString("android.media.metadata.TITLE", title)
                putString("android.media.metadata.DISPLAY_TITLE", displayTitle)
                putString("android.media.metadata.DISPLAY_SUBTITLE", displayText)
            })

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
            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.e("RadioService", "❌ Chyba při aktualizaci notifikace: ${e.message}")
        }
    }

    private fun playRadio(radio: Radio) {
        Log.d("RadioService", "Spouštím rádio: ${radio.name}")
        try {
            if (!wakeLock.isHeld) {
                wakeLock.acquire(24 * 60 * 60 * 1000L)
            }
            
            initMediaSession()
            
            _currentRadio.value = radio
            _currentMetadata.value = null
            
            // Zobrazení "Čekejte chvíli..."
            updateNotification(loading = true)
            
            // Vytvoření MediaItem s metadaty
            val mediaItem = MediaItem.Builder()
                .setUri(radio.streamUrl)
                .setMediaMetadata(
                    androidx.media3.common.MediaMetadata.Builder()
                        .setTitle(radio.name)
                        .setDisplayTitle(radio.name)
                        .setDescription(radio.description)
                        .setArtist("")
                        .setIsBrowsable(false)
                        .setIsPlayable(true)
                        .setExtras(Bundle().apply {
                            putString("station_name", radio.name)
                        })
                        .build()
                )
                .build()
            
            // Spuštění na hlavním vlákně
            serviceScope.launch(Dispatchers.Main.immediate) {
                try {
                    // Reset ExoPlayeru
                    exoPlayer.stop()
                    exoPlayer.clearMediaItems()
                    
                    // Reinicializace ExoPlayeru
                    exoPlayer = ExoPlayer.Builder(this@RadioService)
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
                    
                    // Nastavení a přehrání MediaItem
                    exoPlayer.apply {
                        setMediaItem(mediaItem)
                        volume = 1.0f
                        prepare()
                        playWhenReady = true
                    }
                    
                    _isPlaying.value = true
                    mediaSession.isActive = true
                    
                    // Aktualizace MediaSession s výchozími metadaty
                    updateMediaMetadata("", "")
                    
                    // Aktualizace notifikace
                    updateNotification()
                    broadcastPlaybackState()
                    
                    RadioWidgetProvider.updateWidgets(applicationContext, true, radio.id)
                    
                    Log.d("RadioService", "Rádio úspěšně spuštěno: ${radio.name}")
                } catch (e: Exception) {
                    Log.e("RadioService", "Chyba při spouštění rádia na hlavním vlákně: ${e.message}", e)
                    stopPlayback()
                }
            }
        } catch (e: Exception) {
            Log.e("RadioService", "Chyba při spouštění rádia: ${e.message}", e)
            stopPlayback()
        }
    }

    private fun pausePlayback() {
        Log.d("RadioService", "Pozastavuji přehrávání")
        serviceScope.launch(Dispatchers.Main.immediate) {
            try {
                exoPlayer.pause()
                _isPlaying.value = false
                updatePlaybackState()
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
                exoPlayer.release()
                
                // Reinicializace ExoPlayeru pro další použití
                exoPlayer = ExoPlayer.Builder(this@RadioService)
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
                
                // Nastavení posluchačů
                setupPlayer()
                
                // Aktualizace stavů
                _isPlaying.value = false
                _currentRadio.value = null
                _currentMetadata.value = null
                
                // Deaktivace MediaSession
                mediaSession.isActive = false
                
                // Odeslání broadcastu o změně stavu
                broadcastPlaybackState()
                
                // Uvolnění WakeLock
                if (wakeLock.isHeld) {
                    wakeLock.release()
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
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Ovládání přehrávání internetového rádia"
            setShowBadge(false)
        }
        notificationManager.createNotificationChannel(channel)
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
            
            // Uvolnění ExoPlayeru
            exoPlayer.release()
            
            // Uvolnění MediaSession
            mediaSession.release()
            
            // Uvolnění WakeLock
            if (wakeLock.isHeld) {
                wakeLock.release()
            }
            
            // Odregistrace receiveru
            unregisterReceiver(batteryReceiver)
            
            // Zrušení coroutine scope
            serviceJob.cancel()
            
            // Uvolnění ImageLoaderu
            imageLoader.shutdown()
            
            Log.d("RadioService", "Zdroje úspěšně uvolněny")
        } catch (e: Exception) {
            Log.e("RadioService", "Chyba při uvolňování zdrojů: ${e.message}")
        }
    }
} 
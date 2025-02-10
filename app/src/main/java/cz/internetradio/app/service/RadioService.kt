package cz.internetradio.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
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

class RadioService : Service() {

    private lateinit var exoPlayer: ExoPlayer
    private lateinit var radioRepository: RadioRepository
    private lateinit var database: RadioDatabase
    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var wakeLock: PowerManager.WakeLock

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private val _currentRadio = MutableStateFlow<Radio?>(null)
    val currentRadio: StateFlow<Radio?> = _currentRadio

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _currentMetadata = MutableStateFlow<String?>(null)
    val currentMetadata: StateFlow<String?> = _currentMetadata

    private var bufferSize = 2000 // Výchozí velikost bufferu v ms
    
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

    companion object {
        const val ACTION_PLAY = "cz.internetradio.app.action.PLAY"
        const val ACTION_PAUSE = "cz.internetradio.app.action.PAUSE"
        const val ACTION_NEXT = "cz.internetradio.app.action.NEXT"
        const val ACTION_PREVIOUS = "cz.internetradio.app.action.PREVIOUS"
        const val EXTRA_RADIO_ID = "radio_id"
        private const val NOTIFICATION_CHANNEL_ID = "radio_channel"
        private const val NOTIFICATION_ID = 1
        private const val WAKELOCK_TAG = "RadioService::WakeLock"
    }

    override fun onCreate() {
        super.onCreate()
        
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
        
        // Optimalizovaný ExoPlayer
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
        
        // Inicializace Room databáze
        database = Room.databaseBuilder(
            applicationContext,
            RadioDatabase::class.java,
            "radio_database"
        ).build()
        
        // Inicializace repository s RadioDao
        radioRepository = RadioRepository(database.radioDao())
        
        // Inicializace MediaSession
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
            })
        }
        
        setupPlayer()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        
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
                _isPlaying.value = isPlaying
                updatePlaybackState()
                // Aktualizace widgetu
                RadioWidgetProvider.updateWidgets(
                    applicationContext,
                    isPlaying,
                    _currentRadio.value?.id
                )
            }

            override fun onMediaMetadataChanged(mediaMetadata: androidx.media3.common.MediaMetadata) {
                val title = mediaMetadata.title?.toString()
                val artist = mediaMetadata.artist?.toString()
                
                val metadata = when {
                    !title.isNullOrBlank() && !artist.isNullOrBlank() -> "$artist - $title"
                    !title.isNullOrBlank() -> title
                    !artist.isNullOrBlank() -> artist
                    else -> null
                }
                
                _currentMetadata.value = metadata
                updateMediaMetadata(artist, title)
            }
        })
    }

    private fun updatePlaybackState() {
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
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
            )
            .build()

        mediaSession.setPlaybackState(playbackState)
    }

    private fun updateMediaMetadata(artist: String?, title: String?) {
        val radio = _currentRadio.value ?: return
        
        val metadataBuilder = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist ?: radio.name)
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title ?: radio.description)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, radio.name)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST, radio.name)
            .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, title ?: radio.name)
            .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, artist ?: radio.description)

        mediaSession.setMetadata(metadataBuilder.build())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> {
                val radioId = intent.getStringExtra(EXTRA_RADIO_ID)
                if (radioId != null) {
                    serviceScope.launch {
                        val radio = radioRepository.getRadioById(radioId)
                        radio?.let { playRadio(it) }
                    }
                }
            }
            ACTION_PAUSE -> pausePlayback()
            ACTION_NEXT -> playNextRadio()
            ACTION_PREVIOUS -> playPreviousRadio()
        }
        return START_STICKY
    }

    private fun playRadio(radio: Radio) {
        if (!wakeLock.isHeld) {
            wakeLock.acquire(24 * 60 * 60 * 1000L) // 24 hodin maximum
        }
        
        _currentRadio.value = radio
        exoPlayer.setMediaItem(MediaItem.fromUri(radio.streamUrl))
        exoPlayer.prepare()
        exoPlayer.play()
        _isPlaying.value = true
        mediaSession.isActive = true
        updateMediaMetadata(null, radio.name)
        
        // Aktualizace notifikace a widgetu
        startForeground(NOTIFICATION_ID, createNotification())
        RadioWidgetProvider.updateWidgets(applicationContext, true, radio.id)
    }

    private fun pausePlayback() {
        exoPlayer.pause()
        _isPlaying.value = false
        updatePlaybackState()
        
        if (wakeLock.isHeld) {
            wakeLock.release()
        }
        
        // Aktualizace notifikace a widgetu
        startForeground(NOTIFICATION_ID, createNotification())
        RadioWidgetProvider.updateWidgets(
            applicationContext,
            false,
            _currentRadio.value?.id
        )
    }

    private fun playNextRadio() {
        serviceScope.launch {
            val favoriteRadios = radioRepository.getFavoriteRadios().first()
            val currentIndex = favoriteRadios.indexOfFirst { it.id == _currentRadio.value?.id }
            if (currentIndex < favoriteRadios.size - 1) {
                playRadio(favoriteRadios[currentIndex + 1])
            }
        }
    }

    private fun playPreviousRadio() {
        serviceScope.launch {
            val favoriteRadios = radioRepository.getFavoriteRadios().first()
            val currentIndex = favoriteRadios.indexOfFirst { it.id == _currentRadio.value?.id }
            if (currentIndex > 0) {
                playRadio(favoriteRadios[currentIndex - 1])
            }
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Radio Playback",
            NotificationManager.IMPORTANCE_LOW
        )
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification() = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
        .setContentTitle("Internet Radio")
        .setContentText("Přehrávání rádia")
        .setSmallIcon(R.drawable.ic_radio_default)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setStyle(androidx.media.app.NotificationCompat.MediaStyle()
            .setMediaSession(mediaSession.sessionToken))
        .build()

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
        unregisterReceiver(batteryReceiver)
        if (wakeLock.isHeld) {
            wakeLock.release()
        }
        exoPlayer.release()
        mediaSession.release()
        serviceJob.cancel()
    }
} 
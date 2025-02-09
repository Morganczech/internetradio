package cz.internetradio.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
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

class RadioService : Service() {

    private lateinit var exoPlayer: ExoPlayer
    private lateinit var radioRepository: RadioRepository
    private lateinit var database: RadioDatabase

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private val _currentRadio = MutableStateFlow<Radio?>(null)
    val currentRadio: StateFlow<Radio?> = _currentRadio

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _currentMetadata = MutableStateFlow<String?>(null)
    val currentMetadata: StateFlow<String?> = _currentMetadata

    companion object {
        const val ACTION_PLAY = "cz.internetradio.app.action.PLAY"
        const val ACTION_PAUSE = "cz.internetradio.app.action.PAUSE"
        const val ACTION_NEXT = "cz.internetradio.app.action.NEXT"
        const val ACTION_PREVIOUS = "cz.internetradio.app.action.PREVIOUS"
        const val EXTRA_RADIO_ID = "radio_id"
        private const val NOTIFICATION_CHANNEL_ID = "radio_channel"
        private const val NOTIFICATION_ID = 1
    }

    override fun onCreate() {
        super.onCreate()
        exoPlayer = ExoPlayer.Builder(this).build()
        
        // Inicializace Room databáze
        database = Room.databaseBuilder(
            applicationContext,
            RadioDatabase::class.java,
            "radio_database"
        ).build()
        
        // Inicializace repository s RadioDao
        radioRepository = RadioRepository(database.radioDao())
        
        setupPlayer()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
    }

    private fun setupPlayer() {
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
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
            }
        })
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
        _currentRadio.value = radio
        exoPlayer.setMediaItem(MediaItem.fromUri(radio.streamUrl))
        exoPlayer.prepare()
        exoPlayer.play()
        _isPlaying.value = true
        // Aktualizace widgetu
        RadioWidgetProvider.updateWidgets(applicationContext, true, radio.id)
    }

    private fun pausePlayback() {
        exoPlayer.pause()
        _isPlaying.value = false
        // Aktualizace widgetu
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
        .build()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        exoPlayer.release()
        serviceJob.cancel()
    }
} 
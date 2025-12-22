package cz.internetradio.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaStyleNotificationHelper
import coil.ImageLoader
import coil.request.ImageRequest
import cz.internetradio.app.MainActivity
import cz.internetradio.app.R
import cz.internetradio.app.model.Radio

class RadioNotificationManager(
    private val context: Context,
    private val mediaSession: MediaSession
) {
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val imageLoader = ImageLoader.Builder(context).crossfade(true).build()

    companion object {
        private const val TAG = "RadioNotificationMgr"
        const val NOTIFICATION_ID = 1
        const val DISCONNECTED_NOTIFICATION_ID = 2
        private const val CHANNEL_ID = "radio_channel"
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Přehrávání rádia",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Ovládání přehrávání internetového rádia"
            setShowBadge(false)
            setSound(null, null)
            enableVibration(false)
        }
        notificationManager.createNotificationChannel(channel)
    }

    fun getInitialNotification(): android.app.Notification {
        return createNotificationBuilder(null, false, null, false).build()
    }

    fun updateNotification(
        radio: Radio?,
        isPlaying: Boolean,
        metadata: String?,
        isLoading: Boolean = false
    ) {
        val builder = createNotificationBuilder(radio, isPlaying, metadata, isLoading)
        
        radio?.imageUrl?.let { imageUrl ->
            val request = ImageRequest.Builder(context)
                .data(imageUrl)
                .target { drawable ->
                    val bitmap = (drawable as? BitmapDrawable)?.bitmap
                    bitmap?.let {
                        builder.setLargeIcon(it)
                        notificationManager.notify(NOTIFICATION_ID, builder.build())
                    }
                }
                .build()
            imageLoader.enqueue(request)
        }

        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }

    fun updateNotificationWithInfo(radio: Radio?, info: String) {
        if (radio == null) return
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(radio.name)
            .setContentText(info)
            .setSmallIcon(R.drawable.ic_radio_default)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
        
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    fun showAudioOutputDisconnectedNotification() {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Audio výstup odpojen")
            .setContentText("Přehrávání bylo pozastaveno - připojte reproduktor")
            .setSmallIcon(R.drawable.ic_radio_default)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setTimeoutAfter(5000)
            .build()
        
        notificationManager.notify(DISCONNECTED_NOTIFICATION_ID, notification)
    }

    fun cancelNotification(id: Int) {
        notificationManager.cancel(id)
    }

    private fun createNotificationBuilder(
        radio: Radio?,
        isPlaying: Boolean,
        metadata: String?,
        isLoading: Boolean
    ): NotificationCompat.Builder {
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context, 0, contentIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val (artist, title) = if (metadata?.contains(" - ") == true) {
            val parts = metadata.split(" - ", limit = 2)
            parts[0].trim() to parts[1].trim()
        } else {
            null to metadata
        }

        val displayTitle = radio?.name ?: "Internet Radio"
        val displayText = if (isLoading) "Načítám..." else when {
            !artist.isNullOrBlank() && !title.isNullOrBlank() -> "$artist - $title"
            !title.isNullOrBlank() -> title
            !artist.isNullOrBlank() -> artist
            else -> "Připraveno k přehrávání"
        }

        val mediaStyle = MediaStyleNotificationHelper.MediaStyle(mediaSession)
            .setShowActionsInCompactView(0, 1, 2)

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(displayTitle)
            .setContentText(displayText)
            .setSubText(if (isLoading) null else title)
            .setStyle(mediaStyle)
            .setSmallIcon(if (isPlaying) R.drawable.ic_notification_play else R.drawable.ic_pause)
            .setContentIntent(contentPendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true) // Vždy ongoing pro stabilitu Foreground služby
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .apply {
                addAction(createAction(RadioService.ACTION_PREVIOUS, R.drawable.ic_skip_previous, "Předchozí", 1))
                addAction(
                    createAction(
                        if (isPlaying) RadioService.ACTION_PAUSE else RadioService.ACTION_PLAY,
                        if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play,
                        if (isPlaying) "Pozastavit" else "Přehrát",
                        0
                    )
                )
                addAction(createAction(RadioService.ACTION_NEXT, R.drawable.ic_skip_next, "Další", 2))
                addAction(createAction(RadioService.ACTION_STOP, R.drawable.ic_notification_close, "Ukončit", 3))
            }
    }

    private fun createAction(action: String, icon: Int, title: String, requestCode: Int): NotificationCompat.Action {
        val intent = Intent(context, RadioService::class.java).apply { this.action = action }
        val pendingIntent = PendingIntent.getService(
            context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Action(icon, title, pendingIntent)
    }
}

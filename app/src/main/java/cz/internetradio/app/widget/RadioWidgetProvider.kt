package cz.internetradio.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.widget.RemoteViews
import androidx.room.Room
import coil.Coil
import coil.request.ImageRequest
import coil.transform.CircleCropTransformation
import cz.internetradio.app.MainActivity
import cz.internetradio.app.R
import cz.internetradio.app.data.RadioDatabase
import cz.internetradio.app.model.Radio
import cz.internetradio.app.repository.RadioRepository
import cz.internetradio.app.service.RadioService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class RadioWidgetProvider : AppWidgetProvider() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + job)
    private lateinit var radioRepository: RadioRepository

    companion object {
        private var currentRadioId: String? = null
        private var isPlaying = false

        fun updateWidgets(context: Context, playing: Boolean, radioId: String?) {
            isPlaying = playing
            currentRadioId = radioId
            
            val intent = Intent(context, RadioWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            }
            val widgetIds = AppWidgetManager.getInstance(context)
                .getAppWidgetIds(ComponentName(context, RadioWidgetProvider::class.java))
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds)
            context.sendBroadcast(intent)
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // Inicializace repository
        val database = Room.databaseBuilder(
            context.applicationContext,
            RadioDatabase::class.java,
            "radio_database"
        ).build()
        radioRepository = RadioRepository(database.radioDao())

        scope.launch {
            val favoriteRadios = radioRepository.getFavoriteRadios().first()
            val currentRadio = currentRadioId?.let { radioRepository.getRadioById(it) } ?: favoriteRadios.firstOrNull()
            
            appWidgetIds.forEach { appWidgetId ->
                updateAppWidget(context, appWidgetManager, appWidgetId, currentRadio)
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        when (intent.action) {
            AppWidgetManager.ACTION_APPWIDGET_UPDATE -> {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(ComponentName(context, javaClass))
                onUpdate(context, appWidgetManager, appWidgetIds)
            }
            RadioService.ACTION_PLAY,
            RadioService.ACTION_PAUSE,
            RadioService.ACTION_NEXT,
            RadioService.ACTION_PREVIOUS -> {
                // Spuštění služby s příslušnou akcí
                val serviceIntent = Intent(context, RadioService::class.java).apply {
                    action = intent.action
                    putExtra(RadioService.EXTRA_RADIO_ID, currentRadioId)
                }
                context.startForegroundService(serviceIntent)
            }
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        radio: Radio?
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_player)

        // Nastavení kliknutí na celý widget pro otevření aplikace
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_radio_icon, pendingIntent)

        // Nastavení tlačítek pro ovládání
        setButtonClickListeners(context, views)

        // Aktualizace vzhledu
        radio?.let {
            views.setTextViewText(R.id.widget_radio_name, it.name)
            views.setImageViewResource(R.id.widget_radio_icon, R.drawable.ic_radio_default)
            
            // Načtení obrázku pomocí Coil
            val request = ImageRequest.Builder(context)
                .data(it.imageUrl)
                .target { drawable ->
                    if (drawable is BitmapDrawable) {
                        views.setImageViewBitmap(R.id.widget_radio_icon, drawable.bitmap)
                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    }
                }
                .build()
            Coil.imageLoader(context).enqueue(request)
        }

        // Nastavení ikon podle stavu přehrávání
        views.setImageViewResource(
            R.id.widget_play_pause,
            if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow
        )

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun setButtonClickListeners(context: Context, views: RemoteViews) {
        // Přehrát/Pozastavit
        val playPauseIntent = Intent(context, RadioWidgetProvider::class.java).apply {
            action = if (isPlaying) RadioService.ACTION_PAUSE else RadioService.ACTION_PLAY
        }
        val playPausePendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            playPauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_play_pause, playPausePendingIntent)

        // Předchozí stanice
        val previousIntent = Intent(context, RadioWidgetProvider::class.java).apply {
            action = RadioService.ACTION_PREVIOUS
        }
        val previousPendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            previousIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_previous, previousPendingIntent)

        // Další stanice
        val nextIntent = Intent(context, RadioWidgetProvider::class.java).apply {
            action = RadioService.ACTION_NEXT
        }
        val nextPendingIntent = PendingIntent.getBroadcast(
            context,
            2,
            nextIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_next, nextPendingIntent)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        job.cancel()
    }
} 
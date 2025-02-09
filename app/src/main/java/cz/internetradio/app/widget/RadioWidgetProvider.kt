package cz.internetradio.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import coil.Coil
import coil.request.ImageRequest
import cz.internetradio.app.MainActivity
import cz.internetradio.app.R
import cz.internetradio.app.service.RadioService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class RadioWidgetProvider : AppWidgetProvider() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + job)

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { appWidgetId ->
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        when (intent.action) {
            RadioService.ACTION_PLAY,
            RadioService.ACTION_PAUSE,
            RadioService.ACTION_NEXT,
            RadioService.ACTION_PREVIOUS -> {
                context.startService(Intent(context, RadioService::class.java).apply {
                    action = intent.action
                    putExtra(RadioService.EXTRA_RADIO_ID, intent.getStringExtra(RadioService.EXTRA_RADIO_ID))
                })
            }
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
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

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun setButtonClickListeners(context: Context, views: RemoteViews) {
        // Přehrát/Pozastavit
        val playPauseIntent = Intent(context, RadioWidgetProvider::class.java).apply {
            action = RadioService.ACTION_PLAY
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
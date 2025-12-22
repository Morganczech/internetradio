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
import cz.internetradio.app.api.RadioBrowserApi
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.EntryPointAccessors
import android.util.Log

class RadioWidgetProvider : AppWidgetProvider() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface RadioWidgetEntryPoint {
        fun getRadioBrowserApi(): RadioBrowserApi
    }

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + job)
    private lateinit var radioRepository: RadioRepository

    companion object {
        private var currentRadioId: String? = null
        private var isPlaying = false
        private var isInitialized = false // Přidáno pro kontrolu inicializace

        fun updateWidgets(context: Context, playing: Boolean, radioId: String?) {
            Log.d("RadioWidgetProvider", "updateWidgets volán: playing=$playing, radioId=$radioId, isInitialized=$isInitialized")
            
            // Aktualizujeme widgety pouze pokud jsou již inicializovány
            if (isInitialized) {
                isPlaying = playing
                currentRadioId = radioId
                
                val intent = Intent(context, RadioWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                }
                val widgetIds = AppWidgetManager.getInstance(context)
                    .getAppWidgetIds(ComponentName(context, RadioWidgetProvider::class.java))
                Log.d("RadioWidgetProvider", "Nalezeno ${widgetIds.size} widgetů pro aktualizaci: ${widgetIds.joinToString()}")
                
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds)
                context.sendBroadcast(intent)
                Log.d("RadioWidgetProvider", "Broadcast odeslán pro aktualizaci widgetů")
            } else {
                Log.d("RadioWidgetProvider", "Widgety nejsou inicializovány, aktualizace přeskočena")
            }
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        Log.d("RadioWidgetProvider", "onUpdate volán pro ${appWidgetIds.size} widgetů: ${appWidgetIds.joinToString()}")
        
        // Získání závislostí přes EntryPoint
        Log.d("RadioWidgetProvider", "Získávám závislosti přes EntryPoint")
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            RadioWidgetEntryPoint::class.java
        )
        
        // Inicializace repository
        Log.d("RadioWidgetProvider", "Inicializuji repository")
        val database = Room.databaseBuilder(
            context.applicationContext,
            RadioDatabase::class.java,
            "radio_database"
        ).build()
        radioRepository = RadioRepository(database.radioDao(), entryPoint.getRadioBrowserApi())
        Log.d("RadioWidgetProvider", "Repository úspěšně inicializován")

        // Nejdříve zobrazíme widget s výchozími hodnotami
        Log.d("RadioWidgetProvider", "Zobrazuji widgety s výchozími hodnotami")
        appWidgetIds.forEach { appWidgetId ->
            Log.d("RadioWidgetProvider", "Aktualizuji widget ID: $appWidgetId s výchozími hodnotami")
            updateAppWidget(context, appWidgetManager, appWidgetId, null)
        }

        scope.launch {
            try {
                Log.d("RadioWidgetProvider", "Načítám data pro widgety...")
                val favoriteRadios = radioRepository.getFavoriteRadios().first()
                val currentRadio = currentRadioId?.let { radioRepository.getRadioById(it) } ?: favoriteRadios.firstOrNull()
                
                Log.d("RadioWidgetProvider", "Data načtena: currentRadio=${currentRadio?.name}, favoriteRadios=${favoriteRadios.size}")
                
                Log.d("RadioWidgetProvider", "Aktualizuji widgety s načtenými daty")
                appWidgetIds.forEach { appWidgetId ->
                    Log.d("RadioWidgetProvider", "Aktualizuji widget ID: $appWidgetId s daty: ${currentRadio?.name ?: "null"}")
                    updateAppWidget(context, appWidgetManager, appWidgetId, currentRadio)
                }
                
                // Označíme widgety jako inicializované
                isInitialized = true
                Log.d("RadioWidgetProvider", "Widgety úspěšně inicializovány")
                
            } catch (e: Exception) {
                Log.e("RadioWidgetProvider", "Chyba při inicializaci widgetů: ${e.message}")
                e.printStackTrace()
                // I při chybě označíme jako inicializované, aby se aplikace nezasekla
                isInitialized = true
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        Log.d("RadioWidgetProvider", "onReceive volán s akcí: ${intent.action}")

        when (intent.action) {
            AppWidgetManager.ACTION_APPWIDGET_UPDATE -> {
                Log.d("RadioWidgetProvider", "Přijata akce APPWIDGET_UPDATE")
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(ComponentName(context, javaClass))
                Log.d("RadioWidgetProvider", "Nalezeno ${appWidgetIds.size} widgetů pro aktualizaci: ${appWidgetIds.joinToString()}")
                onUpdate(context, appWidgetManager, appWidgetIds)
            }
            AppWidgetManager.ACTION_APPWIDGET_ENABLED -> {
                Log.d("RadioWidgetProvider", "Widget povolen")
            }
            AppWidgetManager.ACTION_APPWIDGET_DISABLED -> {
                Log.d("RadioWidgetProvider", "Widget zakázán")
            }
            RadioService.ACTION_PLAY,
            RadioService.ACTION_PAUSE,
            RadioService.ACTION_NEXT,
            RadioService.ACTION_PREVIOUS -> {
                // Spouštíme službu pouze pokud uživatel klikl na tlačítko
                Log.d("RadioWidgetProvider", "Uživatel klikl na tlačítko: ${intent.action}")
                val serviceIntent = Intent(context, RadioService::class.java).apply {
                    action = intent.action
                    putExtra(RadioService.EXTRA_RADIO_ID, currentRadioId)
                }
                Log.d("RadioWidgetProvider", "Spouštím službu s akcí: ${intent.action}")
                context.startForegroundService(serviceIntent)
            }
            else -> {
                Log.d("RadioWidgetProvider", "Neznámá akce: ${intent.action}")
            }
        }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        Log.d("RadioWidgetProvider", "Widget povolen - první widget přidán na plochu")
        isInitialized = true
        
        // Získáme všechny widgety a aktualizujeme je
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(ComponentName(context, javaClass))
        Log.d("RadioWidgetProvider", "onEnabled: Nalezeno ${appWidgetIds.size} widgetů: ${appWidgetIds.joinToString()}")
        
        if (appWidgetIds.isNotEmpty()) {
            Log.d("RadioWidgetProvider", "onEnabled: Spouštím onUpdate pro nalezené widgety")
            onUpdate(context, appWidgetManager, appWidgetIds)
        } else {
            Log.d("RadioWidgetProvider", "onEnabled: Žádné widgety nenalezeny")
        }
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        Log.d("RadioWidgetProvider", "Widget zakázán - poslední widget odebrán z plochy")
        isInitialized = false
        job.cancel()
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        radio: Radio?
    ) {
        Log.d("RadioWidgetProvider", "updateAppWidget volán pro ID: $appWidgetId, radio: ${radio?.name ?: "null"}")
        
        val views = RemoteViews(context.packageName, R.layout.widget_player)

        // Nastavení kliknutí na celý widget pro otevření aplikace
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_radio_icon, pendingIntent)
        Log.d("RadioWidgetProvider", "Click listener pro ikonu rádia nastaven")

        // Nastavení tlačítek pro ovládání
        setButtonClickListeners(context, views)

        // Aktualizace vzhledu - pouze pokud máme rádio
        if (radio != null) {
            Log.d("RadioWidgetProvider", "Aktualizuji widget s rádiem: ${radio.name}")
            views.setTextViewText(R.id.widget_radio_name, radio.name)
            views.setImageViewResource(R.id.widget_radio_icon, R.drawable.ic_radio_default)
            
            // Načtení obrázku pomocí Coil
            val request = ImageRequest.Builder(context)
                .data(radio.imageUrl)
                .target { drawable ->
                    if (drawable is BitmapDrawable) {
                        views.setImageViewBitmap(R.id.widget_radio_icon, drawable.bitmap)
                        appWidgetManager.updateAppWidget(appWidgetId, views)
                        Log.d("RadioWidgetProvider", "Obrázek rádia načten a nastaven")
                    }
                }
                .build()
            Coil.imageLoader(context).enqueue(request)
        } else {
            // Výchozí stav - žádné rádio
            Log.d("RadioWidgetProvider", "Aktualizuji widget s výchozími hodnotami")
            views.setTextViewText(R.id.widget_radio_name, context.getString(R.string.widget_radio_name))
            views.setImageViewResource(R.id.widget_radio_icon, R.drawable.ic_radio_default)
        }

        // Nastavení metadat (pokud jsou dostupná)
        val metadata = getCurrentMetadata(context)
        if (!metadata.isNullOrBlank()) {
            views.setTextViewText(R.id.widget_metadata, metadata)
            Log.d("RadioWidgetProvider", "Metadata nastavena: $metadata")
        } else {
            views.setTextViewText(R.id.widget_metadata, context.getString(R.string.widget_metadata))
            Log.d("RadioWidgetProvider", "Metadata nastavena na výchozí hodnotu")
        }

        // Nastavení ikon podle stavu přehrávání
        val playPauseIcon = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow
        views.setImageViewResource(R.id.widget_play_pause, playPauseIcon)
        Log.d("RadioWidgetProvider", "Ikona play/pause nastavena: ${if (isPlaying) "pause" else "play"}")

        appWidgetManager.updateAppWidget(appWidgetId, views)
        Log.d("RadioWidgetProvider", "Widget aktualizován pro ID: $appWidgetId")
        
        // Kontrola, zda se widget správně zobrazuje
        val widgetInfo = appWidgetManager.getAppWidgetInfo(appWidgetId)
        Log.d("RadioWidgetProvider", "Widget info pro ID $appWidgetId:")
        Log.d("RadioWidgetProvider", "  - MinWidth: ${widgetInfo.minWidth}")
        Log.d("RadioWidgetProvider", "  - MinHeight: ${widgetInfo.minHeight}")
        Log.d("RadioWidgetProvider", "  - Layout: ${widgetInfo.initialLayout}")
        Log.d("RadioWidgetProvider", "  - Provider: ${widgetInfo.provider.className}")
    }

    private fun getCurrentMetadata(context: Context): String? {
        // Zde byste měli implementovat získání aktuálních metadat
        // Prozatím vracíme null, aby se zobrazila výchozí hodnota
        Log.d("RadioWidgetProvider", "getCurrentMetadata volán - vracím null")
        return null
    }

    private fun setButtonClickListeners(context: Context, views: RemoteViews) {
        Log.d("RadioWidgetProvider", "Nastavuji click listenery pro tlačítka")
        
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
        Log.d("RadioWidgetProvider", "Play/Pause tlačítko nastaveno")

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
        Log.d("RadioWidgetProvider", "Předchozí tlačítko nastaveno")

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
        Log.d("RadioWidgetProvider", "Další tlačítko nastaveno")
        
        Log.d("RadioWidgetProvider", "Všechna tlačítka úspěšně nastavena")
    }
}

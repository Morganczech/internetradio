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

open class RadioWidgetProvider : AppWidgetProvider() {

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
        private var cachedBitmap: Bitmap? = null
        private var cachedRadioId: String? = null

        fun updateWidgets(context: Context, playing: Boolean, radioId: String?, metadata: String? = null) {
            
            if (isInitialized) {
                isPlaying = playing
                currentRadioId = radioId
                
                context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
                    .edit()
                    .putString("current_metadata", metadata)
                    .apply()
                
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val widgetClasses = listOf(
                    RadioWidgetProvider::class.java,
                    CompactWidgetProvider::class.java,
                    ControlWidgetProvider::class.java,
                    MediumWidgetProvider::class.java,
                    LargeWidgetProvider::class.java
                )

                widgetClasses.forEach { cls ->
                    val ids = appWidgetManager.getAppWidgetIds(ComponentName(context, cls))
                    if (ids.isNotEmpty()) {
                        val intent = Intent(context, cls).apply {
                            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                        }
                        context.sendBroadcast(intent)
                    }
                }
            } else {
            }
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        
        // Získání závislostí přes EntryPoint
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            RadioWidgetEntryPoint::class.java
        )
        
        // Inicializace repository
        val database = Room.databaseBuilder(
            context.applicationContext,
            RadioDatabase::class.java,
            "radio_database"
        ).build()
        radioRepository = RadioRepository(database.radioDao(), entryPoint.getRadioBrowserApi())

        scope.launch {
            try {
                val favoriteRadios = radioRepository.getFavoriteRadios().first()
                val currentRadio = currentRadioId?.let { radioRepository.getRadioById(it) } ?: favoriteRadios.firstOrNull()
                
                appWidgetIds.forEach { appWidgetId ->
                    updateAppWidget(context, appWidgetManager, appWidgetId, currentRadio)
                }
                
                // Označíme widgety jako inicializované
                isInitialized = true
                
            } catch (e: Exception) {
                if (cz.internetradio.app.BuildConfig.DEBUG) Log.e("RadioWidgetProvider", "Chyba při inicializaci widgetů: ${e.message}")
                e.printStackTrace()
                // I při chybě označíme jako inicializované, aby se aplikace nezasekla
                isInitialized = true
            }
        }
    }

    open fun getLayoutId(): Int = R.layout.widget_player

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        when (intent.action) {
            AppWidgetManager.ACTION_APPWIDGET_UPDATE -> {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(ComponentName(context, javaClass))
                onUpdate(context, appWidgetManager, appWidgetIds)
            }
            AppWidgetManager.ACTION_APPWIDGET_ENABLED -> {
            }
            AppWidgetManager.ACTION_APPWIDGET_DISABLED -> {
            }
            RadioService.ACTION_PLAY,
            RadioService.ACTION_PAUSE,
            RadioService.ACTION_NEXT,
            RadioService.ACTION_PREVIOUS -> {
                // Spouštíme službu pouze pokud uživatel klikl na tlačítko
                val serviceIntent = Intent(context, RadioService::class.java).apply {
                    action = intent.action
                    putExtra(RadioService.EXTRA_RADIO_ID, currentRadioId)
                }
                context.startForegroundService(serviceIntent)
            }
            else -> {
            }
        }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        isInitialized = true
        
        // Získáme všechny widgety a aktualizujeme je
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(ComponentName(context, javaClass))
        
        if (appWidgetIds.isNotEmpty()) {
            onUpdate(context, appWidgetManager, appWidgetIds)
        } else {
        }
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
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
        
        val views = RemoteViews(context.packageName, getLayoutId())

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

        // Aktualizace vzhledu - pouze pokud máme rádio
        if (radio != null) {
            views.setTextViewText(R.id.widget_radio_name, radio.name)
            
            // 1. VŽDY zobrazíme nejdříve to, co máme v paměti (i když je to stará ikona),
            // abychom zabránili probliknutí na placeholder.
            // Placeholder použijeme jen při úplně prvním načtení.
            if (cachedBitmap != null) {
                views.setImageViewBitmap(R.id.widget_radio_icon, cachedBitmap)
            } else {
                views.setImageViewResource(R.id.widget_radio_icon, R.drawable.ic_radio_default)
            }

            // 2. Pokud se změnilo ID (nebo cache chybí), spustíme načítání
            if (radio.id != cachedRadioId || cachedBitmap == null) {
                val request = ImageRequest.Builder(context)
                    .data(radio.imageUrl)
                    .target(
                        onSuccess = { result ->
                            // V callbacku ověříme, zda je tato odpověď stále aktuální
                            // (zda uživatel mezitím nepřepnul dál)
                            // Použijeme 'currentRadioId' z Companion, který reprezentuje nejnovější požadavek
                            if (radio.id == currentRadioId) {
                                val bitmap = (result as BitmapDrawable).bitmap
                                cachedBitmap = bitmap
                                cachedRadioId = radio.id
                                
                                // Vyvoláme bezpečný update widgetu
                                try {
                                    val provider = appWidgetManager.getAppWidgetInfo(appWidgetId).provider
                                    val intent = Intent().apply {
                                        component = provider
                                        action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(appWidgetId))
                                    }
                                    context.sendBroadcast(intent)
                                } catch (e: Exception) {
                                    views.setImageViewBitmap(R.id.widget_radio_icon, bitmap)
                                    appWidgetManager.updateAppWidget(appWidgetId, views)
                                }
                            }
                        },
                        onError = {
                             // Při chybě neděláme nic - necháme zobrazenou starou ikonu nebo placeholder
                             // To je lepší než probliknutí
                        }
                    )
                    .build()
                Coil.imageLoader(context).enqueue(request)
            }
        } else {
            // Výchozí stav - žádné rádio
            views.setTextViewText(R.id.widget_radio_name, context.getString(R.string.widget_radio_name))
            views.setImageViewResource(R.id.widget_radio_icon, R.drawable.ic_radio_default)
        }

        // Nastavení metadat (pokud jsou dostupná)
        val metadata = getCurrentMetadata(context)
        if (!metadata.isNullOrBlank()) {
            views.setTextViewText(R.id.widget_metadata, metadata)
        } else {
            views.setTextViewText(R.id.widget_metadata, context.getString(R.string.widget_metadata))
        }

        // Nastavení ikon podle stavu přehrávání
        val playPauseIcon = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow
        views.setImageViewResource(R.id.widget_play_pause, playPauseIcon)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun getCurrentMetadata(context: Context): String? {
        val prefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
        return prefs.getString("current_metadata", null)
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
}

package cz.internetradio.app

import android.os.Bundle
import android.content.IntentFilter
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.Context
import android.os.BatteryManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import coil.compose.AsyncImage
import cz.internetradio.app.model.Radio
import cz.internetradio.app.navigation.Screen
import cz.internetradio.app.screens.*
import cz.internetradio.app.viewmodel.RadioViewModel
import cz.internetradio.app.screens.AddRadioViewModel
import dagger.hilt.android.AndroidEntryPoint
import android.view.WindowManager
import android.view.View
import androidx.activity.viewModels
import androidx.compose.animation.*
import cz.internetradio.app.components.AudioVisualizer
import androidx.compose.ui.draw.alpha
import android.util.Log
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import java.util.Locale
import android.content.res.Configuration
import cz.internetradio.app.model.Language
import androidx.lifecycle.lifecycleScope

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: RadioViewModel by viewModels()
    
    private val powerConnectionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_POWER_CONNECTED -> {
                    Log.d("MainActivity", "Napájení připojeno")
                }
                Intent.ACTION_POWER_DISCONNECTED -> {
                    Log.d("MainActivity", "Napájení odpojeno")
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Nastavení jazyka
        lifecycleScope.launch {
            viewModel.currentLanguage.collect { language ->
                updateLocale(language)
            }
        }
        
        // Registrace přijímače pro sledování stavu nabíjení
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
        registerReceiver(powerConnectionReceiver, filter)
        
        // Nastavení tmavé systémové lišty
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN

        // Skrytí systémových pruhů při dotyku
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)

        setContent {
            MaterialTheme(
                colors = darkColors(),
                content = {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colors.background
                    ) {
                        val navController = rememberNavController()
                        
                        NavHost(
                            navController = navController,
                            startDestination = Screen.AllStations.route
                        ) {
                            composable(Screen.Favorites.route) {
                                FavoritesScreen(
                                    viewModel = viewModel,
                                    onNavigateToAllStations = {
                                        navController.navigate(Screen.AllStations.route)
                                    },
                                    onNavigateToSettings = {
                                        navController.navigate(Screen.Settings.route)
                                    },
                                    onNavigateToFavoriteSongs = {
                                        navController.navigate(Screen.FavoriteSongs.route)
                                    },
                                    onNavigateToBrowseStations = {
                                        navController.navigate(Screen.BrowseStations.route)
                                    },
                                    onNavigateToPopularStations = {
                                        navController.navigate(Screen.PopularStations.route)
                                    },
                                    onNavigateToAddRadio = { 
                                        navController.navigate(Screen.AddRadio.route) 
                                    },
                                    onNavigateToEdit = { radioId ->
                                        navController.navigate(Screen.EditRadio.createRoute(radioId))
                                    }
                                )
                            }
                            
                            composable(Screen.AllStations.route) {
                                AllStationsScreen(
                                    viewModel = viewModel,
                                    onNavigateToSettings = {
                                        navController.navigate(Screen.Settings.route)
                                    },
                                    onNavigateToBrowseStations = {
                                        navController.navigate(Screen.BrowseStations.route)
                                    },
                                    onNavigateToPopularStations = {
                                        navController.navigate(Screen.PopularStations.route)
                                    },
                                    onNavigateToAddRadio = { 
                                        navController.navigate(Screen.AddRadio.route) 
                                    },
                                    onNavigateToEdit = { radioId ->
                                        navController.navigate(Screen.EditRadio.createRoute(radioId))
                                    },
                                    onNavigateToFavoriteSongs = {
                                        navController.navigate(Screen.FavoriteSongs.route)
                                    }
                                )
                            }
                            
                            composable(Screen.BrowseStations.route) {
                                BrowseStationsScreen(
                                    viewModel = viewModel,
                                    onNavigateBack = {
                                        navController.popBackStack()
                                    },
                                    onNavigateToFavoriteSongs = {
                                        navController.navigate(Screen.FavoriteSongs.route)
                                    }
                                )
                            }
                            
                            composable(Screen.PopularStations.route) {
                                PopularStationsScreen(
                                    viewModel = viewModel,
                                    onNavigateBack = { navController.popBackStack() },
                                    onNavigateToEdit = { radioId ->
                                        navController.navigate(Screen.EditRadio.createRoute(radioId))
                                    },
                                    onNavigateToFavoriteSongs = {
                                        navController.navigate(Screen.FavoriteSongs.route)
                                    }
                                )
                            }
                            
                            composable(Screen.Settings.route) {
                                SettingsScreen(
                                    viewModel = viewModel,
                                    onNavigateBack = { navController.popBackStack() },
                                    onNavigateToEqualizer = { navController.navigate(Screen.Equalizer.route) },
                                    onNavigateToAddRadio = { navController.navigate(Screen.AddRadio.route) }
                                )
                            }
                            
                            composable(Screen.Equalizer.route) {
                                EqualizerScreen(
                                    viewModel = viewModel,
                                    onNavigateBack = {
                                        navController.popBackStack()
                                    }
                                )
                            }
                            
                            composable(Screen.FavoriteSongs.route) {
                                FavoriteSongsScreen(
                                    viewModel = viewModel,
                                    onNavigateBack = {
                                        navController.popBackStack()
                                    }
                                )
                            }
                            
                            composable(Screen.AddRadio.route) {
                                AddRadioScreen(
                                    viewModel = hiltViewModel<AddRadioViewModel>(),
                                    onNavigateBack = {
                                        navController.popBackStack()
                                    }
                                )
                            }
                            
                            composable(
                                route = Screen.EditRadio.route,
                                arguments = listOf(navArgument("radioId") { type = NavType.StringType })
                            ) { backStackEntry ->
                                val radioId = backStackEntry.arguments?.getString("radioId") ?: return@composable
                                AddRadioScreen(
                                    viewModel = hiltViewModel<AddRadioViewModel>(),
                                    onNavigateBack = { navController.popBackStack() },
                                    radioToEdit = radioId
                                )
                            }
                        }
                    }
                }
            )
        }
    }
    
    private fun updateLocale(language: Language) {
        val locale = when (language) {
            Language.SYSTEM -> resources.configuration.locales[0]
            else -> Locale(language.code)
        }
        
        val config = Configuration(resources.configuration).apply {
            setLocale(locale)
        }
        
        resources.updateConfiguration(config, resources.displayMetrics)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(powerConnectionReceiver)
    }
}

@Composable
fun RadioItem(
    radio: Radio,
    isSelected: Boolean,
    onRadioClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    isCustomStation: Boolean = false
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onRadioClick),
        elevation = if (isSelected) 8.dp else 2.dp,
        backgroundColor = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(radio.startColor, radio.endColor)
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .padding(end = 16.dp)
                    ) {
                        AsyncImage(
                            model = radio.imageUrl,
                            contentDescription = stringResource(R.string.radio_logo_description, radio.name),
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit,
                            fallback = painterResource(id = R.drawable.ic_radio_default),
                            error = painterResource(id = R.drawable.ic_radio_default)
                        )
                    }
                    Column {
                        Text(
                            text = radio.name,
                            style = MaterialTheme.typography.h6,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = Color.White
                        )
                        radio.description?.let { description ->
                            Text(
                                text = if (description.isBlank()) stringResource(R.string.radio_default_description) 
                                      else if (description.length > 50) description.take(50) + "..." 
                                      else description,
                                style = MaterialTheme.typography.body2,
                                modifier = Modifier.padding(top = 4.dp),
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        } ?: Text(
                            text = stringResource(R.string.radio_default_description),
                            style = MaterialTheme.typography.body2,
                            modifier = Modifier.padding(top = 4.dp),
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onFavoriteClick) {
                        Icon(
                            imageVector = if (radio.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (radio.isFavorite) "Odebrat z oblíbených" else "Přidat do oblíbených",
                            tint = Color.White
                        )
                    }
                    
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Více možností",
                            tint = Color.White
                        )
                    }
                    
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            onClick = {
                                showMenu = false
                                onEditClick()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Upravit stanici")
                        }
                        DropdownMenuItem(
                            onClick = {
                                showMenu = false
                                onDeleteClick()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Smazat stanici")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlayerControls(
    radio: Radio,
    viewModel: RadioViewModel,
    onNavigateToFavoriteSongs: () -> Unit
) {
    val volume by viewModel.volume.collectAsState()
    val sleepTimer by viewModel.sleepTimerMinutes.collectAsState()
    val remainingMinutes by viewModel.remainingTimeMinutes.collectAsState()
    val remainingSeconds by viewModel.remainingTimeSeconds.collectAsState()
    val currentMetadata by viewModel.currentMetadata.collectAsState()
    val currentRadio by viewModel.currentRadio.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    var showTimerDropdown by remember { mutableStateOf(false) }
    var isExpanded by remember { mutableStateOf(false) }
    val favoriteRadios by viewModel.getFavoriteRadios().collectAsState(initial = emptyList())

    // Použijeme aktuální rádio ze stavu místo parametru
    val displayedRadio = currentRadio ?: radio

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clickable { isExpanded = !isExpanded },
        elevation = 8.dp
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(displayedRadio.startColor, displayedRadio.endColor)
                    )
                )
        ) {
            AudioVisualizer(
                modifier = Modifier
                    .matchParentSize()
                    .alpha(0.2f),
                baseColor1 = Color.White,
                baseColor2 = Color.White.copy(alpha = 0.5f),
                isPlaying = isPlaying
            )

            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Základní informace
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = displayedRadio.name,
                            style = MaterialTheme.typography.h6,
                            color = Color.White
                        )
                        // Zobrazení kvality streamu
                        displayedRadio.bitrate?.let { bitrate ->
                            Text(
                                text = "$bitrate kbps",
                                style = MaterialTheme.typography.caption,
                                color = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                        currentMetadata?.let { metadata ->
                            Text(
                                text = metadata,
                                style = MaterialTheme.typography.body2,
                                color = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!isExpanded) {
                            IconButton(onClick = { viewModel.togglePlayPause() }) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Pozastavit" else "Přehrát",
                                    tint = Color.White
                                )
                            }
                        }
                        IconButton(onClick = { isExpanded = !isExpanded }) {
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (isExpanded) "Sbalit" else "Rozbalit",
                                tint = Color.White
                            )
                        }
                    }
                }

                // Rozšířená verze
                if (isExpanded) {
                    Column(
                        modifier = Modifier.padding(top = 16.dp)
                    ) {
                        // Ovládání hlasitosti
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeDown,
                                contentDescription = "Snížit hlasitost",
                                tint = Color.White
                            )
                            
                            Slider(
                                value = volume,
                                onValueChange = { viewModel.setVolume(it) },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp),
                                valueRange = 0f..1f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color.White,
                                    activeTrackColor = Color.White,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                                )
                            )
                            
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = "Zvýšit hlasitost",
                                tint = Color.White
                            )
                        }
                        
                        // Ovládací tlačítka přehrávání
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { viewModel.playPreviousStation() }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipPrevious,
                                    contentDescription = "Předchozí stanice",
                                    tint = Color.White
                                )
                            }

                            IconButton(
                                onClick = { viewModel.togglePlayPause() }
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Pozastavit" else "Přehrát",
                                    tint = Color.White
                                )
                            }

                            IconButton(
                                onClick = { viewModel.playNextStation() }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipNext,
                                    contentDescription = "Další stanice",
                                    tint = Color.White
                                )
                            }
                        }

                        // Řádek s ikonami pro správu skladeb a časovač
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            if (currentMetadata != null) {
                                IconButton(
                                    onClick = { viewModel.saveSongToFavorites() }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlaylistAdd,
                                        contentDescription = "Uložit skladbu",
                                        tint = Color.White
                                    )
                                }
                            }
                            
                            IconButton(
                                onClick = onNavigateToFavoriteSongs
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QueueMusic,
                                    contentDescription = "Zobrazit oblíbené skladby",
                                    tint = Color.White
                                )
                            }

                            IconButton(
                                onClick = { showTimerDropdown = true }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = "Časovač vypnutí",
                                    tint = Color.White
                                )
                            }
                        }
                        
                        DropdownMenu(
                            expanded = showTimerDropdown,
                            onDismissRequest = { showTimerDropdown = false }
                        ) {
                            DropdownMenuItem(onClick = {
                                viewModel.setSleepTimer(0)
                                showTimerDropdown = false
                            }) {
                                Text(stringResource(R.string.sleep_timer_off))
                            }
                            (5..60 step 5).forEach { minutes ->
                                DropdownMenuItem(onClick = {
                                    viewModel.setSleepTimer(minutes)
                                    showTimerDropdown = false
                                }) {
                                    Text(stringResource(R.string.sleep_timer_minutes, minutes))
                                }
                            }
                        }

                        // Zobrazení zprávy o uložení skladby
                        val songSavedMessage by viewModel.showSongSavedMessage.collectAsState()
                        val scope = rememberCoroutineScope()
                        
                        songSavedMessage?.let { message ->
                            Text(
                                text = message,
                                style = MaterialTheme.typography.caption,
                                color = Color.White,
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .align(Alignment.CenterHorizontally)
                            )
                            LaunchedEffect(message) {
                                scope.launch {
                                    delay(2000)
                                    viewModel.dismissSongSavedMessage()
                                }
                            }
                        }

                        // Zobrazení zbývajícího času časovače
                        val minutes = remainingMinutes ?: 0
                        val seconds = remainingSeconds ?: 0
                        if (minutes > 0 || seconds > 0) {
                            Text(
                                text = stringResource(R.string.sleep_timer_remaining, minutes, String.format("%02d", seconds)),
                                style = MaterialTheme.typography.caption,
                                modifier = Modifier.padding(top = 4.dp),
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
} 
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
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import cz.internetradio.app.navigation.Screen
import cz.internetradio.app.screens.*
import cz.internetradio.app.viewmodel.RadioViewModel
import cz.internetradio.app.viewmodel.AddRadioViewModel
import dagger.hilt.android.AndroidEntryPoint
import android.view.WindowManager
import android.view.View
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.ui.draw.alpha
import android.util.Log
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import java.util.Locale
import android.content.res.Configuration
import android.content.res.Resources
import cz.internetradio.app.model.Language
import androidx.compose.foundation.isSystemInDarkTheme
import android.os.Build
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import cz.internetradio.app.model.Radio
import cz.internetradio.app.model.RadioCategory
import cz.internetradio.app.components.AudioVisualizer

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: RadioViewModel by viewModels()

    private val powerConnectionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_POWER_CONNECTED -> Log.d("MainActivity", "Napájení připojeno")
                Intent.ACTION_POWER_DISCONNECTED -> Log.d("MainActivity", "Napájení odpojeno")
            }
        }
    }

    override fun attachBaseContext(newBase: Context) {
        val prefs = newBase.getSharedPreferences("radio_prefs", Context.MODE_PRIVATE)
        val languageCode = prefs.getString("language", "system")
        val locale = if (languageCode == "system") {
            Resources.getSystem().configuration.locales[0]
        } else {
            Locale(languageCode!!)
        }
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        val context = newBase.createConfigurationContext(config)
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)

        registerReceiver(powerConnectionReceiver, IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        })

        setContent {
            val currentLanguage by viewModel.currentLanguage.collectAsState()
            val songSavedMessage by viewModel.showSongSavedMessage.collectAsState()
            val scaffoldState = rememberScaffoldState()

            LaunchedEffect(currentLanguage) {
                updateLocale(currentLanguage)
            }

            LaunchedEffect(songSavedMessage) {
                songSavedMessage?.let {
                    scaffoldState.snackbarHostState.showSnackbar(it)
                    viewModel.dismissSongSavedMessage()
                }
            }

            val prefs = remember { getSharedPreferences("radio_prefs", Context.MODE_PRIVATE) }
            var showWelcomeDialog by remember { 
                mutableStateOf(!prefs.getBoolean("welcome_shown", false)) 
            }
            
            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                Log.d("MainActivity", "Notification permission granted: $isGranted")
            }

            if (showWelcomeDialog) {
                WelcomeDialog(
                    onDismiss = {
                        showWelcomeDialog = false
                        prefs.edit().putBoolean("welcome_shown", true).apply()
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                )
            }

            MaterialTheme(
                colors = if (isSystemInDarkTheme()) {
                    darkColors(
                        primary = Color(0xFF9C27B0),
                        primaryVariant = Color(0xFF7B1FA2),
                        secondary = Color(0xFFE040FB),
                        background = Color(0xFF121212),
                        surface = Color(0xFF1E1E1E),
                        error = Color(0xFFCF6679)
                    )
                } else {
                    lightColors(
                        primary = Color(0xFF9C27B0),
                        primaryVariant = Color(0xFF7B1FA2),
                        secondary = Color(0xFFE040FB),
                        background = Color.White,
                        surface = Color(0xFFF5F5F5),
                        error = Color(0xFFB00020)
                    )
                },
                content = {
                    Scaffold(
                        scaffoldState = scaffoldState
                    ) { padding ->
                        Surface(
                            modifier = Modifier.fillMaxSize().padding(padding),
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
                                        onNavigateToAllStations = { navController.navigate(Screen.AllStations.route) },
                                        onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                                        onNavigateToFavoriteSongs = { navController.navigate(Screen.FavoriteSongs.route) },
                                        onNavigateToBrowseStations = { navController.navigate(Screen.BrowseStations.route) },
                                        onNavigateToPopularStations = { navController.navigate(Screen.PopularStations.route) },
                                        onNavigateToAddRadio = { navController.navigate(Screen.AddRadio.route) },
                                        onNavigateToEdit = { radioId -> navController.navigate(Screen.EditRadio.createRoute(radioId)) }
                                    )
                                }
                                
                                composable(Screen.AllStations.route) {
                                    AllStationsScreen(
                                        viewModel = viewModel,
                                        onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                                        onNavigateToBrowseStations = { navController.navigate(Screen.BrowseStations.route) },
                                        onNavigateToPopularStations = { navController.navigate(Screen.PopularStations.route) },
                                        onNavigateToAddRadio = { navController.navigate(Screen.AddRadio.route) },
                                        onNavigateToEdit = { radioId -> navController.navigate(Screen.EditRadio.createRoute(radioId)) },
                                        onNavigateToFavoriteSongs = { navController.navigate(Screen.FavoriteSongs.route) }
                                    )
                                }
                                
                                composable(Screen.BrowseStations.route) {
                                    BrowseStationsScreen(
                                        viewModel = viewModel,
                                        onNavigateBack = { navController.popBackStack() },
                                        onNavigateToFavoriteSongs = { navController.navigate(Screen.FavoriteSongs.route) }
                                    )
                                }
                                
                                composable(Screen.PopularStations.route) {
                                    PopularStationsScreen(
                                        viewModel = viewModel,
                                        onNavigateBack = { navController.popBackStack() },
                                        onNavigateToEdit = { radioId -> navController.navigate(Screen.EditRadio.createRoute(radioId)) },
                                        onNavigateToFavoriteSongs = { navController.navigate(Screen.FavoriteSongs.route) }
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
                }
            )
        }
    }
    
    private fun updateLocale(language: Language) {
        // Locale is applied in attachBaseContext logic mostly.
        // This method triggers recreation if needed or updates configuration for runtime changes.
        val locale = when (language) {
            Language.SYSTEM -> Resources.getSystem().configuration.locales[0]
            else -> Locale(language.code)
        }
        
        if (resources.configuration.locales[0].language != locale.language) {
             val config = Configuration(resources.configuration).apply {
                setLocale(locale)
            }
            @Suppress("DEPRECATION")
            resources.updateConfiguration(config, resources.displayMetrics)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(powerConnectionReceiver)
    }
}

@Composable
fun WelcomeDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = { },
        title = { 
            Text(
                text = stringResource(R.string.welcome_title),
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Bold
            ) 
        },
        text = { 
            Text(
                text = stringResource(R.string.welcome_message),
                style = MaterialTheme.typography.body1
            ) 
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(stringResource(R.string.action_continue))
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun RadioItem(
    radio: Radio,
    isSelected: Boolean,
    onRadioClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false,
    useUnifiedColor: Boolean = false
) {
    var showMenu by remember { mutableStateOf(false) }

    val gradientColors = if (useUnifiedColor) {
        cz.internetradio.app.ui.theme.Gradients.UNIFIED_COLOR_PAIR
    } else {
        Pair(radio.startColor, radio.endColor)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = if (isCompact) 4.dp else 8.dp)
            .clickable(onClick = onRadioClick),
        elevation = if (isSelected) 8.dp else 2.dp,
        backgroundColor = Color.Transparent
    ) {
        Box(
            modifier = Modifier.background(
                brush = Brush.horizontalGradient(listOf(gradientColors.first, gradientColors.second))
            )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(if (isCompact) 8.dp else 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(if (isCompact) 40.dp else 64.dp).padding(end = 16.dp)) {
                        AsyncImage(
                            model = radio.imageUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit,
                            fallback = painterResource(id = R.drawable.ic_radio_default),
                            error = painterResource(id = R.drawable.ic_radio_default)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = radio.name, style = if (isCompact) MaterialTheme.typography.subtitle1 else MaterialTheme.typography.h6, color = Color.White)
                        if (!isCompact) {
                            Text(
                                text = radio.description,
                                style = MaterialTheme.typography.body2,
                                color = Color.White.copy(alpha = 0.7f),
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onFavoriteClick) {
                        Icon(imageVector = if (radio.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, contentDescription = null, tint = Color.White)
                    }
                    IconButton(onClick = { showMenu = true }) {
                        Icon(imageVector = Icons.Default.MoreVert, contentDescription = null, tint = Color.White)
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(onClick = { showMenu = false; onEditClick() }) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colors.onSurface)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.action_edit))
                        }
                        DropdownMenuItem(onClick = { showMenu = false; onDeleteClick() }) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colors.onSurface)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.action_delete))
                        }
                    }
                }
            }
        }
    }
}




@OptIn(ExperimentalAnimationApi::class)
@Composable
fun PlayerControls(
    radio: Radio,
    viewModel: RadioViewModel,
    onNavigateToFavoriteSongs: () -> Unit,
    onNavigateToCategory: (RadioCategory) -> Unit,
    forceMiniPlayer: Boolean = false
) {
    val volume by viewModel.volume.collectAsState()
    val remainingMinutes by viewModel.remainingTimeMinutes.collectAsState()
    val remainingSeconds by viewModel.remainingTimeSeconds.collectAsState()
    val currentMetadata by viewModel.currentMetadata.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val playbackContext by viewModel.playbackContext.collectAsState()
    val useUnifiedAccentColor by viewModel.useUnifiedAccentColor.collectAsState()
    var isExpanded by remember { mutableStateOf(false) }
    
    // Ensure isExpanded is always false if forceMiniPlayer is true
    LaunchedEffect(forceMiniPlayer) {
        if (forceMiniPlayer) {
            isExpanded = false
        }
    }

    var showTimerDropdown by remember { mutableStateOf(false) }

    val visualGradient = if (useUnifiedAccentColor) {
        cz.internetradio.app.ui.theme.Gradients.UNIFIED_COLOR_PAIR
    } else if (playbackContext == RadioCategory.VSE) {
        cz.internetradio.app.ui.theme.Gradients.getGradientForCategory(RadioCategory.VSE)
    } else {
        Pair(radio.startColor, radio.endColor)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            // Removed padding to make it full width
            .animateContentSize()
            .clickable(enabled = !isExpanded && !forceMiniPlayer) { isExpanded = true },
        elevation = if (isExpanded) 12.dp else 4.dp,
        shape = androidx.compose.ui.graphics.RectangleShape
    ) {
        Box(modifier = Modifier.background(brush = Brush.verticalGradient(listOf(visualGradient.first, visualGradient.second)))) {
            cz.internetradio.app.components.AudioVisualizer(
                modifier = Modifier.matchParentSize().alpha(0.2f),
                baseColor1 = Color.White,
                baseColor2 = Color.White.copy(alpha = 0.5f),
                isPlaying = isPlaying
            )
            
            Column(modifier = Modifier.padding(16.dp)) {
                
                // --- LAYER 1: INFO & MINI-CONTROLS ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Info Section
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = radio.name,
                            style = MaterialTheme.typography.h6,
                            color = Color.White
                        )
                        val metadataText = currentMetadata ?: "No data"
                        val metadataAlpha = if (currentMetadata != null) 0.7f else 0.5f

                        Text(
                            text = metadataText,
                            style = MaterialTheme.typography.body2,
                            color = Color.White.copy(alpha = metadataAlpha),
                            maxLines = 2,
                            minLines = 2,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        
                        // Chips (Visible only in Expanded)
                        AnimatedVisibility(visible = isExpanded) {
                            Row(
                                modifier = Modifier.padding(top = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    backgroundColor = Color.White.copy(alpha = 0.15f),
                                    elevation = 0.dp
                                ) {
                                    Text(
                                        text = stringResource(radio.category.getTitleRes()),
                                        style = MaterialTheme.typography.caption,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        color = Color.White
                                    )
                                }
                                radio.bitrate?.let { bitrate ->
                                    Card(
                                        shape = RoundedCornerShape(12.dp),
                                        backgroundColor = Color.White.copy(alpha = 0.15f),
                                        elevation = 0.dp
                                    ) {
                                        Text(
                                            text = stringResource(R.string.player_bitrate_format, bitrate),
                                            style = MaterialTheme.typography.caption,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Mini Controls (Hidden when Expanded)
                    if (!isExpanded) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { viewModel.togglePlayPause() }) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                            }
                            if (!forceMiniPlayer) {
                                IconButton(onClick = { isExpanded = true }) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowUp,
                                        contentDescription = "Rozbalit",
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                // --- EXPANDED CONTENT ---
                AnimatedVisibility(visible = isExpanded && !forceMiniPlayer) {
                    Column {
                        Spacer(modifier = Modifier.height(32.dp))

                        // --- LAYER 2: CORE CONTROLS (Prev / Play / Next) ---
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { viewModel.playPreviousStation() },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipPrevious,
                                    contentDescription = "Předchozí",
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            // Big Play/Pause
                            IconButton(
                                onClick = { viewModel.togglePlayPause() },
                                modifier = Modifier
                                    .size(72.dp)
                                    .background(Color.White.copy(alpha = 0.2f), androidx.compose.foundation.shape.CircleShape)
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Pauza" else "Přehrát",
                                    tint = Color.White,
                                    modifier = Modifier.size(48.dp)
                                )
                            }

                            IconButton(
                                onClick = { viewModel.playNextStation() },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipNext,
                                    contentDescription = "Další",
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        // --- LAYER 3: SECONDARY CONTROLS ---
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Sleep Timer
                            Box {
                                IconButton(onClick = { showTimerDropdown = true }) {
                                    val tint = if (remainingMinutes != null) MaterialTheme.colors.secondary else Color.White.copy(alpha = 0.7f)
                                    Icon(Icons.Default.Timer, "Časovač", tint = tint)
                                }

                                val menuBackgroundColor = visualGradient.first

                                MaterialTheme(
                                    colors = MaterialTheme.colors.copy(surface = menuBackgroundColor),
                                    shapes = MaterialTheme.shapes.copy(medium = RoundedCornerShape(12.dp))
                                ) {
                                    DropdownMenu(
                                        expanded = showTimerDropdown,
                                        onDismissRequest = { showTimerDropdown = false }
                                    ) {
                                        DropdownMenuItem(onClick = { viewModel.setSleepTimer(null); showTimerDropdown = false }) {
                                            Text(stringResource(R.string.sleep_timer_off), color = Color.White)
                                        }
                                        listOf(5, 15, 30, 45, 60).forEach { m ->
                                            DropdownMenuItem(onClick = { viewModel.setSleepTimer(m); showTimerDropdown = false }) {
                                                Text(stringResource(R.string.sleep_timer_minutes, m), color = Color.White)
                                            }
                                        }
                                    }
                                }
                            }

                            // Volume
                            Box {
                                var showVolume by remember { mutableStateOf(false) }
                                IconButton(onClick = { showVolume = true }) {
                                    Icon(Icons.Default.VolumeUp, "Hlasitost", tint = Color.White.copy(alpha = 0.7f))
                                }

                                val volumeMenuColor = visualGradient.first.copy(alpha = 0.95f)
                                
                                MaterialTheme(
                                    colors = MaterialTheme.colors.copy(surface = volumeMenuColor),
                                    shapes = MaterialTheme.shapes.copy(medium = RoundedCornerShape(12.dp))
                                ) {
                                    DropdownMenu(
                                        expanded = showVolume,
                                        onDismissRequest = { showVolume = false }
                                    ) {
                                        Box(modifier = Modifier.size(width = 200.dp, height = 50.dp).padding(horizontal = 16.dp)) {
                                            Slider(
                                                value = volume,
                                                onValueChange = { viewModel.setVolume(it) },
                                                modifier = Modifier.align(Alignment.Center),
                                                colors = SliderDefaults.colors(
                                                    thumbColor = Color.White,
                                                    activeTrackColor = Color.White,
                                                    inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                                                )
                                            )
                                        }
                                    }
                                }
                            }

                            // Save Song
                            IconButton(
                                onClick = { if (currentMetadata != null) viewModel.saveSongToFavorites() },
                                enabled = currentMetadata != null
                            ) {
                                Icon(
                                    Icons.Default.PlaylistAdd,
                                    "Uložit skladbu",
                                    tint = if (currentMetadata != null) Color.White.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.3f)
                                )
                            }

                            // Favorite Songs List
                            IconButton(onClick = onNavigateToFavoriteSongs) {
                                Icon(Icons.Default.QueueMusic, "Seznam skladeb", tint = Color.White.copy(alpha = 0.7f))
                            }
                        }

                        // Countdown display
                        if ((remainingMinutes ?: 0) > 0 || (remainingSeconds ?: 0) > 0) {
                            Text(
                                text = stringResource(R.string.player_time_remaining, remainingMinutes ?: 0, remainingSeconds ?: 0),
                                style = MaterialTheme.typography.caption,
                                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 16.dp),
                                color = MaterialTheme.colors.secondary
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Collapse Button
                        IconButton(
                            onClick = { isExpanded = false },
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Sbalit",
                                tint = Color.White.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }
}

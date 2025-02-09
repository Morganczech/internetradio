package cz.internetradio.app

import android.os.Bundle
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
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import coil.compose.AsyncImage
import cz.internetradio.app.model.Radio
import cz.internetradio.app.navigation.Screen
import cz.internetradio.app.screens.AllStationsScreen
import cz.internetradio.app.screens.FavoritesScreen
import cz.internetradio.app.viewmodel.RadioViewModel
import dagger.hilt.android.AndroidEntryPoint
import android.view.WindowManager
import android.view.View
import androidx.activity.viewModels

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: RadioViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Nastavení tmavé systémové lišty
        window.statusBarColor = Color.Black.toArgb()
        window.navigationBarColor = Color.Black.toArgb()
        
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }

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
                            startDestination = Screen.Favorites.route
                        ) {
                            composable(Screen.Favorites.route) {
                                FavoritesScreen(
                                    viewModel = viewModel,
                                    onNavigateToAllStations = {
                                        navController.navigate(Screen.AllStations.route)
                                    }
                                )
                            }
                            composable(Screen.AllStations.route) {
                                AllStationsScreen(
                                    viewModel = viewModel,
                                    onNavigateBack = {
                                        navController.popBackStack()
                                    }
                                )
                            }
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun RadioItem(
    radio: Radio,
    isSelected: Boolean,
    onRadioClick: () -> Unit,
    onFavoriteClick: () -> Unit
) {
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
                            contentDescription = "Logo ${radio.name}",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
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
                                text = description,
                                style = MaterialTheme.typography.body2,
                                modifier = Modifier.padding(top = 4.dp),
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
                
                IconButton(onClick = onFavoriteClick) {
                    Icon(
                        imageVector = if (radio.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (radio.isFavorite) "Odebrat z oblíbených" else "Přidat do oblíbených",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun PlayerControls(
    radio: Radio,
    isPlaying: Boolean,
    onPlayPauseClick: () -> Unit,
    viewModel: RadioViewModel
) {
    val volume by viewModel.volume.collectAsState()
    val sleepTimer by viewModel.sleepTimerMinutes.collectAsState()
    val remainingTime by viewModel.remainingTimeMinutes.collectAsState()
    val currentMetadata by viewModel.currentMetadata.collectAsState()
    var showTimerDropdown by remember { mutableStateOf(false) }
    val favoriteRadios by viewModel.getFavoriteRadios().collectAsState(initial = emptyList())

    val timerOptions = (5..60 step 5).toList()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = 8.dp
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(radio.startColor, radio.endColor)
                    )
                )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Název stanice
                Text(
                    text = "Nyní hraje: ${radio.name}",
                    style = MaterialTheme.typography.subtitle1,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp),
                    color = Color.White
                )
                
                // Metadata (název písně)
                currentMetadata?.let { metadata ->
                    Text(
                        text = metadata,
                        style = MaterialTheme.typography.body2,
                        modifier = Modifier.padding(bottom = 16.dp),
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }

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
                        modifier = Modifier.size(24.dp),
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
                        modifier = Modifier.size(24.dp),
                        tint = Color.White
                    )
                }
                
                // Ovládací tlačítka
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Tlačítko předchozí
                    IconButton(
                        onClick = {
                            val currentIndex = favoriteRadios.indexOfFirst { it.id == radio.id }
                            if (currentIndex > 0) {
                                viewModel.playRadio(favoriteRadios[currentIndex - 1])
                            }
                        },
                        enabled = favoriteRadios.indexOfFirst { it.id == radio.id } > 0
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Předchozí stanice",
                            tint = Color.White
                        )
                    }

                    // Tlačítko play/pause
                    IconButton(
                        onClick = onPlayPauseClick,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pozastavit" else "Přehrát",
                            modifier = Modifier.size(32.dp),
                            tint = Color.White
                        )
                    }

                    // Tlačítko další
                    IconButton(
                        onClick = {
                            val currentIndex = favoriteRadios.indexOfFirst { it.id == radio.id }
                            if (currentIndex < favoriteRadios.size - 1) {
                                viewModel.playRadio(favoriteRadios[currentIndex + 1])
                            }
                        },
                        enabled = favoriteRadios.indexOfFirst { it.id == radio.id } < favoriteRadios.size - 1
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Další stanice",
                            tint = Color.White
                        )
                    }
                }

                // Timer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(
                        onClick = { showTimerDropdown = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = "Časovač vypnutí",
                            tint = Color.White
                        )
                    }
                    
                    DropdownMenu(
                        expanded = showTimerDropdown,
                        onDismissRequest = { showTimerDropdown = false }
                    ) {
                        DropdownMenuItem(onClick = {
                            viewModel.setSleepTimer(0)
                            showTimerDropdown = false
                        }) {
                            Text("Vypnout časovač")
                        }
                        timerOptions.forEach { minutes ->
                            DropdownMenuItem(onClick = {
                                viewModel.setSleepTimer(minutes)
                                showTimerDropdown = false
                            }) {
                                Text("$minutes minut")
                            }
                        }
                    }
                }
                
                // Zobrazení zbývajícího času časovače
                remainingTime?.let { time ->
                    if (time > 0) {
                        Text(
                            text = "Zbývá: $time min",
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
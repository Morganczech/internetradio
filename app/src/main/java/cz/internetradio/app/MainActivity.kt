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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Timer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import cz.internetradio.app.model.Radio
import cz.internetradio.app.viewmodel.RadioViewModel
import dagger.hilt.android.AndroidEntryPoint
import android.view.WindowManager
import android.view.View

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
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
                        MainScreen()
                    }
                }
            )
        }
    }
}

@Composable
fun MainScreen(viewModel: RadioViewModel = hiltViewModel()) {
    val currentRadio by viewModel.currentRadio.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Text(
            text = "Internetové Rádio",
            style = MaterialTheme.typography.h5,
            modifier = Modifier.padding(16.dp)
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            items(viewModel.radioStations) { radio ->
                RadioItem(
                    radio = radio,
                    isSelected = radio.id == currentRadio?.id,
                    onRadioClick = { viewModel.playRadio(radio) }
                )
            }
        }

        currentRadio?.let { radio ->
            PlayerControls(
                radio = radio,
                isPlaying = isPlaying,
                onPlayPauseClick = { viewModel.togglePlayPause() }
            )
        }
    }
}

@Composable
fun RadioItem(
    radio: Radio,
    isSelected: Boolean,
    onRadioClick: () -> Unit
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
                modifier = Modifier.padding(16.dp),
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
        }
    }
}

@Composable
fun PlayerControls(
    radio: Radio,
    isPlaying: Boolean,
    onPlayPauseClick: () -> Unit,
    viewModel: RadioViewModel = hiltViewModel()
) {
    val volume by viewModel.volume.collectAsState()
    val sleepTimer by viewModel.sleepTimerMinutes.collectAsState()
    val currentMetadata by viewModel.currentMetadata.collectAsState()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 32.dp),
        elevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Nyní hraje: ${radio.name}",
                style = MaterialTheme.typography.subtitle1,
                fontWeight = FontWeight.Bold
            )
            
            // Zobrazení aktuální skladby
            currentMetadata?.let { metadata ->
                Text(
                    text = "Nyní hraje: $metadata",
                    style = MaterialTheme.typography.body2,
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
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
                    contentDescription = "Snížit hlasitost"
                )
                Slider(
                    value = volume,
                    onValueChange = { viewModel.setVolume(it) },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                )
                Icon(
                    imageVector = Icons.Default.VolumeUp,
                    contentDescription = "Zvýšit hlasitost"
                )
            }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tlačítko přehrát/pozastavit
                IconButton(onClick = onPlayPauseClick) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pozastavit" else "Přehrát"
                    )
                }
                
                // Časovač vypnutí
                IconButton(
                    onClick = {
                        when (sleepTimer) {
                            null -> viewModel.setSleepTimer(30)
                            30 -> viewModel.setSleepTimer(60)
                            60 -> viewModel.setSleepTimer(90)
                            else -> viewModel.setSleepTimer(null)
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = "Časovač vypnutí"
                    )
                }
            }
            
            // Zobrazení časovače
            sleepTimer?.let { minutes ->
                Text(
                    text = "Vypnutí za: $minutes min",
                    style = MaterialTheme.typography.caption,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
} 
package cz.internetradio.app.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cz.internetradio.app.model.EqualizerPreset
import cz.internetradio.app.viewmodel.RadioViewModel
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border

@Composable
fun EqualizerScreen(
    viewModel: RadioViewModel,
    onNavigateBack: () -> Unit
) {
    val equalizerEnabled by viewModel.equalizerEnabled.collectAsState()
    val currentPreset by viewModel.currentPreset.collectAsState()
    val bandValues by viewModel.bandValues.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top App Bar
        TopAppBar(
            title = { Text("Equalizer") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Zpět",
                        tint = Color.White
                    )
                }
            },
            backgroundColor = MaterialTheme.colors.surface,
            elevation = 4.dp
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Zapnutí/vypnutí equalizeru
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Povolit equalizer",
                    style = MaterialTheme.typography.subtitle1,
                    color = Color.White
                )
                Switch(
                    checked = equalizerEnabled,
                    onCheckedChange = { viewModel.setEqualizerEnabled(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colors.primary,
                        checkedTrackColor = MaterialTheme.colors.primary.copy(alpha = 0.5f)
                    )
                )
            }

            if (equalizerEnabled) {
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "Předvolby",
                    style = MaterialTheme.typography.subtitle1,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Předvolby
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(EqualizerPreset.values()) { preset ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (preset == currentPreset) 
                                MaterialTheme.colors.primary 
                            else 
                                Color.Transparent,
                            modifier = Modifier
                                .border(
                                    width = 1.dp,
                                    color = if (preset == currentPreset) 
                                        MaterialTheme.colors.primary 
                                    else 
                                        Color.White.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable { viewModel.setEqualizerPreset(preset) }
                        ) {
                            Text(
                                text = preset.title,
                                color = if (preset == currentPreset) 
                                    Color.White 
                                else 
                                    Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "Nastavení pásem",
                    style = MaterialTheme.typography.subtitle1,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Posuvníky pro jednotlivá pásma
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    bandValues.forEachIndexed { index, value ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "${value.toInt()}dB",
                                style = MaterialTheme.typography.caption,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                            
                            Slider(
                                value = value,
                                onValueChange = { viewModel.setBandValue(index, it) },
                                valueRange = -12f..12f,
                                modifier = Modifier
                                    .height(200.dp)
                                    .width(40.dp),
                                colors = SliderDefaults.colors(
                                    thumbColor = Color.White,
                                    activeTrackColor = MaterialTheme.colors.primary
                                )
                            )
                            
                            Text(
                                text = when (index) {
                                    0 -> "60Hz"
                                    1 -> "230Hz"
                                    2 -> "910Hz"
                                    3 -> "3.6kHz"
                                    else -> "14kHz"
                                },
                                style = MaterialTheme.typography.caption,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }
} 
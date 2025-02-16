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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.stringResource
import cz.internetradio.app.R

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
                    text = stringResource(R.string.equalizer_bands),
                    style = MaterialTheme.typography.subtitle1,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Posuvníky pro jednotlivá pásma
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    bandValues.forEachIndexed { index, value ->
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = when (index) {
                                        0 -> stringResource(R.string.equalizer_band_60hz)
                                        1 -> stringResource(R.string.equalizer_band_230hz)
                                        2 -> stringResource(R.string.equalizer_band_910hz)
                                        3 -> stringResource(R.string.equalizer_band_3_6khz)
                                        else -> stringResource(R.string.equalizer_band_14khz)
                                    },
                                    style = MaterialTheme.typography.caption,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = stringResource(R.string.equalizer_db_format, value.toInt()),
                                    style = MaterialTheme.typography.caption,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }
                            
                            Slider(
                                value = value,
                                onValueChange = { viewModel.setBandValue(index, it) },
                                valueRange = -12f..12f,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                colors = SliderDefaults.colors(
                                    thumbColor = Color.White,
                                    activeTrackColor = MaterialTheme.colors.primary,
                                    inactiveTrackColor = MaterialTheme.colors.primary.copy(alpha = 0.24f)
                                )
                            )
                        }
                    }
                }
            }
        }
    }
} 
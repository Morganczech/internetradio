package cz.internetradio.app.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
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
fun EqualizerControls(
    viewModel: RadioViewModel,
    modifier: Modifier = Modifier
) {
    val equalizerEnabled by viewModel.equalizerEnabled.collectAsState()
    val currentPreset by viewModel.currentPreset.collectAsState()
    val bandValues by viewModel.bandValues.collectAsState()
    
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Zapnutí/vypnutí equalizeru
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Equalizer",
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

        // Předvolby
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
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

        // Posuvníky pro jednotlivá pásma
        if (equalizerEnabled) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                bandValues.forEachIndexed { index, value ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Slider(
                            value = value,
                            onValueChange = { viewModel.setBandValue(index, it) },
                            valueRange = -12f..12f,
                            modifier = Modifier
                                .height(150.dp)
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
                        Text(
                            text = "${value.toInt()}dB",
                            style = MaterialTheme.typography.caption,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
} 
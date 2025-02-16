package cz.internetradio.app.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cz.internetradio.app.model.Radio
import cz.internetradio.app.viewmodel.RadioViewModel

@Composable
fun PlayerScreen(
    radio: Radio,
    viewModel: RadioViewModel,
    metadata: String?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Název stanice
        Text(
            text = radio.name,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        // Zobrazení kvality streamu
        radio.bitrate?.let { bitrate ->
            Text(
                text = "$bitrate kbps",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        
        // Zobrazení metadat
        metadata?.let { meta ->
            Text(
                text = meta,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
        
        // Ovládací prvky přehrávače
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Zde můžete přidat tlačítka pro ovládání přehrávače
            // např. play/pause, předchozí/další stanice, atd.
        }
    }
} 
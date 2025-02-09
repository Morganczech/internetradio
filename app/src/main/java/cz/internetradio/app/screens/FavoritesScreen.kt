package cz.internetradio.app.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cz.internetradio.app.PlayerControls
import cz.internetradio.app.RadioItem
import cz.internetradio.app.viewmodel.RadioViewModel

@Composable
fun FavoritesScreen(
    viewModel: RadioViewModel,
    onNavigateToAllStations: () -> Unit
) {
    val currentRadio by viewModel.currentRadio.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val favoriteRadios by viewModel.getFavoriteRadios().collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Oblíbené stanice",
                style = MaterialTheme.typography.h5
            )
            
            IconButton(onClick = onNavigateToAllStations) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Přidat stanice"
                )
            }
        }

        if (favoriteRadios.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Zatím nemáte žádné oblíbené stanice",
                    style = MaterialTheme.typography.body1
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(favoriteRadios.take(8)) { radio ->
                    RadioItem(
                        radio = radio,
                        isSelected = radio.id == currentRadio?.id,
                        onRadioClick = { viewModel.playRadio(radio) },
                        onFavoriteClick = { 
                            viewModel.toggleFavorite(radio)
                            if (radio.id == currentRadio?.id) {
                                viewModel.stopPlayback()
                            }
                        }
                    )
                }
            }
        }

        currentRadio?.let { radio ->
            PlayerControls(
                radio = radio,
                isPlaying = isPlaying,
                onPlayPauseClick = { viewModel.togglePlayPause() },
                viewModel = viewModel
            )
        }
    }
} 
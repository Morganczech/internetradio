package cz.internetradio.app.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cz.internetradio.app.PlayerControls
import cz.internetradio.app.RadioItem
import cz.internetradio.app.viewmodel.RadioViewModel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically

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
            .navigationBarsPadding()
    ) {
        // Hlavička
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Oblíbené stanice",
                style = MaterialTheme.typography.h5,
                color = Color.White
            )
            
            IconButton(
                onClick = onNavigateToAllStations,
                modifier = Modifier
                    .size(48.dp)
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Přidat stanice",
                    tint = Color.White
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
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = Color.White.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "Zatím nemáte žádné oblíbené stanice",
                        style = MaterialTheme.typography.body1,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                    Button(
                        onClick = onNavigateToAllStations,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = MaterialTheme.colors.primary
                        )
                    ) {
                        Text("Procházet stanice")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(bottom = if (currentRadio != null) 0.dp else 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
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

        // Animovaný přechod pro přehrávač
        AnimatedVisibility(
            visible = currentRadio != null,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
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
} 
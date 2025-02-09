package cz.internetradio.app.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
fun AllStationsScreen(
    viewModel: RadioViewModel,
    onNavigateBack: () -> Unit
) {
    val currentRadio by viewModel.currentRadio.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val allRadios by viewModel.getAllRadios().collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        TopAppBar(
            title = { Text("Všechny stanice") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Zpět"
                    )
                }
            },
            backgroundColor = MaterialTheme.colors.surface,
            elevation = 4.dp
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            items(allRadios) { radio ->
                RadioItem(
                    radio = radio,
                    isSelected = radio.id == currentRadio?.id,
                    onRadioClick = { viewModel.playRadio(radio) },
                    onFavoriteClick = { viewModel.toggleFavorite(radio) }
                )
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
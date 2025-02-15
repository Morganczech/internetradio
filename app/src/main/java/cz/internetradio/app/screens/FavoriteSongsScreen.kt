package cz.internetradio.app.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cz.internetradio.app.viewmodel.RadioViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun FavoriteSongsScreen(
    viewModel: RadioViewModel,
    onNavigateBack: () -> Unit
) {
    val favoriteSongs by viewModel.getAllFavoriteSongs().collectAsState(initial = emptyList())
    var showOptionsMenu by remember { mutableStateOf<Long?>(null) }
    val scaffoldState = rememberScaffoldState()
    val message by viewModel.showSongSavedMessage.collectAsState()
    
    LaunchedEffect(message) {
        message?.let {
            scaffoldState.snackbarHostState.showSnackbar(it)
            viewModel.dismissSongSavedMessage()
        }
    }
    
    Scaffold(
        scaffoldState = scaffoldState,
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Top App Bar
            TopAppBar(
                title = { Text("Oblíbené skladby") },
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

            if (favoriteSongs.isEmpty()) {
                // Prázdný stav
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
                            imageVector = Icons.Default.PlaylistAdd,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = Color.White.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "Zatím nemáte žádné oblíbené skladby",
                            style = MaterialTheme.typography.body1,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
            } else {
                // Seznam skladeb
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(favoriteSongs.sortedByDescending { it.addedAt }) { song ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            backgroundColor = MaterialTheme.colors.surface,
                            elevation = 2.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Informace o skladbě
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 8.dp)
                                ) {
                                    Text(
                                        text = song.title,
                                        style = MaterialTheme.typography.subtitle1,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    song.artist?.let { artist ->
                                        Text(
                                            text = artist,
                                            style = MaterialTheme.typography.body2,
                                            color = Color.White.copy(alpha = 0.7f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                    Text(
                                        text = song.radioName,
                                        style = MaterialTheme.typography.caption,
                                        color = Color.White.copy(alpha = 0.5f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }

                                // Menu s možnostmi
                                Box {
                                    IconButton(onClick = { showOptionsMenu = song.id }) {
                                        Icon(
                                            imageVector = Icons.Default.MoreVert,
                                            contentDescription = "Více možností",
                                            tint = Color.White
                                        )
                                    }

                                    DropdownMenu(
                                        expanded = showOptionsMenu == song.id,
                                        onDismissRequest = { showOptionsMenu = null }
                                    ) {
                                        DropdownMenuItem(onClick = {
                                            viewModel.copyToClipboard(song)
                                            showOptionsMenu = null
                                        }) {
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                contentDescription = null
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Kopírovat do schránky")
                                        }
                                        DropdownMenuItem(onClick = {
                                            viewModel.deleteFavoriteSong(song)
                                            showOptionsMenu = null
                                        }) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = null,
                                                tint = Color.Red
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                "Odstranit",
                                                color = Color.Red
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
} 
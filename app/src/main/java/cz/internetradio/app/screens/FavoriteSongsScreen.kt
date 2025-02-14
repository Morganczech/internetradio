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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.ui.res.painterResource
import cz.internetradio.app.R
import cz.internetradio.app.config.ApiConfig
import java.net.URLEncoder

@Composable
fun FavoriteSongsScreen(
    viewModel: RadioViewModel,
    onNavigateBack: () -> Unit
) {
    val favoriteSongs by viewModel.getAllFavoriteSongs().collectAsState(initial = emptyList())
    var selectedSongs by remember { mutableStateOf(setOf<Long>()) }
    var showSortMenu by remember { mutableStateOf(false) }
    var currentSortOrder by remember { mutableStateOf(SortOrder.DATE_DESC) }
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
            // Top App Bar s akcemi
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
                actions = {
                    // Zobrazit akce pouze pokud jsou vybrané skladby
                    if (selectedSongs.isNotEmpty()) {
                        IconButton(onClick = {
                            viewModel.exportToSpotify(favoriteSongs.filter { selectedSongs.contains(it.id) })
                        }) {
                            Icon(
                                imageVector = Icons.Default.PlaylistAdd,
                                contentDescription = "Exportovat do Spotify",
                                tint = Color.White
                            )
                        }
                        IconButton(onClick = {
                            viewModel.exportToYouTubeMusic(favoriteSongs.filter { selectedSongs.contains(it.id) })
                        }) {
                            Icon(
                                imageVector = Icons.Default.MusicVideo,
                                contentDescription = "Exportovat do YouTube Music",
                                tint = Color.White
                            )
                        }
                        IconButton(onClick = {
                            selectedSongs.forEach { songId ->
                                favoriteSongs.find { it.id == songId }?.let {
                                    viewModel.deleteFavoriteSong(it)
                                }
                            }
                            selectedSongs = emptySet()
                        }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Odstranit vybrané",
                                tint = Color.White
                            )
                        }
                    } else {
                        // Standardní akce když není nic vybráno
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.Sort,
                                contentDescription = "Seřadit",
                                tint = Color.White
                            )
                        }
                        IconButton(onClick = {
                            selectedSongs = favoriteSongs.map { it.id }.toSet()
                        }) {
                            Icon(
                                imageVector = Icons.Default.SelectAll,
                                contentDescription = "Vybrat vše",
                                tint = Color.White
                            )
                        }
                    }
                },
                backgroundColor = MaterialTheme.colors.surface,
                elevation = 4.dp
            )

            // Dropdown menu pro řazení
            DropdownMenu(
                expanded = showSortMenu,
                onDismissRequest = { showSortMenu = false }
            ) {
                SortOrder.values().forEach { sortOrder ->
                    DropdownMenuItem(onClick = {
                        currentSortOrder = sortOrder
                        showSortMenu = false
                    }) {
                        Text(sortOrder.displayName)
                    }
                }
            }

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
                    val sortedSongs = when (currentSortOrder) {
                        SortOrder.DATE_DESC -> favoriteSongs.sortedByDescending { it.addedAt }
                        SortOrder.DATE_ASC -> favoriteSongs.sortedBy { it.addedAt }
                        SortOrder.TITLE -> favoriteSongs.sortedBy { it.title }
                        SortOrder.ARTIST -> favoriteSongs.sortedBy { it.artist ?: "" }
                        SortOrder.RADIO -> favoriteSongs.sortedBy { it.radioName }
                    }

                    items(sortedSongs) { song ->
                        val isSelected = selectedSongs.contains(song.id)
                        
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .toggleable(
                                    value = isSelected,
                                    onValueChange = {
                                        selectedSongs = if (it) {
                                            selectedSongs + song.id
                                        } else {
                                            selectedSongs - song.id
                                        }
                                    }
                                ),
                            backgroundColor = if (isSelected) 
                                MaterialTheme.colors.primary.copy(alpha = 0.2f)
                            else 
                                MaterialTheme.colors.surface,
                            elevation = 2.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Checkbox pro výběr
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { checked ->
                                        selectedSongs = if (checked) {
                                            selectedSongs + song.id
                                        } else {
                                            selectedSongs - song.id
                                        }
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = MaterialTheme.colors.primary,
                                        uncheckedColor = Color.White.copy(alpha = 0.7f)
                                    )
                                )

                                // Obrázek alba (placeholder)
                                var albumArtUrl by remember { mutableStateOf<String?>(null) }
                                LaunchedEffect(song.artist, song.title) {
                                    albumArtUrl = viewModel.getAlbumArtUrl(song.artist, song.title)
                                }
                                AsyncImage(
                                    model = albumArtUrl,
                                    contentDescription = "Album art",
                                    modifier = Modifier
                                        .size(56.dp)
                                        .padding(8.dp),
                                    contentScale = ContentScale.Crop,
                                    error = painterResource(R.drawable.ic_album_placeholder),
                                    placeholder = painterResource(R.drawable.ic_album_placeholder)
                                )

                                // Informace o skladbě
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 8.dp)
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

                                // Menu s třemi tečkami
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
                                            viewModel.exportToSpotify(listOf(song))
                                            showOptionsMenu = null
                                        }) {
                                            Icon(
                                                imageVector = Icons.Default.PlaylistAdd,
                                                contentDescription = null
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Přidat do Spotify")
                                        }
                                        DropdownMenuItem(onClick = {
                                            viewModel.exportToYouTubeMusic(listOf(song))
                                            showOptionsMenu = null
                                        }) {
                                            Icon(
                                                imageVector = Icons.Default.MusicVideo,
                                                contentDescription = null
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Přidat do YouTube Music")
                                        }
                                        DropdownMenuItem(onClick = {
                                            viewModel.playOnYouTube(song)
                                            showOptionsMenu = null
                                        }) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = null
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Přehrát na YouTube")
                                        }
                                        DropdownMenuItem(onClick = {
                                            viewModel.searchLyrics(song)
                                            showOptionsMenu = null
                                        }) {
                                            Icon(
                                                imageVector = Icons.Default.Description,
                                                contentDescription = null
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Vyhledat text písně")
                                        }
                                        DropdownMenuItem(onClick = {
                                            viewModel.shareSong(song)
                                            showOptionsMenu = null
                                        }) {
                                            Icon(
                                                imageVector = Icons.Default.Share,
                                                contentDescription = null
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Sdílet")
                                        }
                                        Divider()
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

enum class SortOrder(val displayName: String) {
    DATE_DESC("Nejnovější"),
    DATE_ASC("Nejstarší"),
    TITLE("Podle názvu"),
    ARTIST("Podle interpreta"),
    RADIO("Podle rádia")
} 
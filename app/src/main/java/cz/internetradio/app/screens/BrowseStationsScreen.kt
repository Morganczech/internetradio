package cz.internetradio.app.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cz.internetradio.app.model.RadioStation
import cz.internetradio.app.viewmodel.RadioViewModel
import cz.internetradio.app.model.RadioCategory
import androidx.compose.foundation.background
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import cz.internetradio.app.PlayerControls

@Composable
fun BrowseStationsScreen(
    viewModel: RadioViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToFavoriteSongs: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var stations by remember { mutableStateOf<List<RadioStation>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<RadioCategory?>(null) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var selectedStation by remember { mutableStateOf<RadioStation?>(null) }
    val currentRadio by viewModel.currentRadio.collectAsState()

    // Načtení místních stanic při zobrazení obrazovky
    LaunchedEffect(Unit) {
        viewModel.refreshLocalStations()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // TopAppBar s tlačítkem zpět
        TopAppBar(
            title = { Text("Vyhledat stanice") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Zpět")
                }
            },
            backgroundColor = MaterialTheme.colors.surface
        )

        // Vyhledávací pole
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { query ->
                searchQuery = query
                if (query.length >= 3) {
                    Log.d("BrowseStationsScreen", "Vyhledávám stanice pro dotaz: $query")
                    isLoading = true
                    viewModel.searchStations(query) { result: List<RadioStation>? ->
                        Log.d("BrowseStationsScreen", "Nalezeno stanic: ${result?.size ?: 0}")
                        stations = result ?: emptyList()
                        isLoading = false
                    }
                } else {
                    stations = emptyList()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            label = { Text("Vyhledat stanici online...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Vyhledat") }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Seznam stanic
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(androidx.compose.ui.Alignment.CenterHorizontally)
            )
        } else if (searchQuery.length < 3) {
            // Zobrazení místních stanic, když není aktivní vyhledávání
            val localStations by viewModel.localStations.collectAsState()
            val countryCode = viewModel.currentCountryCode.collectAsState().value
            
            localStations?.let { stations ->
                if (stations.isNotEmpty()) {
                    Text(
                        text = countryCode?.let { RadioCategory.getLocalizedTitle(it) } ?: "Místní stanice",
                        style = MaterialTheme.typography.h6,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    LazyColumn {
                        items(stations) { station ->
                            StationItem(
                                station = station.apply { 
                                    isFromRadioBrowser = false
                                    category = RadioCategory.MISTNI
                                },
                                onAddToFavorites = {
                                    selectedStation = station
                                    showCategoryDialog = true
                                    selectedCategory = null
                                }
                            )
                        }
                    }
                } else {
                    Text(
                        text = "Zadejte název stanice pro vyhledávání",
                        style = MaterialTheme.typography.body1,
                        modifier = Modifier
                            .padding(16.dp)
                            .align(androidx.compose.ui.Alignment.CenterHorizontally)
                    )
                }
            } ?: run {
                Text(
                    text = "Zadejte název stanice pro vyhledávání",
                    style = MaterialTheme.typography.body1,
                    modifier = Modifier
                        .padding(16.dp)
                        .align(androidx.compose.ui.Alignment.CenterHorizontally)
                )
            }
        } else {
            LazyColumn {
                items(stations) { station ->
                    StationItem(
                        station = station,
                        onAddToFavorites = {
                            selectedStation = station
                            showCategoryDialog = true
                            selectedCategory = null
                        }
                    )
                }
            }
        }

        // Přehrávač
        AnimatedVisibility(
            visible = currentRadio != null,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            currentRadio?.let { radio ->
                PlayerControls(
                    radio = radio,
                    viewModel = viewModel,
                    onNavigateToFavoriteSongs = onNavigateToFavoriteSongs
                )
            }
        }
    }

    // Dialog pro výběr kategorie při přidávání do oblíbených
    if (showCategoryDialog) {
        AlertDialog(
            onDismissRequest = { 
                showCategoryDialog = false
                selectedStation = null
            },
            title = { Text("Vyberte kategorii") },
            text = {
                Column {
                    // Filtrujeme kategorie - odstraníme OSTATNI a VLASTNI
                    RadioCategory.values()
                        .filter { category -> 
                            category != RadioCategory.OSTATNI && 
                            category != RadioCategory.VLASTNI 
                        }
                        .forEach { category ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedCategory == category,
                                    onClick = { selectedCategory = category }
                                )
                                Text(
                                    text = category.title,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        selectedCategory?.let { category ->
                            selectedStation?.let { station ->
                                viewModel.addStationToFavorites(station, category)
                            }
                        }
                        showCategoryDialog = false
                        selectedStation = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = selectedCategory != null
                ) {
                    Text("Přidat do kategorie")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showCategoryDialog = false 
                    selectedStation = null
                }) {
                    Text("Zrušit")
                }
            }
        )
    }
}

@Composable
private fun StationItem(
    station: RadioStation,
    onAddToFavorites: () -> Unit
) {
    val colors = listOf(
        station.category?.startColor ?: Color(0xFF2196F3),
        station.category?.endColor ?: Color(0xFF1976D2)
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        elevation = 4.dp,
        backgroundColor = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.horizontalGradient(colors = colors)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = station.name,
                    style = MaterialTheme.typography.h6,
                    color = Color.White
                )
                if (!station.tags.isNullOrBlank()) {
                    Text(
                        text = station.tags.let { 
                            if (it.length > 50) it.substring(0, 47) + "..." 
                            else it 
                        },
                        style = MaterialTheme.typography.body2,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
                
                // Dočasné zobrazení URL streamu
                Text(
                    text = "Stream URL: ${station.url_resolved ?: station.url}",
                    style = MaterialTheme.typography.caption,
                    color = Color.White.copy(alpha = 0.7f)
                )
                
                if (station.isFromRadioBrowser) {
                    TextButton(
                        onClick = onAddToFavorites,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Přidat do kategorie")
                    }
                }
            }
        }
    }
} 
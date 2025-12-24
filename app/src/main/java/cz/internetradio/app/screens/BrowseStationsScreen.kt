import android.util.Log
import androidx.compose.foundation.layout.*
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.stringResource
import cz.internetradio.app.R
import cz.internetradio.app.ui.theme.Gradients
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.draw.clip
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController

@OptIn(ExperimentalMaterialApi::class, androidx.compose.ui.ExperimentalComposeUiApi::class)
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
    var selectedStation by remember { mutableStateOf<RadioStation?>(null) }
    var showFilters by remember { mutableStateOf(false) }
    var selectedCountry by remember { mutableStateOf<String?>(null) }
    var selectedOrder by remember { mutableStateOf("votes") }
    var minBitrate by remember { mutableStateOf<Int?>(null) }

    val currentRadio by viewModel.currentRadio.collectAsState()
    val isCompactMode by viewModel.isCompactMode.collectAsState()
    
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Načtení místních stanic a spolehlivý automatický focus
    LaunchedEffect(Unit) {
        viewModel.refreshLocalStations()
        delay(150) // Prodleva pro spolehlivé vysunutí klávesnice po animaci navigace
        focusRequester.requestFocus()
        keyboardController?.show()
    }
    
    val sheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        skipHalfExpanded = true
    )
    val scope = rememberCoroutineScope()

    val configuration = LocalConfiguration.current
    val screenHeight = with(LocalDensity.current) {
        configuration.screenHeightDp.dp
    }
    val bottomSheetHeight = screenHeight * 0.6f

    ModalBottomSheetLayout(
        sheetState = sheetState,
        sheetContent = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(bottomSheetHeight)
                    .background(MaterialTheme.colors.surface)
                    .navigationBarsPadding()
            ) {
                Text(
                    text = stringResource(R.string.settings_add_station),
                    style = MaterialTheme.typography.h6,
                    modifier = Modifier.padding(12.dp)
                )

                Divider()

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    items(
                        items = RadioCategory.values()
                            .filter { category -> category != RadioCategory.VLASTNI },
                        key = { it.name }
                    ) { category ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 1.dp)
                                .clickable { selectedCategory = category },
                            color = if (selectedCategory == category) 
                                MaterialTheme.colors.primary.copy(alpha = 0.1f)
                            else
                                Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedCategory == category,
                                    onClick = { selectedCategory = category }
                                )
                                Text(
                                    text = stringResource(category.getTitleRes()),
                                    style = MaterialTheme.typography.body1,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    }
                }

                Divider()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = { 
                            selectedStation = null
                            selectedCategory = null
                            scope.launch {
                                sheetState.hide()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.action_cancel),
                            style = MaterialTheme.typography.button
                        )
                    }
                    Button(
                        onClick = {
                            selectedCategory?.let { category ->
                                selectedStation?.let { station ->
                                    viewModel.addStationToFavorites(station, category)
                                }
                            }
                            selectedStation = null
                            selectedCategory = null
                            scope.launch {
                                sheetState.hide()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        enabled = selectedCategory != null,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = MaterialTheme.colors.primary,
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.action_save_to_category),
                            style = MaterialTheme.typography.button,
                            color = MaterialTheme.colors.onPrimary
                        )
                    }
                }
            }
        },
        sheetShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_search)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.nav_back))
                    }
                },
                actions = {

                    IconButton(onClick = { showFilters = !showFilters }) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filtry",
                            tint = if (showFilters) MaterialTheme.colors.primary else Color.White
                        )
                    }
                },
                backgroundColor = MaterialTheme.colors.surface
            )

            AnimatedVisibility(visible = showFilters) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.filter_sort_by),
                        style = MaterialTheme.typography.subtitle1,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedOrder == "votes",
                            onClick = { selectedOrder = "votes" },
                            text = stringResource(R.string.filter_sort_by_popularity)
                        )
                        FilterChip(
                            selected = selectedOrder == "name",
                            onClick = { selectedOrder = "name" },
                            text = stringResource(R.string.filter_sort_by_name)
                        )
                        FilterChip(
                            selected = selectedOrder == "bitrate",
                            onClick = { selectedOrder = "bitrate" },
                            text = stringResource(R.string.filter_sort_by_quality)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stringResource(R.string.filter_country),
                        style = MaterialTheme.typography.subtitle1,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedCountry == "Czech Republic",
                            onClick = { selectedCountry = if (selectedCountry != "Czech Republic") "Czech Republic" else null },
                            text = stringResource(R.string.filter_country_czech)
                        )
                        FilterChip(
                            selected = selectedCountry == "Slovakia",
                            onClick = { selectedCountry = if (selectedCountry != "Slovakia") "Slovakia" else null },
                            text = stringResource(R.string.filter_country_slovak)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stringResource(R.string.filter_quality),
                        style = MaterialTheme.typography.subtitle1,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = minBitrate == 64,
                            onClick = { minBitrate = if (minBitrate != 64) 64 else null },
                            text = stringResource(R.string.filter_quality_64)
                        )
                        FilterChip(
                            selected = minBitrate == 128,
                            onClick = { minBitrate = if (minBitrate != 128) 128 else null },
                            text = stringResource(R.string.filter_quality_128)
                        )
                        FilterChip(
                            selected = minBitrate == 256,
                            onClick = { minBitrate = if (minBitrate != 256) 256 else null },
                            text = stringResource(R.string.filter_quality_256)
                        )
                    }
                }
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { query ->
                    searchQuery = query
                    if (query.length >= 3) {
                        isLoading = true
                        viewModel.searchStations(
                            query = query,
                            country = selectedCountry,
                            minBitrate = minBitrate,
                            orderBy = selectedOrder
                        ) { result: List<RadioStation>? ->
                            stations = result ?: emptyList()
                            isLoading = false
                        }
                    } else {
                        stations = emptyList()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .focusRequester(focusRequester),
                label = { Text(stringResource(R.string.search_hint)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = stringResource(R.string.nav_search)) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(androidx.compose.ui.Alignment.CenterHorizontally)
                )
            } else if (searchQuery.length < 3) {
                val localStations by viewModel.localStations.collectAsState()
                val countryCode = viewModel.currentCountryCode.collectAsState().value
                
                localStations?.let { stations ->
                    if (stations.isNotEmpty()) {
                        Text(
                            text = countryCode?.let { RadioCategory.getLocalizedTitle(it) } ?: stringResource(R.string.category_local),
                            style = MaterialTheme.typography.h6,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(
                                items = stations,
                                key = { it.stationuuid ?: it.url }
                            ) { station ->
                                StationItem(
                                    station = station.apply { 
                                        isFromRadioBrowser = false
                                        category = RadioCategory.MISTNI
                                    },
                                    onAddToFavorites = {
                                        selectedStation = station
                                        selectedCategory = null
                                        scope.launch {
                                            sheetState.show()
                                        }
                                    },
                                    isCompact = isCompactMode
                                )
                            }
                        }
                    } else {
                        Text(
                            text = stringResource(R.string.search_min_chars),
                            style = MaterialTheme.typography.body1,
                            modifier = Modifier
                                .padding(16.dp)
                                .align(androidx.compose.ui.Alignment.CenterHorizontally)
                        )
                    }
                } ?: run {
                    Text(
                        text = stringResource(R.string.search_min_chars),
                        style = MaterialTheme.typography.body1,
                        modifier = Modifier
                            .padding(16.dp)
                            .align(androidx.compose.ui.Alignment.CenterHorizontally)
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(
                        items = stations,
                        key = { it.stationuuid ?: it.url }
                    ) { station ->
                        StationItem(
                            station = station,
                            onAddToFavorites = {
                                selectedStation = station
                                selectedCategory = null
                                scope.launch {
                                    sheetState.show()
                                }
                            },
                            isCompact = isCompactMode
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = currentRadio != null,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                currentRadio?.let { radio ->
                    PlayerControls(
                        radio = radio,
                        viewModel = viewModel,
                        onNavigateToFavoriteSongs = onNavigateToFavoriteSongs,
                        onNavigateToCategory = { category ->
                            onNavigateBack()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun StationItem(
    station: RadioStation,
    onAddToFavorites: () -> Unit,
    isCompact: Boolean = false
) {
    val colors = when (val category = station.category) {
        null -> {
            val defaultGradient = Gradients.getGradientForCategory(RadioCategory.OSTATNI)
            listOf(defaultGradient.first, defaultGradient.second)
        }
        else -> {
            val gradient = Gradients.getGradientForCategory(category)
            listOf(gradient.first, gradient.second)
        }
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = if (isCompact) 2.dp else 4.dp),
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
                    .padding(if (isCompact) 8.dp else 16.dp),
                verticalArrangement = Arrangement.spacedBy(if (isCompact) 4.dp else 8.dp)
                verticalArrangement = Arrangement.spacedBy(if (isCompact) 4.dp else 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                   Box(
                        modifier = Modifier
                            .size(if (isCompact) 40.dp else 48.dp)
                            .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                    ) {
                        AsyncImage(
                            model = station.favicon,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit,
                            fallback = painterResource(id = R.drawable.ic_radio_default),
                            error = painterResource(id = R.drawable.ic_radio_default)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = station.name,
                            style = if (isCompact) MaterialTheme.typography.subtitle1 else MaterialTheme.typography.h6,
                            color = Color.White
                        )
                    }
                }
                if (!isCompact && !station.tags.isNullOrBlank()) {
                    Text(
                        text = station.tags,
                        style = MaterialTheme.typography.body2,
                        color = Color.White.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    station.countrycode?.let { countryCode ->
                        Text(
                            text = countryCode.uppercase(),
                            style = MaterialTheme.typography.caption,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                    station.bitrate?.let { bitrate ->
                        Text(
                            text = "$bitrate kbps",
                            style = MaterialTheme.typography.caption,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
                
                if (station.isFromRadioBrowser) {
                    TextButton(
                        onClick = onAddToFavorites,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.action_save_to_category),
                            style = MaterialTheme.typography.button,
                            color = MaterialTheme.colors.onPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    text: String
) {
    Surface(
        modifier = Modifier
            .height(32.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) MaterialTheme.colors.primary else Color.Transparent,
        border = if (!selected) ButtonDefaults.outlinedBorder else null,
        elevation = 0.dp
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.body2,
                color = if (selected) Color.White else Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

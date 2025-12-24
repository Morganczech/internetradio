package cz.internetradio.app.screens

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.google.gson.Gson
import cz.internetradio.app.RadioItem
import cz.internetradio.app.model.Radio
import cz.internetradio.app.model.RadioCategory
import cz.internetradio.app.viewmodel.RadioViewModel
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.res.stringResource
import cz.internetradio.app.PlayerControls
import cz.internetradio.app.R
import android.util.Log
import cz.internetradio.app.ui.theme.Gradients
import cz.internetradio.app.utils.normalizeForSearch
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import kotlinx.coroutines.delay

data class Country(
    val name: String,
    val stations: List<PopularStation>
)

data class PopularStation(
    val id: String,
    val name: String,
    val streamUrl: String,
    val imageUrl: String,
    val description: String,
    val listeners: Int
)

private val COUNTRIES = mapOf(
    "CZ" to "stations_cz.json",
    "SK" to "stations_sk.json"
)

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun PopularStationsScreen(
    viewModel: RadioViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit,
    onNavigateToFavoriteSongs: () -> Unit
) {
    val context = LocalContext.current
    var selectedCountry by remember { mutableStateOf<String?>(null) }
    var countries by remember { mutableStateOf<Map<String, Country>>(emptyMap()) }
    val currentRadio by viewModel.currentRadio.collectAsState()
    val allRadios by viewModel.getAllRadios().collectAsState(initial = emptyList())
    val isCompactMode by viewModel.isCompactMode.collectAsState()
    
    var searchQuery by remember { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        val loadedCountries = mutableMapOf<String, Country>()
        COUNTRIES.forEach { (code, filename) ->
            try {
                val jsonString = context.assets.open(filename).bufferedReader().use { it.readText() }
                val country = Gson().fromJson(jsonString, Country::class.java)
                loadedCountries[code] = country
            } catch (e: Exception) {
                Log.e("PopularStationsScreen", "Chyba při načítání souboru $filename", e)
            }
        }
        countries = loadedCountries
    }

    LaunchedEffect(showSearch) {
        if (showSearch) {
            delay(100)
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        TopAppBar(
            title = { Text(if (selectedCountry == null) "Populární stanice" else countries[selectedCountry]?.name ?: "") },
            navigationIcon = {
                IconButton(onClick = { 
                    if (selectedCountry == null) onNavigateBack()
                    else {
                        selectedCountry = null
                        showSearch = false
                        searchQuery = ""
                    }
                }) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = stringResource(R.string.nav_back),
                        tint = MaterialTheme.colors.onSurface
                    )
                }
            },
            actions = {
                if (selectedCountry != null) {
                    IconButton(onClick = { viewModel.setCompactMode(!isCompactMode) }) {
                        Icon(
                            imageVector = if (isCompactMode) Icons.Default.ViewModule else Icons.Default.ViewList,
                            contentDescription = "Změnit zobrazení",
                            tint = MaterialTheme.colors.onSurface
                        )
                    }
                    IconButton(onClick = { 
                        showSearch = !showSearch
                        if (!showSearch) searchQuery = ""
                    }) {
                        Icon(
                            imageVector = if (showSearch) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = null,
                            tint = if (showSearch) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface
                        )
                    }
                }
            },
            backgroundColor = MaterialTheme.colors.surface,
            elevation = 0.dp
        )

        Divider(color = MaterialTheme.colors.onSurface.copy(alpha = 0.08f))

        AnimatedVisibility(
            visible = showSearch,
            enter = slideInVertically(initialOffsetY = { -it }),
            exit = slideOutVertically(targetOffsetY = { -it })
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .focusRequester(focusRequester),
                placeholder = { Text("Hledat v populárních...") },
                leadingIcon = { Icon(Icons.Default.FilterList, contentDescription = null) },
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = MaterialTheme.colors.primary,
                    unfocusedBorderColor = MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(8.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() })
            )
        }

        if (selectedCountry == null) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(countries.keys.toList()) { countryCode ->
                    CountryCard(
                        countryCode = countryCode,
                        countryName = countries[countryCode]?.name ?: "",
                        onClick = { selectedCountry = countryCode }
                    )
                }
            }
        } else {
            var displayedStationsCount by remember { mutableStateOf(10) }
            
            val filteredStations = remember(selectedCountry, searchQuery, countries) {
                val allStations = countries[selectedCountry]?.stations ?: emptyList()
                if (searchQuery.isBlank()) {
                    allStations
                } else {
                    val normalizedQuery = searchQuery.normalizeForSearch()
                    allStations.filter { it.name.normalizeForSearch().contains(normalizedQuery) }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val stationsToShow = if (searchQuery.isBlank()) {
                    filteredStations.take(displayedStationsCount)
                } else {
                    filteredStations
                }

                items(stationsToShow, key = { it.id }) { station ->
                    val savedRadio = allRadios.find { it.id == station.id }
                    val radio = Radio(
                        id = station.id,
                        name = station.name,
                        streamUrl = station.streamUrl,
                        imageUrl = station.imageUrl,
                        description = station.description,
                        category = RadioCategory.MISTNI,
                        startColor = Gradients.getGradientForCategory(RadioCategory.MISTNI).first,
                        endColor = Gradients.getGradientForCategory(RadioCategory.MISTNI).second,
                        isFavorite = savedRadio?.isFavorite ?: false
                    )

                    RadioItem(
                        radio = radio,
                        isSelected = radio.id == currentRadio?.id,
                        onRadioClick = { viewModel.playRadio(radio) },
                        onFavoriteClick = { viewModel.toggleFavorite(radio) },
                        onEditClick = { onNavigateToEdit(radio.id) },
                        onDeleteClick = { viewModel.removeStation(radio.id) },
                        isCompact = isCompactMode
                    )
                }
                
                if (searchQuery.isBlank() && displayedStationsCount < filteredStations.size) {
                    item {
                        Button(
                            onClick = { displayedStationsCount += 10 },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Zobrazit další")
                        }
                    }
                }
            }
        }

        AnimatedVisibility(visible = currentRadio != null) {
            currentRadio?.let { radio ->
                PlayerControls(
                    radio = radio,
                    viewModel = viewModel,
                    onNavigateToFavoriteSongs = onNavigateToFavoriteSongs,
                    onNavigateToCategory = { onNavigateBack() }
                )
            }
        }
    }
}

@Composable
fun CountryCard(
    countryCode: String,
    countryName: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        elevation = 2.dp,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val flagResId = when (countryCode) {
                "CZ" -> R.drawable.flag_cz
                "SK" -> R.drawable.flag_sk
                else -> R.drawable.ic_radio_default
            }
            
            Image(
                painter = painterResource(id = flagResId),
                contentDescription = countryName,
                modifier = Modifier
                    .weight(1f)
                    .padding(bottom = 8.dp)
            )
            
            Text(
                text = countryName,
                style = MaterialTheme.typography.subtitle1,
                color = MaterialTheme.colors.onSurface
            )
        }
    }
}

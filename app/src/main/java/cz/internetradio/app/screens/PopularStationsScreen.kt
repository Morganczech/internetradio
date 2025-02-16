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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import cz.internetradio.app.RadioItem
import cz.internetradio.app.model.Radio
import cz.internetradio.app.model.RadioCategory
import cz.internetradio.app.viewmodel.RadioViewModel
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.ui.res.stringResource
import cz.internetradio.app.PlayerControls
import cz.internetradio.app.R
import android.util.Log

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
    val isPlaying by viewModel.isPlaying.collectAsState()
    val allRadios by viewModel.getAllRadios().collectAsState(initial = emptyList())

    // Načtení JSON souborů
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
                    else selectedCountry = null
                }) {
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

        if (selectedCountry == null) {
            // Zobrazení ikon zemí
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
            // Zobrazení stanic pro vybranou zemi
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(
                    top = 8.dp,
                    bottom = if (currentRadio != null) 0.dp else 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                countries[selectedCountry]?.stations?.let { stations ->
                    items(stations) { station ->
                        // Kontrola, zda je stanice v oblíbených
                        val savedRadio = allRadios.find { it.id == station.id }
                        val radio = Radio(
                            id = station.id,
                            name = station.name,
                            streamUrl = station.streamUrl,
                            imageUrl = station.imageUrl,
                            description = station.description,
                            category = RadioCategory.OSTATNI,
                            startColor = Color(0xFF1976D2).copy(alpha = 0.8f),
                            endColor = Color(0xFF2196F3).copy(alpha = 0.6f),
                            isFavorite = savedRadio?.isFavorite ?: false
                        )

                        RadioItem(
                            radio = radio,
                            isSelected = radio.id == currentRadio?.id,
                            onRadioClick = { viewModel.playRadio(radio) },
                            onFavoriteClick = { viewModel.toggleFavorite(radio) },
                            onEditClick = { onNavigateToEdit(radio.id) },
                            onDeleteClick = { viewModel.removeStation(radio.id) }
                        )
                    }
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
        elevation = 4.dp
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
                style = MaterialTheme.typography.subtitle1
            )
        }
    }
} 
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cz.internetradio.app.PlayerControls
import cz.internetradio.app.RadioItem
import cz.internetradio.app.viewmodel.RadioViewModel
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import cz.internetradio.app.model.RadioCategory
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AllStationsScreen(
    viewModel: RadioViewModel,
    onNavigateBack: () -> Unit
) {
    val currentRadio by viewModel.currentRadio.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val allRadios by viewModel.getAllRadios().collectAsState(initial = emptyList())
    val showMaxFavoritesError by viewModel.showMaxFavoritesError.collectAsState()
    var selectedCategory by remember { mutableStateOf<RadioCategory?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top App Bar
        TopAppBar(
            title = { Text("Všechny stanice") },
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

        // Vyhledávací pole
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("Vyhledat rádio...") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Vyhledat",
                    tint = Color.White.copy(alpha = 0.7f)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Vymazat",
                            tint = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            },
            colors = TextFieldDefaults.outlinedTextFieldColors(
                textColor = Color.White,
                cursorColor = Color.White,
                placeholderColor = Color.White.copy(alpha = 0.7f),
                focusedBorderColor = MaterialTheme.colors.primary,
                unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(8.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = { keyboardController?.hide() }
            )
        )

        // Kategorie
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            item {
                CategoryChip(
                    text = "Vše",
                    isSelected = selectedCategory == null,
                    onClick = { selectedCategory = null }
                )
            }
            items(RadioCategory.values()) { category ->
                CategoryChip(
                    text = category.title,
                    isSelected = selectedCategory == category,
                    onClick = { selectedCategory = category }
                )
            }
        }

        // Seznam stanic
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
            val filteredRadios = allRadios
                .filter { radio ->
                    (selectedCategory == null || radio.category == selectedCategory) &&
                    (searchQuery.isEmpty() || radio.name.contains(searchQuery, ignoreCase = true))
                }
                .sortedBy { it.name.lowercase() }

            if (filteredRadios.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = if (searchQuery.isEmpty()) 
                                    "Žádné stanice v této kategorii" 
                                else 
                                    "Žádné stanice nenalezeny",
                                style = MaterialTheme.typography.body1,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            } else {
                items(filteredRadios) { radio ->
                    RadioItem(
                        radio = radio,
                        isSelected = radio.id == currentRadio?.id,
                        onRadioClick = { viewModel.playRadio(radio) },
                        onFavoriteClick = { viewModel.toggleFavorite(radio) },
                        onRemoveClick = { viewModel.removeStation(radio.id) }
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
                    isPlaying = isPlaying,
                    onPlayPauseClick = { viewModel.togglePlayPause() },
                    viewModel = viewModel
                )
            }
        }
    }

    // Dialog s upozorněním na maximální počet oblíbených stanic
    if (showMaxFavoritesError) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissMaxFavoritesError() },
            title = { Text("Maximální počet oblíbených") },
            text = { 
                val maxFavorites by viewModel.maxFavorites.collectAsState()
                Text("Můžete mít maximálně $maxFavorites oblíbených stanic. Prosím, odeberte některou stanici z oblíbených před přidáním nové, nebo zvyšte limit v nastavení aplikace.") 
            },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissMaxFavoritesError() }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
fun CategoryChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .height(32.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        backgroundColor = if (isSelected) MaterialTheme.colors.primary else Color.Transparent,
        border = if (!isSelected) ButtonDefaults.outlinedBorder else null,
        elevation = 0.dp
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.body2.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                ),
                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f)
            )
        }
    }
} 
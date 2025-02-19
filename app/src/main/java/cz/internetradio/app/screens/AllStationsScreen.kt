package cz.internetradio.app.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.res.stringResource
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import kotlinx.coroutines.launch
import cz.internetradio.app.R

@OptIn(ExperimentalComposeUiApi::class, ExperimentalPagerApi::class)
@Composable
fun AllStationsScreen(
    viewModel: RadioViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToBrowseStations: () -> Unit,
    onNavigateToPopularStations: () -> Unit,
    onNavigateToAddRadio: () -> Unit,
    onNavigateToEdit: (String) -> Unit,
    onNavigateToFavoriteSongs: () -> Unit
) {
    val currentRadio by viewModel.currentRadio.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val allRadios by viewModel.getAllRadios().collectAsState(initial = emptyList())
    val showMaxFavoritesError by viewModel.showMaxFavoritesError.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current
    val coroutineScope = rememberCoroutineScope()

    // Seznam všech kategorií v požadovaném pořadí
    val categories = remember {
        listOf(
            RadioCategory.VLASTNI,
            RadioCategory.MISTNI,
            RadioCategory.VSE
        ) + RadioCategory.values().filter { 
            it != RadioCategory.VSE &&
            it != RadioCategory.VLASTNI && 
            it != RadioCategory.MISTNI &&
            it != RadioCategory.OSTATNI
        } + listOf(RadioCategory.OSTATNI)
    }

    // Stav pro HorizontalPager
    val pagerState = rememberPagerState()
    
    // Stav pro LazyRow s kategoriemi
    val lazyRowState = rememberLazyListState()

    // Synchronizace selectedCategory s pagerState
    var selectedCategory by rememberSaveable { mutableStateOf(categories[0]) }
    
    LaunchedEffect(pagerState.currentPage) {
        selectedCategory = categories[pagerState.currentPage]
    }

    // Sledování offsetu pro plynulý posun kategorií
    LaunchedEffect(pagerState.currentPage, pagerState.currentPageOffset) {
        // Vypočítáme cílový index pro scrollování
        val currentIndex = pagerState.currentPage
        val targetIndex = when {
            pagerState.currentPageOffset > 0 -> currentIndex + 1
            pagerState.currentPageOffset < 0 -> currentIndex - 1
            else -> currentIndex
        }
        
        // Plynulý posun na cílovou pozici
        if (targetIndex in categories.indices) {
            val offset = pagerState.currentPageOffset
            lazyRowState.scrollToItem(
                index = currentIndex,
                scrollOffset = (offset * lazyRowState.layoutInfo.viewportEndOffset).toInt()
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        var showSearch by remember { mutableStateOf(false) }

        // Top App Bar
        TopAppBar(
            title = { Text(stringResource(R.string.nav_my_stations)) },
            actions = {
                IconButton(onClick = { showSearch = !showSearch }) {
                    Icon(
                        imageVector = if (showSearch) Icons.Default.Clear else Icons.Default.Search,
                        contentDescription = stringResource(R.string.nav_search),
                        tint = Color.White
                    )
                }
                IconButton(onClick = onNavigateToPopularStations) {
                    Icon(
                        imageVector = Icons.Default.Flag,
                        contentDescription = stringResource(R.string.nav_popular_stations),
                        tint = Color.White
                    )
                }
                IconButton(onClick = onNavigateToBrowseStations) {
                    Icon(
                        imageVector = Icons.Default.ManageSearch,
                        contentDescription = stringResource(R.string.nav_search_stations),
                        tint = Color.White
                    )
                }
                IconButton(onClick = onNavigateToSettings) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = stringResource(R.string.nav_settings_desc),
                        tint = Color.White
                    )
                }
            },
            backgroundColor = MaterialTheme.colors.surface,
            elevation = 4.dp
        )

        // Vyhledávací pole - zobrazí se pouze když je showSearch true
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
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text(stringResource(R.string.search_hint_saved)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = stringResource(R.string.nav_search),
                        tint = Color.White.copy(alpha = 0.7f)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = stringResource(R.string.action_clear),
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
        }

        // Kategorie
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = if (showSearch) 8.dp else 16.dp, bottom = 16.dp)
        ) {
            LazyRow(
                state = lazyRowState,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                items(categories) { category ->
                    CategoryChip(
                        category = category,
                        isSelected = selectedCategory == category,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(categories.indexOf(category))
                            }
                        }
                    )
                }
            }
        }

        // HorizontalPager pro přepínání mezi kategoriemi
        HorizontalPager(
            count = categories.size,
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            val category = categories[page]
            
            // Seznam stanic pro aktuální kategorii
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    bottom = if (currentRadio != null) 0.dp else 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filteredRadios = if (searchQuery.isEmpty()) {
                    allRadios.filter { radio ->
                        when {
                            category == RadioCategory.VSE -> true
                            category == RadioCategory.VLASTNI -> radio.isFavorite
                            category == RadioCategory.OSTATNI -> {
                                // Stanice, které nejsou v žádné specifické kategorii
                                val specificCategories = RadioCategory.values().filter { 
                                    it != RadioCategory.VSE && 
                                    it != RadioCategory.VLASTNI && 
                                    it != RadioCategory.OSTATNI 
                                }
                                !specificCategories.contains(radio.category)
                            }
                            else -> radio.category == category
                        }
                    }
                } else {
                    // Při vyhledávání prohledáváme všechny kategorie
                    allRadios.filter { radio ->
                        radio.name.contains(searchQuery, ignoreCase = true)
                    }
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
                    onNavigateToFavoriteSongs = onNavigateToFavoriteSongs,
                    onNavigateToCategory = { category ->
                        // Najdeme index kategorie a přepneme na ni
                        val categoryIndex = categories.indexOf(category)
                        if (categoryIndex != -1) {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(categoryIndex)
                            }
                        }
                    }
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
    category: RadioCategory,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    // Definice barev pro pozadí
    val backgroundColor = if (isSelected) {
        when (category) {
            // Pro obecné kategorie použijeme výchozí primární barvu
            RadioCategory.VSE,
            RadioCategory.VLASTNI -> MaterialTheme.colors.primary
            else -> category.startColor
        }
    } else Color.Transparent

    Card(
        modifier = Modifier
            .height(32.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        backgroundColor = backgroundColor,
        border = if (!isSelected) ButtonDefaults.outlinedBorder else null,
        elevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (category == RadioCategory.VLASTNI) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f)
                )
            }
            Text(
                text = stringResource(category.getTitleRes()),
                style = MaterialTheme.typography.body2.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                ),
                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f)
            )
        }
    }
} 
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
import androidx.compose.ui.graphics.Brush
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
import cz.internetradio.app.ui.theme.Gradients
import androidx.compose.foundation.gestures.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.*
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.composed
import androidx.compose.runtime.remember
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import cz.internetradio.app.components.DraggableRadioItem
import android.util.Log
import kotlin.math.roundToInt
import cz.internetradio.app.model.Radio
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.foundation.lazy.LazyItemScope

@OptIn(ExperimentalComposeUiApi::class, ExperimentalPagerApi::class, ExperimentalFoundationApi::class)
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

    // Implementace drag & drop s mutableStateListOf
    val radiosList = remember {
        mutableStateListOf<Radio>()
    }

    LaunchedEffect(allRadios, selectedCategory, searchQuery) {
        radiosList.clear()
        radiosList.addAll(
            if (searchQuery.isEmpty()) {
                allRadios.filter { radio ->
                    when {
                        selectedCategory == RadioCategory.VSE -> true
                        selectedCategory == RadioCategory.VLASTNI -> radio.isFavorite
                        selectedCategory == RadioCategory.OSTATNI -> {
                            val specificCategories = RadioCategory.values().filter { 
                                it != RadioCategory.VSE && 
                                it != RadioCategory.VLASTNI && 
                                it != RadioCategory.OSTATNI 
                            }
                            !specificCategories.contains(radio.category)
                        }
                        else -> radio.category == selectedCategory
                    }
                }
            } else {
                allRadios.filter { radio ->
                    radio.name.contains(searchQuery, ignoreCase = true)
                }
            }.sortedBy { it.name.lowercase() }
        )
    }

    val dragDropState = rememberDragDropState(
        onMove = { fromIndex, toIndex ->
            if (fromIndex != toIndex && fromIndex in radiosList.indices && toIndex in radiosList.indices) {
                val item = radiosList.removeAt(fromIndex)
                radiosList.add(toIndex, item)
                // Aktualizujeme pořadí v ViewModel
                viewModel.updateStationOrder(selectedCategory, fromIndex, toIndex)
            }
        }
    )

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
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(
                    top = 8.dp,
                    bottom = if (currentRadio != null) 0.dp else 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (radiosList.isEmpty()) {
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
                    itemsIndexed(
                        items = radiosList,
                        key = { _, radio -> radio.id }
                    ) { index, radio ->
                        DraggableRadioItem(
                            dragDropState = dragDropState,
                            index = index,
                            listSize = radiosList.size,
                            itemModifier = Modifier.animateItemPlacement()
                        ) { isDragging ->
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

                    // Přidání tlačítka "Zobrazit další" pro kategorii MISTNI
                    if (selectedCategory == RadioCategory.MISTNI && searchQuery.isEmpty()) {
                        item {
                            Button(
                                onClick = { viewModel.loadMoreStations() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text("Zobrazit další")
                            }
                        }
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
    val colors = if (isSelected) {
        when (category) {
            // Pro obecné kategorie použijeme výchozí primární barvu
            RadioCategory.VSE,
            RadioCategory.VLASTNI -> listOf(MaterialTheme.colors.primary, MaterialTheme.colors.primary)
            else -> {
                val gradient = Gradients.getGradientForCategory(category)
                listOf(gradient.first, gradient.second)
            }
        }
    } else listOf(Color.Transparent, Color.Transparent)

    Card(
        modifier = Modifier
            .height(32.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        backgroundColor = Color.Transparent,
        border = if (!isSelected) ButtonDefaults.outlinedBorder else null,
        elevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.horizontalGradient(colors = colors)
                )
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
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
}

@Composable
private fun rememberDragDropState(
    onMove: (Int, Int) -> Unit
): DragDropState {
    val state = remember {
        DragDropState(
            onMove = onMove
        )
    }
    return state
}

class DragDropState(
    val onMove: (Int, Int) -> Unit
) {
    var draggingItemIndex by mutableStateOf<Int?>(null)
        private set
    var draggedOverItemIndex by mutableStateOf<Int?>(null)
        private set
    var draggedOffset by mutableStateOf(0f)
        private set
    private var lastTargetIndex: Int? = null
    private val itemHeight = 150f // Výška karty v pixelech

    private fun calculateTargetIndex(
        draggedIndex: Int,
        draggedOffset: Float,
        listSize: Int
    ): Int? {
        // Výpočet relativní pozice vzhledem k výšce karty
        val relativePosition = draggedOffset / itemHeight
        
        // Určení směru pohybu
        val direction = when {
            relativePosition > 0.3f -> 1  // Pohyb dolů
            relativePosition < -0.3f -> -1 // Pohyb nahoru
            else -> 0 // Žádný pohyb
        }
        
        // Výpočet nového indexu s omezením na jednu pozici
        return when {
            direction != 0 -> (draggedIndex + direction).coerceIn(0, listSize - 1)
            else -> null
        }
    }

    fun onDragStart(index: Int) {
        Log.d("DragDropState", "Starting drag for index $index")
        draggingItemIndex = index
        draggedOverItemIndex = null
        draggedOffset = 0f
        lastTargetIndex = null
    }

    fun onDragEnd() {
        Log.d("DragDropState", "Ending drag")
        // Provedeme finální přesun pouze pokud máme platný cílový index
        if (lastTargetIndex != null && draggingItemIndex != null && lastTargetIndex != draggingItemIndex) {
            onMove(draggingItemIndex!!, lastTargetIndex!!)
        }
        draggingItemIndex = null
        draggedOverItemIndex = null
        draggedOffset = 0f
        lastTargetIndex = null
    }

    fun onDraggedOver(index: Int, offsetY: Float, listSize: Int) {
        if (draggingItemIndex == null) return

        // Akumulujeme offset pro plynulejší pohyb
        draggedOffset += offsetY
        
        Log.d("DragDropState", "Dragging item $index, dragAmount: $offsetY")
        
        // Výpočet nového cílového indexu
        val targetIndex = calculateTargetIndex(draggingItemIndex!!, draggedOffset, listSize)
        
        // Aktualizujeme pouze pokud:
        // 1. Máme platný cílový index
        // 2. Je jiný než aktuální
        // 3. Nepřeskakujeme více než jednu pozici
        if (targetIndex != null && targetIndex != lastTargetIndex) {
            lastTargetIndex = targetIndex
            draggedOverItemIndex = targetIndex
            // Resetujeme offset po přesunu
            draggedOffset = 0f
            Log.d("DragDropState", "Target index updated to: $targetIndex")
        }
    }
} 
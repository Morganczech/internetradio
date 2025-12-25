package cz.internetradio.app.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.ui.text.style.TextAlign
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.ExperimentalFoundationApi
import cz.internetradio.app.components.DraggableRadioItem
import cz.internetradio.app.components.rememberDragDropState
import android.util.Log
import cz.internetradio.app.model.Radio
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import kotlinx.coroutines.delay
import cz.internetradio.app.utils.normalizeForSearch

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
    val allRadios by viewModel.getAllRadios().collectAsState(initial = emptyList())
    val showMaxFavoritesError by viewModel.showMaxFavoritesError.collectAsState()
    val useUnifiedAccentColor by viewModel.useUnifiedAccentColor.collectAsState()
    
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showSearch by rememberSaveable { mutableStateOf(false) }
    var isReorderMode by rememberSaveable { mutableStateOf(false) }
    
    val keyboardController = LocalSoftwareKeyboardController.current
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val focusRequester = remember { FocusRequester() }

    val categories = remember {
        listOf(RadioCategory.VLASTNI, RadioCategory.MISTNI, RadioCategory.VSE) + 
        RadioCategory.values().filter { 
            it != RadioCategory.VSE && it != RadioCategory.VLASTNI && 
            it != RadioCategory.MISTNI && it != RadioCategory.OSTATNI 
        } + listOf(RadioCategory.OSTATNI)
    }

    val pagerState = rememberPagerState()
    val lazyRowState = rememberLazyListState()
    var selectedCategory by rememberSaveable { mutableStateOf(categories[0]) }

    LaunchedEffect(pagerState.currentPage) {
        selectedCategory = categories[pagerState.currentPage]
    }

    LaunchedEffect(pagerState.currentPage, pagerState.currentPageOffset) {
        val currentIndex = pagerState.currentPage
        if (currentIndex in categories.indices) {
            val offset = pagerState.currentPageOffset
            lazyRowState.scrollToItem(
                index = currentIndex,
                scrollOffset = (offset * lazyRowState.layoutInfo.viewportEndOffset).toInt()
            )
        }
    }

    LaunchedEffect(showSearch) {
        if (showSearch) {
            delay(100)
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        
        TopAppBar(
            title = { /* Prázdné pro čistý design */ },
            actions = {
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

                IconButton(onClick = onNavigateToBrowseStations) {
                    Icon(
                        imageVector = Icons.Default.AddCircleOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colors.onSurface
                    )
                }

                IconButton(onClick = { isReorderMode = !isReorderMode }) {
                    Icon(
                        imageVector = if (isReorderMode) Icons.Default.Check else Icons.Default.Reorder,
                        contentDescription = null,
                        tint = if (isReorderMode) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface
                    )
                }

                IconButton(onClick = onNavigateToSettings) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = MaterialTheme.colors.onSurface
                    )
                }
            },
            backgroundColor = MaterialTheme.colors.surface,
            elevation = 0.dp
        )

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
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .focusRequester(focusRequester),
                placeholder = { Text(stringResource(R.string.search_hint_saved)) },
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

        val showOfflineEmptyState = allRadios.isEmpty() && !viewModel.isNetworkAvailable()

        if (showOfflineEmptyState) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp).padding(bottom = 16.dp),
                        tint = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                    )
                    Text(
                        text = stringResource(R.string.empty_state_offline_title),
                        style = MaterialTheme.typography.h6,
                        color = MaterialTheme.colors.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.empty_state_offline_desc),
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
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
                            useUnifiedColor = useUnifiedAccentColor,
                            onClick = { coroutineScope.launch { pagerState.animateScrollToPage(categories.indexOf(category)) } }
                        )
                    }
                }
            }

            HorizontalPager(
                count = categories.size,
                state = pagerState,
                modifier = Modifier.weight(1f),
                userScrollEnabled = !isReorderMode
            ) { page ->
                val category = categories[page]
                
                // OPRAVA: Inteligentní filtrování bez diakritiky
                val pageRadios = remember(allRadios, category, searchQuery) {
                    val filtered = if (searchQuery.isNotEmpty()) {
                        val normalizedQuery = searchQuery.normalizeForSearch()
                        allRadios.filter { it.name.normalizeForSearch().contains(normalizedQuery) }
                            .sortedBy { it.name.lowercase() }
                    } else {
                        allRadios.filter { radio ->
                            when (category) {
                                RadioCategory.VSE -> true
                                RadioCategory.VLASTNI -> radio.isFavorite
                                else -> radio.category == category
                            }
                        }
                    }
                    mutableStateListOf<Radio>().apply { addAll(filtered) }
                }

                val pageDragDropState = rememberDragDropState(
                    onMove = { from, to ->
                        if (from in pageRadios.indices && to in pageRadios.indices) {
                            val item = pageRadios.removeAt(from)
                            pageRadios.add(to, item)
                        }
                        viewModel.updateStationOrder(category, from, to)
                    }
                )

                key(category.name) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        userScrollEnabled = pageDragDropState.draggingItemId == null
                    ) {
                        itemsIndexed(
                            items = pageRadios,
                            key = { _, radio -> radio.id }
                        ) { index, radio ->
                            DraggableRadioItem(
                                dragDropState = pageDragDropState,
                                index = index,
                                radio = radio
                            ) { isDragging ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isReorderMode) {
                                        Icon(
                                            imageVector = Icons.Default.DragHandle,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .padding(start = 16.dp)
                                                .size(32.dp)
                                                .pointerInput(Unit) {
                                                    detectDragGestures(
                                                        onDragStart = { _ ->
                                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                            pageDragDropState.onDragStart(index, radio.id)
                                                        },
                                                        onDragEnd = { 
                                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                            pageDragDropState.onDragEnd() 
                                                        },
                                                        onDragCancel = { pageDragDropState.onDragCancel() },
                                                        onDrag = { change, dragAmount ->
                                                            change.consume()
                                                            pageDragDropState.onDragged(dragAmount.y, pageRadios.size) {
                                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                            }
                                                        }
                                                    )
                                                },
                                            tint = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                                        )
                                    }
                                    
                                    RadioItem(
                                        radio = radio,
                                        isSelected = radio.id == currentRadio?.id,
                                        onRadioClick = { if (!isReorderMode) viewModel.playRadio(radio, category) },
                                        onFavoriteClick = { viewModel.toggleFavorite(radio) },
                                        onEditClick = { onNavigateToEdit(radio.id) },
                                        onDeleteClick = { viewModel.removeStation(radio.id) },
                                        modifier = Modifier.weight(1f),
                                        useUnifiedColor = useUnifiedAccentColor
                                    )
                                }
                            }
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
                    onNavigateToCategory = { cat ->
                        coroutineScope.launch { pagerState.animateScrollToPage(categories.indexOf(cat)) }
                    }
                )
            }
        }
    }

    if (showMaxFavoritesError) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissMaxFavoritesError() },
            title = { Text(stringResource(R.string.msg_favorites_limit)) },
            text = { 
                val maxFavorites by viewModel.maxFavorites.collectAsState()
                Text(stringResource(R.string.msg_favorites_limit_description, maxFavorites)) 
            },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissMaxFavoritesError() }) {
                    Text(stringResource(R.string.action_close))
                }
            }
        )
    }
}

@Composable
fun CategoryChip(category: RadioCategory, isSelected: Boolean, useUnifiedColor: Boolean, onClick: () -> Unit) {
    val colors = if (isSelected) {
        val gradient = Gradients.getGradientForCategory(category, useUnified = useUnifiedColor)
        listOf(gradient.first, gradient.second)
    } else listOf(Color.Transparent, Color.Transparent)

    Card(
        modifier = Modifier.height(32.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        backgroundColor = Color.Transparent,
        border = if (!isSelected) ButtonDefaults.outlinedBorder else null,
        elevation = 0.dp
    ) {
        Box(modifier = Modifier.background(brush = Brush.horizontalGradient(colors = colors)).padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(category.getTitleRes()), 
                color = if (isSelected) Color.White else MaterialTheme.colors.onSurface
            )
        }
    }
}

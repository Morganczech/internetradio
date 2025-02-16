package cz.internetradio.app.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cz.internetradio.app.R
import cz.internetradio.app.ui.theme.Gradients
import cz.internetradio.app.screens.AddRadioViewModel
import cz.internetradio.app.model.RadioCategory

@Composable
fun AddRadioScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddRadioViewModel = hiltViewModel(),
    radioToEdit: String? = null
) {
    var showGradientPicker by remember { mutableStateOf(false) }
    var showCategoryPicker by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val selectedGradientId by viewModel.selectedGradientId.collectAsState()
    
    var name by remember { mutableStateOf("") }
    var streamUrl by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(RadioCategory.MISTNI) }
    val validationError by viewModel.validationError.collectAsState()

    // Načtení existujícího rádia pro úpravu
    LaunchedEffect(radioToEdit) {
        if (radioToEdit != null) {
            viewModel.loadRadioForEdit(radioToEdit)?.let { radio ->
                name = radio.name
                streamUrl = radio.streamUrl
                imageUrl = radio.imageUrl
                description = radio.description
                selectedCategory = radio.category
                radio.gradientId?.let { viewModel.setSelectedGradient(it) }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding()
            .verticalScroll(scrollState)
    ) {
        TopAppBar(
            title = { Text(if (radioToEdit != null) "Upravit stanici" else "Přidat stanici") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Zpět")
                }
            },
            backgroundColor = MaterialTheme.colors.surface,
            elevation = 0.dp
        )

        // Formulář pro úpravu stanice
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.add_station_name)) },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = streamUrl,
                onValueChange = { streamUrl = it },
                label = { Text(stringResource(R.string.add_station_stream_url)) },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = imageUrl,
                onValueChange = { imageUrl = it },
                label = { Text(stringResource(R.string.add_station_image_url)) },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(stringResource(R.string.add_station_description)) },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )

            // Výběr kategorie
            OutlinedTextField(
                value = stringResource(selectedCategory.getTitleRes()),
                onValueChange = { },
                enabled = false,
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showCategoryPicker = true },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null
                    )
                }
            )

            DropdownMenu(
                expanded = showCategoryPicker,
                onDismissRequest = { showCategoryPicker = false }
            ) {
                // Filtrujeme kategorie - odstraníme OSTATNI a VLASTNI
                RadioCategory.values()
                    .filter { category -> 
                        category != RadioCategory.OSTATNI && 
                        category != RadioCategory.VLASTNI 
                    }
                    .forEach { category ->
                        DropdownMenuItem(
                            onClick = {
                                selectedCategory = category
                                showCategoryPicker = false
                            }
                        ) {
                            Text(stringResource(category.getTitleRes()))
                        }
                    }
            }

            // Výběr gradientu
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showGradientPicker = true },
                shape = RoundedCornerShape(8.dp),
                elevation = 4.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = selectedGradientId?.let { id ->
                                    Gradients.availableGradients.find { it.id == id }?.colors?.let { 
                                        listOf(it.first, it.second)
                                    }
                                } ?: listOf(Color.Gray, Color.DarkGray)
                            )
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedGradientId?.let { id ->
                                Gradients.availableGradients.find { it.id == id }?.name
                            } ?: "Vybrat gradient",
                            color = Color.White
                        )
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = "Vybrat",
                            tint = Color.White
                        )
                    }
                }
            }

            // Tlačítko pro uložení
            Button(
                onClick = {
                    if (radioToEdit != null) {
                        viewModel.updateRadio(
                            radioId = radioToEdit,
                            name = name,
                            streamUrl = streamUrl,
                            imageUrl = if (imageUrl.isBlank()) null else imageUrl,
                            description = if (description.isBlank()) null else description,
                            category = selectedCategory,
                            onSuccess = onNavigateBack
                        )
                    } else {
                        viewModel.addRadio(
                            name = name,
                            streamUrl = streamUrl,
                            imageUrl = if (imageUrl.isBlank()) null else imageUrl,
                            description = if (description.isBlank()) null else description,
                            category = selectedCategory,
                            onSuccess = onNavigateBack
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank() && streamUrl.isNotBlank()
            ) {
                Text(if (radioToEdit != null) stringResource(R.string.add_station_save_changes) else stringResource(R.string.add_station_add))
            }
        }

        // Zobrazení chyby validace
        validationError?.let { error ->
            Text(
                text = when (error) {
                    is AddRadioViewModel.ValidationError.DuplicateStreamUrl -> stringResource(R.string.add_station_error_duplicate_url)
                    is AddRadioViewModel.ValidationError.DuplicateName -> stringResource(R.string.add_station_error_duplicate_name)
                    is AddRadioViewModel.ValidationError.StreamError -> error.message
                },
                color = MaterialTheme.colors.error,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }

    if (showGradientPicker) {
        AlertDialog(
            onDismissRequest = { showGradientPicker = false },
            title = { Text("Vyberte gradient") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Gradients.availableGradients.forEach { gradient ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                                .clickable {
                                    viewModel.setSelectedGradient(gradient.id)
                                    showGradientPicker = false
                                },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(
                                                gradient.colors.first,
                                                gradient.colors.second
                                            )
                                        )
                                    )
                            ) {
                                Text(
                                    text = gradient.name,
                                    color = Color.White,
                                    modifier = Modifier
                                        .align(Alignment.CenterStart)
                                        .padding(16.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showGradientPicker = false }) {
                    Text("Zrušit")
                }
            }
        )
    }
} 
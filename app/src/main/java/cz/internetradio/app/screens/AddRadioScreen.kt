package cz.internetradio.app.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.ui.Alignment
import cz.internetradio.app.model.RadioCategory
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@Composable
fun AddRadioScreen(
    viewModel: AddRadioViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    radioToEdit: String? = null
) {
    var name by remember { mutableStateOf("") }
    var streamUrl by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(RadioCategory.VLASTNI) }
    var isDropdownExpanded by remember { mutableStateOf(false) }
    
    // Načtení existující stanice pro editaci
    LaunchedEffect(radioToEdit) {
        if (radioToEdit != null) {
            viewModel.loadRadioForEdit(radioToEdit)?.let { radio ->
                name = radio.name
                streamUrl = radio.streamUrl
                imageUrl = radio.imageUrl ?: ""
                description = radio.description ?: ""
                selectedCategory = radio.category
            }
        }
    }

    // Validační stavy
    var nameError by remember { mutableStateOf(false) }
    var streamUrlError by remember { mutableStateOf(false) }

    val validationError by viewModel.validationError.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            TopAppBar(
                title = { Text(if (radioToEdit != null) "Upravit stanici" else "Přidat stanici") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Zpět",
                            tint = MaterialTheme.colors.onPrimary
                        )
                    }
                },
                backgroundColor = MaterialTheme.colors.surface,
                elevation = 4.dp
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (validationError) {
                    is AddRadioViewModel.ValidationError.DuplicateStreamUrl -> {
                        Text(
                            text = "Stanice s touto URL již existuje",
                            color = MaterialTheme.colors.error,
                            style = MaterialTheme.typography.caption,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    is AddRadioViewModel.ValidationError.DuplicateName -> {
                        Text(
                            text = "Stanice s tímto názvem již existuje",
                            color = MaterialTheme.colors.error,
                            style = MaterialTheme.typography.caption,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    is AddRadioViewModel.ValidationError.StreamError -> {
                        Text(
                            text = (validationError as AddRadioViewModel.ValidationError.StreamError).message,
                            color = MaterialTheme.colors.error,
                            style = MaterialTheme.typography.caption,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    null -> { /* Bez chyby */ }
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { 
                            name = it
                            nameError = it.isBlank()
                            viewModel.clearValidationError()
                        },
                        label = { Text("Název rádia") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = nameError,
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = MaterialTheme.colors.primary,
                            unfocusedBorderColor = Color.Gray,
                            errorBorderColor = Color.Red,
                            textColor = Color.White,
                            focusedLabelColor = MaterialTheme.colors.primary,
                            unfocusedLabelColor = Color.Gray
                        )
                    )
                    if (nameError) {
                        Text(
                            text = "Název je povinný",
                            color = MaterialTheme.colors.error,
                            style = MaterialTheme.typography.caption,
                            modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                        )
                    }
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = streamUrl,
                        onValueChange = { 
                            streamUrl = it
                            streamUrlError = !it.startsWith("http://") && !it.startsWith("https://")
                            viewModel.clearValidationError()
                        },
                        label = { Text("Stream URL") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = streamUrlError,
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = MaterialTheme.colors.primary,
                            unfocusedBorderColor = Color.Gray,
                            errorBorderColor = Color.Red,
                            textColor = Color.White,
                            focusedLabelColor = MaterialTheme.colors.primary,
                            unfocusedLabelColor = Color.Gray
                        )
                    )
                    Text(
                        text = if (streamUrlError) "URL musí začínat http:// nebo https://"
                              else "Např. https://ice.actve.net/fm-evropa2-128",
                        color = if (streamUrlError) MaterialTheme.colors.error else Color.Gray,
                        style = MaterialTheme.typography.caption,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }

                OutlinedTextField(
                    value = imageUrl,
                    onValueChange = { imageUrl = it },
                    label = { Text("URL ikony (volitelné)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = MaterialTheme.colors.primary,
                        unfocusedBorderColor = Color.Gray,
                        textColor = Color.White,
                        focusedLabelColor = MaterialTheme.colors.primary,
                        unfocusedLabelColor = Color.Gray
                    )
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Popis (volitelný)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = MaterialTheme.colors.primary,
                        unfocusedBorderColor = Color.Gray,
                        textColor = Color.White,
                        focusedLabelColor = MaterialTheme.colors.primary,
                        unfocusedLabelColor = Color.Gray
                    )
                )

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedCategory.title,
                        onValueChange = { },
                        label = { Text("Kategorie") },
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { isDropdownExpanded = true }) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Vybrat kategorii",
                                    tint = Color.White
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = MaterialTheme.colors.primary,
                            unfocusedBorderColor = Color.Gray,
                            textColor = Color.White,
                            focusedLabelColor = MaterialTheme.colors.primary,
                            unfocusedLabelColor = Color.Gray
                        )
                    )

                    DropdownMenu(
                        expanded = isDropdownExpanded,
                        onDismissRequest = { isDropdownExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        RadioCategory.values()
                            .sortedWith(compareBy { 
                                when(it) {
                                    RadioCategory.VLASTNI -> -1
                                    RadioCategory.OSTATNI -> RadioCategory.values().size
                                    else -> it.ordinal
                                }
                            })
                            .forEach { category ->
                                DropdownMenuItem(
                                    onClick = {
                                        selectedCategory = category
                                        isDropdownExpanded = false
                                    }
                                ) {
                                    Text(category.title)
                                }
                            }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            "Zrušit",
                            style = MaterialTheme.typography.button.copy(
                                fontSize = 16.sp
                            )
                        )
                    }

                    Button(
                        onClick = {
                            nameError = name.isBlank()
                            streamUrlError = !streamUrl.startsWith("http://") && !streamUrl.startsWith("https://")
                            
                            if (!nameError && !streamUrlError) {
                                if (radioToEdit != null) {
                                    viewModel.updateRadio(
                                        radioId = radioToEdit,
                                        name = name,
                                        streamUrl = streamUrl,
                                        imageUrl = imageUrl.takeIf { it.isNotBlank() },
                                        description = description.takeIf { it.isNotBlank() },
                                        category = selectedCategory,
                                        onSuccess = { onNavigateBack() }
                                    )
                                } else {
                                    viewModel.addRadio(
                                        name = name,
                                        streamUrl = streamUrl,
                                        imageUrl = imageUrl.takeIf { it.isNotBlank() },
                                        description = description.takeIf { it.isNotBlank() },
                                        category = selectedCategory,
                                        onSuccess = { onNavigateBack() }
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = MaterialTheme.colors.primary,
                            contentColor = Color.White
                        ),
                        enabled = name.isNotBlank() && streamUrl.isNotBlank()
                    ) {
                        Text(
                            "Uložit",
                            style = MaterialTheme.typography.button.copy(
                                fontSize = 16.sp
                            )
                        )
                    }
                }
            }
        }
    }
} 
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

@Composable
fun AddRadioScreen(
    viewModel: AddRadioViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var streamUrl by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(RadioCategory.VLASTNI) }
    var isDropdownExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        TopAppBar(
            title = { Text("Přidat stanici") },
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
                .fillMaxSize()
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Název rádia") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = streamUrl,
                onValueChange = { streamUrl = it },
                label = { Text("Stream URL") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = imageUrl,
                onValueChange = { imageUrl = it },
                label = { Text("URL ikony (volitelné)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Popis (volitelný)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Výběr kategorie
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
                                contentDescription = "Vybrat kategorii"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                DropdownMenu(
                    expanded = isDropdownExpanded,
                    onDismissRequest = { isDropdownExpanded = false },
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    // Seřadíme kategorie tak, aby VLASTNI byla první a OSTATNI poslední
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

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (name.isNotBlank() && streamUrl.isNotBlank()) {
                        viewModel.addRadio(
                            name = name,
                            streamUrl = streamUrl,
                            imageUrl = imageUrl.takeIf { it.isNotBlank() },
                            description = description.takeIf { it.isNotBlank() },
                            category = selectedCategory
                        )
                        onNavigateBack()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Uložit")
            }
        }
    }
} 
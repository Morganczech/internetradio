package cz.internetradio.app.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Download
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cz.internetradio.app.viewmodel.RadioViewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

@Composable
fun SettingsScreen(
    viewModel: RadioViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToEqualizer: () -> Unit,
    onNavigateToAddRadio: () -> Unit
) {
    val maxFavorites by viewModel.maxFavorites.collectAsState()
    val equalizerEnabled by viewModel.equalizerEnabled.collectAsState()
    val fadeOutDuration by viewModel.fadeOutDuration.collectAsState()
    var showFadeOutDialog by remember { mutableStateOf(false) }
    
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { viewModel.exportSettings(it) }
    }
    
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.importSettings(it) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top App Bar
        TopAppBar(
            title = { Text("Nastavení") },
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

        // Obsah nastavení
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Přidat stanici
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onNavigateToAddRadio)
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Přidat stanici",
                        style = MaterialTheme.typography.subtitle1,
                        color = Color.White
                    )
                }
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = "Přejít na přidání stanice",
                    tint = Color.White.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = Color.White.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Maximální počet oblíbených stanic",
                style = MaterialTheme.typography.subtitle1,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Slider pro nastavení maximálního počtu oblíbených
            Slider(
                value = maxFavorites.toFloat(),
                onValueChange = { viewModel.setMaxFavorites(it.toInt()) },
                valueRange = 10f..20f,
                steps = 9,
                modifier = Modifier.fillMaxWidth()
            )
            
            Text(
                text = "Aktuální limit: $maxFavorites stanic",
                style = MaterialTheme.typography.caption,
                color = Color.White.copy(alpha = 0.7f)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Poznámka: Pokud snížíte limit pod aktuální počet oblíbených stanic, " +
                      "nebudete moci přidat další, dokud některé neodeberete.",
                style = MaterialTheme.typography.caption,
                color = Color.White.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(32.dp))
            Divider(color = Color.White.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(16.dp))

            // Nastavení doby fade-outu
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showFadeOutDialog = true }
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Doba zeslabení zvuku",
                        style = MaterialTheme.typography.subtitle1,
                        color = Color.White
                    )
                    Text(
                        text = "$fadeOutDuration sekund",
                        style = MaterialTheme.typography.caption,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "Před vypnutím časovače spánku",
                        style = MaterialTheme.typography.caption,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = "Změnit dobu zeslabení",
                    tint = Color.White.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = Color.White.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(16.dp))

            // Tlačítko pro přechod na equalizer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onNavigateToEqualizer)
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Equalizer",
                        style = MaterialTheme.typography.subtitle1,
                        color = Color.White
                    )
                }
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = "Přejít na equalizer",
                    tint = Color.White.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = Color.White.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(16.dp))

            // Export nastavení
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { 
                        exportLauncher.launch("touchradio_settings.json")
                    }
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Upload,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Exportovat nastavení",
                            style = MaterialTheme.typography.subtitle1,
                            color = Color.White
                        )
                        Text(
                            text = "Uložit nastavení a oblíbené stanice do souboru",
                            style = MaterialTheme.typography.caption,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = "Exportovat",
                    tint = Color.White.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = Color.White.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(16.dp))

            // Import nastavení
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { 
                        importLauncher.launch(arrayOf("application/json"))
                    }
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Importovat nastavení",
                            style = MaterialTheme.typography.subtitle1,
                            color = Color.White
                        )
                        Text(
                            text = "Načíst nastavení a oblíbené stanice ze souboru",
                            style = MaterialTheme.typography.caption,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = "Importovat",
                    tint = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }

    if (showFadeOutDialog) {
        AlertDialog(
            onDismissRequest = { showFadeOutDialog = false },
            title = { Text("Doba zeslabení zvuku") },
            text = {
                Column {
                    Text(
                        text = "Nastavte dobu, po kterou se bude postupně snižovat hlasitost před vypnutím časovače spánku.",
                        style = MaterialTheme.typography.body2,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    listOf(30, 60, 90, 120).forEach { seconds ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setFadeOutDuration(seconds)
                                    showFadeOutDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = fadeOutDuration == seconds,
                                onClick = {
                                    viewModel.setFadeOutDuration(seconds)
                                    showFadeOutDialog = false
                                }
                            )
                            Text(
                                text = "$seconds sekund",
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFadeOutDialog = false }) {
                    Text("Zavřít")
                }
            }
        )
    }
} 
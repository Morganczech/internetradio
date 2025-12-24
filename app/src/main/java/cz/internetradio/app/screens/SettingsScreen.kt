package cz.internetradio.app.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import cz.internetradio.app.R
import cz.internetradio.app.viewmodel.RadioViewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import cz.internetradio.app.model.Language

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
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showClearDataDialog by remember { mutableStateOf(false) }
    val currentLanguage by viewModel.currentLanguage.collectAsState()
    val scrollState = rememberScrollState()
    
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
            title = { Text(stringResource(R.string.settings_title)) },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = stringResource(R.string.nav_back),
                        tint = MaterialTheme.colors.onSurface
                    )
                }
            },
            backgroundColor = MaterialTheme.colors.surface,
            elevation = 0.dp
        )

        // Obsah nastavení
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
        ) {
            // Výběr jazyka
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showLanguageDialog = true }
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = null,
                        tint = MaterialTheme.colors.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.settings_language),
                            style = MaterialTheme.typography.subtitle1,
                            color = MaterialTheme.colors.onSurface
                        )
                        Text(
                            text = stringResource(currentLanguage.nameRes),
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

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
                        tint = MaterialTheme.colors.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = stringResource(R.string.settings_add_station),
                        style = MaterialTheme.typography.subtitle1,
                        color = MaterialTheme.colors.onSurface
                    )
                }
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            Text(
                text = stringResource(R.string.settings_max_favorites),
                style = MaterialTheme.typography.subtitle1,
                color = MaterialTheme.colors.onSurface,
                modifier = Modifier.padding(top = 16.dp)
            )
            
            // Slider
            Slider(
                value = maxFavorites.toFloat(),
                onValueChange = { viewModel.setMaxFavorites(it.toInt()) },
                valueRange = 10f..50f,
                steps = 39,
                modifier = Modifier.fillMaxWidth()
            )
            
            Text(
                text = stringResource(R.string.settings_current_limit, maxFavorites),
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
            )
            
            Text(
                text = stringResource(R.string.settings_favorites_note),
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 8.dp)
            )

            Divider(modifier = Modifier.padding(vertical = 16.dp))

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
                        text = stringResource(R.string.settings_fade_duration),
                        style = MaterialTheme.typography.subtitle1,
                        color = MaterialTheme.colors.onSurface
                    )
                    Text(
                        text = stringResource(R.string.settings_seconds_format, fadeOutDuration),
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                    )
                }
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Equalizer
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
                        tint = MaterialTheme.colors.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = stringResource(R.string.nav_equalizer),
                        style = MaterialTheme.typography.subtitle1,
                        color = MaterialTheme.colors.onSurface
                    )
                }
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Export
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { 
                        val date = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd").format(java.time.LocalDate.now())
                        exportLauncher.launch("touchradio_settings_$date.json") 
                    }
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Upload, null, tint = MaterialTheme.colors.onSurface, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(stringResource(R.string.settings_export), style = MaterialTheme.typography.subtitle1, color = MaterialTheme.colors.onSurface)
                }
                Icon(Icons.Default.KeyboardArrowRight, null, tint = MaterialTheme.colors.onSurface.copy(alpha = 0.7f))
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Import
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { importLauncher.launch(arrayOf("application/json")) }
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Download, null, tint = MaterialTheme.colors.onSurface, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(stringResource(R.string.settings_import), style = MaterialTheme.typography.subtitle1, color = MaterialTheme.colors.onSurface)
                }
                Icon(Icons.Default.KeyboardArrowRight, null, tint = MaterialTheme.colors.onSurface.copy(alpha = 0.7f))
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Vymazat data
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showClearDataDialog = true }
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Delete, null, tint = Color.Red, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Vymazat data aplikace", style = MaterialTheme.typography.subtitle1, color = Color.Red)
                        Text("Vymaže všechna data a nastavení", style = MaterialTheme.typography.caption, color = Color.Red.copy(alpha = 0.7f))
                    }
                }
                Icon(Icons.Default.KeyboardArrowRight, null, tint = Color.Red.copy(alpha = 0.7f))
            }
        }
    }

    // Dialogy - opravené barvy textu
    if (showFadeOutDialog) {
        AlertDialog(
            onDismissRequest = { showFadeOutDialog = false },
            title = { Text("Doba zeslabení zvuku") },
            text = {
                Column {
                    Text("Nastavte dobu zeslabení před vypnutím.", style = MaterialTheme.typography.body2)
                    listOf(30, 60, 90, 120).forEach { seconds ->
                        Row(Modifier.fillMaxWidth().clickable { viewModel.setFadeOutDuration(seconds); showFadeOutDialog = false }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = fadeOutDuration == seconds, onClick = { viewModel.setFadeOutDuration(seconds); showFadeOutDialog = false })
                            Text("$seconds sekund", modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showFadeOutDialog = false }) { Text("Zavřít") } }
        )
    }

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(stringResource(R.string.settings_language)) },
            text = {
                Column {
                    Language.values().forEach { language ->
                        Row(Modifier.fillMaxWidth().clickable { viewModel.setLanguage(language); showLanguageDialog = false }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = language == currentLanguage, onClick = { viewModel.setLanguage(language); showLanguageDialog = false })
                            Spacer(Modifier.width(16.dp))
                            Text(text = stringResource(language.nameRes), style = MaterialTheme.typography.body1)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showLanguageDialog = false }) { Text(stringResource(R.string.action_close)) } }
        )
    }

    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            title = { Text("Vymazat data aplikace") },
            text = { Text("Opravdu chcete vymazat všechna data aplikace? Tato akce je nevratná.", style = MaterialTheme.typography.body1) },
            confirmButton = { TextButton(onClick = { viewModel.clearAllData(); showClearDataDialog = false }, colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)) { Text("Vymazat") } },
            dismissButton = { TextButton(onClick = { showClearDataDialog = false }) { Text("Zrušit") } }
        )
    }
}

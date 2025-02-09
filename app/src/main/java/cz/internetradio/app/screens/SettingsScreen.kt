package cz.internetradio.app.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cz.internetradio.app.viewmodel.RadioViewModel

@Composable
fun SettingsScreen(
    viewModel: RadioViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToEqualizer: () -> Unit
) {
    val maxFavorites by viewModel.maxFavorites.collectAsState()
    
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
                valueRange = 5f..20f,
                steps = 14,
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
        }
    }
} 
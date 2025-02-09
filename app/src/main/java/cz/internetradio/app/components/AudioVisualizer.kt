package cz.internetradio.app.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.StateFlow

@Composable
fun AudioVisualizer(
    modifier: Modifier = Modifier,
    startColor: Color,
    endColor: Color,
    spectrumData: StateFlow<FloatArray>,
    isPlaying: Boolean
) {
    val spectrum by spectrumData.collectAsState()
    
    Canvas(modifier = modifier.fillMaxSize()) {
        val barWidth = size.width / (spectrum.size * 2f)
        val spacing = barWidth
        val cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
        
        val gradient = Brush.verticalGradient(
            colors = listOf(startColor, endColor),
            startY = 0f,
            endY = size.height
        )

        spectrum.forEachIndexed { index, magnitude ->
            val height = if (isPlaying) magnitude * size.height else 0f
            val x = index * (barWidth + spacing) + (size.width - spectrum.size * (barWidth + spacing)) / 2

            drawRoundRect(
                brush = gradient,
                topLeft = Offset(x, size.height - height),
                size = Size(barWidth, height),
                cornerRadius = cornerRadius
            )
        }
    }
} 
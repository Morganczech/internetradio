package cz.internetradio.app.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.random.Random

@Composable
fun AudioVisualizer(
    modifier: Modifier = Modifier,
    startColor: Color,
    endColor: Color,
    isPlaying: Boolean
) {
    val barCount = 32
    val infiniteTransition = rememberInfiniteTransition()
    
    // Animované hodnoty pro každý sloupec
    val barHeights = List(barCount) { index ->
        val delay = index * 100
        val animatedValue by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 1000,
                    easing = FastOutSlowInEasing,
                    delayMillis = delay
                ),
                repeatMode = RepeatMode.Reverse
            )
        )
        if (isPlaying) {
            0.2f + (animatedValue * 0.6f) * Random.nextFloat()
        } else {
            0.1f
        }
    }

    Canvas(modifier = modifier) {
        val barWidth = size.width / (barCount * 2f)
        val gradient = Brush.verticalGradient(
            colors = listOf(startColor, endColor),
            startY = 0f,
            endY = size.height
        )

        barHeights.forEachIndexed { index, height ->
            val x = index * (barWidth * 2f) + barWidth / 2f
            val barHeight = size.height * height
            
            drawRect(
                brush = gradient,
                topLeft = Offset(x, size.height - barHeight),
                size = Size(barWidth, barHeight)
            )
        }
    }
} 
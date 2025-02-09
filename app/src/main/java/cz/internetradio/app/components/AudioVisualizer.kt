package cz.internetradio.app.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.cos
import android.util.Log
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.drawscope.scale

private const val TAG = "AudioVisualizer"

@Composable
fun AudioVisualizer(
    modifier: Modifier = Modifier,
    baseColor1: Color,
    baseColor2: Color,
    isPlaying: Boolean
) {
    var phase by remember { mutableStateOf(0f) }
    var errorState by remember { mutableStateOf<String?>(null) }
    
    // Pulzující animace
    val pulseAnim = rememberInfiniteTransition(label = "pulse")
    val scale by pulseAnim.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    // Rotační animace
    val rotationAnim = rememberInfiniteTransition(label = "rotation")
    val rotation by rotationAnim.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing)
        ),
        label = "rotation"
    )
    
    DisposableEffect(Unit) {
        Log.d(TAG, "AudioVisualizer composable initialized")
        onDispose {
            Log.d(TAG, "AudioVisualizer composable disposed")
        }
    }
    
    // Animace fáze
    LaunchedEffect(isPlaying) {
        Log.d(TAG, "LaunchedEffect started, isPlaying: $isPlaying")
        try {
            while (isPlaying) {
                phase = (phase + 2f) % 360f
                delay(16) // ~60fps
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in animation loop", e)
            errorState = e.message
        }
    }
    
    Canvas(modifier = modifier.fillMaxSize()) {
        val colors = listOf(
            baseColor1,
            baseColor1.copy(alpha = 0.7f),
            baseColor2.copy(alpha = 0.7f),
            baseColor2
        )
        
        // Vytvoření několika vln s různými parametry
        val createWavePath = { offset: Float, frequency: Float, amplitude: Float ->
            Path().apply {
                val phaseRadians = (phase + offset) * PI.toFloat() / 180f
                val steps = 50
                val baseAmplitude = if (isPlaying) size.height * amplitude else 0f
                
                moveTo(0f, size.height)
                
                for (i in 0..steps) {
                    val x = size.width * i / steps
                    val progress = i.toFloat() / steps
                    
                    val y = size.height * 0.5f + 
                            sin(progress * frequency * PI.toFloat() + phaseRadians) * baseAmplitude +
                            cos(progress * (frequency/2) * PI.toFloat() - phaseRadians) * (baseAmplitude * 0.5f)
                    
                    lineTo(x, y)
                }
                
                lineTo(size.width, size.height)
                close()
            }
        }
        
        // Aplikace scale transformace
        scale(scale) {
            // Vykreslení několika vln s různými parametry
            val waves = listOf(
                Triple(0f, 2f, 0.1f),      // Základní vlna
                Triple(45f, 3f, 0.08f),    // Rychlejší, menší vlna
                Triple(90f, 1.5f, 0.12f),  // Pomalejší, větší vlna
                Triple(135f, 4f, 0.06f)    // Nejrychlejší, nejmenší vlna
            )
            
            waves.forEachIndexed { index, (offset, freq, amp) ->
                val alpha = 1f - (index * 0.2f)
                drawPath(
                    path = createWavePath(offset + rotation, freq, amp),
                    brush = Brush.verticalGradient(
                        colors = colors.map { it.copy(alpha = it.alpha * alpha) },
                        startY = 0f,
                        endY = size.height
                    )
                )
            }
        }
    }
} 
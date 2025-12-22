package cz.internetradio.app.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt
import cz.internetradio.app.model.Radio

@Composable
fun DraggableRadioItem(
    dragDropState: DragDropState,
    index: Int,
    radio: Radio,
    modifier: Modifier = Modifier,
    content: @Composable (isDragging: Boolean) -> Unit
) {
    val initial = dragDropState.initialIndex
    val target = dragDropState.targetIndex
    val isDragging = dragDropState.draggingItemId == radio.id
    
    // Odhadovaná výška karty v pixelech (vč. paddingu a mezer)
    // Pro produkční nasazení by bylo lepší měřit přes onGloballyPositioned,
    // ale pro stabilitu a fixní rádio karty je konstanta bezpečnější.
    val itemHeightPx = 280f 

    val translationY = when {
        isDragging -> dragDropState.draggedOffset
        // Posun ostatních nahoru při tažení dolů
        initial != null && target != null && target > initial && index > initial && index <= target -> -itemHeightPx
        // Posun ostatních dolů při tažení nahoru
        initial != null && target != null && target < initial && index < initial && index >= target -> itemHeightPx
        else -> 0f
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                this.translationY = translationY
                this.shadowElevation = if (isDragging) 8.dp.toPx() else 0f
                this.alpha = if (isDragging) 0.9f else 1f
            }
            .zIndex(if (isDragging) 10f else 0f)
    ) {
        content(isDragging)
    }
}

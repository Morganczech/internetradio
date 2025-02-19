package cz.internetradio.app.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.zIndex
import cz.internetradio.app.screens.DragDropState
import android.util.Log
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DraggableRadioItem(
    dragDropState: DragDropState,
    index: Int,
    listSize: Int,
    itemModifier: Modifier = Modifier,
    content: @Composable (isDragging: Boolean) -> Unit
) {
    val isDragging = dragDropState.draggingItemIndex == index
    val isTarget = dragDropState.draggedOverItemIndex == index

    Box(
        modifier = Modifier
            .then(if (!isDragging) itemModifier else Modifier)
            .offset { 
                IntOffset(
                    x = 0, 
                    y = if (isDragging) dragDropState.draggedOffset.roundToInt() else 0
                ) 
            }
            .zIndex(when {
                isDragging -> 3f  // Přetahovaná karta nejvýše
                isTarget -> 2f    // Cílová pozice uprostřed
                else -> 1f        // Ostatní karty nejníže
            })
            .pointerInput(dragDropState) {
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        Log.d("DraggableRadioItem", "Drag started on item $index")
                        dragDropState.onDragStart(index)
                    },
                    onDragEnd = {
                        Log.d("DraggableRadioItem", "Drag ended on item $index")
                        dragDropState.onDragEnd()
                    },
                    onDragCancel = {
                        Log.d("DraggableRadioItem", "Drag cancelled on item $index")
                        dragDropState.onDragEnd()
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        Log.d("DraggableRadioItem", "Dragging item $index, dragAmount: ${dragAmount.y}")
                        dragDropState.onDraggedOver(index, dragAmount.y, listSize)
                    }
                )
            }
    ) {
        content(isDragging)
    }
} 
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
import cz.internetradio.app.model.Radio

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DraggableRadioItem(
    dragDropState: DragDropState,
    index: Int,
    listSize: Int,
    radio: Radio,
    items: List<Radio>,
    itemModifier: Modifier = Modifier,
    content: @Composable (isDragging: Boolean) -> Unit
) {
    val isDragging = dragDropState.draggingItemId == radio.id
    val isTarget = dragDropState.draggedOverItemId == radio.id
    val offsetY = if (isDragging) dragDropState.draggedOffset else 0f

    Box(
        modifier = Modifier
            .then(itemModifier)
            .offset { IntOffset(0, offsetY.roundToInt()) }
            .zIndex(when {
                isDragging -> 2f  // Přetahovaná karta nejvýše
                isTarget -> 1f    // Cílová pozice uprostřed
                else -> 0f        // Ostatní karty nejníže
            })
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset ->
                        Log.d("DraggableRadioItem", "Drag started on item ${radio.id} at ${offset.y}")
                        dragDropState.onDragStart(index, offset.y, radio.id)
                    },
                    onDragEnd = {
                        Log.d("DraggableRadioItem", "Drag ended on item ${radio.id}")
                        dragDropState.onDragEnd()
                    },
                    onDragCancel = {
                        Log.d("DraggableRadioItem", "Drag cancelled on item ${radio.id}")
                        dragDropState.onDragEnd()
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        Log.d("DraggableRadioItem", "Dragging item ${radio.id} at ${change.position.y}")
                        dragDropState.onDraggedOver(change.position.y, listSize, items)
                    }
                )
            }
    ) {
        content(isDragging)
    }
} 
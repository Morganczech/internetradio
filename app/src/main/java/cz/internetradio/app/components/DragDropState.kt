package cz.internetradio.app.components

import androidx.compose.runtime.*
import cz.internetradio.app.model.Radio

@Composable
fun rememberDragDropState(
    onMove: (Int, Int) -> Unit
): DragDropState {
    return remember {
        DragDropState { fromIndex, toIndex ->
            onMove(fromIndex, toIndex)
        }
    }
}

class DragDropState(
    private val onMove: (Int, Int) -> Unit
) {
    var draggingItemId by mutableStateOf<String?>(null)
        private set

    var initialIndex by mutableStateOf<Int?>(null)
        private set

    var targetIndex by mutableStateOf<Int?>(null)
        private set

    var draggedOffset by mutableStateOf(0f)
        private set

    private val itemHeight = 280f // Musí odpovídat výšce v DraggableRadioItem

    fun onDragStart(index: Int, itemId: String) {
        this.draggingItemId = itemId
        this.initialIndex = index
        this.targetIndex = index
        this.draggedOffset = 0f
    }

    fun onDragged(dragAmountY: Float, listSize: Int, onTargetChanged: () -> Unit) {
        val initial = initialIndex ?: return
        
        // Akumulujeme posun prstu
        draggedOffset += dragAmountY

        // Výpočet nové cílové pozice
        val deltaIndex = (draggedOffset / itemHeight).toInt()
        val newTargetIndex = (initial + deltaIndex).coerceIn(0, listSize - 1)
        
        if (targetIndex != newTargetIndex) {
            targetIndex = newTargetIndex
            onTargetChanged()
        }
    }

    fun onDragEnd() {
        val from = initialIndex
        val to = targetIndex
        
        if (from != null && to != null && from != to) {
            onMove(from, to)
        }
        reset()
    }

    fun onDragCancel() {
        reset()
    }

    private fun reset() {
        draggingItemId = null
        targetIndex = null
        initialIndex = null
        draggedOffset = 0f
    }
}

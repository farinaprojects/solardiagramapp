package br.com.solardiagram.ui.screens.editor

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import br.com.solardiagram.domain.model.Component
import br.com.solardiagram.domain.model.Point2

@Stable
class EditorCanvasInteractionState {
    var localPan by mutableStateOf<Point2?>(null)
    var localScale by mutableStateOf<Float?>(null)


    var boxSelecting by mutableStateOf(false)
    var boxStart by mutableStateOf<Offset?>(null)
    var boxEnd by mutableStateOf<Offset?>(null)

    var guideX by mutableStateOf<Float?>(null)
    var guideY by mutableStateOf<Float?>(null)

    var palettePreviewComponent by mutableStateOf<Component?>(null)
    var palettePreviewWorld by mutableStateOf<Point2?>(null)

    var overridePositions by mutableStateOf<Map<String, Point2>?>(null)
    var draggingIds by mutableStateOf<Set<String>>(emptySet())
    var pinnedSelectionIds by mutableStateOf<Set<String>>(emptySet())

    fun effectivePan(statePan: Point2): Point2 = localPan ?: statePan

    fun effectiveScale(stateScale: Float): Float = localScale ?: stateScale

    fun selectionRect(): Rect? {
        val a = boxStart ?: return null
        val b = boxEnd ?: return null
        return screenRect(a, b)
    }

    fun clearPinnedSelection() {
        pinnedSelectionIds = emptySet()
    }

    fun clearSelectionBox() {
        boxSelecting = false
        boxStart = null
        boxEnd = null
    }

    fun showPalettePreview(component: Component, world: Point2) {
        palettePreviewComponent = component
        palettePreviewWorld = world
    }

    fun clearPalettePreview() {
        palettePreviewComponent = null
        palettePreviewWorld = null
    }

    fun resetTransientVisuals() {
        localPan = null
        localScale = null
        clearPalettePreview()
        overridePositions = null
        draggingIds = emptySet()
        guideX = null
        guideY = null
    }

    fun resetAfterHistoryNavigation() {
        resetTransientVisuals()
        clearSelectionBox()
    }
}

fun screenRect(a: Offset, b: Offset): Rect {
    val left = minOf(a.x, b.x)
    val top = minOf(a.y, b.y)
    val right = maxOf(a.x, b.x)
    val bottom = maxOf(a.y, b.y)
    return Rect(left, top, right, bottom)
}

@androidx.compose.runtime.Composable
fun rememberEditorCanvasInteractionState(): EditorCanvasInteractionState = remember {
    EditorCanvasInteractionState()
}
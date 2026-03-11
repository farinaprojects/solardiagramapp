package br.com.solardiagram.ui.screens.editor

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import br.com.solardiagram.domain.model.Component
import br.com.solardiagram.domain.model.Point2
import br.com.solardiagram.ui.viewmodel.CanvasHighlights
import br.com.solardiagram.ui.viewmodel.EditorUiState

data class EditorCanvasFrame(
    val components: List<Component>,
    val pan: Point2,
    val scale: Float,
    val draggingIds: Set<String>,
    val selectionBox: Rect?,
    val guideWorldX: Float?,
    val guideWorldY: Float?,
    val previewComponent: Component?
)

object EditorCanvasSceneRenderer {

    fun DrawScope.render(
        frame: EditorCanvasFrame,
        state: EditorUiState,
        previewStartPort: Pair<String, String>?,
        previewEndWorld: Point2?,
        previewSnapTarget: Pair<String, String>?,
        highlights: CanvasHighlights,
        visualSelectedIds: Set<String>
    ) {
        drawGrid(size.width, size.height, frame.scale, frame.pan)
        drawGuides(frame.guideWorldX, frame.guideWorldY, frame.scale, frame.pan)
        drawConnections(frame, state, highlights)
        drawPreviewWire(frame, previewStartPort, previewEndWorld, previewSnapTarget)
        drawPalettePreview(frame)
        drawComponents(frame, state, highlights, visualSelectedIds)
        drawPreviewSnap(frame, previewSnapTarget)
        drawSelectionBox(frame.selectionBox)
    }

    private fun DrawScope.drawGuides(
        guideWorldX: Float?,
        guideWorldY: Float?,
        scale: Float,
        pan: Point2
    ) {
        guideWorldX?.let { wx ->
            val sx = wx * scale + pan.x
            drawLine(
                color = Color(0x33007AFF),
                start = Offset(sx, 0f),
                end = Offset(sx, size.height),
                strokeWidth = 2f
            )
        }

        guideWorldY?.let { wy ->
            val sy = wy * scale + pan.y
            drawLine(
                color = Color(0x33007AFF),
                start = Offset(0f, sy),
                end = Offset(size.width, sy),
                strokeWidth = 2f
            )
        }
    }

    private fun DrawScope.drawConnections(
        frame: EditorCanvasFrame,
        state: EditorUiState,
        highlights: CanvasHighlights
    ) {
        WireRenderer.drawWires(
            drawScope = this,
            connections = state.project.connections,
            components = frame.components,
            scale = frame.scale,
            pan = frame.pan,
            errorConnectionIds = highlights.errorConnectionIds,
            warnConnectionIds = highlights.warnConnectionIds,
            selectedConnectionId = state.selectedConnectionId
        )
    }

    private fun DrawScope.drawPreviewWire(
        frame: EditorCanvasFrame,
        previewStartPort: Pair<String, String>?,
        previewEndWorld: Point2?,
        previewSnapTarget: Pair<String, String>?
    ) {
        if (previewStartPort == null || previewEndWorld == null) return

        val (fromCompId, fromPortId) = previewStartPort
        val fromComp = frame.components.firstOrNull { it.id == fromCompId }
        val fromScreen = fromComp?.let {
            ComponentRenderer.portCanvasPosition(
                component = it,
                portId = fromPortId,
                scale = frame.scale,
                pan = frame.pan
            )
        }

        val endScreen = Offset(
            x = previewEndWorld.x * frame.scale + frame.pan.x,
            y = previewEndWorld.y * frame.scale + frame.pan.y
        )

        if (fromScreen != null) {
            WireRenderer.drawPreview(
                drawScope = this,
                from = fromScreen,
                to = endScreen,
                highlight = previewSnapTarget != null
            )
        }
    }

    private fun DrawScope.drawPalettePreview(frame: EditorCanvasFrame) {
        val preview = frame.previewComponent ?: return
        val previewBounds = Rect(
            left = 0f,
            top = 0f,
            right = size.width,
            bottom = size.height
        )

        drawContext.canvas.saveLayer(previewBounds, Paint().apply { alpha = 0.45f })
        try {
            ComponentRenderer.drawComponent(
                drawScope = this,
                component = preview,
                scale = frame.scale,
                pan = frame.pan,
                isSelected = false,
                isDragging = true,
                isError = false,
                isWarning = false
            )
        } finally {
            drawContext.canvas.restore()
        }
    }

    private fun DrawScope.drawComponents(
        frame: EditorCanvasFrame,
        state: EditorUiState,
        highlights: CanvasHighlights,
        visualSelectedIds: Set<String>
    ) {
        frame.components.forEach { component ->
            val isError = component.id in highlights.errorComponentIds
            val isWarning = component.id in highlights.warnComponentIds

            ComponentRenderer.drawComponent(
                drawScope = this,
                component = component,
                scale = frame.scale,
                pan = frame.pan,
                isSelected = component.id in visualSelectedIds,
                isDragging = component.id in frame.draggingIds,
                isError = isError,
                isWarning = (!isError && isWarning)
            )
        }
    }

    private fun DrawScope.drawPreviewSnap(
        frame: EditorCanvasFrame,
        previewSnapTarget: Pair<String, String>?
    ) {
        previewSnapTarget ?: return
        val (componentId, portId) = previewSnapTarget

        val component = frame.components.firstOrNull { it.id == componentId }
        val port = component?.portById(portId)

        if (component != null && port != null) {
            val index = component.ports.indexOfFirst { it.id == portId }
            val portWorld = ComponentRenderer.portWorldPosition(component, port, index)
            val portScreen = Offset(
                x = portWorld.x * frame.scale + frame.pan.x,
                y = portWorld.y * frame.scale + frame.pan.y
            )

            drawCircle(
                color = Color(0xFF43A047),
                radius = 14f,
                center = portScreen,
                alpha = 0.25f
            )
            drawCircle(
                color = Color(0xFF43A047),
                radius = 9f,
                center = portScreen,
                alpha = 0.45f
            )
        }
    }

    private fun DrawScope.drawSelectionBox(selectionBox: Rect?) {
        selectionBox ?: return
        drawRect(
            color = Color(0x332196F3),
            topLeft = Offset(selectionBox.left, selectionBox.top),
            size = Size(selectionBox.width, selectionBox.height)
        )

        drawRect(
            color = Color(0xFF1E88E5),
            topLeft = Offset(selectionBox.left, selectionBox.top),
            size = Size(selectionBox.width, selectionBox.height),
            style = Stroke(width = 2f)
        )
    }
}

private fun DrawScope.drawGrid(
    width: Float,
    height: Float,
    scale: Float,
    pan: Point2
) {
    val gridSizePx = (32 * scale).coerceAtLeast(12f)
    val left = -pan.x % gridSizePx
    val top = -pan.y % gridSizePx
    val stroke = 1f

    var x = left
    while (x <= width) {
        drawLine(
            color = Color(0xFFEEEEEE),
            start = Offset(x, 0f),
            end = Offset(x, height),
            strokeWidth = stroke
        )
        x += gridSizePx
    }

    var y = top
    while (y <= height) {
        drawLine(
            color = Color(0xFFEEEEEE),
            start = Offset(0f, y),
            end = Offset(width, y),
            strokeWidth = stroke
        )
        y += gridSizePx
    }
}
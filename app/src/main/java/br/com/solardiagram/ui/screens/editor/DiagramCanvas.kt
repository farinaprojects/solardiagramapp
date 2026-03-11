package br.com.solardiagram.ui.screens.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import br.com.solardiagram.domain.model.Component
import br.com.solardiagram.domain.model.Point2
import br.com.solardiagram.ui.viewmodel.CanvasHighlights
import br.com.solardiagram.ui.viewmodel.EditorUiState

@Composable
fun DiagramCanvas(
    modifier: Modifier = Modifier,
    state: EditorUiState,
    previewStartPort: Pair<String, String>?,
    previewEndWorld: Point2?,
    previewSnapTarget: Pair<String, String>?,
    highlights: CanvasHighlights,
    panOverride: Point2? = null,
    scaleOverride: Float? = null,
    draggingComponentId: String? = null,
    draggingWorldPos: Point2? = null,
    overridePositions: Map<String, Point2>? = null,
    selectionBox: Rect? = null,
    guideWorldX: Float? = null,
    guideWorldY: Float? = null,
    previewComponentTemplate: Component? = null,
    previewComponentWorldPos: Point2? = null,
    visualSelectedIds: Set<String> = state.selectedComponentIds
) {
    val frame = buildEditorCanvasFrame(
        state = state,
        panOverride = panOverride,
        scaleOverride = scaleOverride,
        draggingComponentId = draggingComponentId,
        draggingWorldPos = draggingWorldPos,
        overridePositions = overridePositions,
        selectionBox = selectionBox,
        guideWorldX = guideWorldX,
        guideWorldY = guideWorldY,
        previewComponentTemplate = previewComponentTemplate,
        previewComponentWorldPos = previewComponentWorldPos
    )

    Box(modifier = modifier.background(MaterialTheme.colorScheme.background)) {
        Canvas(Modifier.fillMaxSize()) {
            with(EditorCanvasSceneRenderer) {
                render(
                    frame = frame,
                    state = state,
                    previewStartPort = previewStartPort,
                    previewEndWorld = previewEndWorld,
                    previewSnapTarget = previewSnapTarget,
                    highlights = highlights,
                    visualSelectedIds = visualSelectedIds
                )
            }
        }
    }
}

private fun buildEditorCanvasFrame(
    state: EditorUiState,
    panOverride: Point2?,
    scaleOverride: Float?,
    draggingComponentId: String?,
    draggingWorldPos: Point2?,
    overridePositions: Map<String, Point2>?,
    selectionBox: Rect?,
    guideWorldX: Float?,
    guideWorldY: Float?,
    previewComponentTemplate: Component?,
    previewComponentWorldPos: Point2?
): EditorCanvasFrame {
    val effectivePan = panOverride ?: state.pan
    val effectiveScale = scaleOverride ?: state.scale

    val effectiveComponents = state.project.components.map { component ->
        val posOverride = overridePositions?.get(component.id)
            ?: if (component.id == draggingComponentId) draggingWorldPos else null

        if (posOverride != null) {
            component.copy(transform = component.transform.copy(position = posOverride))
        } else {
            component
        }
    }

    val draggingIds = overridePositions?.keys
        ?: if (draggingComponentId != null) setOf(draggingComponentId) else emptySet()

    return EditorCanvasFrame(
        components = effectiveComponents,
        pan = effectivePan,
        scale = effectiveScale,
        draggingIds = draggingIds,
        selectionBox = selectionBox,
        guideWorldX = guideWorldX,
        guideWorldY = guideWorldY,
        previewComponent = previewComponentTemplate?.copy(
            transform = previewComponentTemplate.transform.copy(
                position = previewComponentWorldPos ?: previewComponentTemplate.transform.position
            )
        )
    )
}
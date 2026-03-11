package br.com.solardiagram.ui.screens.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.dp
import br.com.solardiagram.domain.model.Component

@Composable
fun PaletteDraggableItem(
    label: String,
    templateFactory: () -> Component,
    onDrop: (Component, Offset) -> Unit,
    onPreviewStart: ((Component, Offset) -> Unit)? = null,
    onPreviewMove: ((Component, Offset) -> Unit)? = null,
    onPreviewEnd: (() -> Unit)? = null
) {
    var dragging by remember { mutableStateOf(false) }
    var itemOriginInWindow by remember { mutableStateOf(Offset.Zero) }
    var dragPositionInWindow by remember { mutableStateOf(Offset.Zero) }
    var activeTemplate by remember { mutableStateOf<Component?>(null) }

    Box(
        modifier = Modifier
            .onGloballyPositioned { coords ->
                itemOriginInWindow = coords.positionInWindow()
            }
            .fillMaxWidth()
            .background(
                color = if (dragging) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
                } else {
                    MaterialTheme.colorScheme.surface
                },
                shape = RoundedCornerShape(6.dp)
            )
            .border(
                width = 1.dp,
                color = if (dragging) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                },
                shape = RoundedCornerShape(6.dp)
            )
            .pointerInput(label) {
                detectDragGestures(
                    onDragStart = { offset ->
                        dragging = true
                        val template = templateFactory()
                        activeTemplate = template
                        dragPositionInWindow = itemOriginInWindow + offset
                        onPreviewStart?.invoke(template, dragPositionInWindow)
                    },
                    onDragCancel = {
                        dragging = false
                        activeTemplate = null
                        onPreviewEnd?.invoke()
                    },
                    onDragEnd = {
                        val template = activeTemplate
                        val windowPos = dragPositionInWindow
                        dragging = false
                        activeTemplate = null
                        onPreviewEnd?.invoke()
                        if (template != null) {
                            onDrop(template, windowPos)
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragPositionInWindow += dragAmount
                        activeTemplate?.let { template ->
                            onPreviewMove?.invoke(template, dragPositionInWindow)
                        }
                    }
                )
            }
            .padding(10.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
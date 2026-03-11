package br.com.solardiagram.ui.screens.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun EditorQuickToolbar(
    modifier: Modifier = Modifier,
    selectionContext: EditorSelectionContext,
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onZoomOut: () -> Unit,
    onZoomIn: () -> Unit,
    onResetZoom: () -> Unit,
    onFitAll: () -> Unit,
    onAlignHorizontal: (() -> Unit)? = null,
    onAlignVertical: (() -> Unit)? = null,
    onDistributeHorizontal: (() -> Unit)? = null,
    onDistributeVertical: (() -> Unit)? = null,
    onDeleteSelection: (() -> Unit)? = null,
    onDuplicateSelection: (() -> Unit)? = null,
    onRotateSelection: (() -> Unit)? = null
) {
    val showZoomActions = selectionContext is EditorSelectionContext.None
    val showArrangementActions =
        selectionContext is EditorSelectionContext.MultipleComponents &&
                selectionContext.count >= 2

    Surface(
        modifier = modifier.wrapContentWidth(),
        tonalElevation = 4.dp,
        shadowElevation = 4.dp,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 1.dp, vertical = 1.dp),
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            ToolbarSymbolButton(
                symbol = "⟲",
                enabled = canUndo,
                onClick = onUndo
            )

            ToolbarSymbolButton(
                symbol = "⟳",
                enabled = canRedo,
                onClick = onRedo
            )

            if (showZoomActions) {
                ToolbarSymbolButton(symbol = "−", onClick = onZoomOut)
                ToolbarSymbolButton(symbol = "+", onClick = onZoomIn)
                ToolbarCompactButton(symbol = "1:1", onClick = onResetZoom)
                ToolbarSymbolButton(symbol = "⛶", onClick = onFitAll)
            }

            if (showArrangementActions) {
                ToolbarCompactButton(symbol = "⋯", onClick = onAlignHorizontal)
                ToolbarCompactButton(symbol = "⋮", onClick = onAlignVertical)
                ToolbarCompactButton(symbol = "Ⅲ", onClick = onDistributeHorizontal)
                ToolbarCompactButton(symbol = "≡", onClick = onDistributeVertical)
            }
        }
    }
}

@Composable
private fun ToolbarCompactButton(
    symbol: String,
    onClick: (() -> Unit)?,
    enabled: Boolean = onClick != null
) {
    TextButton(
        onClick = { onClick?.invoke() },
        enabled = enabled,
        modifier = Modifier.defaultMinSize(minWidth = 30.dp, minHeight = 28.dp)
    ) {
        Text(
            text = symbol,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 3
        )
    }
}

@Composable
private fun ToolbarSymbolButton(
    symbol: String,
    onClick: (() -> Unit)?,
    enabled: Boolean = onClick != null
) {
    TextButton(
        onClick = { onClick?.invoke() },
        enabled = enabled,
        modifier = Modifier.defaultMinSize(minWidth = 28.dp, minHeight = 28.dp)
    ) {
        Text(
            text = symbol,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}
package br.com.solardiagram.ui.screens.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SelectionFloatingToolbar(
    modifier: Modifier = Modifier,
    onDuplicate: (() -> Unit)?,
    onDelete: (() -> Unit)?,
    onRotate: (() -> Unit)?
) {
    Surface(
        modifier = modifier,
        tonalElevation = 3.dp,
        shadowElevation = 5.dp,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 1.dp, vertical = 1.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            FloatingToolbarButton(symbol = "⧉", onClick = onDuplicate)
            FloatingToolbarButton(symbol = "⌫", onClick = onDelete)
            FloatingToolbarButton(symbol = "↻", onClick = onRotate)
        }
    }
}

@Composable
fun SelectionBoundsOverlay(
    modifier: Modifier = Modifier,
    borderColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.72f),
    fillColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
) {
    Box(
        modifier = modifier
            .background(fillColor)
            .border(width = 1.5.dp, color = borderColor, shape = RoundedCornerShape(0.dp))
    )
}

@Composable
private fun FloatingToolbarButton(
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

fun unionRect(rects: List<Rect>): Rect? {
    if (rects.isEmpty()) return null
    var left = rects.first().left
    var top = rects.first().top
    var right = rects.first().right
    var bottom = rects.first().bottom
    for (rect in rects.drop(1)) {
        left = minOf(left, rect.left)
        top = minOf(top, rect.top)
        right = maxOf(right, rect.right)
        bottom = maxOf(bottom, rect.bottom)
    }
    return Rect(left, top, right, bottom)
}
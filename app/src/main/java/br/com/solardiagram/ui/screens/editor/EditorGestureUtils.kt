package br.com.solardiagram.ui.screens.editor

import androidx.compose.foundation.gestures.awaitDragOrCancellation
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.consumePositionChange

/**
 * Loop contínuo robusto (1 dedo):
 * - awaitDragOrCancellation mantém o stream ativo
 * - consumePositionChange estabiliza deltas
 * - NÃO consome initialDown (evita “morrer” em alguns touch drivers)
 */
suspend fun AwaitPointerEventScope.dragLoopContinuous(
    initialDown: PointerInputChange,
    onMove: (Offset) -> Unit,
    onUp: (Offset) -> Unit
) {
    val pointerId = initialDown.id
    var lastPos = initialDown.position

    while (true) {
        val drag = awaitDragOrCancellation(pointerId) ?: run {
            onUp(lastPos)
            return
        }

        if (!drag.pressed) {
            onUp(drag.position)
            return
        }

        val pos = drag.position
        if (pos != lastPos) {
            onMove(pos)
            drag.consumePositionChange()
            lastPos = pos
        } else {
            drag.consumePositionChange()
        }
    }
}
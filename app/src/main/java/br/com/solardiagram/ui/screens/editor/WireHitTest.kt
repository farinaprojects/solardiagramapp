package br.com.solardiagram.ui.screens.editor

import androidx.compose.ui.geometry.Offset
import br.com.solardiagram.domain.model.Component
import br.com.solardiagram.domain.model.Connection
import br.com.solardiagram.domain.model.Point2
import kotlin.math.sqrt

object WireHitTest {

    fun hitConnection(
        screenPoint: Offset,
        connections: List<Connection>,
        components: List<Component>,
        scale: Float,
        pan: Point2,
        thresholdPx: Float = 18f
    ): String? {
        connections.forEach { connection ->
            val fromComponent = components.firstOrNull { it.id == connection.fromComponentId } ?: return@forEach
            val toComponent = components.firstOrNull { it.id == connection.toComponentId } ?: return@forEach

            val fromPort = fromComponent.portById(connection.fromPortId) ?: return@forEach
            val toPort = toComponent.portById(connection.toPortId) ?: return@forEach

            val fromIndex = fromComponent.ports.indexOfFirst { it.id == fromPort.id }.coerceAtLeast(0)
            val toIndex = toComponent.ports.indexOfFirst { it.id == toPort.id }.coerceAtLeast(0)

            val fromWorld = ComponentRenderer.portWorldPosition(fromComponent, fromPort, fromIndex)
            val toWorld = ComponentRenderer.portWorldPosition(toComponent, toPort, toIndex)

            val fromScreen = Offset(
                x = fromWorld.x * scale + pan.x,
                y = fromWorld.y * scale + pan.y
            )
            val toScreen = Offset(
                x = toWorld.x * scale + pan.x,
                y = toWorld.y * scale + pan.y
            )

            val distance = distancePointToSegment(screenPoint, fromScreen, toScreen)
            if (distance <= thresholdPx) {
                return connection.id
            }
        }
        return null
    }

    private fun distancePointToSegment(
        p: Offset,
        a: Offset,
        b: Offset
    ): Float {
        val abx = b.x - a.x
        val aby = b.y - a.y
        val apx = p.x - a.x
        val apy = p.y - a.y

        val abLenSq = abx * abx + aby * aby
        if (abLenSq <= 0.0001f) {
            val dx = p.x - a.x
            val dy = p.y - a.y
            return sqrt(dx * dx + dy * dy)
        }

        val t = ((apx * abx) + (apy * aby)) / abLenSq
        val clamped = t.coerceIn(0f, 1f)

        val cx = a.x + clamped * abx
        val cy = a.y + clamped * aby

        val dx = p.x - cx
        val dy = p.y - cy
        return sqrt(dx * dx + dy * dy)
    }
}
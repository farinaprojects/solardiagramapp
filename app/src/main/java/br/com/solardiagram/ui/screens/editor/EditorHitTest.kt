package br.com.solardiagram.ui.screens.editor

import androidx.compose.ui.geometry.Offset
import br.com.solardiagram.domain.model.Component
import br.com.solardiagram.domain.model.Connection
import br.com.solardiagram.domain.model.Point2

sealed interface EditorHitTarget {
    data class Component(val componentId: String) : EditorHitTarget
    data class Port(val componentId: String, val portId: String) : EditorHitTarget
    data class Connection(val connectionId: String) : EditorHitTarget
    data object None : EditorHitTarget
}

fun hitTestEditorTarget(
    components: List<Component>,
    connections: List<Connection>,
    screenPoint: Offset,
    scale: Float,
    pan: Point2,
    portRadiusPx: Float = 16f,
    connectionThresholdPx: Float = 18f
): EditorHitTarget {
    components.asReversed().forEach { component ->
        component.ports.forEachIndexed { index, port ->
            val world = EditorGeometry.portWorldPosition(component, port, index)
            val screen = Offset(
                x = world.x * scale + pan.x,
                y = world.y * scale + pan.y
            )
            val dx = screenPoint.x - screen.x
            val dy = screenPoint.y - screen.y
            if ((dx * dx + dy * dy) <= portRadiusPx * portRadiusPx) {
                return EditorHitTarget.Port(component.id, port.id)
            }
        }
    }

    components.asReversed().forEach { component ->
        val rect = EditorGeometry.componentScreenRect(
            component = component,
            scale = scale,
            pan = pan
        )
        if (rect.contains(screenPoint)) {
            return EditorHitTarget.Component(component.id)
        }
    }

    val connectionId = WireHitTest.hitConnection(
        screenPoint = screenPoint,
        connections = connections,
        components = components,
        scale = scale,
        pan = pan,
        thresholdPx = connectionThresholdPx
    )
    if (connectionId != null) {
        return EditorHitTarget.Connection(connectionId)
    }

    return EditorHitTarget.None
}
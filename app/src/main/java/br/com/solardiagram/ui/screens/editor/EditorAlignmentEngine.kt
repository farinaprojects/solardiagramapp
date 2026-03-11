package br.com.solardiagram.ui.screens.editor

import br.com.solardiagram.domain.model.Component
import br.com.solardiagram.domain.model.Point2

object EditorAlignmentEngine {

    fun alignHorizontally(components: List<Component>): Map<String, Point2> {
        if (components.size < 2) return emptyMap()
        val targetY = components.map { it.transform.position.y }.average().toFloat()
        return components.associate { component ->
            component.id to component.transform.position.copy(y = targetY)
        }
    }

    fun alignVertically(components: List<Component>): Map<String, Point2> {
        if (components.size < 2) return emptyMap()
        val targetX = components.map { it.transform.position.x }.average().toFloat()
        return components.associate { component ->
            component.id to component.transform.position.copy(x = targetX)
        }
    }

    fun distributeHorizontally(components: List<Component>): Map<String, Point2> {
        if (components.size < 3) return emptyMap()

        val sorted = components.sortedBy { it.transform.position.x }
        val firstX = sorted.first().transform.position.x
        val lastX = sorted.last().transform.position.x
        if (sorted.size <= 2 || firstX == lastX) return emptyMap()

        val step = (lastX - firstX) / (sorted.lastIndex)
        return sorted.mapIndexed { index, component ->
            val targetX = firstX + step * index
            component.id to component.transform.position.copy(x = targetX)
        }.toMap()
    }

    fun distributeVertically(components: List<Component>): Map<String, Point2> {
        if (components.size < 3) return emptyMap()

        val sorted = components.sortedBy { it.transform.position.y }
        val firstY = sorted.first().transform.position.y
        val lastY = sorted.last().transform.position.y
        if (sorted.size <= 2 || firstY == lastY) return emptyMap()

        val step = (lastY - firstY) / (sorted.lastIndex)
        return sorted.mapIndexed { index, component ->
            val targetY = firstY + step * index
            component.id to component.transform.position.copy(y = targetY)
        }.toMap()
    }
}

package br.com.solardiagram.domain.engine

import br.com.solardiagram.domain.model.ComponentType
import br.com.solardiagram.domain.model.DiagramProject
import br.com.solardiagram.domain.model.Point2

class DiagramAutoLayoutEngine(
    private val startX: Float = 140f,
    private val startY: Float = 120f,
    private val columnGap: Float = 240f,
    private val rowGap: Float = 160f
) {
    fun arrange(project: DiagramProject): DiagramProject {
        val grouped = project.components.groupBy { columnFor(it.type) }
        val repositioned = project.components.map { component ->
            val column = columnFor(component.type)
            val indexInColumn = grouped[column].orEmpty().indexOfFirst { it.id == component.id }.coerceAtLeast(0)
            component.copy(
                transform = component.transform.copy(
                    position = Point2(
                        x = startX + column * columnGap,
                        y = startY + indexInColumn * rowGap
                    )
                )
            )
        }
        return project.copy(components = repositioned)
    }

    private fun columnFor(type: ComponentType): Int = when (type) {
        ComponentType.GRID_SOURCE -> 0
        ComponentType.PV_MODULE -> 0
        ComponentType.MICROINVERTER, ComponentType.STRING_INVERTER -> 1
        ComponentType.BREAKER, ComponentType.DPS -> 2
        ComponentType.BARL, ComponentType.BARN, ComponentType.BARPE, ComponentType.AC_BUS, ComponentType.GROUND_BAR -> 3
        ComponentType.QDG -> 4
        ComponentType.LOAD -> 5
    }
}

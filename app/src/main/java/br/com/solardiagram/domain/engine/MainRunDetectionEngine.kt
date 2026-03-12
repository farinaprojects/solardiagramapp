package br.com.solardiagram.domain.engine

import br.com.solardiagram.domain.electrical.ElectricalGraph
import br.com.solardiagram.domain.model.Component
import br.com.solardiagram.domain.model.DiagramProject
import br.com.solardiagram.domain.model.ElectricalSpecs

class MainRunDetectionEngine(
    private val current: CurrentEstimationEngine
) {
    data class MainRun(val currentA: Double, val voltageV: Double)

    fun detectMainAcRun(project: DiagramProject): MainRun? =
        detectMainAcRun(project.components)

    fun detectMainAcRun(components: List<Component>): MainRun? {
        val i = current.estimateProjectAcCurrentA(components) ?: return null
        return MainRun(i, 220.0)
    }

    fun detectMainAcRun(graph: ElectricalGraph, context: ProjectValidationContext): MainRun? {
        val connectedComponents = graph.nodes
            .mapNotNull { context.component(it.componentId) }
            .distinctBy { it.id }
        return detectMainAcRun(connectedComponents)
    }

    fun detectMainDcRun(project: DiagramProject): MainRun? =
        detectMainDcRun(project.components)

    fun detectMainDcRun(components: List<Component>): MainRun? {
        val mod = components.firstOrNull { it.specs is ElectricalSpecs.PvModuleSpecs } ?: return null
        val s = mod.specs as ElectricalSpecs.PvModuleSpecs
        return MainRun(s.iScA, s.vOcV)
    }

    fun detectMainDcRun(graph: ElectricalGraph, context: ProjectValidationContext): MainRun? {
        val connectedComponents = graph.nodes
            .mapNotNull { context.component(it.componentId) }
            .distinctBy { it.id }
        return detectMainDcRun(connectedComponents)
    }
}

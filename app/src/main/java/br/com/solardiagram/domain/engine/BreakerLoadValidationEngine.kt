package br.com.solardiagram.domain.engine

import br.com.solardiagram.domain.model.ComponentType
import br.com.solardiagram.domain.model.DiagramProject
import br.com.solardiagram.domain.model.ElectricalSpecs
import kotlin.math.roundToInt

class BreakerLoadValidationEngine {
    fun evaluate(project: DiagramProject): List<ValidationIssue> {
        val loads = project.components.filter { it.type == ComponentType.LOAD }
        if (loads.isEmpty()) return emptyList()
        val loadPowerById = loads.associate { it.id to ((it.specs as? ElectricalSpecs.LoadSpecs)?.powerW ?: 0.0) }
        val issues = mutableListOf<ValidationIssue>()

        project.components
            .filter { it.type == ComponentType.BREAKER }
            .forEach { breaker ->
                val breakerSpecs = breaker.specs as? ElectricalSpecs.BreakerSpecs ?: return@forEach
                val downstreamLoadPower = project.connections
                    .filter { it.fromComponentId == breaker.id || it.toComponentId == breaker.id }
                    .mapNotNull { conn ->
                        val otherId = if (conn.fromComponentId == breaker.id) conn.toComponentId else conn.fromComponentId
                        loadPowerById[otherId]
                    }
                    .sum()

                if (downstreamLoadPower <= 0.0) return@forEach
                val voltage = loads.firstNotNullOfOrNull { (it.specs as? ElectricalSpecs.LoadSpecs)?.voltageV } ?: 220.0
                val estimatedCurrent = downstreamLoadPower / voltage
                if (estimatedCurrent > breakerSpecs.ratedCurrentA) {
                    issues += ValidationIssue(
                        id = "AMP_BREAKER_OVERLOAD_${breaker.id}",
                        severity = Severity.WARNING,
                        code = "AMP_BREAKER_OVERLOAD",
                        message = "Disjuntor ${breaker.name} pode estar subdimensionado: carga estimada ${downstreamLoadPower.roundToInt()} W / ${voltage.roundToInt()} V ≈ ${"%.1f".format(estimatedCurrent)} A para ${breakerSpecs.ratedCurrentA.roundToInt()} A.",
                        componentId = breaker.id,
                        category = ValidationCategory.AMPACITY,
                        componentType = ComponentType.BREAKER
                    )
                }
            }
        return issues
    }
}

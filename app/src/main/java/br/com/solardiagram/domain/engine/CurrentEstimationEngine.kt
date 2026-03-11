package br.com.solardiagram.domain.engine

import br.com.solardiagram.domain.model.*

class CurrentEstimationEngine {
    fun estimateProjectAcCurrentA(components: List<Component>): Double? {
        // MVP heuristic: if any breaker exists, use max rated
        val breakers = components.mapNotNull { it.specs as? ElectricalSpecs.BreakerSpecs }
        return breakers.maxOfOrNull { it.ratedCurrentA }
    }
}

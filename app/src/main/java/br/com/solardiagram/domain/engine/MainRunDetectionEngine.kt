package br.com.solardiagram.domain.engine

import br.com.solardiagram.domain.model.*

class MainRunDetectionEngine(
    private val current: CurrentEstimationEngine
) {
    data class MainRun(val currentA: Double, val voltageV: Double)

    fun detectMainAcRun(components: List<Component>, connections: List<Connection>): MainRun? {
        val i = current.estimateProjectAcCurrentA(components) ?: return null
        // MVP: default 220V
        return MainRun(i, 220.0)
    }

    fun detectMainDcRun(components: List<Component>): MainRun? {
        // MVP: if any PV module exists, use its Isc/Voc
        val mod = components.firstOrNull { it.specs is ElectricalSpecs.PvModuleSpecs } ?: return null
        val s = mod.specs as ElectricalSpecs.PvModuleSpecs
        return MainRun(s.iScA, s.vOcV)
    }
}

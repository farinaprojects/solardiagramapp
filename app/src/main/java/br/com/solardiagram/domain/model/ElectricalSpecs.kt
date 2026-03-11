package br.com.solardiagram.domain.model

sealed class ElectricalSpecs {
    data class PvModuleSpecs(
        val pMaxW: Double,
        val vMpV: Double,
        val iMpA: Double,
        val vOcV: Double,
        val iScA: Double
    ): ElectricalSpecs()

    data class MicroInverterSpecs(
        val maxDcPowerW: Double,
        val maxDcVoltageV: Double,
        val maxDcCurrentA: Double,
        val acNominalPowerW: Double,
        val acVoltageV: Double,
        val maxAcCurrentA: Double,
        val phases: SystemPhase,
        val dcInputPairs: Int = 2
    ): ElectricalSpecs()

    data class StringInverterSpecs(
        val maxDcPowerW: Double,
        val maxDcVoltageV: Double,
        val maxDcCurrentA: Double,
        val acNominalPowerW: Double,
        val acVoltageV: Double,
        val maxAcCurrentA: Double,
        val mpptCount: Int,
        val phases: SystemPhase
    ): ElectricalSpecs()

    data class AcBusSpecs(
        val maxBusCurrentA: Double,
        val phases: SystemPhase,
        val hasNeutral: Boolean = true,
        val hasGround: Boolean = true
    ): ElectricalSpecs()

    data class QdgSpecs(
        val maxBusCurrentA: Double,
        val phases: SystemPhase
    ): ElectricalSpecs()

    data class BreakerSpecs(
        val ratedCurrentA: Double,
        val curve: BreakerCurve,
        val poles: BreakerPole,
        val applicableTo: CurrentKind
    ): ElectricalSpecs()

    data class DpsSpecs(
        val maxVoltageV: Double,
        val applicableTo: CurrentKind
    ): ElectricalSpecs()

    data class GroundingSpecs(
        val isMainEarthPoint: Boolean = false
    ): ElectricalSpecs()

    data class LoadSpecs(
        val label: String,
        val powerW: Double,
        val voltageV: Double,
        val phases: SystemPhase
    ): ElectricalSpecs()
}

package br.com.solardiagram.domain.engine

import br.com.solardiagram.domain.model.ComponentType
import br.com.solardiagram.domain.model.DiagramProject
import br.com.solardiagram.domain.model.ElectricalSpecs
import br.com.solardiagram.domain.model.SystemPhase
import kotlin.math.roundToInt

data class ProjectInsights(
    val microInverterCount: Int,
    val stringInverterCount: Int,
    val breakerCount: Int,
    val loadCount: Int,
    val totalAcPowerW: Double,
    val totalLoadW: Double,
    val estimatedCurrentA: Double,
    val nominalVoltageV: Double,
    val dominantPhase: SystemPhase
) {
    fun toSummaryPairs(): List<Pair<String, String>> = listOf(
        "Projeto" to "Micros: $microInverterCount • Strings: $stringInverterCount • Disjuntores: $breakerCount • Cargas: $loadCount",
        "Potência" to "Geração AC ${totalAcPowerW.roundToInt()} W • Cargas ${totalLoadW.roundToInt()} W",
        "Elétrica" to "Fase $dominantPhase • Tensão ${nominalVoltageV.roundToInt()} V • Corrente estimada ${"%.1f".format(estimatedCurrentA)} A"
    )
}

class ProjectInsightEngine {
    fun analyze(project: DiagramProject): ProjectInsights {
        val microSpecs = project.components.mapNotNull { it.specs as? ElectricalSpecs.MicroInverterSpecs }
        val stringSpecs = project.components.mapNotNull { it.specs as? ElectricalSpecs.StringInverterSpecs }
        val loadSpecs = project.components.mapNotNull { it.specs as? ElectricalSpecs.LoadSpecs }
        val phases = (microSpecs.map { it.phases } + stringSpecs.map { it.phases } + loadSpecs.map { it.phases })
        val dominantPhase = phases.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key ?: SystemPhase.BI
        val nominalVoltage = (loadSpecs.firstOrNull()?.voltageV ?: microSpecs.firstOrNull()?.acVoltageV ?: stringSpecs.firstOrNull()?.acVoltageV ?: 220.0)
        val totalAcPower = microSpecs.sumOf { it.acNominalPowerW } + stringSpecs.sumOf { it.acNominalPowerW }
        val totalLoad = loadSpecs.sumOf { it.powerW }
        val basePower = maxOf(totalAcPower, totalLoad)
        val estimatedCurrent = if (nominalVoltage > 0.0) basePower / nominalVoltage else 0.0
        return ProjectInsights(
            microInverterCount = project.components.count { it.type == ComponentType.MICROINVERTER },
            stringInverterCount = project.components.count { it.type == ComponentType.STRING_INVERTER },
            breakerCount = project.components.count { it.type == ComponentType.BREAKER },
            loadCount = project.components.count { it.type == ComponentType.LOAD },
            totalAcPowerW = totalAcPower,
            totalLoadW = totalLoad,
            estimatedCurrentA = estimatedCurrent,
            nominalVoltageV = nominalVoltage,
            dominantPhase = dominantPhase
        )
    }
}

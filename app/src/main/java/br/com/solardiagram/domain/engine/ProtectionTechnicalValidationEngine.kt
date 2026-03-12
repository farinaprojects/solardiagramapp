package br.com.solardiagram.domain.engine

import br.com.solardiagram.domain.electrical.ElectricalFlow
import br.com.solardiagram.domain.electrical.ElectricalFlowKind
import br.com.solardiagram.domain.electrical.ElectricalProtection
import br.com.solardiagram.domain.model.Component
import br.com.solardiagram.domain.model.ComponentType
import br.com.solardiagram.domain.model.ElectricalSpecs
import br.com.solardiagram.domain.model.SystemPhase
import kotlin.math.abs

class ProtectionTechnicalValidationEngine(
    private val ampacityEngine: AmpacityEngine = AmpacityEngine()
) {

    fun evaluate(context: ProjectValidationContext): List<ValidationIssue> {
        val issues = mutableListOf<ValidationIssue>()

        context.protections.forEach { protection ->
            val breaker = context.component(protection.breakerComponentId) ?: return@forEach
            val breakerSpecs = breaker.specs as? ElectricalSpecs.BreakerSpecs ?: return@forEach

            val flows = protection.protectedFlowIds.mapNotNull { flowId ->
                context.flows.firstOrNull { it.id == flowId }
            }
            if (flows.isEmpty()) return@forEach

            flows.forEach { flow ->
                issues += evaluateBreakerVsFlowCurrent(
                    context = context,
                    protection = protection,
                    breaker = breaker,
                    breakerSpecs = breakerSpecs,
                    flow = flow
                )
                issues += evaluateBreakerVsCableAmpacity(
                    context = context,
                    protection = protection,
                    breaker = breaker,
                    breakerSpecs = breakerSpecs,
                    flow = flow
                )
            }
        }

        return issues
    }

    private fun evaluateBreakerVsFlowCurrent(
        context: ProjectValidationContext,
        protection: ElectricalProtection,
        breaker: Component,
        breakerSpecs: ElectricalSpecs.BreakerSpecs,
        flow: ElectricalFlow
    ): List<ValidationIssue> {
        val estimatedCurrentA = estimateFlowCurrentA(context, flow, breakerSpecs.ratedCurrentA) ?: return emptyList()
        if (estimatedCurrentA <= 0.0) return emptyList()
        if (breakerSpecs.ratedCurrentA >= estimatedCurrentA) return emptyList()

        val severity = if (breakerSpecs.ratedCurrentA < estimatedCurrentA * 0.9) {
            Severity.ERROR
        } else {
            Severity.WARNING
        }

        return listOf(
            ValidationIssue(
                id = "prot-breaker-below-flow-${protection.breakerComponentId}-${flow.id}",
                severity = severity,
                code = "PROT_BREAKER_BELOW_ESTIMATED_FLOW_CURRENT",
                message = "O disjuntor ${breaker.name} (${fmt(breakerSpecs.ratedCurrentA)}A) está abaixo da corrente estimada do fluxo ${describeFlow(flow)} (${fmt(estimatedCurrentA)}A). Verifique a coordenação entre proteção e carga/geração do ramal.",
                componentId = protection.breakerComponentId,
                category = ValidationCategory.PROTECTION,
                componentType = breaker.type
            )
        )
    }

    private fun evaluateBreakerVsCableAmpacity(
        context: ProjectValidationContext,
        protection: ElectricalProtection,
        breaker: Component,
        breakerSpecs: ElectricalSpecs.BreakerSpecs,
        flow: ElectricalFlow
    ): List<ValidationIssue> {
        val issues = mutableListOf<ValidationIssue>()
        val flowConnections = flow.pathEdges.mapNotNull { edge -> context.connectionForEdge(edge) }
        if (flowConnections.isEmpty()) return emptyList()

        val connectionsWithCable = flowConnections.filter { it.meta.overrideCableMm2 != null }
        val connectionsWithoutCable = flowConnections.filter { it.meta.overrideCableMm2 == null }

        if (connectionsWithoutCable.isNotEmpty()) {
            val labels = connectionsWithoutCable.joinToString(", ") { it.meta.label ?: it.id }
            issues += ValidationIssue(
                id = "prot-flow-no-cable-${protection.breakerComponentId}-${flow.id}",
                severity = Severity.INFO,
                code = "PROT_FLOW_NO_CABLE_SPEC",
                message = "O fluxo ${describeFlow(flow)} protegido por ${breaker.name} possui trecho(s) sem bitola definida ($labels). Sem essa informação a verificação técnica entre proteção e ampacidade fica incompleta.",
                componentId = protection.breakerComponentId,
                category = ValidationCategory.PROTECTION,
                componentType = breaker.type
            )
        }

        if (connectionsWithCable.isEmpty()) return issues

        val ampacities = connectionsWithCable.mapNotNull { conn ->
            val mm2 = conn.meta.overrideCableMm2 ?: return@mapNotNull null
            val allowable = ampacityEngine.allowableCurrentA(
                AmpacityInput(
                    mm2 = mm2,
                    material = conn.meta.conductorMaterial,
                    installation = conn.meta.installationMethod,
                    insulation = conn.meta.insulation,
                    ambientTempC = conn.meta.ambientTempC,
                    grouping = conn.meta.grouping
                )
            ) ?: return@mapNotNull null
            Triple(conn.id, conn.meta.label ?: conn.id, allowable)
        }

        if (ampacities.isEmpty()) return issues

        val weakest = ampacities.minByOrNull { it.third } ?: return issues
        val weakestLabel = weakest.second
        val weakestAmpacityA = weakest.third

        if (breakerSpecs.ratedCurrentA > weakestAmpacityA) {
            val severity = if (breakerSpecs.ratedCurrentA > weakestAmpacityA * 1.2) {
                Severity.ERROR
            } else {
                Severity.WARNING
            }

            issues += ValidationIssue(
                id = "prot-breaker-above-ampacity-${protection.breakerComponentId}-${flow.id}-${weakest.first}",
                severity = severity,
                code = "PROT_BREAKER_ABOVE_CABLE_AMPACITY",
                message = "O disjuntor ${breaker.name} (${fmt(breakerSpecs.ratedCurrentA)}A) excede a ampacidade ajustada do trecho $weakestLabel (${fmt(weakestAmpacityA)}A) no fluxo ${describeFlow(flow)}. Revise a coordenação entre proteção e cabeamento do ramal.",
                componentId = protection.breakerComponentId,
                connectionId = weakest.first,
                category = ValidationCategory.PROTECTION,
                componentType = breaker.type
            )
        }

        return issues
    }

    private fun estimateFlowCurrentA(
        context: ProjectValidationContext,
        flow: ElectricalFlow,
        fallbackBreakerA: Double
    ): Double? {
        val destination = context.component(flow.destinationComponentId)
        val source = context.component(flow.sourceComponentId)

        val loadCurrent = (destination?.specs as? ElectricalSpecs.LoadSpecs)?.let { load ->
            load.powerW.takeIf { it > 0.0 }?.let { powerW ->
                val voltageV = load.voltageV.takeIf { it > 0.0 } ?: defaultVoltageFor(load.phases)
                powerW / voltageV
            }
        }

        val generationCurrent = when (val specs = source?.specs) {
            is ElectricalSpecs.MicroInverterSpecs -> specs.maxAcCurrentA.takeIf { it > 0.0 }
                ?: specs.acNominalPowerW.takeIf { it > 0.0 && specs.acVoltageV > 0.0 }?.div(specs.acVoltageV)
            is ElectricalSpecs.StringInverterSpecs -> specs.maxAcCurrentA.takeIf { it > 0.0 }
                ?: specs.acNominalPowerW.takeIf { it > 0.0 && specs.acVoltageV > 0.0 }?.div(specs.acVoltageV)
            else -> null
        }

        return when (flow.kind) {
            ElectricalFlowKind.GRID_TO_LOAD,
            ElectricalFlowKind.QDG_FALLBACK_TO_LOAD -> loadCurrent ?: fallbackBreakerA.takeIf { it > 0.0 }

            ElectricalFlowKind.GENERATION_TO_LOAD -> listOfNotNull(loadCurrent, generationCurrent).maxOrNull()
                ?: fallbackBreakerA.takeIf { it > 0.0 }

            ElectricalFlowKind.GENERATION_TO_GRID_EXPORT -> generationCurrent ?: fallbackBreakerA.takeIf { it > 0.0 }
        }
    }

    private fun defaultVoltageFor(phases: SystemPhase): Double = when (phases) {
        SystemPhase.MONO -> 127.0
        SystemPhase.BI -> 220.0
        SystemPhase.TRI -> 380.0
    }

    private fun describeFlow(flow: ElectricalFlow): String {
        val kindLabel = when (flow.kind) {
            ElectricalFlowKind.GRID_TO_LOAD -> "rede→carga"
            ElectricalFlowKind.GENERATION_TO_LOAD -> "geração→carga"
            ElectricalFlowKind.GENERATION_TO_GRID_EXPORT -> "geração→rede"
            ElectricalFlowKind.QDG_FALLBACK_TO_LOAD -> "QDG→carga"
        }
        return "$kindLabel (${flow.sourceType.name} → ${flow.destinationType.name})"
    }

    private fun fmt(value: Double): String = String.format("%.2f", abs(value))
}

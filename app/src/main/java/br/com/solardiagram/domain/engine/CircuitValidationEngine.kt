package br.com.solardiagram.domain.engine

import br.com.solardiagram.domain.electrical.ElectricalCircuit
import br.com.solardiagram.domain.electrical.ElectricalPathFinder
import br.com.solardiagram.domain.model.Component
import br.com.solardiagram.domain.model.ComponentType
import br.com.solardiagram.domain.model.ElectricalPhase
import br.com.solardiagram.domain.model.ElectricalSpecs
import br.com.solardiagram.domain.model.PhysicalTerminalRole
import br.com.solardiagram.domain.model.Port
import br.com.solardiagram.domain.model.PortDirection
import br.com.solardiagram.domain.model.PortKind
import kotlin.math.abs

class CircuitValidationEngine {

    fun evaluate(context: ProjectValidationContext): List<ValidationIssue> {
        val issues = mutableListOf<ValidationIssue>()

        context.circuits.forEach { circuit ->
            issues += evaluateSingleTerminal(context, circuit)
            issues += evaluateSemanticEndpoints(context, circuit)
            issues += evaluateFunctionalPath(context, circuit)
            issues += evaluateProtectionPresence(context, circuit)
            issues += evaluateCircuitEnvelope(context, circuit)
        }

        return issues.distinctBy { it.id }
    }

    private fun evaluateSingleTerminal(
        context: ProjectValidationContext,
        circuit: ElectricalCircuit
    ): List<ValidationIssue> {
        if (circuit.terminalKeys.size != 1) return emptyList()

        val terminalKey = circuit.terminalKeys.first()
        val componentId = terminalKey.first
        val portId = terminalKey.second

        val component = context.component(componentId)
        val port = context.port(componentId, portId)

        val componentName = component?.name ?: componentId
        val portName = port?.name ?: portId

        return listOf(
            ValidationIssue(
                id = "circuit-single-terminal-${circuit.id}",
                severity = Severity.WARNING,
                code = "CIRCUIT_SINGLE_TERMINAL",
                message = "Circuito com apenas um terminal detectado em ${componentName} (${portName}). Verifique se há ligação incompleta ou componente eletricamente isolado.",
                componentId = componentId,
                category = ValidationCategory.CIRCUIT,
                componentType = component?.type
            )
        )
    }

    private fun evaluateSemanticEndpoints(
        context: ProjectValidationContext,
        circuit: ElectricalCircuit
    ): List<ValidationIssue> {
        if (!requiresSemanticEndpointValidation(circuit)) return emptyList()

        val terminals = resolveCircuitTerminalRefs(context, circuit)
        if (terminals.isEmpty()) return emptyList()

        val hasGridSourceInProject = context.project.components.any { it.type == ComponentType.GRID_SOURCE }
        val hasSource = terminals.any { isSemanticSource(it, hasGridSourceInProject) }
        val hasDestination = terminals.any(::isSemanticDestination)

        val sampleComponent = terminals.firstOrNull()?.component
        val componentNames = terminals
            .map { it.component.name }
            .distinct()
            .take(4)
            .joinToString(", ")

        val issues = mutableListOf<ValidationIssue>()

        if (!hasSource) {
            issues += ValidationIssue(
                id = "circuit-no-source-${circuit.id}",
                severity = Severity.WARNING,
                code = "CIRCUIT_NO_SOURCE",
                message = "Circuito sem origem elétrica útil detectada. O agrupamento contém ${componentNames.ifBlank { "terminais sem identificação" }}. Verifique se há gerador, alimentação de quadro ou ponto de entrada efetivamente ligado.",
                componentId = sampleComponent?.id,
                category = ValidationCategory.CIRCUIT,
                componentType = sampleComponent?.type
            )
        }

        if (!hasDestination) {
            issues += ValidationIssue(
                id = "circuit-no-destination-${circuit.id}",
                severity = Severity.WARNING,
                code = "CIRCUIT_NO_DESTINATION",
                message = "Circuito sem destino elétrico útil detectado. O agrupamento contém ${componentNames.ifBlank { "terminais sem identificação" }}. Verifique se há carga, entrada de conversão ou ponto consumidor realmente conectado.",
                componentId = sampleComponent?.id,
                category = ValidationCategory.CIRCUIT,
                componentType = sampleComponent?.type
            )
        }

        return issues
    }

    private fun evaluateFunctionalPath(
        context: ProjectValidationContext,
        circuit: ElectricalCircuit
    ): List<ValidationIssue> {
        if (!requiresSemanticEndpointValidation(circuit)) return emptyList()

        val terminals = resolveCircuitTerminalRefs(context, circuit)
        if (terminals.isEmpty()) return emptyList()

        val hasGridSourceInProject = context.project.components.any { it.type == ComponentType.GRID_SOURCE }
        val sources = terminals.filter { isSemanticSource(it, hasGridSourceInProject) }
        val destinations = terminals.filter(::isSemanticDestination)

        if (sources.isEmpty() || destinations.isEmpty()) return emptyList()

        val hasFunctionalPath = sources.any { source ->
            destinations.any { destination ->
                if (source.terminalKey == destination.terminalKey) {
                    false
                } else {
                    ElectricalPathFinder.hasPathBetweenNodes(
                        edges = circuit.edges,
                        startNodeId = source.nodeId,
                        endNodeId = destination.nodeId
                    )
                }
            }
        }

        if (hasFunctionalPath) return emptyList()

        val sampleComponent = sources.firstOrNull()?.component ?: destinations.firstOrNull()?.component
        val sourceNames = sources.map { it.component.name }.distinct().take(3).joinToString(", ")
        val destinationNames = destinations.map { it.component.name }.distinct().take(3).joinToString(", ")

        return listOf(
            ValidationIssue(
                id = "circuit-no-functional-path-${circuit.id}",
                severity = Severity.WARNING,
                code = "CIRCUIT_NO_FUNCTIONAL_PATH",
                message = "Circuito com origem e destino sem caminho funcional válido entre eles. Origem: ${sourceNames.ifBlank { "não identificada" }}. Destino: ${destinationNames.ifBlank { "não identificado" }}. Verifique continuidade elétrica, barramentos e conexões intermediárias.",
                componentId = sampleComponent?.id,
                category = ValidationCategory.CIRCUIT,
                componentType = sampleComponent?.type
            )
        )
    }

    private fun evaluateProtectionPresence(
        context: ProjectValidationContext,
        circuit: ElectricalCircuit
    ): List<ValidationIssue> {
        if (circuit.loadComponentIds.isEmpty()) return emptyList()
        if (circuit.currentKind != br.com.solardiagram.domain.model.CurrentKind.AC) return emptyList()
        if (circuit.protectionComponentIds.isNotEmpty()) return emptyList()

        val firstLoad = circuit.loadComponentIds.firstOrNull() ?: return emptyList()
        val load = context.component(firstLoad) ?: return emptyList()

        return listOf(
            ValidationIssue(
                id = "circuit-unprotected-load-${circuit.id}",
                severity = Severity.WARNING,
                code = "CIRCUIT_LOAD_BRANCH_WITHOUT_PROTECTION",
                message = "Circuito com carga ${load.name} foi identificado sem disjuntor associado no caminho principal. Revise a proteção do ramal antes da carga.",
                componentId = load.id,
                category = ValidationCategory.CIRCUIT,
                componentType = load.type
            )
        )
    }

    private fun evaluateCircuitEnvelope(
        context: ProjectValidationContext,
        circuit: ElectricalCircuit
    ): List<ValidationIssue> {
        val issues = mutableListOf<ValidationIssue>()
        if (circuit.loadComponentIds.isEmpty()) return issues

        val breaker = circuit.protectionComponentIds
            .mapNotNull { context.component(it) }
            .firstOrNull { it.type == ComponentType.BREAKER }

        val breakerSpecs = breaker?.specs as? ElectricalSpecs.BreakerSpecs
        val aggregateCurrent = circuit.aggregateCurrentA

        if (breaker != null && breakerSpecs != null && aggregateCurrent != null && aggregateCurrent > breakerSpecs.ratedCurrentA) {
            issues += ValidationIssue(
                id = "circuit-breaker-overload-${circuit.id}",
                severity = if (aggregateCurrent > breakerSpecs.ratedCurrentA * 1.15) Severity.ERROR else Severity.WARNING,
                code = "CIRCUIT_BREAKER_OVERLOAD",
                message = "Circuito protegido por ${breaker.name} soma corrente estimada de ${fmt(aggregateCurrent)} A, acima da corrente nominal de ${fmt(breakerSpecs.ratedCurrentA)} A. Revise a divisão de cargas ou a proteção do ramal.",
                componentId = breaker.id,
                category = ValidationCategory.CIRCUIT,
                componentType = breaker.type
            )
        }

        val nominalVoltage = circuit.nominalVoltageV
        circuit.loadComponentIds.forEach { loadId ->
            val component = context.component(loadId) ?: return@forEach
            val loadSpecs = component.specs as? ElectricalSpecs.LoadSpecs ?: return@forEach
            if (nominalVoltage != null && abs(loadSpecs.voltageV - nominalVoltage) > loadSpecs.voltageV * 0.15) {
                issues += ValidationIssue(
                    id = "circuit-load-envelope-${circuit.id}-${component.id}",
                    severity = Severity.WARNING,
                    code = "CIRCUIT_LOAD_OUTSIDE_ENVELOPE",
                    message = "Carga ${component.name} está em circuito com envelope nominal aproximado de ${fmt(nominalVoltage)} V, mas sua especificação está em ${fmt(loadSpecs.voltageV)} V. Revise o circuito descoberto e a configuração da carga.",
                    componentId = component.id,
                    category = ValidationCategory.CIRCUIT,
                    componentType = component.type
                )
            }
        }

        return issues
    }

    private fun resolveCircuitTerminalRefs(
        context: ProjectValidationContext,
        circuit: ElectricalCircuit
    ): List<CircuitTerminalRef> {
        return circuit.terminalKeys.mapNotNull { (componentId, portId) ->
            val component = context.component(componentId) ?: return@mapNotNull null
            val port = context.port(componentId, portId) ?: return@mapNotNull null
            CircuitTerminalRef(component = component, port = port)
        }
    }

    private fun requiresSemanticEndpointValidation(circuit: ElectricalCircuit): Boolean {
        if (circuit.kind == PortKind.PE) return false
        if (circuit.phase == ElectricalPhase.PE) return false
        return circuit.kind != null || circuit.phase != null
    }

    private fun isSemanticSource(ref: CircuitTerminalRef, hasGridSourceInProject: Boolean): Boolean {
        val role = ref.port.spec?.terminalRole
        val name = ref.port.name

        return when (ref.component.type) {
            ComponentType.PV_MODULE -> ref.port.kind == PortKind.DC_POS || ref.port.kind == PortKind.DC_NEG

            ComponentType.GRID_SOURCE -> {
                role == PhysicalTerminalRole.LINE ||
                    ref.port.direction == PortDirection.OUTPUT
            }

            ComponentType.MICROINVERTER,
            ComponentType.STRING_INVERTER -> {
                role == PhysicalTerminalRole.AC_OUTPUT ||
                    ref.port.direction == PortDirection.OUTPUT
            }

            ComponentType.QDG -> {
                !hasGridSourceInProject && (
                    role == PhysicalTerminalRole.LINE ||
                        name.contains("line", ignoreCase = true) ||
                        name.contains("in", ignoreCase = true) ||
                        name.contains("entrada", ignoreCase = true) ||
                        name.contains("alim", ignoreCase = true)
                    )
            }

            else -> false
        }
    }

    private fun isSemanticDestination(ref: CircuitTerminalRef): Boolean {
        val role = ref.port.spec?.terminalRole
        val name = ref.port.name

        return when (ref.component.type) {
            ComponentType.LOAD -> true

            ComponentType.MICROINVERTER,
            ComponentType.STRING_INVERTER -> {
                role == PhysicalTerminalRole.DC_INPUT ||
                    ref.port.direction == PortDirection.INPUT
            }

            ComponentType.QDG -> {
                role == PhysicalTerminalRole.LOAD ||
                    name.contains("load", ignoreCase = true) ||
                    name.contains("out", ignoreCase = true) ||
                    name.contains("saída", ignoreCase = true) ||
                    name.contains("saida", ignoreCase = true)
            }

            else -> false
        }
    }

    private fun fmt(value: Double): String = String.format("%.1f", value)

    private data class CircuitTerminalRef(
        val component: Component,
        val port: Port
    ) {
        val terminalKey: Pair<String, String>
            get() = component.id to port.id

        val nodeId: String
            get() = "${component.id}::${port.id}"
    }
}

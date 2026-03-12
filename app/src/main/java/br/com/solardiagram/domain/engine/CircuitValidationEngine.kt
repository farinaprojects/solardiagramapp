package br.com.solardiagram.domain.engine

import br.com.solardiagram.domain.electrical.ElectricalCircuit
import br.com.solardiagram.domain.electrical.ElectricalPathFinder
import br.com.solardiagram.domain.model.Component
import br.com.solardiagram.domain.model.ComponentType
import br.com.solardiagram.domain.model.ElectricalPhase
import br.com.solardiagram.domain.model.PhysicalTerminalRole
import br.com.solardiagram.domain.model.Port
import br.com.solardiagram.domain.model.PortDirection
import br.com.solardiagram.domain.model.PortKind

class CircuitValidationEngine {

    fun evaluate(context: ProjectValidationContext): List<ValidationIssue> {
        val issues = mutableListOf<ValidationIssue>()

        context.circuits.forEach { circuit ->
            issues += evaluateSingleTerminal(context, circuit)
            issues += evaluateSemanticEndpoints(context, circuit)
            issues += evaluateFunctionalPath(context, circuit)
        }

        return issues
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

        val hasSource = terminals.any(::isSemanticSource)
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

        val sources = terminals.filter(::isSemanticSource)
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

    private fun isSemanticSource(ref: CircuitTerminalRef): Boolean {
        val role = ref.port.spec?.terminalRole
        val name = ref.port.name

        return when (ref.component.type) {
            ComponentType.PV_MODULE -> ref.port.kind == PortKind.DC_POS || ref.port.kind == PortKind.DC_NEG

            ComponentType.MICROINVERTER,
            ComponentType.STRING_INVERTER -> {
                role == PhysicalTerminalRole.AC_OUTPUT ||
                    ref.port.direction == PortDirection.OUTPUT
            }

            ComponentType.QDG -> {
                role == PhysicalTerminalRole.LINE ||
                    name.contains("line", ignoreCase = true) ||
                    name.contains("in", ignoreCase = true) ||
                    name.contains("entrada", ignoreCase = true)
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

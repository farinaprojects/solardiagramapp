package br.com.solardiagram.domain.electrical

import br.com.solardiagram.domain.model.ComponentType
import br.com.solardiagram.domain.model.DiagramProject
import br.com.solardiagram.domain.model.PhysicalTerminalRole
import br.com.solardiagram.domain.model.PortDirection
import java.util.UUID

object ElectricalFlowAnalyzer {

    fun analyze(project: DiagramProject, graph: ElectricalGraph): List<ElectricalFlow> {
        val terminals = graph.nodes.flatMap { node ->
            node.terminals.mapNotNull { terminal ->
                val component = project.components.firstOrNull { it.id == node.componentId } ?: return@mapNotNull null
                val port = component.portById(terminal.portId) ?: return@mapNotNull null
                FlowTerminalRef(component.id, component.type, port.id, port.name, port.direction, port.spec?.terminalRole)
            }
        }

        val hasGridSource = terminals.any { it.componentType == ComponentType.GRID_SOURCE }
        val sources = terminals.filter { isSemanticSource(it, hasGridSource) }
        val destinations = terminals.filter { isSemanticDestination(it, hasGridSource) }

        val flows = mutableListOf<ElectricalFlow>()
        val seen = mutableSetOf<String>()

        sources.forEach { source ->
            destinations.forEach { destination ->
                if (source.componentId == destination.componentId && source.portId == destination.portId) return@forEach

                val kind = classifyFlow(source, destination, hasGridSource) ?: return@forEach

                val path = ElectricalPathFinder.findFirstPathBetweenNodes(
                    edges = graph.edges,
                    startNodeId = graph.nodeId(source.componentId, source.portId),
                    endNodeId = graph.nodeId(destination.componentId, destination.portId)
                ) ?: return@forEach

                val signature = listOf(kind.name, source.componentId, source.portId, destination.componentId, destination.portId) +
                    path.map { it.connectionId ?: "${it.fromNodeId}->${it.toNodeId}" }
                val key = signature.joinToString("|")
                if (!seen.add(key)) return@forEach

                val componentIds = linkedSetOf(source.componentId)
                path.forEach { edge ->
                    componentIds += edge.fromComponentId
                    componentIds += edge.toComponentId
                }
                componentIds += destination.componentId

                flows += ElectricalFlow(
                    id = UUID.randomUUID().toString(),
                    kind = kind,
                    sourceComponentId = source.componentId,
                    sourcePortId = source.portId,
                    sourceType = source.componentType,
                    destinationComponentId = destination.componentId,
                    destinationPortId = destination.portId,
                    destinationType = destination.componentType,
                    pathEdges = path,
                    componentIds = componentIds.toList()
                )
            }
        }

        return flows
    }

    private fun classifyFlow(
        source: FlowTerminalRef,
        destination: FlowTerminalRef,
        hasGridSource: Boolean
    ): ElectricalFlowKind? {
        return when {
            isGridSourceTerminal(source) && isLoadSideDestination(destination) -> ElectricalFlowKind.GRID_TO_LOAD
            isGenerationSource(source) && isLoadSideDestination(destination) -> ElectricalFlowKind.GENERATION_TO_LOAD
            isGenerationSource(source) && isGridExportDestination(destination) -> ElectricalFlowKind.GENERATION_TO_GRID_EXPORT
            !hasGridSource && isQdgFallbackSource(source) && isLoadSideDestination(destination) -> ElectricalFlowKind.QDG_FALLBACK_TO_LOAD
            else -> null
        }
    }

    private fun isSemanticSource(ref: FlowTerminalRef, hasGridSource: Boolean): Boolean = when (ref.componentType) {
        ComponentType.GRID_SOURCE -> isGridSourceTerminal(ref)
        ComponentType.PV_MODULE -> false
        ComponentType.MICROINVERTER, ComponentType.STRING_INVERTER -> isGenerationSource(ref)
        ComponentType.QDG -> !hasGridSource && isQdgFallbackSource(ref)
        else -> false
    }

    private fun isSemanticDestination(ref: FlowTerminalRef, hasGridSource: Boolean): Boolean {
        return isLoadSideDestination(ref) || isGridExportDestination(ref)
    }

    private fun isGridSourceTerminal(ref: FlowTerminalRef): Boolean {
        return ref.componentType == ComponentType.GRID_SOURCE && (
            ref.role == PhysicalTerminalRole.LINE ||
                ref.direction == PortDirection.OUTPUT ||
                ref.portName.contains("rede", ignoreCase = true)
            )
    }

    private fun isGenerationSource(ref: FlowTerminalRef): Boolean {
        return when (ref.componentType) {
            ComponentType.MICROINVERTER, ComponentType.STRING_INVERTER -> {
                ref.role == PhysicalTerminalRole.AC_OUTPUT || ref.direction == PortDirection.OUTPUT
            }
            else -> false
        }
    }

    private fun isQdgFallbackSource(ref: FlowTerminalRef): Boolean {
        return ref.componentType == ComponentType.QDG && (
            ref.role == PhysicalTerminalRole.LINE ||
                ref.portName.contains("line", ignoreCase = true) ||
                ref.portName.contains("in", ignoreCase = true) ||
                ref.portName.contains("alim", ignoreCase = true)
            )
    }

    private fun isLoadSideDestination(ref: FlowTerminalRef): Boolean = when (ref.componentType) {
        ComponentType.LOAD -> true
        ComponentType.MICROINVERTER, ComponentType.STRING_INVERTER -> {
            ref.role == PhysicalTerminalRole.DC_INPUT || ref.direction == PortDirection.INPUT
        }
        ComponentType.QDG -> {
            ref.role == PhysicalTerminalRole.LOAD ||
                ref.portName.contains("load", ignoreCase = true) ||
                ref.portName.contains("dist", ignoreCase = true) ||
                ref.portName.contains("out", ignoreCase = true)
        }
        else -> false
    }

    private fun isGridExportDestination(ref: FlowTerminalRef): Boolean {
        return ref.componentType == ComponentType.GRID_SOURCE && (
            ref.role == PhysicalTerminalRole.LINE ||
                ref.portName.contains("rede", ignoreCase = true)
            )
    }

    private data class FlowTerminalRef(
        val componentId: String,
        val componentType: ComponentType,
        val portId: String,
        val portName: String,
        val direction: PortDirection,
        val role: PhysicalTerminalRole?
    )
}

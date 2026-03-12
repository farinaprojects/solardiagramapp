package br.com.solardiagram.domain.electrical

import br.com.solardiagram.domain.model.Component
import br.com.solardiagram.domain.model.Connection
import br.com.solardiagram.domain.model.DiagramProject

object ElectricalGraphBuilder {

    fun build(project: DiagramProject): ElectricalGraph =
        build(project.components, project.connections)

    fun build(
        components: List<Component>,
        connections: List<Connection>
    ): ElectricalGraph {
        val nodes = components.map { component ->
            val terminals = component.ports.map { port ->
                ElectricalTerminal(
                    componentId = component.id,
                    portId = port.id,
                    portName = port.name,
                    phase = port.spec?.phase,
                    kind = port.kind,
                    terminalRole = port.spec?.terminalRole
                )
            }

            ElectricalNode(
                componentId = component.id,
                componentName = component.name,
                type = component.type,
                terminals = terminals
            )
        }

        val knownNodeIds = nodes.flatMap { node ->
            node.terminals.map { terminal -> nodeId(node.componentId, terminal.portId) }
        }.toHashSet()

        val edges = connections.mapNotNull { connection ->
            val fromNodeId = nodeId(connection.fromComponentId, connection.fromPortId)
            val toNodeId = nodeId(connection.toComponentId, connection.toPortId)

            if (fromNodeId !in knownNodeIds || toNodeId !in knownNodeIds) {
                null
            } else {
                ElectricalEdge(
                    fromNodeId = fromNodeId,
                    toNodeId = toNodeId,
                    connectionId = connection.id
                )
            }
        }

        return ElectricalGraph(nodes = nodes, edges = edges)
    }

    fun nodeId(componentId: String, portId: String): String = "$componentId::$portId"
}
package br.com.solardiagram.domain.engine

import br.com.solardiagram.domain.model.DiagramProject

class ElectricalGraphBuilder {

    fun build(project: DiagramProject): ElectricalGraph {
        val nodes = project.components.flatMap { component ->
            component.ports.map { port ->
                ElectricalGraphNode(
                    nodeId = nodeId(component.id, port.id),
                    componentId = component.id,
                    componentName = component.name,
                    componentType = component.type,
                    portId = port.id,
                    portName = port.name,
                    portKind = port.kind,
                    direction = port.direction
                )
            }
        }

        val knownNodeIds = nodes.asSequence().map { it.nodeId }.toHashSet()
        val edges = project.connections.mapNotNull { connection ->
            val fromNodeId = nodeId(connection.fromComponentId, connection.fromPortId)
            val toNodeId = nodeId(connection.toComponentId, connection.toPortId)
            if (fromNodeId !in knownNodeIds || toNodeId !in knownNodeIds) {
                null
            } else {
                ElectricalGraphEdge(
                    edgeId = connection.id,
                    connectionId = connection.id,
                    fromNodeId = fromNodeId,
                    toNodeId = toNodeId,
                    fromComponentId = connection.fromComponentId,
                    toComponentId = connection.toComponentId
                )
            }
        }

        return ElectricalGraph(nodes = nodes, edges = edges)
    }

    private fun nodeId(componentId: String, portId: String): String = "$componentId::$portId"
}

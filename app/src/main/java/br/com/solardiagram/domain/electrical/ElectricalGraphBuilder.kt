package br.com.solardiagram.domain.electrical

import br.com.solardiagram.domain.model.Component
import br.com.solardiagram.domain.model.Connection

object ElectricalGraphBuilder {

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

        val edges = connections.map { connection ->
            ElectricalEdge(
                fromComponentId = connection.fromComponentId,
                fromPortId = connection.fromPortId,
                toComponentId = connection.toComponentId,
                toPortId = connection.toPortId
            )
        }

        return ElectricalGraph(
            nodes = nodes,
            edges = edges
        )
    }
}
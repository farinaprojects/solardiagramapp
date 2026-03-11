package br.com.solardiagram.domain.electrical

import br.com.solardiagram.domain.model.ComponentType
import br.com.solardiagram.domain.model.ElectricalPhase
import br.com.solardiagram.domain.model.PhysicalTerminalRole

object ElectricalComponentSemantics {

    fun internalConnections(node: ElectricalNode): List<ElectricalInternalConnection> {
        return when (node.type) {
            ComponentType.BARL,
            ComponentType.BARN,
            ComponentType.BARPE -> connectAll(node)

            ComponentType.GROUND_BAR -> connectAll(node)

            ComponentType.AC_BUS -> connectByPhase(node)

            ComponentType.BREAKER -> breakerConnections(node)

            ComponentType.QDG -> connectByPhase(node)

            ComponentType.LOAD,
            ComponentType.PV_MODULE,
            ComponentType.MICROINVERTER,
            ComponentType.STRING_INVERTER,
            ComponentType.DPS -> emptyList()
        }
    }

    private fun connectAll(node: ElectricalNode): List<ElectricalInternalConnection> {
        if (node.terminals.size < 2) return emptyList()

        val result = mutableListOf<ElectricalInternalConnection>()
        for (i in 0 until node.terminals.lastIndex) {
            for (j in i + 1 until node.terminals.size) {
                result += ElectricalInternalConnection(
                    componentId = node.componentId,
                    fromPortId = node.terminals[i].portId,
                    toPortId = node.terminals[j].portId
                )
            }
        }
        return result
    }

    private fun connectByPhase(node: ElectricalNode): List<ElectricalInternalConnection> {
        return node.terminals
            .groupBy { it.phase }
            .values
            .flatMap { terminals ->
                pairwise(node.componentId, terminals.map { it.portId })
            }
    }

    private fun breakerConnections(node: ElectricalNode): List<ElectricalInternalConnection> {
        val byPhase = node.terminals.groupBy { it.phase }
        val result = mutableListOf<ElectricalInternalConnection>()

        byPhase.forEach { (_, terminals) ->
            val lineSide = terminals.filter {
                it.terminalRole == PhysicalTerminalRole.LINE ||
                        it.portName.contains("line", ignoreCase = true) ||
                        it.portName.contains("in", ignoreCase = true)
            }

            val loadSide = terminals.filter {
                it.terminalRole == PhysicalTerminalRole.LOAD ||
                        it.portName.contains("load", ignoreCase = true) ||
                        it.portName.contains("out", ignoreCase = true)
            }

            if (lineSide.isNotEmpty() && loadSide.isNotEmpty()) {
                lineSide.forEach { line ->
                    loadSide.forEach { load ->
                        result += ElectricalInternalConnection(
                            componentId = node.componentId,
                            fromPortId = line.portId,
                            toPortId = load.portId
                        )
                    }
                }
            } else if (terminals.size >= 2) {
                result += pairwise(node.componentId, terminals.map { it.portId })
            }
        }

        return result
    }

    private fun pairwise(
        componentId: String,
        portIds: List<String>
    ): List<ElectricalInternalConnection> {
        if (portIds.size < 2) return emptyList()

        val result = mutableListOf<ElectricalInternalConnection>()
        for (i in 0 until portIds.lastIndex) {
            for (j in i + 1 until portIds.size) {
                result += ElectricalInternalConnection(
                    componentId = componentId,
                    fromPortId = portIds[i],
                    toPortId = portIds[j]
                )
            }
        }
        return result
    }

    fun terminalKey(componentId: String, portId: String): Pair<String, String> {
        return componentId to portId
    }

    fun samePhase(a: ElectricalTerminal, b: ElectricalTerminal): Boolean {
        return a.phase == b.phase
    }

    fun sameAcLane(a: ElectricalTerminal, b: ElectricalTerminal): Boolean {
        return samePhase(a, b)
    }

    fun isPhaseTerminal(terminal: ElectricalTerminal): Boolean {
        return terminal.phase != null && terminal.phase != ElectricalPhase.NONE
    }
}
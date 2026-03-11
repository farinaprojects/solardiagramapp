package br.com.solardiagram.domain.electrical

object ElectricalValidationEngine {

    fun validate(graph: ElectricalGraph): List<ElectricalValidationIssue> {

        val issues = mutableListOf<ElectricalValidationIssue>()

        validateOpenTerminals(graph, issues)

        validatePhaseMismatch(graph, issues)

        return issues
    }

    private fun validateOpenTerminals(
        graph: ElectricalGraph,
        issues: MutableList<ElectricalValidationIssue>
    ) {

        val connectedPorts = mutableSetOf<Pair<String, String>>()

        graph.edges.forEach {
            connectedPorts.add(it.fromComponentId to it.fromPortId)
            connectedPorts.add(it.toComponentId to it.toPortId)
        }

        graph.nodes.forEach { node ->
            node.terminals.forEach { terminal ->

                val key = terminal.componentId to terminal.portId

                if (!connectedPorts.contains(key)) {

                    issues.add(
                        ElectricalValidationIssue(
                            severity = ElectricalIssueSeverity.WARNING,
                            message = "Terminal não conectado: ${terminal.portName}",
                            componentId = terminal.componentId,
                            portId = terminal.portId
                        )
                    )
                }
            }
        }
    }

    private fun validatePhaseMismatch(
        graph: ElectricalGraph,
        issues: MutableList<ElectricalValidationIssue>
    ) {

        val terminalIndex = mutableMapOf<Pair<String, String>, ElectricalTerminal>()

        graph.nodes.forEach { node ->
            node.terminals.forEach { terminal ->
                terminalIndex[terminal.componentId to terminal.portId] = terminal
            }
        }

        graph.edges.forEach { edge ->

            val from = terminalIndex[edge.fromComponentId to edge.fromPortId]
            val to = terminalIndex[edge.toComponentId to edge.toPortId]

            if (from == null || to == null) return@forEach

            if (from.phase != null && to.phase != null && from.phase != to.phase) {

                issues.add(
                    ElectricalValidationIssue(
                        severity = ElectricalIssueSeverity.ERROR,
                        message = "Conexão entre fases diferentes: ${from.phase} → ${to.phase}",
                        componentId = edge.fromComponentId,
                        portId = edge.fromPortId
                    )
                )
            }
        }
    }
}
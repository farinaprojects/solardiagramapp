package br.com.solardiagram.domain.electrical

import br.com.solardiagram.domain.model.ElectricalPhase
import br.com.solardiagram.domain.model.PortKind
import java.util.UUID

object ElectricalCircuitAnalyzer {

    fun analyze(graph: ElectricalGraph): List<ElectricalCircuit> {
        val terminalIndex = buildTerminalIndex(graph)

        val externalAdjacency = buildExternalUndirectedAdjacency(graph)
        val internalAdjacency = buildInternalUndirectedAdjacency(graph)
        val mergedAdjacency = mergeAdjacency(externalAdjacency, internalAdjacency)

        val visited = mutableSetOf<Pair<String, String>>()
        val circuits = mutableListOf<ElectricalCircuit>()

        terminalIndex.keys.forEach { start ->
            if (start in visited) return@forEach

            val startTerminal = terminalIndex[start] ?: return@forEach
            val collectedTerminals = mutableSetOf<Pair<String, String>>()
            val collectedEdges = mutableListOf<ElectricalEdge>()

            dfsCollect(
                current = start,
                terminalIndex = terminalIndex,
                adjacency = mergedAdjacency,
                visited = visited,
                collectedTerminals = collectedTerminals,
                collectedEdges = collectedEdges,
                referencePhase = startTerminal.phase,
                referenceKind = startTerminal.kind
            )

            if (collectedTerminals.isNotEmpty()) {
                circuits += ElectricalCircuit(
                    id = UUID.randomUUID().toString(),
                    phase = startTerminal.phase,
                    kind = startTerminal.kind,
                    terminalKeys = collectedTerminals,
                    edges = collectedEdges.distinct()
                )
            }
        }

        return circuits
    }

    private fun buildTerminalIndex(
        graph: ElectricalGraph
    ): Map<Pair<String, String>, ElectricalTerminal> {
        val map = mutableMapOf<Pair<String, String>, ElectricalTerminal>()
        graph.nodes.forEach { node ->
            node.terminals.forEach { terminal ->
                map[terminal.componentId to terminal.portId] = terminal
            }
        }
        return map
    }

    private fun buildExternalUndirectedAdjacency(
        graph: ElectricalGraph
    ): Map<Pair<String, String>, List<Pair<Pair<String, String>, ElectricalEdge>>> {
        val adjacency =
            mutableMapOf<Pair<String, String>, MutableList<Pair<Pair<String, String>, ElectricalEdge>>>()

        graph.edges.forEach { edge ->
            val fromKey = edge.fromComponentId to edge.fromPortId
            val toKey = edge.toComponentId to edge.toPortId

            adjacency.getOrPut(fromKey) { mutableListOf() }.add(toKey to edge)
            adjacency.getOrPut(toKey) { mutableListOf() }.add(fromKey to edge)
        }

        return adjacency
    }

    private fun buildInternalUndirectedAdjacency(
        graph: ElectricalGraph
    ): Map<Pair<String, String>, List<Pair<Pair<String, String>, ElectricalEdge>>> {
        val adjacency =
            mutableMapOf<Pair<String, String>, MutableList<Pair<Pair<String, String>, ElectricalEdge>>>()

        graph.nodes.forEach { node ->
            val internals = ElectricalComponentSemantics.internalConnections(node)
            internals.forEach { internal ->
                val fromKey = internal.componentId to internal.fromPortId
                val toKey = internal.componentId to internal.toPortId

                val syntheticEdge = ElectricalEdge(
                    fromNodeId = "${internal.componentId}:${internal.fromPortId}",
                    toNodeId = "${internal.componentId}:${internal.toPortId}",
                    connectionId = null
                )

                adjacency.getOrPut(fromKey) { mutableListOf() }.add(toKey to syntheticEdge)
                adjacency.getOrPut(toKey) { mutableListOf() }.add(fromKey to syntheticEdge)
            }
        }

        return adjacency
    }

    private fun mergeAdjacency(
        externalAdjacency: Map<Pair<String, String>, List<Pair<Pair<String, String>, ElectricalEdge>>>,
        internalAdjacency: Map<Pair<String, String>, List<Pair<Pair<String, String>, ElectricalEdge>>>
    ): Map<Pair<String, String>, List<Pair<Pair<String, String>, ElectricalEdge>>> {
        val keys = (externalAdjacency.keys + internalAdjacency.keys).toSet()
        val merged =
            mutableMapOf<Pair<String, String>, MutableList<Pair<Pair<String, String>, ElectricalEdge>>>()

        keys.forEach { key ->
            merged.getOrPut(key) { mutableListOf() }
                .addAll(externalAdjacency[key].orEmpty())
            merged.getOrPut(key) { mutableListOf() }
                .addAll(internalAdjacency[key].orEmpty())
        }

        return merged
    }

    private fun dfsCollect(
        current: Pair<String, String>,
        terminalIndex: Map<Pair<String, String>, ElectricalTerminal>,
        adjacency: Map<Pair<String, String>, List<Pair<Pair<String, String>, ElectricalEdge>>>,
        visited: MutableSet<Pair<String, String>>,
        collectedTerminals: MutableSet<Pair<String, String>>,
        collectedEdges: MutableList<ElectricalEdge>,
        referencePhase: ElectricalPhase?,
        referenceKind: PortKind?
    ) {
        if (current in visited) return

        val currentTerminal = terminalIndex[current] ?: return
        if (!isCompatible(currentTerminal, referencePhase, referenceKind)) return

        visited += current
        collectedTerminals += current

        val neighbors = adjacency[current].orEmpty()
        neighbors.forEach { (nextKey, edge) ->
            val nextTerminal = terminalIndex[nextKey] ?: return@forEach
            if (!isCompatible(nextTerminal, referencePhase, referenceKind)) return@forEach

            collectedEdges += edge

            dfsCollect(
                current = nextKey,
                terminalIndex = terminalIndex,
                adjacency = adjacency,
                visited = visited,
                collectedTerminals = collectedTerminals,
                collectedEdges = collectedEdges,
                referencePhase = referencePhase,
                referenceKind = referenceKind
            )
        }
    }

    private fun isCompatible(
        terminal: ElectricalTerminal,
        referencePhase: ElectricalPhase?,
        referenceKind: PortKind?
    ): Boolean {
        val samePhase = terminal.phase == referencePhase
        val sameKind = terminal.kind == referenceKind

        if (referencePhase != null && referenceKind != null) {
            return samePhase && sameKind
        }

        if (referencePhase != null) {
            return samePhase
        }

        if (referenceKind != null) {
            return sameKind
        }

        return true
    }
}
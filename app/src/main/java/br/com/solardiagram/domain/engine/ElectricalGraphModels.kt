package br.com.solardiagram.domain.engine

import br.com.solardiagram.domain.model.ComponentType
import br.com.solardiagram.domain.model.PortDirection
import br.com.solardiagram.domain.model.PortKind

/**
 * Grafo elétrico em nível de portas. Cada nó representa uma porta de componente.
 * As arestas representam conexões explícitas do diagrama.
 */
data class ElectricalGraphNode(
    val nodeId: String,
    val componentId: String,
    val componentName: String,
    val componentType: ComponentType,
    val portId: String,
    val portName: String,
    val portKind: PortKind,
    val direction: PortDirection
)

data class ElectricalGraphEdge(
    val edgeId: String,
    val connectionId: String,
    val fromNodeId: String,
    val toNodeId: String,
    val fromComponentId: String,
    val toComponentId: String
)

data class ElectricalGraph(
    val nodes: List<ElectricalGraphNode> = emptyList(),
    val edges: List<ElectricalGraphEdge> = emptyList()
) {
    private val nodesById: Map<String, ElectricalGraphNode> = nodes.associateBy { it.nodeId }
    private val edgesByNodeId: Map<String, List<ElectricalGraphEdge>> = buildMap {
        nodes.forEach { put(it.nodeId, emptyList()) }
        edges.forEach { edge ->
            put(edge.fromNodeId, (get(edge.fromNodeId) ?: emptyList()) + edge)
            put(edge.toNodeId, (get(edge.toNodeId) ?: emptyList()) + edge)
        }
    }

    fun node(nodeId: String): ElectricalGraphNode? = nodesById[nodeId]

    fun nodeId(componentId: String, portId: String): String = "$componentId::$portId"

    fun edgesForNode(nodeId: String): List<ElectricalGraphEdge> = edgesByNodeId[nodeId].orEmpty()

    fun edgesForComponent(componentId: String): List<ElectricalGraphEdge> =
        edges.filter { it.fromComponentId == componentId || it.toComponentId == componentId }

    fun nodesForComponent(componentId: String): List<ElectricalGraphNode> =
        nodes.filter { it.componentId == componentId }

    fun adjacentComponentIds(componentId: String): Set<String> =
        edgesForComponent(componentId).flatMap { edge ->
            listOf(edge.fromComponentId, edge.toComponentId)
        }.filterNot { it == componentId }.toSet()

    fun connectedComponentGroups(): List<Set<String>> {
        val all = nodes.map { it.componentId }.toSet()
        if (all.isEmpty()) return emptyList()

        val adjacency = all.associateWith { adjacentComponentIds(it) }
        val visited = mutableSetOf<String>()
        val groups = mutableListOf<Set<String>>()

        all.forEach { start ->
            if (start in visited) return@forEach
            val queue = ArrayDeque<String>()
            val group = mutableSetOf<String>()
            queue += start
            visited += start
            while (queue.isNotEmpty()) {
                val current = queue.removeFirst()
                group += current
                adjacency[current].orEmpty().forEach { next ->
                    if (visited.add(next)) queue += next
                }
            }
            groups += group
        }
        return groups
    }
}

package br.com.solardiagram.domain.electrical

data class ElectricalGraph(
    val nodes: List<ElectricalNode>,
    val edges: List<ElectricalEdge>
) {
    fun nodeId(componentId: String, portId: String): String = "$componentId::$portId"

    fun edgesForNode(nodeId: String): List<ElectricalEdge> =
        edges.filter { it.fromNodeId == nodeId || it.toNodeId == nodeId }

    fun edgesForComponent(componentId: String): List<ElectricalEdge> =
        edges.filter { it.fromComponentId == componentId || it.toComponentId == componentId }

    fun nodesForComponent(componentId: String): List<ElectricalNode> =
        nodes.filter { it.componentId == componentId }

    fun adjacentComponentIds(componentId: String): Set<String> =
        edgesForComponent(componentId)
            .flatMap { listOf(it.fromComponentId, it.toComponentId) }
            .filterNot { it == componentId }
            .toSet()

    fun connectedComponentGroups(): List<Set<String>> {
        val allComponents = nodes.map { it.componentId }.toSet()
        if (allComponents.isEmpty()) return emptyList()

        val adjacency = allComponents.associateWith { adjacentComponentIds(it) }
        val visited = mutableSetOf<String>()
        val groups = mutableListOf<Set<String>>()

        allComponents.forEach { start ->
            if (start in visited) return@forEach

            val queue = ArrayDeque<String>()
            val group = mutableSetOf<String>()

            queue.add(start)
            visited.add(start)

            while (queue.isNotEmpty()) {
                val current = queue.removeFirst()
                group.add(current)

                adjacency[current].orEmpty().forEach { next ->
                    if (visited.add(next)) queue.add(next)
                }
            }

            groups.add(group)
        }

        return groups
    }
}
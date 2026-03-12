package br.com.solardiagram.domain.electrical

object ElectricalPathFinder {

    fun findPaths(
        graph: ElectricalGraph,
        startComponentId: String,
        endComponentId: String,
        maxPaths: Int = 16,
        maxDepth: Int = 64
    ): List<List<ElectricalEdge>> {
        val startNodeIds = graph.nodesForComponent(startComponentId)
            .flatMap { node -> node.terminals.map { terminal -> graph.nodeId(node.componentId, terminal.portId) } }
            .distinct()

        val endNodeIds = graph.nodesForComponent(endComponentId)
            .flatMap { node -> node.terminals.map { terminal -> graph.nodeId(node.componentId, terminal.portId) } }
            .distinct()

        if (startNodeIds.isEmpty() || endNodeIds.isEmpty()) return emptyList()

        val allPaths = mutableListOf<List<ElectricalEdge>>()
        val seenSignatures = mutableSetOf<String>()

        startNodeIds.forEach { startNodeId ->
            endNodeIds.forEach { endNodeId ->
                val paths = findPathsBetweenNodes(
                    edges = graph.edges,
                    startNodeId = startNodeId,
                    endNodeId = endNodeId,
                    maxPaths = maxPaths,
                    maxDepth = maxDepth
                )

                paths.forEach { path ->
                    val signature = path.joinToString("|") {
                        it.connectionId ?: "${it.fromNodeId}->${it.toNodeId}"
                    }
                    if (seenSignatures.add(signature)) {
                        allPaths += path
                    }
                }
            }
        }

        return allPaths
    }

    fun hasPathBetweenNodes(
        edges: List<ElectricalEdge>,
        startNodeId: String,
        endNodeId: String
    ): Boolean {
        if (startNodeId == endNodeId) return true
        return findFirstPathBetweenNodes(edges, startNodeId, endNodeId) != null
    }

    fun findFirstPathBetweenNodes(
        edges: List<ElectricalEdge>,
        startNodeId: String,
        endNodeId: String,
        maxDepth: Int = 64
    ): List<ElectricalEdge>? {
        if (startNodeId == endNodeId) return emptyList()

        val adjacency = buildUndirectedAdjacency(edges)
        val visitedNodes = mutableSetOf(startNodeId)
        val path = mutableListOf<ElectricalEdge>()

        fun dfs(currentNodeId: String, depth: Int): Boolean {
            if (depth > maxDepth) return false
            if (currentNodeId == endNodeId) return true

            val neighbors = adjacency[currentNodeId].orEmpty()
            for ((nextNodeId, edge) in neighbors) {
                if (!visitedNodes.add(nextNodeId)) continue

                path += edge
                val found = dfs(nextNodeId, depth + 1)
                if (found) return true

                path.removeAt(path.lastIndex)
                visitedNodes.remove(nextNodeId)
            }

            return false
        }

        return if (dfs(startNodeId, 0)) path.toList() else null
    }

    fun findPathsBetweenNodes(
        edges: List<ElectricalEdge>,
        startNodeId: String,
        endNodeId: String,
        maxPaths: Int = 16,
        maxDepth: Int = 64
    ): List<List<ElectricalEdge>> {
        if (startNodeId == endNodeId) return listOf(emptyList())

        val adjacency = buildUndirectedAdjacency(edges)
        val paths = mutableListOf<List<ElectricalEdge>>()
        val visitedNodes = mutableSetOf(startNodeId)
        val currentPath = mutableListOf<ElectricalEdge>()

        fun dfs(currentNodeId: String, depth: Int) {
            if (paths.size >= maxPaths) return
            if (depth > maxDepth) return

            if (currentNodeId == endNodeId) {
                paths += currentPath.toList()
                return
            }

            val neighbors = adjacency[currentNodeId].orEmpty()
            for ((nextNodeId, edge) in neighbors) {
                if (paths.size >= maxPaths) return
                if (!visitedNodes.add(nextNodeId)) continue

                currentPath += edge
                dfs(nextNodeId, depth + 1)
                currentPath.removeAt(currentPath.lastIndex)
                visitedNodes.remove(nextNodeId)
            }
        }

        dfs(startNodeId, 0)
        return paths
    }

    private fun buildUndirectedAdjacency(
        edges: List<ElectricalEdge>
    ): Map<String, List<Pair<String, ElectricalEdge>>> {
        val adjacency = mutableMapOf<String, MutableList<Pair<String, ElectricalEdge>>>()

        edges.forEach { edge ->
            adjacency.getOrPut(edge.fromNodeId) { mutableListOf() }
                .add(edge.toNodeId to edge)
            adjacency.getOrPut(edge.toNodeId) { mutableListOf() }
                .add(edge.fromNodeId to edge)
        }

        return adjacency
    }
}

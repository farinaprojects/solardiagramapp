package br.com.solardiagram.domain.electrical

object ElectricalPathFinder {

    fun findPaths(
        graph: ElectricalGraph,
        startComponentId: String,
        endComponentId: String
    ): List<List<ElectricalEdge>> {

        val paths = mutableListOf<List<ElectricalEdge>>()

        val adjacency = graph.edges.groupBy { it.fromComponentId }

        fun dfs(
            currentComponent: String,
            visited: MutableSet<String>,
            path: MutableList<ElectricalEdge>
        ) {

            if (currentComponent == endComponentId) {
                paths.add(path.toList())
                return
            }

            val edges = adjacency[currentComponent] ?: return

            for (edge in edges) {

                if (visited.contains(edge.toComponentId)) continue

                visited.add(edge.toComponentId)
                path.add(edge)

                dfs(edge.toComponentId, visited, path)

                path.removeLast()
                visited.remove(edge.toComponentId)
            }
        }

        dfs(
            startComponentId,
            mutableSetOf(startComponentId),
            mutableListOf()
        )

        return paths
    }
}
package br.com.solardiagram.domain.electrical

import br.com.solardiagram.domain.model.ComponentType
import br.com.solardiagram.domain.model.DiagramProject

object ElectricalProtectionAnalyzer {

    fun analyze(
        project: DiagramProject,
        graph: ElectricalGraph,
        flows: List<ElectricalFlow>,
        installation: ElectricalInstallation
    ): List<ElectricalProtection> {
        val componentsById = project.components.associateBy { it.id }
        val centralQdgId = installation.centralQdgComponentId

        return project.components
            .filter { it.type == ComponentType.BREAKER }
            .mapNotNull { breaker ->
                val breakerFlows = flows.filter { breaker.id in it.componentIds }
                if (breakerFlows.isEmpty()) return@mapNotNull null

                val servesCentralQdg = centralQdgId != null && breakerFlows.any { centralQdgId in it.componentIds }
                val incomingMainFlows = breakerFlows.filter {
                    centralQdgId != null &&
                        it.kind in setOf(
                            ElectricalFlowKind.GRID_TO_LOAD,
                            ElectricalFlowKind.QDG_FALLBACK_TO_LOAD
                        ) &&
                        pathContainsInOrder(it.componentIds, breaker.id, centralQdgId)
                }
                val loadBranchFlows = breakerFlows.filter {
                    it.destinationType == ComponentType.LOAD &&
                        (centralQdgId == null || pathContainsInOrder(it.componentIds, centralQdgId, breaker.id))
                }
                val generationBranchFlows = breakerFlows.filter {
                    it.kind in setOf(
                        ElectricalFlowKind.GENERATION_TO_LOAD,
                        ElectricalFlowKind.GENERATION_TO_GRID_EXPORT
                    )
                }

                val role = when {
                    incomingMainFlows.isNotEmpty() -> ElectricalProtectionRole.MAIN_INCOMING
                    loadBranchFlows.isNotEmpty() && generationBranchFlows.isNotEmpty() -> ElectricalProtectionRole.MIXED_BRANCH
                    loadBranchFlows.isNotEmpty() -> ElectricalProtectionRole.LOAD_BRANCH
                    generationBranchFlows.isNotEmpty() -> ElectricalProtectionRole.GENERATION_BRANCH
                    else -> ElectricalProtectionRole.UNCLASSIFIED
                }

                val upstreamComponentIds = breakerFlows
                    .flatMap { flow -> collectUpstreamComponentIds(flow, breaker.id) }
                    .distinct()

                val downstreamComponentIds = breakerFlows
                    .flatMap { flow -> collectDownstreamComponentIds(flow, breaker.id) }
                    .distinct()

                val protectedComponentIds = breakerFlows
                    .flatMap { it.componentIds }
                    .filterNot { it == breaker.id }
                    .distinct()

                ElectricalProtection(
                    breakerComponentId = breaker.id,
                    breakerName = breaker.name,
                    role = role,
                    protectedFlowIds = breakerFlows.map { it.id }.distinct(),
                    upstreamComponentIds = upstreamComponentIds,
                    downstreamComponentIds = downstreamComponentIds,
                    protectedComponentIds = protectedComponentIds,
                    servesCentralQdg = servesCentralQdg,
                    isMainProtection = role == ElectricalProtectionRole.MAIN_INCOMING
                )
            }
            .sortedWith(
                compareByDescending<ElectricalProtection> { it.isMainProtection }
                    .thenBy { roleRank(it.role) }
                    .thenBy { it.breakerName }
            )
    }

    private fun collectUpstreamComponentIds(flow: ElectricalFlow, breakerId: String): List<String> {
        val index = flow.componentIds.indexOf(breakerId)
        if (index <= 0) return flow.componentIds.takeIf { breakerId !in it }.orEmpty()
        return flow.componentIds.subList(0, index)
    }

    private fun collectDownstreamComponentIds(flow: ElectricalFlow, breakerId: String): List<String> {
        val index = flow.componentIds.indexOf(breakerId)
        if (index == -1 || index >= flow.componentIds.lastIndex) return emptyList()
        return flow.componentIds.subList(index + 1, flow.componentIds.size)
    }

    private fun pathContainsInOrder(componentIds: List<String>, firstId: String, secondId: String): Boolean {
        val firstIndex = componentIds.indexOf(firstId)
        val secondIndex = componentIds.indexOf(secondId)
        return firstIndex != -1 && secondIndex != -1 && firstIndex < secondIndex
    }

    private fun roleRank(role: ElectricalProtectionRole): Int = when (role) {
        ElectricalProtectionRole.MAIN_INCOMING -> 0
        ElectricalProtectionRole.LOAD_BRANCH -> 1
        ElectricalProtectionRole.GENERATION_BRANCH -> 2
        ElectricalProtectionRole.MIXED_BRANCH -> 3
        ElectricalProtectionRole.UNCLASSIFIED -> 4
    }
}

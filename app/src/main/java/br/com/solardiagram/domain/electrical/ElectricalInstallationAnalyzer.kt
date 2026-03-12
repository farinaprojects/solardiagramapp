package br.com.solardiagram.domain.electrical

import br.com.solardiagram.domain.model.ComponentType
import br.com.solardiagram.domain.model.DiagramProject

object ElectricalInstallationAnalyzer {

    fun analyze(
        project: DiagramProject,
        graph: ElectricalGraph,
        flows: List<ElectricalFlow>
    ): ElectricalInstallation {
        val qdgIds = project.components
            .filter { it.type == ComponentType.QDG }
            .map { it.id }

        if (qdgIds.isEmpty()) {
            return ElectricalInstallation(
                centralQdgComponentId = null,
                qdgComponentIds = emptyList(),
                mainSupplyFlows = emptyList(),
                branchFlows = flows.filter { it.destinationType == ComponentType.LOAD },
                generationFlows = flows.filter { it.kind.isGenerationKind() },
                exportFlows = flows.filter { it.kind == ElectricalFlowKind.GENERATION_TO_GRID_EXPORT },
                detachedFlows = flows
            )
        }

        val centralQdgId = qdgIds.maxByOrNull { qdgId ->
            scoreQdg(qdgId = qdgId, graph = graph, flows = flows)
        }

        val mainSupplyFlows = flows.filter { flow ->
            flow.destinationComponentId == centralQdgId &&
                flow.sourceType in setOf(
                    ComponentType.GRID_SOURCE,
                    ComponentType.MICROINVERTER,
                    ComponentType.STRING_INVERTER,
                    ComponentType.QDG
                )
        }

        val branchFlows = flows.filter { flow ->
            flow.destinationType == ComponentType.LOAD &&
                centralQdgId != null &&
                flow.componentIds.contains(centralQdgId)
        }

        val generationFlows = flows.filter { flow ->
            flow.kind.isGenerationKind() && (
                centralQdgId == null ||
                    flow.componentIds.contains(centralQdgId) ||
                    flow.destinationComponentId == centralQdgId
                )
        }

        val exportFlows = generationFlows.filter { it.kind == ElectricalFlowKind.GENERATION_TO_GRID_EXPORT }

        val detachedFlows = flows.filterNot { flow ->
            flow in mainSupplyFlows ||
                flow in branchFlows ||
                flow in generationFlows ||
                (centralQdgId != null && flow.componentIds.contains(centralQdgId))
        }

        return ElectricalInstallation(
            centralQdgComponentId = centralQdgId,
            qdgComponentIds = qdgIds,
            mainSupplyFlows = mainSupplyFlows,
            branchFlows = branchFlows,
            generationFlows = generationFlows,
            exportFlows = exportFlows,
            detachedFlows = detachedFlows
        )
    }

    private fun scoreQdg(
        qdgId: String,
        graph: ElectricalGraph,
        flows: List<ElectricalFlow>
    ): Int {
        val degree = graph.adjacentComponentIds(qdgId).size
        val supplyHits = flows.count { flow ->
            flow.destinationComponentId == qdgId &&
                flow.kind in setOf(
                    ElectricalFlowKind.GRID_TO_LOAD,
                    ElectricalFlowKind.QDG_FALLBACK_TO_LOAD,
                    ElectricalFlowKind.GENERATION_TO_LOAD
                )
        }
        val branchHits = flows.count { flow ->
            flow.destinationType == ComponentType.LOAD && flow.componentIds.contains(qdgId)
        }
        val generationHits = flows.count { flow ->
            flow.kind.isGenerationKind() && (
                flow.componentIds.contains(qdgId) ||
                    flow.destinationComponentId == qdgId
                )
        }

        return degree * 10 + supplyHits * 20 + branchHits * 8 + generationHits * 6
    }

    private fun ElectricalFlowKind.isGenerationKind(): Boolean {
        return this == ElectricalFlowKind.GENERATION_TO_LOAD ||
            this == ElectricalFlowKind.GENERATION_TO_GRID_EXPORT
    }
}

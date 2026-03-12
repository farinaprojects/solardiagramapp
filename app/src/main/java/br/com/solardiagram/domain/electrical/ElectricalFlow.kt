package br.com.solardiagram.domain.electrical

import br.com.solardiagram.domain.model.ComponentType

enum class ElectricalFlowKind {
    GRID_TO_LOAD,
    GENERATION_TO_LOAD,
    GENERATION_TO_GRID_EXPORT,
    QDG_FALLBACK_TO_LOAD
}

data class ElectricalFlow(
    val id: String,
    val kind: ElectricalFlowKind,
    val sourceComponentId: String,
    val sourcePortId: String,
    val sourceType: ComponentType,
    val destinationComponentId: String,
    val destinationPortId: String,
    val destinationType: ComponentType,
    val pathEdges: List<ElectricalEdge>,
    val componentIds: List<String>
)

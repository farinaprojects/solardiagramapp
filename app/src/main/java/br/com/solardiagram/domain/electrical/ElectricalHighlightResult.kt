package br.com.solardiagram.domain.electrical

data class ElectricalHighlightResult(
    val circuitId: String?,
    val componentIds: Set<String>,
    val edges: List<ElectricalEdge>
)
package br.com.solardiagram.domain.electrical

data class ElectricalGraph(
    val nodes: List<ElectricalNode>,
    val edges: List<ElectricalEdge>
)
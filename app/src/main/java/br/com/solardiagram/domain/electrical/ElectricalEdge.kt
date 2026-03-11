package br.com.solardiagram.domain.electrical

data class ElectricalEdge(
    val fromComponentId: String,
    val fromPortId: String,
    val toComponentId: String,
    val toPortId: String
)
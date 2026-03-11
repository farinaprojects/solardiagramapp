package br.com.solardiagram.domain.electrical

data class ElectricalInternalConnection(
    val componentId: String,
    val fromPortId: String,
    val toPortId: String
)
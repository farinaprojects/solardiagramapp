package br.com.solardiagram.domain.electrical

import br.com.solardiagram.domain.model.ComponentType

data class ElectricalNode(
    val componentId: String,
    val componentName: String,
    val type: ComponentType,
    val terminals: List<ElectricalTerminal>
)
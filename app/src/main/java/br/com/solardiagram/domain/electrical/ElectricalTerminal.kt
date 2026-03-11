package br.com.solardiagram.domain.electrical

import br.com.solardiagram.domain.model.ElectricalPhase
import br.com.solardiagram.domain.model.PortKind

data class ElectricalTerminal(
    val componentId: String,
    val portId: String,
    val portName: String,
    val phase: ElectricalPhase?,
    val kind: PortKind
)
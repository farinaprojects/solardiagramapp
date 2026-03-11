package br.com.solardiagram.domain.electrical

import br.com.solardiagram.domain.model.ElectricalPhase
import br.com.solardiagram.domain.model.PortKind

data class ElectricalCircuit(
    val id: String,
    val phase: ElectricalPhase?,
    val kind: PortKind?,
    val terminalKeys: Set<Pair<String, String>>,
    val edges: List<ElectricalEdge>
)
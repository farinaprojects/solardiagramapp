package br.com.solardiagram.domain.electrical

import br.com.solardiagram.domain.model.ComponentType
import br.com.solardiagram.domain.model.CurrentKind
import br.com.solardiagram.domain.model.ElectricalPhase
import br.com.solardiagram.domain.model.PortKind

data class ElectricalCircuit(
    val id: String,
    val phase: ElectricalPhase?,
    val kind: PortKind?,
    val terminalKeys: Set<Pair<String, String>>,
    val edges: List<ElectricalEdge>,
    val sourceComponentId: String? = null,
    val sourceType: ComponentType? = null,
    val componentIds: List<String> = emptyList(),
    val protectionComponentIds: List<String> = emptyList(),
    val loadComponentIds: List<String> = emptyList(),
    val generationComponentIds: List<String> = emptyList(),
    val flowIds: List<String> = emptyList(),
    val currentKind: CurrentKind? = null,
    val nominalVoltageV: Double? = null,
    val aggregateCurrentA: Double? = null,
    val discoveredBy: String = "legacy"
)

package br.com.solardiagram.domain.electrical

import br.com.solardiagram.domain.model.ElectricalPhase
import br.com.solardiagram.domain.model.PortKind

object ElectricalHighlightEngine {

    fun highlightForComponent(
        circuits: List<ElectricalCircuit>,
        selectedComponentId: String
    ): ElectricalHighlightResult {
        val candidates = circuits.filter { circuit ->
            circuit.terminalKeys.any { (componentId, _) -> componentId == selectedComponentId }
        }

        if (candidates.isEmpty()) {
            return ElectricalHighlightResult(
                circuitId = null,
                componentIds = emptySet(),
                edges = emptyList()
            )
        }

        val best = candidates.maxWithOrNull(
            compareBy<ElectricalCircuit> { circuitPriority(it) }
                .thenBy { it.edges.size }
                .thenBy { it.terminalKeys.size }
        ) ?: candidates.first()

        val componentIds = best.terminalKeys
            .map { it.first }
            .toSet()

        return ElectricalHighlightResult(
            circuitId = best.id,
            componentIds = componentIds,
            edges = best.edges
        )
    }

    private fun circuitPriority(circuit: ElectricalCircuit): Int {
        val phase = circuit.phase
        val kind = circuit.kind

        if (isPreferredAcPhase(phase)) {
            return when (phase) {
                ElectricalPhase.L1 -> 100
                ElectricalPhase.L2 -> 99
                ElectricalPhase.L3 -> 98
                ElectricalPhase.N -> 97
                ElectricalPhase.PE -> 96
                else -> 95
            }
        }

        if (kind == PortKind.AC_L || kind == PortKind.AC_N || kind == PortKind.AC_PE || kind == PortKind.PE) {
            return 80
        }

        if (kind == PortKind.DC_POS || kind == PortKind.DC_NEG) {
            return 40
        }

        return 10
    }

    private fun isPreferredAcPhase(phase: ElectricalPhase?): Boolean {
        return when (phase) {
            ElectricalPhase.L1,
            ElectricalPhase.L2,
            ElectricalPhase.L3,
            ElectricalPhase.N,
            ElectricalPhase.PE -> true
            else -> false
        }
    }
}
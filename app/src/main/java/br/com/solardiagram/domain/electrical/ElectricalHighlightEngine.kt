package br.com.solardiagram.domain.electrical

import br.com.solardiagram.domain.model.ElectricalPhase
import br.com.solardiagram.domain.model.PortKind

object ElectricalHighlightEngine {

    fun highlightForComponent(
        circuits: List<ElectricalCircuit>,
        selectedComponentId: String,
        selectedComponentName: String? = null,
        selectedComponentType: String? = null
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
            compareByDescending<ElectricalCircuit> {
                circuitPriority(
                    circuit = it,
                    selectedComponentName = selectedComponentName,
                    selectedComponentType = selectedComponentType
                )
            }
                .thenByDescending { it.edges.size }
                .thenByDescending { it.terminalKeys.size }
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

    private fun circuitPriority(
        circuit: ElectricalCircuit,
        selectedComponentName: String?,
        selectedComponentType: String?
    ): Int {
        var score = baseCircuitPriority(circuit)

        val normalizedName = selectedComponentName
            ?.trim()
            ?.uppercase()
            .orEmpty()

        val normalizedType = selectedComponentType
            ?.trim()
            ?.uppercase()
            .orEmpty()

        if (normalizedName.contains("L1") && circuit.phase == ElectricalPhase.L1) score += 200
        if (normalizedName.contains("L2") && circuit.phase == ElectricalPhase.L2) score += 200
        if (normalizedName.contains("L3") && circuit.phase == ElectricalPhase.L3) score += 200
        if (normalizedName.contains(" N") && circuit.phase == ElectricalPhase.N) score += 200
        if (normalizedName == "N" && circuit.phase == ElectricalPhase.N) score += 200
        if (normalizedName.contains("PE") && circuit.phase == ElectricalPhase.PE) score += 200
        if (normalizedName.contains("TERRA") && circuit.phase == ElectricalPhase.PE) score += 200

        if (normalizedType.contains("BARN") && circuit.phase == ElectricalPhase.N) score += 200
        if (normalizedType.contains("BARPE") && circuit.phase == ElectricalPhase.PE) score += 200
        if (normalizedType.contains("GROUND") && circuit.phase == ElectricalPhase.PE) score += 200

        if (normalizedType.contains("BARL")) {
            when {
                normalizedName.contains("L1") && circuit.phase == ElectricalPhase.L1 -> score += 220
                normalizedName.contains("L2") && circuit.phase == ElectricalPhase.L2 -> score += 220
                normalizedName.contains("L3") && circuit.phase == ElectricalPhase.L3 -> score += 220
            }
        }

        return score
    }

    private fun baseCircuitPriority(circuit: ElectricalCircuit): Int {
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

        if (
            kind == PortKind.AC_L ||
            kind == PortKind.AC_N ||
            kind == PortKind.AC_PE ||
            kind == PortKind.PE
        ) {
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
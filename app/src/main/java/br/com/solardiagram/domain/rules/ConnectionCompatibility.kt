package br.com.solardiagram.domain.rules

import br.com.solardiagram.domain.model.ElectricalPhase
import br.com.solardiagram.domain.model.Port
import br.com.solardiagram.domain.model.PortDirection
import br.com.solardiagram.domain.model.PortKind

object ConnectionCompatibility {
    fun isCompatible(a: PortKind, b: PortKind): Boolean {
        val dc = setOf(PortKind.DC_POS, PortKind.DC_NEG)
        val ac = setOf(PortKind.AC_L, PortKind.AC_N)
        val pe = setOf(PortKind.AC_PE, PortKind.PE)

        return when {
            a in dc && b in dc -> true
            a in ac && b in ac -> a == b
            a in pe && b in pe -> true
            else -> false
        }
    }

    fun isCompatible(a: Port, b: Port): Boolean {
        if (!isCompatible(a.kind, b.kind)) return false

        val aSpec = a.spec
        val bSpec = b.spec
        if (aSpec != null && bSpec != null) {
            val aPhase = aSpec.phase
            val bPhase = bSpec.phase
            if (aPhase != ElectricalPhase.NONE && bPhase != ElectricalPhase.NONE && aPhase != bPhase) {
                if (!(a.kind == PortKind.DC_POS || a.kind == PortKind.DC_NEG)) return false
            }
        }

        if (a.direction == PortDirection.BIDIRECTIONAL || b.direction == PortDirection.BIDIRECTIONAL) return true
        return a.direction != b.direction
    }
}

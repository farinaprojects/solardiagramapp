package br.com.solardiagram.domain.engine

import br.com.solardiagram.domain.model.*

data class AmpacityInput(
    val mm2: Double,
    val material: ConductorMaterial,
    val installation: InstallationMethod,
    val insulation: InsulationClass,
    val ambientTempC: Double,
    val grouping: CircuitGrouping
)

interface AmpacityCatalog {
    fun baseAmpacityA(input: AmpacityInput): Double?
    fun tempFactor(ambientTempC: Double, insulation: InsulationClass): Double
    fun groupingFactor(grouping: CircuitGrouping): Double
}

/**
 * Catálogo DEFAULT (placeholders). Substitua pelos valores normativos.
 */
class DefaultAmpacityCatalog : AmpacityCatalog {
    private val cuConduit = mapOf(1.5 to 15.0, 2.5 to 21.0, 4.0 to 28.0, 6.0 to 36.0, 10.0 to 50.0, 16.0 to 68.0, 25.0 to 89.0, 35.0 to 110.0)
    private val alConduit = mapOf(10.0 to 40.0, 16.0 to 55.0, 25.0 to 72.0, 35.0 to 88.0)

    override fun baseAmpacityA(input: AmpacityInput): Double? {
        return when (input.material) {
            ConductorMaterial.CU -> when (input.installation) {
                InstallationMethod.CONDUIT_EXPOSED, InstallationMethod.CONDUIT_EMBEDDED -> cuConduit[input.mm2]
                InstallationMethod.TRAY, InstallationMethod.FREE_AIR -> cuConduit[input.mm2]?.times(1.15)
            }
            ConductorMaterial.AL -> when (input.installation) {
                InstallationMethod.CONDUIT_EXPOSED, InstallationMethod.CONDUIT_EMBEDDED -> alConduit[input.mm2]
                InstallationMethod.TRAY, InstallationMethod.FREE_AIR -> alConduit[input.mm2]?.times(1.10)
            }
        }
    }

    override fun tempFactor(ambientTempC: Double, insulation: InsulationClass): Double {
        val delta = (ambientTempC - 30.0).coerceAtLeast(0.0)
        val factor = 1.0 - (delta * 0.006)
        return factor.coerceIn(0.70, 1.0)
    }

    override fun groupingFactor(grouping: CircuitGrouping): Double {
        return when (grouping) {
            CircuitGrouping.G1 -> 1.00
            CircuitGrouping.G2 -> 0.80
            CircuitGrouping.G3 -> 0.70
            CircuitGrouping.G4PLUS -> 0.60
        }
    }
}

class AmpacityEngine(private val catalog: AmpacityCatalog = DefaultAmpacityCatalog()) {
    fun allowableCurrentA(input: AmpacityInput): Double? {
        val base = catalog.baseAmpacityA(input) ?: return null
        val kt = catalog.tempFactor(input.ambientTempC, input.insulation)
        val kg = catalog.groupingFactor(input.grouping)
        return base * kt * kg
    }
}

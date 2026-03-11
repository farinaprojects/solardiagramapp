package br.com.solardiagram.domain.engine

import br.com.solardiagram.domain.model.CurrentKind
import br.com.solardiagram.domain.model.SystemPhase
import kotlin.math.max

data class CableSizingInput(
    val currentA: Double,
    val voltageV: Double,
    val lengthMeters: Double,
    val phases: SystemPhase,
    val currentKind: CurrentKind,
    val maxVoltageDropPercent: Double,
    val safetyFactor: Double
)

data class CableSizingRecommendation(
    val selectedMm2: Double,
    val estimatedVoltageDropPercent: Double
)

class CableSizingEngine {
    // Simple resistivity-based approximations (placeholder).
    // Replace with proper tables later.
    private fun resistanceOhmPerKm(mm2: Double, kind: CurrentKind): Double {
        // Copper at 20°C approx: R = 17.241/mm2 Ω·mm²/km -> Ω/km
        val rCu = 17.241 / mm2
        // DC uses same for now; AC additional effects ignored (placeholder)
        return rCu
    }

    fun recommend(input: CableSizingInput, overrideMm2: Double? = null): CableSizingRecommendation {
        val mm2 = overrideMm2 ?: pickByDrop(input)
        val drop = estimateVoltageDropPercent(input, mm2)
        return CableSizingRecommendation(selectedMm2 = mm2, estimatedVoltageDropPercent = drop)
    }

    private fun pickByDrop(input: CableSizingInput): Double {
        val options = listOf(1.5, 2.5, 4.0, 6.0, 10.0, 16.0, 25.0, 35.0, 50.0)
        for (mm2 in options) {
            val d = estimateVoltageDropPercent(input, mm2)
            if (d <= input.maxVoltageDropPercent) return mm2
        }
        return options.last()
    }

    private fun estimateVoltageDropPercent(input: CableSizingInput, mm2: Double): Double {
        val i = input.currentA * input.safetyFactor
        val r = resistanceOhmPerKm(mm2, input.currentKind)
        val lengthKm = max(input.lengthMeters, 0.0) / 1000.0
        // round-trip factor 2 for single-phase approximation
        val vdrop = i * r * lengthKm * 2.0
        return (vdrop / input.voltageV) * 100.0
    }
}

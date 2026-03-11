package br.com.solardiagram.domain.rules

data class NormProfile(
    val maxVoltageDropPercentAc: Double = 4.0,
    val maxVoltageDropPercentDc: Double = 3.0,
    val acCurrentSafetyFactor: Double = 1.25,
    val dcCurrentSafetyFactor: Double = 1.25
)

object NormProfiles {
    val BR_BASE = NormProfile()
}

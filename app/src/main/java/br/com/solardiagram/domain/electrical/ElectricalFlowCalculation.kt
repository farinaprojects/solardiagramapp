package br.com.solardiagram.domain.electrical

data class ElectricalFlowCalculation(

    val flowId: String,

    val currentA: Double?,

    val voltage: Double?,

    val powerW: Double?,

    val pathLengthMeters: Double,

    val segmentsWithoutLength: Int,

    val voltageDropV: Double?,

    val voltageDropPercent: Double?,

    val requiredAmpacity: Double?,

    val limitingCableAmpacity: Double?,

    val smallestCableMm2: Double?,

    val recommendedCableMm2: Double?,

    val notes: List<String>
)
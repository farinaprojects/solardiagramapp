package br.com.solardiagram.domain.engine

import br.com.solardiagram.domain.electrical.ElectricalFlowCalculation

class ElectricalCalculationEngine {

    fun evaluate(context: ProjectValidationContext): Map<String, ElectricalFlowCalculation> {

        val results = mutableMapOf<String, ElectricalFlowCalculation>()

        context.flows.forEach { flow ->

            val notes = mutableListOf<String>()

            notes += "Comprimento de cabos ainda não modelado no domínio."

            val calc = ElectricalFlowCalculation(
                flowId = flow.id,
                currentA = null,
                voltage = null,
                powerW = null,
                pathLengthMeters = 0.0,
                segmentsWithoutLength = flow.componentIds.size,
                voltageDropV = null,
                voltageDropPercent = null,
                requiredAmpacity = null,
                limitingCableAmpacity = null,
                smallestCableMm2 = null,
                recommendedCableMm2 = null,
                notes = notes
            )

            results[flow.id] = calc
        }

        return results
    }
}
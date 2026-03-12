package br.com.solardiagram.domain.engine

class CircuitValidationEngine {

    fun evaluate(context: ProjectValidationContext): List<ValidationIssue> {
        val issues = mutableListOf<ValidationIssue>()

        context.circuits.forEach { circuit ->
            if (circuit.terminalKeys.size != 1) return@forEach

            val terminalKey = circuit.terminalKeys.first()
            val componentId = terminalKey.first
            val portId = terminalKey.second

            val component = context.component(componentId)
            val port = context.port(componentId, portId)

            val componentName = component?.name ?: componentId
            val portName = port?.name ?: portId

            issues += ValidationIssue(
                id = "circuit-single-terminal-${circuit.id}",
                severity = Severity.WARNING,
                code = "CIRCUIT_SINGLE_TERMINAL",
                message = "Circuito com apenas um terminal detectado em ${componentName} (${portName}). Verifique se há ligação incompleta ou componente eletricamente isolado.",
                componentId = componentId,
                category = ValidationCategory.CIRCUIT,
                componentType = component?.type
            )
        }

        return issues
    }
}
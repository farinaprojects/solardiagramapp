package br.com.solardiagram.domain.engine

import br.com.solardiagram.domain.model.DiagramProject
import br.com.solardiagram.domain.model.PortDirection
import br.com.solardiagram.domain.model.PortKind
import br.com.solardiagram.domain.rules.ConnectionCompatibility

class StructuralConnectionValidationEngine {

    fun evaluate(project: DiagramProject): List<ValidationIssue> {
        val issues = mutableListOf<ValidationIssue>()
        val compsById = project.components.associateBy { it.id }
        val portUsage = mutableMapOf<Pair<String, String>, Int>()

        project.connections.forEach { conn ->
            val fromComp = compsById[conn.fromComponentId]
            val toComp = compsById[conn.toComponentId]
            val fromPort = fromComp?.portById(conn.fromPortId)
            val toPort = toComp?.portById(conn.toPortId)

            if (fromComp == null || toComp == null || fromPort == null || toPort == null) {
                issues += ValidationIssue(
                    id = "missing-${conn.id}",
                    severity = Severity.ERROR,
                    code = "STRUCT_MISSING_ENDPOINT",
                    message = "Conexão possui componente ou porta inexistente.",
                    connectionId = conn.id,
                    category = ValidationCategory.STRUCTURAL
                )
                return@forEach
            }

            portUsage[conn.fromComponentId to conn.fromPortId] = (portUsage[conn.fromComponentId to conn.fromPortId] ?: 0) + 1
            portUsage[conn.toComponentId to conn.toPortId] = (portUsage[conn.toComponentId to conn.toPortId] ?: 0) + 1

            if (!ConnectionCompatibility.isCompatible(fromPort, toPort)) {
                issues += ValidationIssue(
                    id = "compat-${conn.id}",
                    severity = Severity.ERROR,
                    code = "STRUCT_PORT_INCOMPATIBLE",
                    message = "Conexão incompatível entre ${fromPort.kind}/${fromPort.direction} e ${toPort.kind}/${toPort.direction}.",
                    connectionId = conn.id,
                    category = ValidationCategory.STRUCTURAL
                )
            }
        }

        project.components.forEach { comp ->
            comp.ports.forEach { port ->
                val count = portUsage[comp.id to port.id] ?: 0
                val isProtective = port.kind == PortKind.PE || port.kind == PortKind.AC_PE
                val isRequired = port.spec?.required == true || (port.direction == PortDirection.INPUT && !isProtective)
                val maxConnections = port.spec?.maxConnections ?: if (port.direction == PortDirection.BIDIRECTIONAL) Int.MAX_VALUE else 1
                when {
                    isRequired && count == 0 -> {
                        val label = when (port.direction) {
                            PortDirection.INPUT -> "Entrada"
                            PortDirection.OUTPUT -> "Saída"
                            PortDirection.BIDIRECTIONAL -> "Terminal"
                        }
                        issues += ValidationIssue(
                            id = "unwired-${comp.id}-${port.id}",
                            severity = Severity.WARNING,
                            code = "STRUCT_REQUIRED_PORT_OPEN",
                            message = "$label ${port.name} de ${comp.name} está sem conexão.",
                            componentId = comp.id,
                            category = ValidationCategory.STRUCTURAL,
                            componentType = comp.type
                        )
                    }
                    count > maxConnections -> {
                        issues += ValidationIssue(
                            id = "multi-${comp.id}-${port.id}",
                            severity = Severity.WARNING,
                            code = "STRUCT_PORT_MULTI_TAP",
                            message = "Porta ${port.name} de ${comp.name} excedeu o limite de conexões permitido ($maxConnections).",
                            componentId = comp.id,
                            category = ValidationCategory.STRUCTURAL,
                            componentType = comp.type
                        )
                    }
                }
            }
        }

        return issues
    }
}

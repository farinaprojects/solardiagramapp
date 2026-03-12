package br.com.solardiagram.domain.engine

import br.com.solardiagram.domain.electrical.ElectricalGraph
import br.com.solardiagram.domain.model.Component
import br.com.solardiagram.domain.model.ComponentType
import br.com.solardiagram.domain.model.DiagramProject
import br.com.solardiagram.domain.model.Port
import br.com.solardiagram.domain.model.PortDirection
import br.com.solardiagram.domain.model.PortKind

class TopologyValidationEngine {

    fun evaluate(project: DiagramProject, graph: ElectricalGraph): List<ValidationIssue> {
        val issues = mutableListOf<ValidationIssue>()

        project.components.forEach { component ->
            val edges = graph.edgesForComponent(component.id)
            val neighbors = graph.adjacentComponentIds(component.id)
            val relevantPorts = component.ports.filter(::isRelevantPort)

            if (relevantPorts.isNotEmpty() && edges.isEmpty()) {
                issues += ValidationIssue(
                    id = "topo-orphan-${component.id}",
                    severity = Severity.WARNING,
                    code = "TOPO_COMPONENT_ORPHAN",
                    message = "Componente ${component.name} está isolado no diagrama, sem conexões elétricas.",
                    componentId = component.id,
                    category = ValidationCategory.TOPOLOGY,
                    componentType = component.type
                )
            }

            relevantPorts.forEach { port ->
                val portEdgeCount = graph.edgesForNode(graph.nodeId(component.id, port.id)).size
                if (isRequiredPort(component, port) && portEdgeCount == 0) {
                    issues += ValidationIssue(
                        id = "topo-open-${component.id}-${port.id}",
                        severity = Severity.WARNING,
                        code = "TOPO_REQUIRED_PORT_OPEN",
                        message = "Porta obrigatória ${port.name} de ${component.name} está desconectada.",
                        componentId = component.id,
                        category = ValidationCategory.TOPOLOGY,
                        componentType = component.type
                    )
                }
            }

            if (
                component.type == ComponentType.AC_BUS ||
                component.type == ComponentType.BARL ||
                component.type == ComponentType.BARN ||
                component.type == ComponentType.BARPE
            ) {
                val nonProtectiveConnectedPorts = component.ports.count { port ->
                    port.kind != PortKind.PE &&
                            graph.edgesForNode(graph.nodeId(component.id, port.id)).isNotEmpty()
                }

                if (nonProtectiveConnectedPorts == 0) {
                    issues += ValidationIssue(
                        id = "topo-bus-empty-${component.id}",
                        severity = Severity.WARNING,
                        code = "TOPO_AC_BUS_UNCONNECTED",
                        message = "Barramento AC ${component.name} não possui conexões elétricas úteis.",
                        componentId = component.id,
                        category = ValidationCategory.TOPOLOGY,
                        componentType = component.type
                    )
                } else if (neighbors.size < 2) {
                    issues += ValidationIssue(
                        id = "topo-bus-path-${component.id}",
                        severity = Severity.WARNING,
                        code = "TOPO_AC_BUS_INCOMPLETE_PATH",
                        message = "Barramento AC ${component.name} está ligado a menos de dois componentes; verifique a composição do circuito.",
                        componentId = component.id,
                        category = ValidationCategory.TOPOLOGY,
                        componentType = component.type
                    )
                }
            }
        }

        val groups = graph.connectedComponentGroups()
        if (groups.size > 1) {
            groups.drop(1).forEachIndexed { index, group ->
                val sample = project.components.firstOrNull { it.id in group } ?: return@forEachIndexed
                issues += ValidationIssue(
                    id = "topo-group-${index + 1}-${sample.id}",
                    severity = Severity.INFO,
                    code = "TOPO_DISCONNECTED_SUBGRAPH",
                    message = "Existe um subconjunto elétrico desconectado contendo ${group.size} componente(s), por exemplo ${sample.name}.",
                    componentId = sample.id,
                    category = ValidationCategory.TOPOLOGY,
                    componentType = sample.type
                )
            }
        }

        return issues.distinctBy { it.id }
    }

    private fun isRelevantPort(port: Port): Boolean = port.kind != PortKind.PE

    private fun isRequiredPort(component: Component, port: Port): Boolean {
        if (port.kind == PortKind.PE) return false
        port.spec?.let { return it.required }

        return when (component.type) {
            ComponentType.PV_MODULE,
            ComponentType.MICROINVERTER,
            ComponentType.STRING_INVERTER,
            ComponentType.BREAKER,
            ComponentType.QDG,
            ComponentType.LOAD -> port.direction != PortDirection.BIDIRECTIONAL

            ComponentType.AC_BUS,
            ComponentType.BARL,
            ComponentType.BARN,
            ComponentType.BARPE -> false

            ComponentType.DPS -> false
            ComponentType.GROUND_BAR -> false
        }
    }
}
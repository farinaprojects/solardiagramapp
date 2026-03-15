package br.com.solardiagram.domain.engine

import br.com.solardiagram.domain.electrical.ElectricalGraph
import br.com.solardiagram.domain.model.Component
import br.com.solardiagram.domain.model.ComponentType
import br.com.solardiagram.domain.model.DiagramProject
import br.com.solardiagram.domain.model.ElectricalSpecs
import br.com.solardiagram.domain.model.Port
import br.com.solardiagram.domain.model.PortDirection
import br.com.solardiagram.domain.model.PortKind
import kotlin.math.abs

class TopologyValidationEngine {

    fun evaluate(project: DiagramProject, graph: ElectricalGraph): List<ValidationIssue> {
        val context = ProjectValidationContext(project = project, graph = graph)
        return evaluate(context)
    }

    fun evaluate(context: ProjectValidationContext): List<ValidationIssue> {
        val project = context.project
        val graph = context.graph
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

        val graphAnalysis = context.graphAnalysis

        if (graphAnalysis.sourceComponentIds.isEmpty() && project.components.any { it.type == ComponentType.LOAD }) {
            issues += ValidationIssue(
                id = "topo-no-source-project",
                severity = Severity.WARNING,
                code = "TOPO_NO_ACTIVE_SOURCE",
                message = "O diagrama possui cargas, mas nenhuma origem elétrica ativa foi identificada. Adicione rede, microinversor, inversor string ou alimentação equivalente.",
                category = ValidationCategory.TOPOLOGY
            )
        }

        graphAnalysis.unreachableLoadComponentIds.forEach { componentId ->
            val component = project.components.firstOrNull { it.id == componentId } ?: return@forEach
            issues += ValidationIssue(
                id = "topo-load-unreachable-${component.id}",
                severity = Severity.WARNING,
                code = "TOPO_LOAD_NOT_REACHABLE_FROM_SOURCE",
                message = "Carga ${component.name} não está eletricamente alcançável a partir de nenhuma fonte do diagrama. Verifique barramentos, disjuntores e continuidade do circuito.",
                componentId = component.id,
                category = ValidationCategory.TOPOLOGY,
                componentType = component.type
            )
        }

        graphAnalysis.unreachableGenerationComponentIds.forEach { componentId ->
            val component = project.components.firstOrNull { it.id == componentId } ?: return@forEach
            issues += ValidationIssue(
                id = "topo-generation-unreachable-${component.id}",
                severity = Severity.INFO,
                code = "TOPO_GENERATION_ISLAND",
                message = "Fonte de geração ${component.name} não alcança nenhuma malha alimentada principal. Verifique se o equipamento está integrado ao barramento, quadro ou ponto de exportação.",
                componentId = component.id,
                category = ValidationCategory.TOPOLOGY,
                componentType = component.type
            )
        }

        val voltageProfile = context.voltageProfile

        voltageProfile.conflictingComponentIds.forEach { componentId ->
            val component = context.component(componentId) ?: return@forEach
            issues += ValidationIssue(
                id = "topo-voltage-conflict-${component.id}",
                severity = Severity.WARNING,
                code = "TOPO_VOLTAGE_CONFLICT",
                message = "Componente ${component.name} recebe tensões nominais conflitantes na malha propagada. Revise fontes, barramentos e compatibilidade entre subsistemas AC/DC.",
                componentId = component.id,
                category = ValidationCategory.TOPOLOGY,
                componentType = component.type
            )
        }

        project.components.forEach { component ->
            when (val specs = component.specs) {
                is ElectricalSpecs.BreakerSpecs -> {
                    val aggregateCurrent = context.currentProfile.currentAtComponent(component.id, specs.applicableTo)?.nominalCurrentA
                    if (aggregateCurrent != null && aggregateCurrent > specs.ratedCurrentA) {
                        issues += ValidationIssue(
                            id = "topo-breaker-current-over-${component.id}",
                            severity = if (aggregateCurrent > specs.ratedCurrentA * 1.15) Severity.ERROR else Severity.WARNING,
                            code = "TOPO_BREAKER_AGGREGATE_CURRENT_EXCEEDED",
                            message = "Disjuntor ${component.name} está atravessado por corrente agregada estimada de ${fmt(aggregateCurrent)} A, acima da corrente nominal de ${fmt(specs.ratedCurrentA)} A. Revise a topologia do ramal e o dimensionamento da proteção.",
                            componentId = component.id,
                            category = ValidationCategory.TOPOLOGY,
                            componentType = component.type
                        )
                    }
                }

                is ElectricalSpecs.QdgSpecs -> {
                    val aggregateCurrent = context.currentProfile.currentAtComponent(component.id, br.com.solardiagram.domain.model.CurrentKind.AC)?.nominalCurrentA
                    if (aggregateCurrent != null && aggregateCurrent > specs.maxBusCurrentA) {
                        issues += ValidationIssue(
                            id = "topo-qdg-current-over-${component.id}",
                            severity = if (aggregateCurrent > specs.maxBusCurrentA * 1.15) Severity.ERROR else Severity.WARNING,
                            code = "TOPO_QDG_AGGREGATE_CURRENT_EXCEEDED",
                            message = "QDG ${component.name} concentra corrente agregada estimada de ${fmt(aggregateCurrent)} A, acima da capacidade nominal de barramento de ${fmt(specs.maxBusCurrentA)} A. Revise a distribuição dos ramais e a capacidade do quadro.",
                            componentId = component.id,
                            category = ValidationCategory.TOPOLOGY,
                            componentType = component.type
                        )
                    }
                }

                is ElectricalSpecs.AcBusSpecs -> {
                    val aggregateCurrent = context.currentProfile.currentAtComponent(component.id, br.com.solardiagram.domain.model.CurrentKind.AC)?.nominalCurrentA
                    if (aggregateCurrent != null && aggregateCurrent > specs.maxBusCurrentA) {
                        issues += ValidationIssue(
                            id = "topo-bus-current-over-${component.id}",
                            severity = if (aggregateCurrent > specs.maxBusCurrentA * 1.15) Severity.ERROR else Severity.WARNING,
                            code = "TOPO_AC_BUS_AGGREGATE_CURRENT_EXCEEDED",
                            message = "Barramento ${component.name} concentra corrente agregada estimada de ${fmt(aggregateCurrent)} A, acima da capacidade nominal de ${fmt(specs.maxBusCurrentA)} A. Revise o agrupamento de circuitos e a capacidade do barramento.",
                            componentId = component.id,
                            category = ValidationCategory.TOPOLOGY,
                            componentType = component.type
                        )
                    }
                }

                else -> Unit
            }
        }

        project.components
            .filter { it.type == ComponentType.LOAD }
            .forEach { component ->
                val loadSpecs = component.specs as? ElectricalSpecs.LoadSpecs ?: return@forEach
                val propagated = voltageProfile.nominalVoltageAtComponent(component.id) ?: return@forEach
                if (abs(propagated - loadSpecs.voltageV) > loadSpecs.voltageV * 0.15) {
                    issues += ValidationIssue(
                        id = "topo-load-voltage-mismatch-${component.id}",
                        severity = Severity.WARNING,
                        code = "TOPO_LOAD_VOLTAGE_MISMATCH",
                        message = "Carga ${component.name} está configurada para ${fmt(loadSpecs.voltageV)} V, mas a malha propagada indica aproximadamente ${fmt(propagated)} V. Verifique a topologia e a tensão nominal do circuito.",
                        componentId = component.id,
                        category = ValidationCategory.TOPOLOGY,
                        componentType = component.type
                    )
                }
            }

        val groups = graphAnalysis.componentGroups
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
            ComponentType.GRID_SOURCE,
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

    private fun fmt(value: Double): String = String.format("%.1f", value)
}

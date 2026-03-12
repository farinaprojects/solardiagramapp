package br.com.solardiagram.domain.engine

import br.com.solardiagram.domain.electrical.ElectricalCircuit
import br.com.solardiagram.domain.electrical.ElectricalCircuitAnalyzer
import br.com.solardiagram.domain.electrical.ElectricalEdge
import br.com.solardiagram.domain.electrical.ElectricalGraph
import br.com.solardiagram.domain.model.Component
import br.com.solardiagram.domain.model.ComponentType
import br.com.solardiagram.domain.model.Connection
import br.com.solardiagram.domain.model.DiagramProject
import br.com.solardiagram.domain.model.Port

data class ProjectValidationOutput(
    val report: ValidationReport,
    val graph: ElectricalGraph? = null
)

data class ValidationReport(
    val issues: List<ValidationIssue> = emptyList(),
    val summary: ValidationSummary = ValidationSummary.from(issues)
)

enum class Severity { INFO, WARNING, ERROR }

enum class ValidationCategory {
    GENERAL,
    STRUCTURAL,
    TOPOLOGY,
    CIRCUIT,
    COMPONENT_RULE,
    VOLTAGE_DROP,
    AMPACITY
}

data class ValidationIssue(
    val id: String,
    val severity: Severity,
    val code: String,
    val message: String,
    val componentId: String? = null,
    val connectionId: String? = null,
    val category: ValidationCategory = ValidationCategory.GENERAL,
    val componentType: ComponentType? = null
)

data class ValidationSummary(
    val total: Int = 0,
    val errors: Int = 0,
    val warnings: Int = 0,
    val infos: Int = 0,
    val byCategory: Map<ValidationCategory, Int> = emptyMap(),
    val byComponentType: Map<ComponentType, Int> = emptyMap()
) {
    companion object {
        fun from(issues: List<ValidationIssue>): ValidationSummary = ValidationSummary(
            total = issues.size,
            errors = issues.count { it.severity == Severity.ERROR },
            warnings = issues.count { it.severity == Severity.WARNING },
            infos = issues.count { it.severity == Severity.INFO },
            byCategory = ValidationCategory.entries.associateWith { category ->
                issues.count { it.category == category }
            }.filterValues { it > 0 },
            byComponentType = issues.mapNotNull { it.componentType }.groupingBy { it }.eachCount()
        )
    }
}

data class ProjectValidationContext(
    val project: DiagramProject,
    val graph: ElectricalGraph
) {
    private val componentsById = project.components.associateBy { it.id }
    private val connectionsById = project.connections.associateBy { it.id }

    val circuits: List<ElectricalCircuit> by lazy {
        ElectricalCircuitAnalyzer.analyze(graph)
    }

    fun component(componentId: String): Component? = componentsById[componentId]

    fun connection(connectionId: String?): Connection? = connectionId?.let { connectionsById[it] }

    fun connectionForEdge(edge: ElectricalEdge): Connection? = connection(edge.connectionId)

    fun port(componentId: String, portId: String): Port? = component(componentId)?.portById(portId)

    fun enrich(issue: ValidationIssue): ValidationIssue {
        val inferredType = issue.componentType
            ?: issue.componentId?.let { componentsById[it]?.type }
            ?: inferTypeFromConnection(issue.connectionId)

        val inferredCategory = if (issue.category != ValidationCategory.GENERAL) {
            issue.category
        } else {
            inferCategory(issue.code)
        }

        return issue.copy(
            category = inferredCategory,
            componentType = inferredType
        )
    }

    private fun inferTypeFromConnection(connectionId: String?): ComponentType? {
        val edge = graph.edges.firstOrNull { it.connectionId == connectionId } ?: return null
        return componentsById[edge.fromComponentId]?.type
            ?: componentsById[edge.toComponentId]?.type
    }

    private fun inferCategory(code: String): ValidationCategory {
        return when {
            code.startsWith("STRUCT_") -> ValidationCategory.STRUCTURAL
            code.startsWith("TOPO_") -> ValidationCategory.TOPOLOGY
            code.startsWith("CIRCUIT_") -> ValidationCategory.CIRCUIT
            code.startsWith("VDROP_") -> ValidationCategory.VOLTAGE_DROP
            code.startsWith("AMP_") -> ValidationCategory.AMPACITY
            code.startsWith("COMP_") -> ValidationCategory.COMPONENT_RULE
            else -> ValidationCategory.GENERAL
        }
    }
}
package br.com.solardiagram.domain.engine

import br.com.solardiagram.domain.electrical.ElectricalGraphBuilder
import br.com.solardiagram.domain.model.ComponentType
import br.com.solardiagram.domain.model.DiagramProject
import br.com.solardiagram.domain.rules.NormProfile
import br.com.solardiagram.domain.rules.NormProfiles

class ProjectValidationEngine(
    private val norm: NormProfile = NormProfiles.BR_BASE,
    private val graphBuilder: ElectricalGraphBuilder = ElectricalGraphBuilder,
    private val structuralEngine: StructuralConnectionValidationEngine = StructuralConnectionValidationEngine(),
    private val topologyEngine: TopologyValidationEngine = TopologyValidationEngine(),
    private val circuitEngine: CircuitValidationEngine = CircuitValidationEngine(),
    private val componentRuleEngine: ComponentRuleValidationEngine = ComponentRuleValidationEngine(),
    private val voltageDropEngine: ConnectionVoltageDropEngine = ConnectionVoltageDropEngine(norm),
    private val ampacityEngine: ConnectionAmpacityEngine = ConnectionAmpacityEngine(norm),
    private val breakerLoadEngine: BreakerLoadValidationEngine = BreakerLoadValidationEngine()
) {

    fun validate(project: DiagramProject): ProjectValidationOutput {
        val graph = graphBuilder.build(project)
        val context = ProjectValidationContext(project = project, graph = graph)

        val issues = buildList {
            addAll(structuralEngine.evaluate(project))
            addAll(topologyEngine.evaluate(project, graph))
            addAll(circuitEngine.evaluate(context))
            addAll(componentRuleEngine.evaluate(project))
            addAll(voltageDropEngine.evaluate(context))
            addAll(ampacityEngine.evaluate(context))
            addAll(breakerLoadEngine.evaluate(project))
        }
            .map { context.enrich(it) }
            .dedupe()
            .sortedWith(validationIssueComparator())

        val report = ValidationReport(
            issues = issues,
            summary = ValidationSummary.from(issues)
        )
        return ProjectValidationOutput(report = report, graph = graph)
    }

    private fun List<ValidationIssue>.dedupe(): List<ValidationIssue> =
        distinctBy { listOf(it.id, it.code, it.componentId, it.connectionId).joinToString("|") }

    private fun validationIssueComparator(): Comparator<ValidationIssue> = compareBy<ValidationIssue>(
        { severityRank(it.severity) },
        { categoryRank(it.category) },
        { componentTypeRank(it.componentType) },
        { it.code },
        { it.componentId ?: "" },
        { it.connectionId ?: "" }
    )

    private fun severityRank(severity: Severity): Int = when (severity) {
        Severity.ERROR -> 0
        Severity.WARNING -> 1
        Severity.INFO -> 2
    }

    private fun categoryRank(category: ValidationCategory): Int = when (category) {
        ValidationCategory.STRUCTURAL -> 0
        ValidationCategory.TOPOLOGY -> 1
        ValidationCategory.CIRCUIT -> 2
        ValidationCategory.COMPONENT_RULE -> 3
        ValidationCategory.AMPACITY -> 4
        ValidationCategory.VOLTAGE_DROP -> 5
        ValidationCategory.GENERAL -> 6
    }

    private fun componentTypeRank(type: ComponentType?): Int = when (type) {
        ComponentType.PV_MODULE -> 0
        ComponentType.MICROINVERTER -> 1
        ComponentType.STRING_INVERTER -> 2
        ComponentType.AC_BUS -> 3
        ComponentType.BARL -> 4
        ComponentType.BARN -> 5
        ComponentType.BARPE -> 6
        ComponentType.BREAKER -> 7
        ComponentType.QDG -> 8
        ComponentType.DPS -> 9
        ComponentType.GROUND_BAR -> 10
        ComponentType.LOAD -> 11
        null -> 12
    }
}
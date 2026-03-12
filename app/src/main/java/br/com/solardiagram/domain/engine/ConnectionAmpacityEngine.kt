package br.com.solardiagram.domain.engine

import br.com.solardiagram.domain.model.Component
import br.com.solardiagram.domain.model.CurrentKind
import br.com.solardiagram.domain.model.ElectricalSpecs
import br.com.solardiagram.domain.model.PortKind
import br.com.solardiagram.domain.rules.NormProfile
import br.com.solardiagram.util.Ids

class ConnectionAmpacityEngine(
    private val norm: NormProfile,
    private val ampacity: AmpacityEngine = AmpacityEngine()
) {
    fun evaluate(project: br.com.solardiagram.domain.model.DiagramProject): List<ValidationIssue> {
        val context = ProjectValidationContext(
            project = project,
            graph = br.com.solardiagram.domain.electrical.ElectricalGraphBuilder.build(project)
        )
        return evaluate(context)
    }

    fun evaluate(context: ProjectValidationContext): List<ValidationIssue> {
        val issues = mutableListOf<ValidationIssue>()

        val currentEngine = CurrentEstimationEngine()
        val detector = MainRunDetectionEngine(currentEngine)
        val mainAc = detector.detectMainAcRun(context.graph, context)
        val mainDc = detector.detectMainDcRun(context.graph, context)

        context.graph.edges.forEach { edge ->
            val conn = context.connectionForEdge(edge) ?: return@forEach
            val mm2 = conn.meta.overrideCableMm2
            if (mm2 == null) {
                issues += ValidationIssue(
                    id = Ids.newId(),
                    severity = Severity.INFO,
                    code = "AMP_CONN_NO_CABLE",
                    message = "Defina a bitola (override mm²) no fio para checar ampacidade do trecho.",
                    connectionId = conn.id,
                    category = ValidationCategory.AMPACITY
                )
                return@forEach
            }

            val kind = inferKind(context, edge) ?: return@forEach
            val (iA, _) = estimateIV(context, edge, mainAc, mainDc) ?: return@forEach

            val allowed = ampacity.allowableCurrentA(
                AmpacityInput(
                    mm2 = mm2,
                    material = conn.meta.conductorMaterial,
                    installation = conn.meta.installationMethod,
                    insulation = conn.meta.insulation,
                    ambientTempC = conn.meta.ambientTempC,
                    grouping = conn.meta.grouping
                )
            ) ?: return@forEach

            if (iA > allowed) {
                val severity = if (iA > allowed * 1.2) Severity.ERROR else Severity.WARNING
                issues += ValidationIssue(
                    id = Ids.newId(),
                    severity = severity,
                    code = "AMP_CONN_EXCEEDED",
                    message = "Corrente estimada excede ampacidade ajustada. I=${fmt(iA)}A > Iz=${fmt(allowed)}A (mm²=${mm2}, ${conn.meta.conductorMaterial}, ${conn.meta.installationMethod}, T=${fmt(conn.meta.ambientTempC)}°C, ${conn.meta.grouping}).",
                    connectionId = conn.id,
                    category = ValidationCategory.AMPACITY
                )
            }
        }

        return issues
    }

    private fun inferKind(context: ProjectValidationContext, edge: br.com.solardiagram.domain.electrical.ElectricalEdge): CurrentKind? {
        val a = context.port(edge.fromComponentId, edge.fromPortId)?.kind ?: return null
        val b = context.port(edge.toComponentId, edge.toPortId)?.kind ?: return null
        val dc = setOf(PortKind.DC_POS, PortKind.DC_NEG)
        val ac = setOf(PortKind.AC_L, PortKind.AC_N, PortKind.AC_PE, PortKind.PE)
        return when {
            a in dc && b in dc -> CurrentKind.DC
            a in ac && b in ac -> CurrentKind.AC
            else -> null
        }
    }

    private fun estimateIV(
        context: ProjectValidationContext,
        edge: br.com.solardiagram.domain.electrical.ElectricalEdge,
        mainAc: MainRunDetectionEngine.MainRun?,
        mainDc: MainRunDetectionEngine.MainRun?
    ): Pair<Double, Double>? {
        val fromComp = context.component(edge.fromComponentId) ?: return null
        val toComp = context.component(edge.toComponentId) ?: return null

        val breaker = listOf(fromComp, toComp).firstOrNull { it.specs is ElectricalSpecs.BreakerSpecs }
        if (breaker != null) {
            val s = breaker.specs as ElectricalSpecs.BreakerSpecs
            val v = mainAc?.voltageV ?: 220.0
            return s.ratedCurrentA.takeIf { it > 0 }?.to(v)
        }

        val mod = listOf(fromComp, toComp).firstOrNull { it.specs is ElectricalSpecs.PvModuleSpecs }
        if (mod != null) {
            val s = mod.specs as ElectricalSpecs.PvModuleSpecs
            if (s.iScA > 0 && s.vOcV > 0) return s.iScA to s.vOcV
        }

        return when {
            mainAc != null -> mainAc.currentA to mainAc.voltageV
            mainDc != null -> mainDc.currentA to mainDc.voltageV
            else -> null
        }
    }

    private fun fmt(v: Double): String = String.format("%.2f", v)
}

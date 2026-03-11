package br.com.solardiagram.domain.engine

import br.com.solardiagram.domain.model.*
import br.com.solardiagram.domain.rules.NormProfile
import br.com.solardiagram.util.Ids

class ConnectionVoltageDropEngine(
    private val norm: NormProfile,
    private val cable: CableSizingEngine = CableSizingEngine()
) {
    fun evaluate(project: DiagramProject): List<ValidationIssue> {
        val issues = mutableListOf<ValidationIssue>()

        val compById = project.components.associateBy { it.id }
        val currentEngine = CurrentEstimationEngine()
        val detector = MainRunDetectionEngine(currentEngine)

        val mainAc = detector.detectMainAcRun(project.components, project.connections)
        val mainDc = detector.detectMainDcRun(project.components)

        project.connections.forEach { conn ->
            val len = conn.meta.lengthMeters ?: return@forEach
            if (len <= 0.0) return@forEach

            val fromComp = compById[conn.fromComponentId] ?: return@forEach
            val toComp = compById[conn.toComponentId] ?: return@forEach
            val fromPort = fromComp.portById(conn.fromPortId) ?: return@forEach
            val toPort = toComp.portById(conn.toPortId) ?: return@forEach

            val kind = inferKind(fromPort.kind, toPort.kind) ?: return@forEach
            val maxDrop = if (kind == CurrentKind.AC) norm.maxVoltageDropPercentAc else norm.maxVoltageDropPercentDc
            val safety = if (kind == CurrentKind.AC) norm.acCurrentSafetyFactor else norm.dcCurrentSafetyFactor

            val estimate = estimateCurrentAndVoltage(kind, fromComp, toComp, mainAc, mainDc)
            if (estimate == null) {
                issues += ValidationIssue(
                    id = Ids.newId(),
                    severity = Severity.INFO,
                    code = "VDROP_CONN_NO_CURRENT",
                    message = "Sem dados suficientes para estimar corrente/tensão no fio. Preencha specs (disjuntor, potência, inversor) e/ou defina bitola.",
                    connectionId = conn.id,
                    category = ValidationCategory.VOLTAGE_DROP
                )
                return@forEach
            }

            val (currentA, voltageV) = estimate
            val input = CableSizingInput(
                currentA = currentA,
                voltageV = voltageV,
                lengthMeters = len,
                phases = conn.meta.phases,
                currentKind = kind,
                maxVoltageDropPercent = maxDrop,
                safetyFactor = safety
            )

            val rec = cable.recommend(input, overrideMm2 = conn.meta.overrideCableMm2)

            if (rec.estimatedVoltageDropPercent > maxDrop) {
                val sev = if (rec.estimatedVoltageDropPercent > maxDrop * 1.5) Severity.ERROR else Severity.WARNING
                issues += ValidationIssue(
                    id = Ids.newId(),
                    severity = sev,
                    code = "VDROP_CONN_EXCEEDED",
                    message = "Queda de tensão excede alvo (${fmt(maxDrop)}%). Estimado ${fmt(rec.estimatedVoltageDropPercent)}% (L=${fmt(len)}m, I=${fmt(currentA)}A, cabo=${rec.selectedMm2}mm²).",
                    connectionId = conn.id,
                    category = ValidationCategory.VOLTAGE_DROP
                )
            }
        }

        return issues
    }

    private fun inferKind(a: PortKind, b: PortKind): CurrentKind? {
        val dc = setOf(PortKind.DC_POS, PortKind.DC_NEG)
        val ac = setOf(PortKind.AC_L, PortKind.AC_N, PortKind.AC_PE, PortKind.PE)
        return when {
            a in dc && b in dc -> CurrentKind.DC
            a in ac && b in ac -> CurrentKind.AC
            else -> null
        }
    }

    private fun estimateCurrentAndVoltage(
        kind: CurrentKind,
        fromComp: Component,
        toComp: Component,
        mainAc: MainRunDetectionEngine.MainRun?,
        mainDc: MainRunDetectionEngine.MainRun?
    ): Pair<Double, Double>? {
        return when (kind) {
            CurrentKind.AC -> {
                val breaker = listOf(fromComp, toComp).firstOrNull { it.specs is ElectricalSpecs.BreakerSpecs }
                if (breaker != null) {
                    val s = breaker.specs as ElectricalSpecs.BreakerSpecs
                    s.ratedCurrentA.takeIf { it > 0 }?.to(mainAc?.voltageV ?: 220.0)
                } else mainAc?.let { it.currentA to it.voltageV }
            }
            CurrentKind.DC -> {
                val mod = listOf(fromComp, toComp).firstOrNull { it.specs is ElectricalSpecs.PvModuleSpecs }
                if (mod != null) {
                    val s = mod.specs as ElectricalSpecs.PvModuleSpecs
                    if (s.iScA > 0 && s.vOcV > 0) return s.iScA to s.vOcV
                }
                mainDc?.let { it.currentA to it.voltageV }
            }
        }
    }

    private fun fmt(v: Double): String = String.format("%.2f", v)
}

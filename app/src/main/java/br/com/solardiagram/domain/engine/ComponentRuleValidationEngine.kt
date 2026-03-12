package br.com.solardiagram.domain.engine

import br.com.solardiagram.domain.model.BreakerPole
import br.com.solardiagram.domain.model.Component
import br.com.solardiagram.domain.model.ComponentType
import br.com.solardiagram.domain.model.CurrentKind
import br.com.solardiagram.domain.model.DiagramProject
import br.com.solardiagram.domain.model.ElectricalPhase
import br.com.solardiagram.domain.model.ElectricalSpecs
import br.com.solardiagram.domain.model.PhysicalTerminalRole
import br.com.solardiagram.domain.model.PortDirection
import br.com.solardiagram.domain.model.PortKind
import br.com.solardiagram.domain.model.SystemPhase

class ComponentRuleValidationEngine {

    fun evaluate(project: DiagramProject): List<ValidationIssue> {
        return project.components.flatMap { component ->
            when (component.type) {
                ComponentType.PV_MODULE -> validatePvModule(component)
                ComponentType.MICROINVERTER -> validateMicroInverter(component)
                ComponentType.STRING_INVERTER -> validateStringInverter(component)
                ComponentType.AC_BUS -> validateAcBus(component)
                ComponentType.BARL -> validateBarL(component)
                ComponentType.BARN -> validateBarN(component)
                ComponentType.BARPE -> validateBarPe(component)
                ComponentType.GRID_SOURCE -> validateQdg(component)
                ComponentType.QDG -> validateQdg(component)
                ComponentType.BREAKER -> validateBreaker(component)
                ComponentType.DPS -> validateDps(component)
                ComponentType.GROUND_BAR -> validateGroundBar(component)
                ComponentType.LOAD -> validateLoad(component)
            }
        }
    }

    private fun validatePvModule(component: Component): List<ValidationIssue> {
        val specs = component.specs as? ElectricalSpecs.PvModuleSpecs ?: return specTypeMismatch(component, "PV_MODULE")
        val issues = mutableListOf<ValidationIssue>()

        if (specs.pMaxW <= 0 || specs.vMpV <= 0 || specs.iMpA <= 0 || specs.vOcV <= 0 || specs.iScA <= 0) {
            issues += componentIssue(component, "COMP_PV_INVALID_SPECS", "Módulo FV ${component.name} possui specs inválidas ou incompletas.")
        }

        val dcPos = component.ports.filter { it.kind == PortKind.DC_POS }
        val dcNeg = component.ports.filter { it.kind == PortKind.DC_NEG }
        if (dcPos.size != 1 || dcNeg.size != 1 || component.ports.size != 2) {
            issues += componentIssue(component, "COMP_PV_REAL_TERMINALS", "Módulo FV ${component.name} deve possuir apenas dois terminais reais: DC+ e DC-.")
        }
        val unexpectedDirectedPorts = component.ports.filter { it.direction != PortDirection.BIDIRECTIONAL }
        if (unexpectedDirectedPorts.isNotEmpty()) {
            issues += componentIssue(component, "COMP_PV_DIRECTIONAL_PORTS", "Módulo FV ${component.name} não deve ser modelado como DC IN/DC OUT; use apenas terminais bidirecionais DC+ e DC-.", Severity.WARNING)
        }
        val missingSeriesMetadata = component.ports.any { port ->
            (port.kind == PortKind.DC_POS || port.kind == PortKind.DC_NEG) && port.spec?.supportsSeriesLink != true
        }
        if (missingSeriesMetadata) {
            issues += componentIssue(component, "COMP_PV_SERIES_METADATA", "Módulo FV ${component.name} deveria marcar seus terminais DC como aptos para composição de string em série.", Severity.INFO)
        }
        return issues
    }

    private fun validateMicroInverter(component: Component): List<ValidationIssue> {
        val specs = component.specs as? ElectricalSpecs.MicroInverterSpecs ?: return specTypeMismatch(component, "MICROINVERTER")
        val issues = mutableListOf<ValidationIssue>()

        if (specs.dcInputPairs <= 0 || specs.maxDcPowerW <= 0 || specs.acNominalPowerW <= 0 || specs.maxAcCurrentA <= 0) {
            issues += componentIssue(component, "COMP_MICRO_INVALID_SPECS", "Microinversor ${component.name} possui specs inválidas ou incompletas.")
        }

        val dcPosInputs = component.ports.count { it.kind == PortKind.DC_POS && it.direction == PortDirection.INPUT }
        val dcNegInputs = component.ports.count { it.kind == PortKind.DC_NEG && it.direction == PortDirection.INPUT }
        if (dcPosInputs != specs.dcInputPairs || dcNegInputs != specs.dcInputPairs) {
            issues += componentIssue(component, "COMP_MICRO_DC_PORT_MISMATCH", "Microinversor ${component.name} deve possuir ${specs.dcInputPairs} pares de entrada DC dedicados.", Severity.WARNING)
        }

        val acPhasePorts = component.ports.filter { it.spec?.terminalRole == PhysicalTerminalRole.AC_OUTPUT }
        val expectedAcTerminals = when (specs.phases) {
            SystemPhase.MONO -> 2
            SystemPhase.BI -> 3
            SystemPhase.TRI -> 4
        }
        if (acPhasePorts.size != expectedAcTerminals) {
            issues += componentIssue(component, "COMP_MICRO_AC_PORTS", "Microinversor ${component.name} deveria expor $expectedAcTerminals terminais AC conforme a fase ${specs.phases}.")
        }

        if (component.ports.none { it.kind == PortKind.PE }) {
            issues += componentIssue(component, "COMP_MICRO_NO_PE", "Microinversor ${component.name} deveria possuir porta PE para aterramento.", Severity.INFO)
        }
        return issues
    }

    private fun validateStringInverter(component: Component): List<ValidationIssue> {
        val specs = component.specs as? ElectricalSpecs.StringInverterSpecs ?: return specTypeMismatch(component, "STRING_INVERTER")
        val issues = mutableListOf<ValidationIssue>()

        if (specs.mpptCount <= 0 || specs.maxDcPowerW <= 0 || specs.acNominalPowerW <= 0) {
            issues += componentIssue(component, "COMP_STR_INV_INVALID_SPECS", "Inversor string ${component.name} possui specs inválidas ou incompletas.")
        }

        val dcInputs = component.ports.filter { it.spec?.terminalRole == PhysicalTerminalRole.DC_INPUT }
        if (dcInputs.size != specs.mpptCount * 2) {
            issues += componentIssue(component, "COMP_STR_INV_MPPT_SCHEMA", "Inversor string ${component.name} deve possuir um par DC+ / DC- por MPPT configurado.")
        }

        val acOutputs = component.ports.filter { it.spec?.terminalRole == PhysicalTerminalRole.AC_OUTPUT }
        val expectedAcOutputs = when (specs.phases) {
            SystemPhase.MONO -> 2
            SystemPhase.BI -> 3
            SystemPhase.TRI -> 4
        }
        if (acOutputs.size != expectedAcOutputs) {
            issues += componentIssue(component, "COMP_STR_INV_AC_SCHEMA", "Inversor string ${component.name} deve possuir ${expectedAcOutputs} terminais AC de saída para a fase ${specs.phases}.")
        }
        return issues
    }

    private fun validateAcBus(component: Component): List<ValidationIssue> {
        val specs = component.specs as? ElectricalSpecs.AcBusSpecs ?: return specTypeMismatch(component, "AC_BUS")
        val issues = mutableListOf<ValidationIssue>()

        if (specs.maxBusCurrentA <= 0) {
            issues += componentIssue(component, "COMP_AC_BUS_INVALID_CURRENT", "Barramento AC ${component.name} possui corrente nominal inválida.")
        }

        val busPorts = component.ports.filter { it.spec?.terminalRole == PhysicalTerminalRole.BUSBAR }
        val linePorts = busPorts.filter { it.kind == PortKind.AC_L }
        val neutralPorts = busPorts.filter { it.kind == PortKind.AC_N }
        val pePorts = busPorts.filter { it.kind == PortKind.PE }
        val distinctLineBars = linePorts.mapNotNull { it.spec?.phase }.distinct()
        val distinctNeutralBars = neutralPorts.mapNotNull { it.spec?.phase }.distinct()
        val distinctPeBars = pePorts.mapNotNull { it.spec?.phase }.distinct()
        val expectedLines = when (specs.phases) {
            SystemPhase.MONO -> 1
            SystemPhase.BI -> 2
            SystemPhase.TRI -> 3
        }
        if (distinctLineBars.size != expectedLines) {
            issues += componentIssue(component, "COMP_AC_BUS_LINE_COUNT", "Barramento AC ${component.name} deveria possuir $expectedLines barra(s) de fase para ${specs.phases}.")
        }
        if (specs.hasNeutral && distinctNeutralBars.isEmpty()) {
            issues += componentIssue(component, "COMP_AC_BUS_NO_NEUTRAL", "Barramento AC ${component.name} foi configurado com neutro, mas não possui barra N.")
        }
        if (specs.hasGround && distinctPeBars.isEmpty()) {
            issues += componentIssue(component, "COMP_AC_BUS_NO_PE", "Barramento AC ${component.name} foi configurado com PE, mas não possui barra de terra.")
        }
        if (busPorts.any { (it.spec?.maxConnections ?: 1) <= 1 }) {
            issues += componentIssue(component, "COMP_AC_BUS_CAPACITY", "Barramento AC ${component.name} deveria aceitar múltiplas conexões por barra no modelo lógico.", Severity.INFO)
        }
        return issues
    }

    private fun validateBarL(component: Component): List<ValidationIssue> {
        val specs = component.specs as? ElectricalSpecs.AcBusSpecs ?: return specTypeMismatch(component, "BARL")
        val issues = mutableListOf<ValidationIssue>()
        if (specs.maxBusCurrentA <= 0) issues += componentIssue(component, "COMP_BARL_INVALID_CURRENT", "BARL ${component.name} possui corrente nominal inválida.")
        val ports = component.ports.filter { it.spec?.terminalRole == PhysicalTerminalRole.BUSBAR }
        val linePorts = ports.filter { it.kind == PortKind.AC_L }
        if (linePorts.size != 2) issues += componentIssue(component, "COMP_BARL_PORT_SCHEMA", "BARL ${component.name} deve possuir exatamente IN e OUT de linha.")
        val phases = linePorts.mapNotNull { it.spec?.phase }.distinct()
        if (phases.size != 1 || phases.firstOrNull() !in listOf(ElectricalPhase.L1, ElectricalPhase.L2, ElectricalPhase.L3)) issues += componentIssue(component, "COMP_BARL_PHASE", "BARL ${component.name} deve representar apenas uma fase L1, L2 ou L3.")
        return issues
    }

    private fun validateBarN(component: Component): List<ValidationIssue> {
        val specs = component.specs as? ElectricalSpecs.AcBusSpecs ?: return specTypeMismatch(component, "BARN")
        val issues = mutableListOf<ValidationIssue>()
        if (specs.maxBusCurrentA <= 0) issues += componentIssue(component, "COMP_BARN_INVALID_CURRENT", "BARN ${component.name} possui corrente nominal inválida.")
        val ports = component.ports.filter { it.spec?.terminalRole == PhysicalTerminalRole.BUSBAR }
        val neutralPorts = ports.filter { it.kind == PortKind.AC_N }
        if (neutralPorts.size != 2) issues += componentIssue(component, "COMP_BARN_PORT_SCHEMA", "BARN ${component.name} deve possuir exatamente IN e OUT de neutro.")
        return issues
    }

    private fun validateBarPe(component: Component): List<ValidationIssue> {
        val specs = component.specs as? ElectricalSpecs.AcBusSpecs ?: return specTypeMismatch(component, "BARPE")
        val issues = mutableListOf<ValidationIssue>()
        if (specs.maxBusCurrentA <= 0) issues += componentIssue(component, "COMP_BARPE_INVALID_CURRENT", "BARPE ${component.name} possui corrente nominal inválida.")
        val ports = component.ports.filter { it.spec?.terminalRole == PhysicalTerminalRole.BUSBAR }
        val pePorts = ports.filter { it.kind == PortKind.PE }
        if (pePorts.size != 2) issues += componentIssue(component, "COMP_BARPE_PORT_SCHEMA", "BARPE ${component.name} deve possuir exatamente IN e OUT de PE.")
        return issues
    }

    private fun validateQdg(component: Component): List<ValidationIssue> {
        val specs = component.specs as? ElectricalSpecs.QdgSpecs ?: return specTypeMismatch(component, "QDG")
        val issues = mutableListOf<ValidationIssue>()

        if (specs.maxBusCurrentA <= 0) {
            issues += componentIssue(component, "COMP_QDG_INVALID_CURRENT", "QDG ${component.name} possui corrente de barramento inválida.")
        }

        val feedPorts = component.ports.filter { it.spec?.terminalRole == PhysicalTerminalRole.LINE }
        val distPorts = component.ports.filter { it.spec?.terminalRole == PhysicalTerminalRole.LOAD }
        if (feedPorts.isEmpty() || distPorts.isEmpty()) {
            issues += componentIssue(component, "COMP_QDG_PORT_SCHEMA", "QDG ${component.name} deve possuir terminais de alimentação e de distribuição no modelo simplificado.")
        }
        return issues
    }

    private fun validateBreaker(component: Component): List<ValidationIssue> {
        val specs = component.specs as? ElectricalSpecs.BreakerSpecs ?: return specTypeMismatch(component, "BREAKER")
        val issues = mutableListOf<ValidationIssue>()

        if (specs.ratedCurrentA <= 0) {
            issues += componentIssue(component, "COMP_BREAKER_INVALID_CURRENT", "Disjuntor ${component.name} possui corrente nominal inválida.")
        }
        if (specs.applicableTo != CurrentKind.AC) {
            issues += componentIssue(component, "COMP_BREAKER_KIND", "Disjuntor ${component.name} deveria estar configurado para corrente AC.", Severity.INFO)
        }

        val linePoles = component.ports.count { it.spec?.terminalRole == PhysicalTerminalRole.LINE }
        val loadPoles = component.ports.count { it.spec?.terminalRole == PhysicalTerminalRole.LOAD }
        val expected = when (specs.poles) {
            BreakerPole.P1 -> 1
            BreakerPole.P2 -> 2
            BreakerPole.P3 -> 3
            BreakerPole.P4 -> 4
        }
        if (linePoles != expected || loadPoles != expected) {
            issues += componentIssue(component, "COMP_BREAKER_POLE_SCHEMA", "Disjuntor ${component.name} não está coerente com ${specs.poles}: line=$linePoles, load=$loadPoles.")
        }
        return issues
    }

    private fun validateDps(component: Component): List<ValidationIssue> {
        val specs = component.specs as? ElectricalSpecs.DpsSpecs ?: return specTypeMismatch(component, "DPS")
        val issues = mutableListOf<ValidationIssue>()
        if (specs.maxVoltageV <= 0) {
            issues += componentIssue(component, "COMP_DPS_INVALID_VOLTAGE", "DPS ${component.name} possui tensão máxima inválida.")
        }
        val hasPe = component.ports.any { it.kind == PortKind.PE }
        if (!hasPe) {
            issues += componentIssue(component, "COMP_DPS_NO_PE", "DPS ${component.name} deve possuir conexão PE.")
        }
        return issues
    }

    private fun validateGroundBar(component: Component): List<ValidationIssue> {
        val specs = component.specs as? ElectricalSpecs.GroundingSpecs ?: return specTypeMismatch(component, "GROUND_BAR")
        val issues = mutableListOf<ValidationIssue>()
        val peCount = component.ports.count { it.kind == PortKind.PE }
        if (peCount == 0) {
            issues += componentIssue(component, "COMP_GROUND_BAR_NO_PE", "Barramento de terra ${component.name} deve possuir ao menos uma porta PE.")
        }
        if (component.ports.any { it.kind != PortKind.PE }) {
            issues += componentIssue(component, "COMP_GROUND_BAR_MIXED_PORTS", "Barramento de terra ${component.name} não deveria misturar PE com outros tipos de porta.", Severity.INFO)
        }
        if (component.ports.any { (it.spec?.maxConnections ?: 1) <= 1 }) {
            issues += componentIssue(component, "COMP_GROUND_BAR_CAPACITY", "Barramento de terra ${component.name} deveria aceitar múltiplas derivações PE no modelo lógico.", Severity.INFO)
        }
        if (!specs.isMainEarthPoint) {
            issues += componentIssue(component, "COMP_GROUND_BAR_NOT_MAIN", "Barramento de terra ${component.name} não está marcado como ponto principal de aterramento.", Severity.INFO)
        }
        return issues
    }

    private fun validateLoad(component: Component): List<ValidationIssue> {
        val specs = component.specs as? ElectricalSpecs.LoadSpecs ?: return specTypeMismatch(component, "LOAD")
        val issues = mutableListOf<ValidationIssue>()
        if (specs.powerW <= 0 || specs.voltageV <= 0) {
            issues += componentIssue(component, "COMP_LOAD_INVALID_SPECS", "Carga ${component.name} possui potência ou tensão inválida.")
        }
        val lineInputs = component.ports.filter { it.spec?.terminalRole == PhysicalTerminalRole.LINE }
        val expectedMin = if (specs.phases == SystemPhase.TRI) 3 else 2
        if (lineInputs.size < expectedMin) {
            issues += componentIssue(component, "COMP_LOAD_PORT_SCHEMA", "Carga ${component.name} deve possuir terminais de alimentação AC compatíveis com a fase configurada.")
        }
        return issues
    }

    private fun specTypeMismatch(component: Component, expected: String): List<ValidationIssue> = listOf(
        componentIssue(
            component = component,
            code = "COMP_SPEC_TYPE_MISMATCH",
            message = "Componente ${component.name} (${component.type}) não está coerente com o tipo de specs esperado para $expected.",
            severity = Severity.ERROR
        )
    )

    private fun componentIssue(
        component: Component,
        code: String,
        message: String,
        severity: Severity = Severity.WARNING
    ): ValidationIssue = ValidationIssue(
        id = "comp-rule-${component.id}-$code",
        severity = severity,
        code = code,
        message = message,
        componentId = component.id,
        category = ValidationCategory.COMPONENT_RULE,
        componentType = component.type
    )
}

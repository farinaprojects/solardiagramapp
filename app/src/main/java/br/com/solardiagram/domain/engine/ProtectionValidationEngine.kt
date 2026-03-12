package br.com.solardiagram.domain.engine

import br.com.solardiagram.domain.electrical.ElectricalFlowKind
import br.com.solardiagram.domain.electrical.ElectricalProtection
import br.com.solardiagram.domain.electrical.ElectricalProtectionRole
import br.com.solardiagram.domain.model.ComponentType

class ProtectionValidationEngine {

    fun evaluate(context: ProjectValidationContext): List<ValidationIssue> {
        val issues = mutableListOf<ValidationIssue>()

        issues += evaluateMissingMainIncomingProtection(context)
        issues += evaluateConflictingMainProtections(context)
        issues += evaluateMissingMainProtectionWithSecondaryBranches(context)
        issues += evaluateLoadsWithoutProtectedPath(context)
        issues += evaluateLoadsWithGenerationOnlyProtection(context)
        issues += evaluateGenerationWithoutDedicatedProtection(context)
        issues += evaluateUnclassifiedBreakers(context)

        return issues
    }

    private fun evaluateMissingMainIncomingProtection(
        context: ProjectValidationContext
    ): List<ValidationIssue> {
        val installation = context.installation
        val centralQdgId = installation.centralQdgComponentId ?: return emptyList()
        if (installation.mainSupplyFlows.isEmpty()) return emptyList()

        val mainProtections = context.protections.filter { it.isMainProtection }
        if (mainProtections.isNotEmpty()) return emptyList()

        val secondaryProtections = context.protections.filterNot { it.isMainProtection }
        if (secondaryProtections.size >= 2) return emptyList()

        val qdg = context.component(centralQdgId)
        val qdgName = qdg?.name ?: "QDG central"

        return listOf(
            ValidationIssue(
                id = "prot-no-main-incoming-$centralQdgId",
                severity = Severity.WARNING,
                code = "PROT_NO_MAIN_INCOMING",
                message = "$qdgName possui alimentação principal identificada, mas não foi encontrada proteção principal dedicada no trajeto até o QDG central.",
                componentId = centralQdgId,
                category = ValidationCategory.PROTECTION,
                componentType = qdg?.type
            )
        )
    }

    private fun evaluateConflictingMainProtections(
        context: ProjectValidationContext
    ): List<ValidationIssue> {
        val installation = context.installation
        val centralQdgId = installation.centralQdgComponentId ?: return emptyList()
        val mainProtections = context.protections.filter { it.isMainProtection }
        if (mainProtections.size <= 1) return emptyList()

        val breakerNames = mainProtections.joinToString(", ") { it.breakerName }

        return mainProtections.map { protection ->
            ValidationIssue(
                id = "prot-multiple-main-${protection.breakerComponentId}",
                severity = Severity.WARNING,
                code = "PROT_MULTIPLE_MAIN_INCOMING",
                message = "Foram identificadas múltiplas proteções principais para o QDG central ($breakerNames). Verifique se há conflito de hierarquia entre os disjuntores de entrada.",
                componentId = protection.breakerComponentId,
                category = ValidationCategory.PROTECTION,
                componentType = ComponentType.BREAKER
            )
        }
    }

    private fun evaluateMissingMainProtectionWithSecondaryBranches(
        context: ProjectValidationContext
    ): List<ValidationIssue> {
        val installation = context.installation
        val centralQdgId = installation.centralQdgComponentId ?: return emptyList()
        if (installation.mainSupplyFlows.isEmpty()) return emptyList()

        val mainProtections = context.protections.filter { it.isMainProtection }
        if (mainProtections.isNotEmpty()) return emptyList()

        val secondaryProtections = context.protections.filter {
            it.role in setOf(
                ElectricalProtectionRole.LOAD_BRANCH,
                ElectricalProtectionRole.GENERATION_BRANCH,
                ElectricalProtectionRole.MIXED_BRANCH
            )
        }
        if (secondaryProtections.size < 2) return emptyList()

        val qdg = context.component(centralQdgId)
        val qdgName = qdg?.name ?: "QDG central"

        return listOf(
            ValidationIssue(
                id = "prot-missing-main-with-secondaries-$centralQdgId",
                severity = Severity.WARNING,
                code = "PROT_MAIN_MISSING_WITH_SECONDARIES",
                message = "$qdgName possui alimentação principal e ${secondaryProtections.size} proteções secundárias identificadas, mas nenhuma proteção principal dedicada foi encontrada. Verifique a hierarquia de proteção da entrada da instalação.",
                componentId = centralQdgId,
                category = ValidationCategory.PROTECTION,
                componentType = qdg?.type
            )
        )
    }

    private fun evaluateLoadsWithoutProtectedPath(
        context: ProjectValidationContext
    ): List<ValidationIssue> {
        val relevantFlows = context.installation.branchFlows
            .filter { it.destinationType == ComponentType.LOAD }

        return relevantFlows.mapNotNull { flow ->
            val hasBreaker = flow.componentIds.any { componentId ->
                context.component(componentId)?.type == ComponentType.BREAKER
            }
            if (hasBreaker) return@mapNotNull null

            val load = context.component(flow.destinationComponentId)
            val loadName = load?.name ?: flow.destinationComponentId

            ValidationIssue(
                id = "prot-load-without-breaker-${flow.destinationComponentId}",
                severity = Severity.WARNING,
                code = "PROT_LOAD_WITHOUT_BREAKER",
                message = "A carga $loadName foi identificada em um fluxo funcional sem disjuntor no trajeto. Verifique a proteção do ramal de carga.",
                componentId = flow.destinationComponentId,
                category = ValidationCategory.PROTECTION,
                componentType = load?.type
            )
        }
    }

    private fun evaluateLoadsWithGenerationOnlyProtection(
        context: ProjectValidationContext
    ): List<ValidationIssue> {
        val loadFlows = context.installation.branchFlows
            .filter { it.destinationType == ComponentType.LOAD }

        return loadFlows.mapNotNull { flow ->
            val breakersOnPath = context.protections.filter { it.breakerComponentId in flow.componentIds }
            if (breakersOnPath.isEmpty()) return@mapNotNull null

            val onlyGenerationProtection = breakersOnPath.all {
                it.role == ElectricalProtectionRole.GENERATION_BRANCH
            }
            if (!onlyGenerationProtection) return@mapNotNull null

            val load = context.component(flow.destinationComponentId)
            val loadName = load?.name ?: flow.destinationComponentId
            val breakerNames = breakersOnPath.joinToString(", ") { it.breakerName }

            ValidationIssue(
                id = "prot-load-wrong-role-${flow.destinationComponentId}",
                severity = Severity.WARNING,
                code = "PROT_LOAD_WITH_GENERATION_ONLY_BREAKER",
                message = "A carga $loadName está em um fluxo de carga protegido apenas por disjuntor(es) classificado(s) como ramal de geração ($breakerNames). Verifique a coerência semântica da proteção no trajeto.",
                componentId = flow.destinationComponentId,
                category = ValidationCategory.PROTECTION,
                componentType = load?.type
            )
        }
    }

    private fun evaluateGenerationWithoutDedicatedProtection(
        context: ProjectValidationContext
    ): List<ValidationIssue> {

        val generationFlows = context.flows.filter {
            it.kind == ElectricalFlowKind.GENERATION_TO_LOAD ||
                it.kind == ElectricalFlowKind.GENERATION_TO_GRID_EXPORT
        }

        return generationFlows.mapNotNull { flow ->

            val source = context.component(flow.sourceComponentId) ?: return@mapNotNull null

            if (source.type !in setOf(
                    ComponentType.MICROINVERTER,
                    ComponentType.STRING_INVERTER
                )
            ) {
                return@mapNotNull null
            }

            val hasBreaker = flow.componentIds.any { componentId ->
                context.component(componentId)?.type == ComponentType.BREAKER
            }

            if (hasBreaker) return@mapNotNull null

            ValidationIssue(
                id = "prot-generation-without-breaker-${source.id}-${flow.kind.name}",
                severity = Severity.WARNING,
                code = "PROT_GENERATION_WITHOUT_BREAKER",
                message = "A geração em ${source.name} possui fluxo funcional sem proteção dedicada por disjuntor. Verifique a proteção do ramal de geração.",
                componentId = source.id,
                category = ValidationCategory.PROTECTION,
                componentType = source.type
            )
        }
    }

    private fun evaluateUnclassifiedBreakers(
        context: ProjectValidationContext
    ): List<ValidationIssue> {
        return context.protections.mapNotNull { protection ->
            if (protection.role != ElectricalProtectionRole.UNCLASSIFIED) return@mapNotNull null
            if (protection.protectedFlowIds.isEmpty()) return@mapNotNull null

            val breaker = context.component(protection.breakerComponentId)
            val breakerName = breaker?.name ?: protection.breakerName

            ValidationIssue(
                id = "prot-breaker-unclassified-${protection.breakerComponentId}",
                severity = Severity.INFO,
                code = "PROT_BREAKER_UNCLASSIFIED",
                message = "O disjuntor $breakerName foi encontrado em fluxo(s) funcional(is), mas não recebeu papel estrutural claro na instalação. Verifique se sua posição e conexões representam corretamente a hierarquia de proteção.",
                componentId = protection.breakerComponentId,
                category = ValidationCategory.PROTECTION,
                componentType = breaker?.type ?: ComponentType.BREAKER
            )
        }
    }
}

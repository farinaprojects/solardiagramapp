package br.com.solardiagram.domain.engine

import br.com.solardiagram.domain.model.DiagramProject
import br.com.solardiagram.domain.rules.NormProfile
import br.com.solardiagram.domain.rules.NormProfiles

class ValidateProjectUseCase(
    norm: NormProfile = NormProfiles.BR_BASE
) {
    private val engine = ProjectValidationEngine(norm = norm)

    fun execute(project: DiagramProject): ProjectValidationOutput = engine.validate(project)
}

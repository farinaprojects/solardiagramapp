package br.com.solardiagram.di

import android.content.Context
import br.com.solardiagram.data.local.LocalJsonProjectRepository
import br.com.solardiagram.data.local.ProjectRepository
import br.com.solardiagram.domain.engine.ValidateProjectUseCase

object ServiceLocator {
    fun projectRepository(ctx: Context): ProjectRepository = LocalJsonProjectRepository(ctx.applicationContext)
    fun validateProjectUseCase(): ValidateProjectUseCase = ValidateProjectUseCase()
}

package br.com.solardiagram.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import br.com.solardiagram.di.ServiceLocator
import br.com.solardiagram.domain.engine.ProjectValidationOutput
import br.com.solardiagram.domain.model.DiagramProject
import br.com.solardiagram.util.AppResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProjectDetailUiState(
    val isLoading: Boolean = false,
    val project: DiagramProject? = null,
    val validation: ProjectValidationOutput? = null,
    val message: String? = null,
    val error: String? = null,
    val deleted: Boolean = false,
    val duplicatedProjectId: String? = null
)

class ProjectDetailViewModel(app: Application): AndroidViewModel(app) {
    private val repo = ServiceLocator.projectRepository(app)
    private val validate = ServiceLocator.validateProjectUseCase()

    private val _state = MutableStateFlow(ProjectDetailUiState(isLoading = true))
    val state: StateFlow<ProjectDetailUiState> = _state

    fun load(id: String) {
        viewModelScope.launch {
            _state.value = ProjectDetailUiState(isLoading = true)
            when (val res = repo.loadProject(id)) {
                is AppResult.Ok -> _state.value = ProjectDetailUiState(isLoading = false, project = res.value)
                is AppResult.Err -> _state.value = ProjectDetailUiState(isLoading = false, error = res.error.message)
            }
        }
    }

    fun save() {
        val p = _state.value.project ?: return
        viewModelScope.launch {
            val updated = p.copy(updatedAtEpochMs = System.currentTimeMillis())
            when (val res = repo.saveProject(updated)) {
                is AppResult.Ok -> _state.value = _state.value.copy(project = updated, message = "Salvo.")
                is AppResult.Err -> _state.value = _state.value.copy(error = res.error.message)
            }
        }
    }

    fun validateNow() {
        val p = _state.value.project ?: return
        viewModelScope.launch {
            val out = validate.execute(p)
            _state.value = _state.value.copy(validation = out, message = "Validado: ${out.report.issues.size} issue(s).")
        }
    }
    fun delete(projectId: String) {
        viewModelScope.launch {
            _state.update {it.copy(isLoading = true, error = null) }

            when (val res = repo.deleteProject(projectId)) {
                is AppResult.Ok -> _state.update {
                    it.copy(isLoading = false, message = "Projeto excluído.", deleted = true)
                }
                is AppResult.Err -> _state.update {
                    it.copy(isLoading = false, error = res.error.message)
                }
            }
        }
    }

    fun clearDeletedFlag() {
        _state.update { it.copy(deleted = false) }
    }

    fun clearDuplicatedFlag() {
        _state.update { it.copy(duplicatedProjectId = null) }
    }

    fun duplicate(sourceProjectId: String, newName: String, newLocation: String?) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, message = null) }

            when (val loaded = repo.loadProject(sourceProjectId)) {
                is AppResult.Err -> _state.update { it.copy(isLoading = false, error = loaded.error.message) }
                is AppResult.Ok -> {
                    val src = loaded.value

                    val newId = br.com.solardiagram.util.Ids.newId()

                    //val copy = src.copy(
                    //    id = newId,
                    //    name = newName,
                     //   location = newLocation,
                     //   updatedAtEpochMs = System.currentTimeMillis()
                    //)

                    val now = System.currentTimeMillis()

                    val copy = src.copy(
                        id = newId,
                        name = newName,
                        location = newLocation,

                        // ✅ ao duplicar: novo projeto = nova data de criação
                        createdAtEpochMs = now,
                        updatedAtEpochMs = now
                    )





                    when (val saved = repo.saveProject(copy)) {
                        is AppResult.Ok -> _state.update {
                            it.copy(
                                isLoading = false,
                                message = "Projeto duplicado.",
                                duplicatedProjectId = newId
                            )
                        }
                        is AppResult.Err -> _state.update { it.copy(isLoading = false, error = saved.error.message) }
                    }
                }
            }
        }
    }

    fun export(projectId: String) {
        // Stub controlado para evoluirmos depois (PDF/compartilhar)
        _state.value = _state.value.copy(message = "Exportação em desenvolvimento (próxima etapa).")
    }

}

package br.com.solardiagram.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import br.com.solardiagram.di.ServiceLocator
import br.com.solardiagram.domain.model.DiagramProject
import br.com.solardiagram.util.AppResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ProjectListUiState(
    val isLoading: Boolean = false,
    val projects: List<DiagramProject> = emptyList(),
    val error: String? = null
)

class ProjectListViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = ServiceLocator.projectRepository(app)

    private val _state = MutableStateFlow(ProjectListUiState(isLoading = true))
    val state: StateFlow<ProjectListUiState> = _state

    init {
        viewModelScope.launch {
            repo.createDemoIfEmpty()
            load()
        }
    }

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            when (val res = repo.listProjects()) {
                is AppResult.Ok ->
                    _state.value = ProjectListUiState(isLoading = false, projects = res.value)
                is AppResult.Err ->
                    _state.value = ProjectListUiState(isLoading = false, error = res.error.message)
            }
        }
    }

    fun refresh() {
        // Sem coroutine extra: reaproveita o load()
        load()
    }

    fun createNewProject(name: String, location: String?, onCreated: (String) -> Unit) {
        viewModelScope.launch {
            when (val res = repo.createProject(name, location)) {
                is AppResult.Ok -> {
                    load()
                    onCreated(res.value.id)
                }
                is AppResult.Err -> {
                    _state.value = _state.value.copy(error = res.error.message)
                }
            }
        }
    }
}
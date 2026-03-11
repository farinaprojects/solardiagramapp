package br.com.solardiagram.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import br.com.solardiagram.di.ServiceLocator
import br.com.solardiagram.domain.electrical.ElectricalCircuit
import br.com.solardiagram.domain.electrical.ElectricalCircuitAnalyzer
import br.com.solardiagram.domain.electrical.ElectricalEdge
import br.com.solardiagram.domain.electrical.ElectricalGraphBuilder
import br.com.solardiagram.domain.electrical.ElectricalHighlightEngine
import br.com.solardiagram.domain.engine.ProjectInsightEngine
import br.com.solardiagram.domain.engine.ProjectInsights
import br.com.solardiagram.domain.engine.ProjectValidationOutput
import br.com.solardiagram.domain.engine.Severity
import br.com.solardiagram.domain.engine.TechnicalReportExporter
import br.com.solardiagram.domain.model.DiagramProject
import br.com.solardiagram.util.AppResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File

data class CanvasHighlights(
    val errorComponentIds: Set<String> = emptySet(),
    val warnComponentIds: Set<String> = emptySet(),
    val errorConnectionIds: Set<String> = emptySet(),
    val warnConnectionIds: Set<String> = emptySet(),
    val semanticFocusedComponentId: String? = null,
    val semanticCircuitId: String? = null,
    val semanticComponentIds: Set<String> = emptySet(),
    val semanticConnectionIds: Set<String> = emptySet()
)

data class EditorHostUiState(
    val isLoading: Boolean = false,
    val editorState: EditorUiState? = null,
    val validation: ProjectValidationOutput? = null,
    val highlights: CanvasHighlights = CanvasHighlights(),
    val insights: ProjectInsights? = null,
    val showIssues: Boolean = true,
    val autoWireOnAdd: Boolean = false,
    val isSaving: Boolean = false,
    val hasUnsavedChanges: Boolean = false,
    val lastSavedAtEpochMs: Long? = null,
    val lastMessage: String? = null,
    val error: String? = null
)

class EditorHostViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = ServiceLocator.projectRepository(app)
    private val validate = ServiceLocator.validateProjectUseCase()
    private val insightEngine = ProjectInsightEngine()
    private val reportExporter = TechnicalReportExporter()

    private val _state = MutableStateFlow(EditorHostUiState())
    val state: StateFlow<EditorHostUiState> = _state

    private var editorVm: EditorViewModel? = null
    private var currentProjectId: String? = null
    private var lastValidatedProjectSnapshot: DiagramProject? = null
    private var lastPersistedProjectSnapshot: DiagramProject? = null
    private var editorCollectJob: Job? = null
    private var autoSaveJob: Job? = null

    private val autoSaveDebounceMs = 1200L

    fun getEditorVm(): EditorViewModel? = editorVm

    fun load(projectId: String) {
        val alreadyLoaded =
            currentProjectId == projectId &&
                    editorVm != null &&
                    _state.value.editorState?.project?.id == projectId

        if (alreadyLoaded) return

        viewModelScope.launch {
            _state.value = _state.value.copy(
                isLoading = true,
                editorState = null,
                validation = null,
                highlights = CanvasHighlights(),
                insights = null,
                lastMessage = null,
                error = null
            )

            editorCollectJob?.cancel()
            editorCollectJob = null

            autoSaveJob?.cancel()
            autoSaveJob = null

            editorVm = null
            currentProjectId = null
            lastValidatedProjectSnapshot = null
            lastPersistedProjectSnapshot = null

            when (val res = repo.loadProject(projectId)) {
                is AppResult.Ok -> {
                    val vm = EditorViewModel(res.value)
                    editorVm = vm
                    currentProjectId = projectId
                    lastPersistedProjectSnapshot = res.value

                    val initialSemanticHighlights = buildSemanticHighlights(
                        project = vm.state.value.project,
                        selectedComponentId = vm.state.value.selectedComponentId
                    )

                    _state.value = _state.value.copy(
                        isLoading = false,
                        editorState = vm.state.value,
                        validation = null,
                        highlights = initialSemanticHighlights,
                        insights = insightEngine.analyze(vm.state.value.project),
                        isSaving = false,
                        hasUnsavedChanges = false,
                        lastSavedAtEpochMs = res.value.updatedAtEpochMs,
                        lastMessage = null,
                        error = null
                    )

                    editorCollectJob = viewModelScope.launch {
                        vm.state.collectLatest { st ->
                            val validationStillFresh = isValidationStillFresh(
                                current = st.project,
                                validated = lastValidatedProjectSnapshot
                            )

                            val hasUnsavedChanges = hasProjectContentChanged(
                                current = st.project,
                                persisted = lastPersistedProjectSnapshot
                            )

                            val validation = _state.value.validation.takeIf { validationStillFresh }
                            val issueHighlights = validation?.let(::buildIssueHighlights) ?: CanvasHighlights()
                            val semanticHighlights = buildSemanticHighlights(
                                project = st.project,
                                selectedComponentId = st.selectedComponentId
                            )

                            _state.value = _state.value.copy(
                                editorState = st,
                                validation = validation,
                                highlights = mergeHighlights(issueHighlights, semanticHighlights),
                                insights = insightEngine.analyze(st.project),
                                hasUnsavedChanges = hasUnsavedChanges
                            )

                            if (!validationStillFresh) {
                                lastValidatedProjectSnapshot = null
                            }

                            scheduleAutoSaveIfNeeded(st.project)
                        }
                    }
                }

                is AppResult.Err -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        editorState = null,
                        validation = null,
                        highlights = CanvasHighlights(),
                        insights = null,
                        error = res.error.message
                    )
                }
            }
        }
    }

    fun toggleIssues() {
        _state.value = _state.value.copy(showIssues = !_state.value.showIssues)
    }

    fun toggleAutoWireOnAdd() {
        val enabled = !_state.value.autoWireOnAdd
        _state.value = _state.value.copy(
            autoWireOnAdd = enabled,
            lastMessage = if (enabled) {
                "Cabeamento automático ao inserir componentes: ON."
            } else {
                "Cabeamento automático ao inserir componentes: OFF."
            }
        )
    }

    fun save() {
        autoSaveJob?.cancel()
        persistProject(showSuccessMessage = true)
    }

    fun autoConnectNow() {
        val vm = editorVm ?: return
        vm.autoConnectBuses()

        val st = vm.state.value
        val issueHighlights = _state.value.validation?.let(::buildIssueHighlights) ?: CanvasHighlights()
        val semanticHighlights = buildSemanticHighlights(
            project = st.project,
            selectedComponentId = st.selectedComponentId
        )

        _state.value = _state.value.copy(
            editorState = st,
            highlights = mergeHighlights(issueHighlights, semanticHighlights),
            insights = insightEngine.analyze(st.project),
            lastMessage = "Conexões automáticas com barramentos aplicadas."
        )
    }

    fun autoLayoutNow() {
        val vm = editorVm ?: return
        vm.autoLayoutProject()

        val st = vm.state.value
        val issueHighlights = _state.value.validation?.let(::buildIssueHighlights) ?: CanvasHighlights()
        val semanticHighlights = buildSemanticHighlights(
            project = st.project,
            selectedComponentId = st.selectedComponentId
        )

        _state.value = _state.value.copy(
            editorState = st,
            highlights = mergeHighlights(issueHighlights, semanticHighlights),
            insights = insightEngine.analyze(st.project),
            lastMessage = "Auto layout aplicado ao diagrama."
        )
    }

    fun exportTechnicalReport() {
        val project = _state.value.editorState?.project ?: return

        viewModelScope.launch {
            runCatching {
                val dir = File(getApplication<Application>().filesDir, "reports")
                reportExporter.export(project, _state.value.validation, dir)
            }.onSuccess { file ->
                _state.value = _state.value.copy(
                    lastMessage = "Relatório técnico exportado: ${file.absolutePath}"
                )
            }.onFailure { err ->
                _state.value = _state.value.copy(
                    error = err.message ?: "Falha ao exportar relatório técnico."
                )
            }
        }
    }

    fun validateNow() {
        val project = _state.value.editorState?.project ?: return
        val selectedComponentId = _state.value.editorState?.selectedComponentId

        viewModelScope.launch {
            val out = validate.execute(project)
            val issueHighlights = buildIssueHighlights(out)
            val semanticHighlights = buildSemanticHighlights(
                project = project,
                selectedComponentId = selectedComponentId
            )
            val mergedHighlights = mergeHighlights(issueHighlights, semanticHighlights)

            val (errors, warnings) = countSeverities(out)
            lastValidatedProjectSnapshot = project

            _state.value = _state.value.copy(
                validation = out,
                highlights = mergedHighlights,
                insights = insightEngine.analyze(project),
                lastMessage = "Validação: $errors erro(s), $warnings aviso(s)."
            )
        }
    }

    private fun scheduleAutoSaveIfNeeded(project: DiagramProject) {
        if (!hasProjectContentChanged(current = project, persisted = lastPersistedProjectSnapshot)) {
            autoSaveJob?.cancel()
            autoSaveJob = null
            return
        }

        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(autoSaveDebounceMs)
            persistProject(showSuccessMessage = false)
        }
    }

    private fun persistProject(showSuccessMessage: Boolean) {
        val vm = editorVm ?: return
        val projectToSave = _state.value.editorState?.project ?: return

        _state.value = _state.value.copy(
            isSaving = true,
            error = null
        )

        viewModelScope.launch {
            val updated = projectToSave.copy(updatedAtEpochMs = System.currentTimeMillis())

            when (val res = repo.saveProject(updated)) {
                is AppResult.Ok -> {
                    lastPersistedProjectSnapshot = updated

                    val currentProject = _state.value.editorState?.project
                    val stillSameProjectVersion =
                        currentProject != null &&
                                !hasProjectContentChanged(
                                    current = currentProject,
                                    persisted = projectToSave
                                )

                    if (stillSameProjectVersion) {
                        vm.replaceProject(updated)
                    }

                    val hasUnsavedChangesNow = currentProject?.let {
                        hasProjectContentChanged(current = it, persisted = updated)
                    } ?: false

                    _state.value = _state.value.copy(
                        isSaving = false,
                        hasUnsavedChanges = hasUnsavedChangesNow,
                        lastSavedAtEpochMs = updated.updatedAtEpochMs,
                        lastMessage = if (showSuccessMessage) "Projeto salvo localmente." else null,
                        error = null
                    )

                    if (hasUnsavedChangesNow && currentProject != null) {
                        scheduleAutoSaveIfNeeded(currentProject)
                    }
                }

                is AppResult.Err -> {
                    _state.value = _state.value.copy(
                        isSaving = false,
                        hasUnsavedChanges = true,
                        error = res.error.message
                    )
                }
            }
        }
    }

    private fun hasProjectContentChanged(
        current: DiagramProject,
        persisted: DiagramProject?
    ): Boolean {
        val saved = persisted ?: return true
        return current.id != saved.id ||
                current.name != saved.name ||
                current.location != saved.location ||
                current.components != saved.components ||
                current.connections != saved.connections
    }

    private fun isValidationStillFresh(
        current: DiagramProject,
        validated: DiagramProject?
    ): Boolean {
        val snapshot = validated ?: return false
        return current.id == snapshot.id &&
                current.name == snapshot.name &&
                current.location == snapshot.location &&
                current.components == snapshot.components &&
                current.connections == snapshot.connections
    }

    override fun onCleared() {
        editorCollectJob?.cancel()
        editorCollectJob = null

        autoSaveJob?.cancel()
        autoSaveJob = null

        super.onCleared()
    }

    fun clearMessage() {
        _state.value = _state.value.copy(lastMessage = null)
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    private fun countSeverities(out: ProjectValidationOutput): Pair<Int, Int> {
        val errs = out.report.issues.count { it.severity == Severity.ERROR }
        val warns = out.report.issues.count { it.severity == Severity.WARNING }
        return errs to warns
    }

    private fun buildIssueHighlights(out: ProjectValidationOutput): CanvasHighlights {
        val errC = mutableSetOf<String>()
        val warnC = mutableSetOf<String>()
        val errConn = mutableSetOf<String>()
        val warnConn = mutableSetOf<String>()

        out.report.issues.forEach { issue ->
            when (issue.severity) {
                Severity.ERROR -> {
                    issue.componentId?.let { errC.add(it) }
                    issue.connectionId?.let { errConn.add(it) }
                }

                Severity.WARNING -> {
                    issue.componentId?.let { warnC.add(it) }
                    issue.connectionId?.let { warnConn.add(it) }
                }

                else -> Unit
            }
        }

        return CanvasHighlights(
            errorComponentIds = errC,
            warnComponentIds = warnC,
            errorConnectionIds = errConn,
            warnConnectionIds = warnConn
        )
    }

    private fun buildSemanticHighlights(
        project: DiagramProject,
        selectedComponentId: String?
    ): CanvasHighlights {
        if (selectedComponentId.isNullOrBlank()) {
            return CanvasHighlights()
        }

        val selectedComponent = project.components.firstOrNull { component ->
            component.id == selectedComponentId
        } ?: return CanvasHighlights()

        return runCatching<CanvasHighlights> {
            val graph = ElectricalGraphBuilder.build(project.components, project.connections)
            val circuits: List<ElectricalCircuit> = ElectricalCircuitAnalyzer.analyze(graph)

            val highlight = ElectricalHighlightEngine.highlightForComponent(
                circuits = circuits,
                selectedComponentId = selectedComponentId
            )

            val semanticComponentIds: Set<String> = highlight.componentIds
            val semanticConnectionIds: Set<String> = highlight.edges
                .map { edge: ElectricalEdge ->
                    "${edge.fromComponentId}:${edge.fromPortId}->${edge.toComponentId}:${edge.toPortId}"
                }
                .toSet()

            CanvasHighlights(
                warnComponentIds = semanticComponentIds,
                warnConnectionIds = semanticConnectionIds,
                semanticFocusedComponentId = selectedComponentId,
                semanticCircuitId = highlight.circuitId,
                semanticComponentIds = semanticComponentIds,
                semanticConnectionIds = semanticConnectionIds
            )
        }.getOrElse { _: Throwable ->
            CanvasHighlights()
        }
    }

    private fun mergeHighlights(
        issueHighlights: CanvasHighlights,
        semanticHighlights: CanvasHighlights
    ): CanvasHighlights {
        return CanvasHighlights(
            errorComponentIds = issueHighlights.errorComponentIds,
            warnComponentIds = issueHighlights.warnComponentIds + semanticHighlights.semanticComponentIds,
            errorConnectionIds = issueHighlights.errorConnectionIds,
            warnConnectionIds = issueHighlights.warnConnectionIds + semanticHighlights.semanticConnectionIds,
            semanticFocusedComponentId = semanticHighlights.semanticFocusedComponentId,
            semanticCircuitId = semanticHighlights.semanticCircuitId,
            semanticComponentIds = semanticHighlights.semanticComponentIds,
            semanticConnectionIds = semanticHighlights.semanticConnectionIds
        )
    }
}
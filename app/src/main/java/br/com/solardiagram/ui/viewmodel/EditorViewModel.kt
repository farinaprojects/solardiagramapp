package br.com.solardiagram.ui.viewmodel

import androidx.lifecycle.ViewModel
import br.com.solardiagram.domain.model.Component
import br.com.solardiagram.domain.model.Connection
import br.com.solardiagram.domain.model.ConnectionMeta
import br.com.solardiagram.domain.model.DiagramProject
import br.com.solardiagram.domain.model.Point2
import br.com.solardiagram.domain.model.Transform2
import br.com.solardiagram.domain.engine.AutoBusConnectionEngine
import br.com.solardiagram.domain.engine.DiagramAutoLayoutEngine
import br.com.solardiagram.domain.rules.ConnectionCompatibility
import br.com.solardiagram.ui.screens.editor.EditorAlignmentEngine
import br.com.solardiagram.util.Ids
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class SelectionSource {
    NONE,
    TAP_COMPONENT,
    BOX,
    DUPLICATE,
    DRAG,
    TAP_CONNECTION
}

data class EditorUiState(
    val project: DiagramProject,
    val selectedComponentIds: Set<String> = emptySet(),
    val selectedConnectionId: String? = null,
    val selectionSource: SelectionSource = SelectionSource.NONE,
    val draggingComponentId: String? = null,
    val connectingFrom: Pair<String, String>? = null,
    val previewWireWorld: Point2? = null,
    val snappedPortTarget: Pair<String, String>? = null,
    val pan: Point2 = Point2(0f, 0f),
    val scale: Float = 1.0f,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false
) {
    val selectedComponentId: String?
        get() = selectedComponentIds.singleOrNull()
}

/**
 * EditorViewModel
 * - Centraliza estado do editor (MVVM)
 * - Implementa Undo/Redo no ViewModel (não na UI)
 * - Centraliza toda a seleção (single + multi) no próprio estado do editor
 *
 * Regra:
 * - Operações discretas de edição (add/delete/update/connect/meta) registram Undo automaticamente.
 * - Operações contínuas (arrasto com dezenas de updates) NÃO registram por frame.
 *   A UI deve chamar pushUndoStep() no início do gesto.
 */
class EditorViewModel(initialProject: DiagramProject) : ViewModel() {
    private val autoBusConnectionEngine = AutoBusConnectionEngine()
    private val autoLayoutEngine = DiagramAutoLayoutEngine()
    private val _state = MutableStateFlow(EditorUiState(project = initialProject))
    val state: StateFlow<EditorUiState> = _state

    // =============================
    // Undo/Redo
    // =============================

    private data class EditorSnapshot(
        val project: DiagramProject,
        val selectedComponentIds: Set<String>,
        val selectedConnectionId: String?,
        val pan: Point2,
        val scale: Float
    )

    private val undoStack = ArrayDeque<EditorSnapshot>()
    private val redoStack = ArrayDeque<EditorSnapshot>()

    private fun makeSnapshot(st: EditorUiState = _state.value): EditorSnapshot =
        EditorSnapshot(
            project = st.project,
            selectedComponentIds = st.selectedComponentIds,
            selectedConnectionId = st.selectedConnectionId,
            pan = st.pan,
            scale = st.scale
        )

    private fun refreshUndoRedoFlags(st: EditorUiState): EditorUiState =
        st.copy(canUndo = undoStack.isNotEmpty(), canRedo = redoStack.isNotEmpty())

    /**
     * Registra um snapshot manualmente.
     * Use para ações discretas de UI (zoom/reset) e no início de gestos contínuos (drag/pan).
     */
    fun pushUndoStep(maxDepth: Int = 80) {
        undoStack.addLast(makeSnapshot())
        while (undoStack.size > maxDepth) undoStack.removeFirst()
        redoStack.clear()
        _state.value = refreshUndoRedoFlags(_state.value)
    }

    private inline fun commitEdit(maxDepth: Int = 80, block: (EditorUiState) -> EditorUiState) {
        undoStack.addLast(makeSnapshot())
        while (undoStack.size > maxDepth) undoStack.removeFirst()
        redoStack.clear()
        val next = block(_state.value)
        _state.value = refreshUndoRedoFlags(next)
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        redoStack.addLast(makeSnapshot())
        val s = undoStack.removeLast()

        val restored = _state.value.copy(
            project = s.project,
            selectedComponentIds = s.selectedComponentIds,
            selectedConnectionId = s.selectedConnectionId,
            draggingComponentId = null,
            connectingFrom = null,
            previewWireWorld = null,
            snappedPortTarget = null,
            pan = s.pan,
            scale = s.scale
        )
        _state.value = refreshUndoRedoFlags(restored)
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        undoStack.addLast(makeSnapshot())
        val s = redoStack.removeLast()

        val restored = _state.value.copy(
            project = s.project,
            selectedComponentIds = s.selectedComponentIds,
            selectedConnectionId = s.selectedConnectionId,
            draggingComponentId = null,
            connectingFrom = null,
            previewWireWorld = null,
            snappedPortTarget = null,
            pan = s.pan,
            scale = s.scale
        )
        _state.value = refreshUndoRedoFlags(restored)
    }

    // =============================
    // View Transform (pan/scale)
    // =============================

    fun setPan(p: Point2) {
        _state.value = _state.value.copy(pan = p)
    }

    fun setScale(scale: Float) {
        _state.value = _state.value.copy(scale = scale)
    }

    fun setPanAndScale(pan: Point2, scale: Float) {
        _state.value = _state.value.copy(pan = pan, scale = scale)
    }

    // =============================
    // Selection
    // =============================

    fun setSelectedComponents(componentIds: Set<String>, source: SelectionSource = SelectionSource.BOX) {
        _state.value = _state.value.copy(
            selectedComponentIds = componentIds.toSet(),
            selectedConnectionId = null,
            selectionSource = if (componentIds.isEmpty()) SelectionSource.NONE else source
        )
    }

    fun selectComponent(componentId: String?, source: SelectionSource = SelectionSource.TAP_COMPONENT) {
        _state.value = _state.value.copy(
            selectedComponentIds = componentId?.let(::setOf) ?: emptySet(),
            selectedConnectionId = null,
            selectionSource = if (componentId == null) SelectionSource.NONE else source
        )
    }

    fun clearSelection() {
        _state.value = _state.value.copy(
            selectedComponentIds = emptySet(),
            selectedConnectionId = null,
            selectionSource = SelectionSource.NONE
        )
    }

    fun selectConnection(connectionId: String?) {
        _state.value = _state.value.copy(
            selectedComponentIds = emptySet(),
            selectedConnectionId = connectionId,
            selectionSource = if (connectionId == null) SelectionSource.NONE else SelectionSource.TAP_CONNECTION
        )
    }

    fun markSelectionAsDragged() {
        val st = _state.value
        if (st.selectedComponentIds.isNotEmpty()) {
            _state.value = st.copy(selectionSource = SelectionSource.DRAG)
        }
    }

    // =============================
    // Drag state
    // =============================

    fun startDragging(componentId: String) {
        _state.value = _state.value.copy(draggingComponentId = componentId)
    }

    fun stopDragging() {
        _state.value = _state.value.copy(draggingComponentId = null)
    }

    // =============================
    // Component ops
    // =============================

    /**
     * Move componente sem registrar undo (porque pode ser chamado muitas vezes durante um gesto).
     * A UI deve chamar pushUndoStep() no início do drag.
     */
    fun moveComponent(componentId: String, newPos: Point2) {
        val proj = _state.value.project
        val comps = proj.components.map { c ->
            if (c.id != componentId) c else c.copy(transform = c.transform.copy(position = newPos))
        }
        _state.value = _state.value.copy(project = proj.copy(components = comps))
    }

    fun addComponent(component: Component, autoConnect: Boolean = false) {
        commitEdit {
            val proj = it.project.copy(components = it.project.components + component)
            val nextProject = if (autoConnect) autoBusConnectionEngine.apply(proj) else proj
            it.copy(project = nextProject)
        }
    }

    fun addComponentAt(
        componentTemplate: Component,
        worldPos: Point2,
        snapTo: (Point2) -> Point2,
        autoConnect: Boolean = false
    ) {
        val placed = componentTemplate.copy(
            id = Ids.newId(),
            transform = Transform2(position = snapTo(worldPos))
        )

        addComponent(placed, autoConnect = autoConnect)

        setSelectedComponents(
            setOf(placed.id),
            source = SelectionSource.DRAG
        )
    }

    fun duplicateSelectedComponents(
        offset: Point2 = Point2(48f, 48f),
        snapTo: (Point2) -> Point2 = { it }
    ): Set<String> {
        val st = _state.value
        val selectedIds = st.selectedComponentIds
        if (selectedIds.isEmpty()) return emptySet()

        val selectedComponents = st.project.components.filter { it.id in selectedIds }
        if (selectedComponents.isEmpty()) return emptySet()

        val remap = selectedComponents.associate { it.id to Ids.newId() }
        val duplicatedComponents = selectedComponents.map { component ->
            component.copy(
                id = remap.getValue(component.id),
                transform = component.transform.copy(
                    position = snapTo(
                        Point2(
                            component.transform.position.x + offset.x,
                            component.transform.position.y + offset.y
                        )
                    )
                )
            )
        }

        val duplicatedConnections = st.project.connections.mapNotNull { connection ->
            val newFrom = remap[connection.fromComponentId] ?: return@mapNotNull null
            val newTo = remap[connection.toComponentId] ?: return@mapNotNull null
            connection.copy(
                id = Ids.newId(),
                fromComponentId = newFrom,
                toComponentId = newTo
            )
        }

        val duplicatedSelectionIds = LinkedHashSet<String>().apply {
            duplicatedComponents.forEach { component -> add(component.id) }
        }

        commitEdit {
            val proj = it.project
            it.copy(
                project = proj.copy(
                    components = proj.components + duplicatedComponents,
                    connections = proj.connections + duplicatedConnections
                ),
                selectedComponentIds = duplicatedSelectionIds.toSet(),
                selectedConnectionId = null,
                selectionSource = SelectionSource.DUPLICATE,
                draggingComponentId = null,
                connectingFrom = null,
                previewWireWorld = null,
                snappedPortTarget = null
            )
        }

        return duplicatedSelectionIds
    }

    private fun applyComponentPositions(newPositions: Map<String, Point2>) {
        if (newPositions.isEmpty()) return

        commitEdit { current ->
            val nextComponents = current.project.components.map { component ->
                val nextPosition = newPositions[component.id] ?: return@map component
                component.copy(transform = component.transform.copy(position = nextPosition))
            }
            current.copy(project = current.project.copy(components = nextComponents))
        }
    }

    fun alignSelectedHorizontally() {
        val selected = _state.value.project.components.filter { it.id in _state.value.selectedComponentIds }
        applyComponentPositions(EditorAlignmentEngine.alignHorizontally(selected))
    }

    fun alignSelectedVertically() {
        val selected = _state.value.project.components.filter { it.id in _state.value.selectedComponentIds }
        applyComponentPositions(EditorAlignmentEngine.alignVertically(selected))
    }

    fun distributeSelectedHorizontally() {
        val selected = _state.value.project.components.filter { it.id in _state.value.selectedComponentIds }
        applyComponentPositions(EditorAlignmentEngine.distributeHorizontally(selected))
    }

    fun distributeSelectedVertically() {
        val selected = _state.value.project.components.filter { it.id in _state.value.selectedComponentIds }
        applyComponentPositions(EditorAlignmentEngine.distributeVertically(selected))
    }

    fun rotateSelectedComponentsClockwise() {
        val selectedIds = _state.value.selectedComponentIds
        if (selectedIds.isEmpty()) return

        commitEdit { current ->
            val nextComponents = current.project.components.map { component ->
                if (component.id !in selectedIds) {
                    component
                } else {
                    component.copy(
                        transform = component.transform.copy(
                            rotationQuarterTurns = component.transform.normalizedQuarterTurns + 1
                        )
                    )
                }
            }
            current.copy(project = current.project.copy(components = nextComponents))
        }
    }

    fun deleteSelectedComponents() {
        val selectedIds = _state.value.selectedComponentIds
        if (selectedIds.isEmpty()) return

        commitEdit {
            val proj = it.project
            val nextComps = proj.components.filterNot { c -> c.id in selectedIds }
            val nextConns = proj.connections.filterNot { c ->
                c.fromComponentId in selectedIds || c.toComponentId in selectedIds
            }
            it.copy(
                project = proj.copy(components = nextComps, connections = nextConns),
                selectedComponentIds = emptySet(),
                selectedConnectionId = null
            )
        }
    }

    fun deleteComponent(componentId: String) {
        commitEdit {
            val proj = it.project
            val nextComps = proj.components.filterNot { c -> c.id == componentId }
            val nextConns = proj.connections.filterNot { c ->
                c.fromComponentId == componentId || c.toComponentId == componentId
            }
            it.copy(
                project = proj.copy(components = nextComps, connections = nextConns),
                selectedComponentIds = emptySet(),
                selectedConnectionId = null
            )
        }
    }

    fun updateComponent(updated: Component) {
        commitEdit {
            val proj = it.project
            val comps = proj.components.map { c -> if (c.id == updated.id) updated else c }
            it.copy(project = proj.copy(components = comps))
        }
    }

    fun updateComponentName(componentId: String, newName: String) {
        commitEdit {
            val proj = it.project
            val comps = proj.components.map { c ->
                if (c.id != componentId) c else c.copy(name = newName)
            }
            it.copy(project = proj.copy(components = comps))
        }
    }

    // =============================
    // Connection ops
    // =============================

    fun updateConnectionMeta(connectionId: String, meta: ConnectionMeta) {
        commitEdit {
            val proj = it.project
            val conns = proj.connections.map { c -> if (c.id != connectionId) c else c.copy(meta = meta) }
            it.copy(project = proj.copy(connections = conns))
        }
    }

    fun startConnection(componentId: String, portId: String) {
        _state.value = _state.value.copy(
            connectingFrom = componentId to portId,
            previewWireWorld = null,
            snappedPortTarget = null
        )
    }

    fun updateWirePreview(world: Point2?, snappedTarget: Pair<String, String>?) {
        _state.value = _state.value.copy(
            previewWireWorld = world,
            snappedPortTarget = snappedTarget
        )
    }

    fun cancelConnection() {
        _state.value = _state.value.copy(
            connectingFrom = null,
            previewWireWorld = null,
            snappedPortTarget = null
        )
    }

    fun tryFinishConnection(toComponentId: String, toPortId: String): Result<Unit> {
        val from = _state.value.connectingFrom
            ?: return Result.failure(IllegalStateException("Sem conexão em andamento"))
        val proj = _state.value.project

        val fromComp = proj.components.firstOrNull { it.id == from.first }
            ?: return Result.failure(IllegalArgumentException("Origem não encontrada"))
        val toComp = proj.components.firstOrNull { it.id == toComponentId }
            ?: return Result.failure(IllegalArgumentException("Destino não encontrado"))

        val fromPort = fromComp.portById(from.second)
            ?: return Result.failure(IllegalArgumentException("Porta origem não encontrada"))
        val toPort = toComp.portById(toPortId)
            ?: return Result.failure(IllegalArgumentException("Porta destino não encontrada"))

        if (!ConnectionCompatibility.isCompatible(fromPort, toPort)) {
            cancelConnection()
            return Result.failure(
                IllegalArgumentException("Conexão incompatível: ${fromPort.kind} ↔ ${toPort.kind}")
            )
        }

        val exists = proj.connections.any { c ->
            (c.fromComponentId == from.first &&
                    c.fromPortId == from.second &&
                    c.toComponentId == toComponentId &&
                    c.toPortId == toPortId) ||
                    (c.fromComponentId == toComponentId &&
                            c.fromPortId == toPortId &&
                            c.toComponentId == from.first &&
                            c.toPortId == from.second)
        }
        if (exists) {
            cancelConnection()
            return Result.failure(IllegalArgumentException("Conexão já existe"))
        }

        val connection = Connection(
            id = Ids.newId(),
            fromComponentId = from.first,
            fromPortId = from.second,
            toComponentId = toComponentId,
            toPortId = toPortId
        )

        commitEdit {
            it.copy(
                project = proj.copy(connections = proj.connections + connection),
                connectingFrom = null,
                previewWireWorld = null,
                snappedPortTarget = null
            )
        }
        return Result.success(Unit)
    }

    fun autoConnectBuses() {
        commitEdit { current ->
            current.copy(project = autoBusConnectionEngine.apply(current.project))
        }
    }

    fun autoLayoutProject() {
        commitEdit { current ->
            current.copy(project = autoLayoutEngine.arrange(current.project))
        }
    }

    fun replaceProject(project: DiagramProject) {
        val current = _state.value
        val selectedComponentIds = current.selectedComponentIds.filterTo(linkedSetOf()) { selectedId ->
            project.components.any { it.id == selectedId }
        }
        val selectedConnectionId = current.selectedConnectionId?.takeIf { selectedId ->
            project.connections.any { it.id == selectedId }
        }

        val next = current.copy(
            project = project,
            selectedComponentIds = selectedComponentIds,
            selectedConnectionId = selectedConnectionId,
            draggingComponentId = null,
            connectingFrom = null,
            previewWireWorld = null,
            snappedPortTarget = null
        )
        _state.value = refreshUndoRedoFlags(next)
    }

    fun deleteConnection(connectionId: String) {
        commitEdit {
            val proj = it.project
            val next = proj.connections.filterNot { c -> c.id == connectionId }
            it.copy(project = proj.copy(connections = next), selectedConnectionId = null)
        }
    }
}
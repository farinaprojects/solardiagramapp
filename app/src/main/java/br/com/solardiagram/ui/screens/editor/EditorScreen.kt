package br.com.solardiagram.ui.screens.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import br.com.solardiagram.domain.model.ComponentFactory
import br.com.solardiagram.domain.model.ElectricalPhase
import br.com.solardiagram.domain.model.Point2
import br.com.solardiagram.ui.viewmodel.CanvasHighlights
import br.com.solardiagram.ui.viewmodel.EditorViewModel
import br.com.solardiagram.ui.viewmodel.SelectionSource



private data class PaletteItemSpec(
    val label: String,
    val factory: () -> br.com.solardiagram.domain.model.Component
)

private data class PaletteSectionSpec(
    val title: String,
    val items: List<PaletteItemSpec>
)

private fun buildPaletteSections(): List<PaletteSectionSpec> = listOf(
    PaletteSectionSpec(
        title = "Fotovoltaico",
        items = listOf(
            PaletteItemSpec("Módulo FV") { ComponentFactory.pvModule() },
            PaletteItemSpec("Microinversor 2") { ComponentFactory.microInverter2() },
            PaletteItemSpec("Microinversor 4") { ComponentFactory.microInverter4() },
            PaletteItemSpec("Inversor String") { ComponentFactory.stringInverter() }
        )
    ),
    PaletteSectionSpec(
        title = "Barramentos",
        items = listOf(
            PaletteItemSpec("BARL (L1)") { ComponentFactory.barL(name = "BAR L1", phase = ElectricalPhase.L1) },
            PaletteItemSpec("BARL (L2)") { ComponentFactory.barL(name = "BAR L2", phase = ElectricalPhase.L2) },
            PaletteItemSpec("BARL (L3)") { ComponentFactory.barL(name = "BAR L3", phase = ElectricalPhase.L3) },
            PaletteItemSpec("BARN") { ComponentFactory.barN() },
            PaletteItemSpec("BARPE") { ComponentFactory.barPe() },
            PaletteItemSpec("Terra (PE)") { ComponentFactory.groundBar() }
        )
    ),
    PaletteSectionSpec(
        title = "Proteção e quadros",
        items = listOf(
            PaletteItemSpec("Rede / Alimentação") { ComponentFactory.gridSource() },
            PaletteItemSpec("QDG / Quadro") { ComponentFactory.qdg() },
            PaletteItemSpec("DPS AC") { ComponentFactory.dpsAc() },
            PaletteItemSpec("Disjuntor Mono") { ComponentFactory.breakerMono(32.0) },
            PaletteItemSpec("Disjuntor Bi") { ComponentFactory.breakerBi(32.0) },
            PaletteItemSpec("Disjuntor Tri") { ComponentFactory.breakerTri(32.0) }
        )
    ),
    PaletteSectionSpec(
        title = "Cargas",
        items = listOf(
            PaletteItemSpec("Carga Mono") { ComponentFactory.loadMono() },
            PaletteItemSpec("Carga Bi") { ComponentFactory.loadBi() },
            PaletteItemSpec("Carga Tri") { ComponentFactory.loadTri() }
        )
    )
)

@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    highlights: CanvasHighlights,
    validationSummary: List<Pair<String, String>>,
    autoWireOnAdd: Boolean,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val interaction = rememberEditorCanvasInteractionState()

    val expandedPaletteWidth: Dp = 180.dp
    val collapsedPaletteWidth: Dp = 28.dp

    var paletteCollapsed by remember { mutableStateOf(false) }
    var validationDrawerOpen by remember { mutableStateOf(false) }
    val paletteWidth: Dp = if (paletteCollapsed) collapsedPaletteWidth else expandedPaletteWidth
    val paletteSections = remember { buildPaletteSections() }

    val expandedSections = remember { mutableStateMapOf<String, Boolean>() }

    paletteSections.forEach { section ->
        if (!expandedSections.containsKey(section.title)) {
            expandedSections[section.title] = false
        }
    }

    val canvasSize = remember { mutableStateOf(Size.Zero) }

    var canvasWindowOrigin by remember { mutableStateOf(Offset.Zero) }
    var canvasWindowRect by remember { mutableStateOf<Rect?>(null) }

    LaunchedEffect(state.project.id) {
        interaction.resetTransientVisuals()
        interaction.clearSelectionBox()
    }

    fun clearTransientInteractionState() {
        interaction.resetTransientVisuals()
        interaction.clearSelectionBox()
    }

    fun updatePalettePreview(
        template: br.com.solardiagram.domain.model.Component,
        windowPos: Offset
    ) {
        val rect = canvasWindowRect
        if (rect == null || !rect.contains(windowPos)) {
            interaction.clearPalettePreview()
            return
        }

        val local = windowPos - canvasWindowOrigin
        val world = SnapEngine.snapToGrid(
            screenToWorld(
                screen = local,
                scale = interaction.effectiveScale(state.scale),
                pan = interaction.effectivePan(state.pan)
            )
        )

        interaction.showPalettePreview(template, world)
    }

    fun clearPalettePreview() {
        interaction.clearPalettePreview()
    }

    val effectiveSelectedIds = if (state.selectedComponentIds.isNotEmpty()) state.selectedComponentIds else interaction.pinnedSelectionIds
    val effectiveSelectedComponentId = effectiveSelectedIds.singleOrNull()
    val selectedComponent = state.project.components.firstOrNull { it.id == effectiveSelectedComponentId }
    val selectedConnection = state.project.connections.firstOrNull { it.id == state.selectedConnectionId }
    val toolbarTopInset: Dp = 78.dp
    val density = LocalDensity.current
    val selectionContext = when {
        state.selectedConnectionId != null -> EditorSelectionContext.Connection
        effectiveSelectedIds.size == 1 && selectedComponent != null -> EditorSelectionContext.SingleComponent(selectedComponent.name)
        effectiveSelectedIds.isNotEmpty() -> EditorSelectionContext.MultipleComponents(effectiveSelectedIds.size)
        else -> EditorSelectionContext.None
    }
    val effectivePan = interaction.effectivePan(state.pan)
    val effectiveScale = interaction.effectiveScale(state.scale)
    val selectedComponentBounds = effectiveSelectedIds.mapNotNull { id ->
        val component = state.project.components.firstOrNull { it.id == id } ?: return@mapNotNull null
        val positionOverride = interaction.overridePositions?.get(id)
        val effectiveComponent = if (positionOverride != null) {
            component.copy(transform = component.transform.copy(position = positionOverride))
        } else {
            component
        }
        ComponentRenderer.componentScreenRect(
            component = effectiveComponent,
            scale = effectiveScale,
            pan = effectivePan
        )
    }
    val componentSelectionRect = unionRect(selectedComponentBounds)

    Box(modifier = modifier.fillMaxSize()) {
        Row(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .width(paletteWidth)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(if (paletteCollapsed) 4.dp else 10.dp)
            ) {
                if (paletteCollapsed) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        TextButton(
                            onClick = { paletteCollapsed = false },
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.width(20.dp)
                        ) {
                            Text("▶")
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Paleta", style = MaterialTheme.typography.titleMedium)
                        TextButton(
                            onClick = { paletteCollapsed = true },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("◀")
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {

                        paletteSections.forEach { section ->

                            val expanded = expandedSections[section.title] == true

                            item(key = "header_${section.title}") {

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            expandedSections[section.title] = !expanded
                                        }
                                ) {

                                    Row(
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {

                                        Text(
                                            text = section.title,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.primary
                                        )

                                        Text(
                                            text = if (expanded) "▼" else "▶"
                                        )
                                    }
                                }
                            }

                            item {

                                AnimatedVisibility(
                                    visible = expanded,
                                    enter = expandVertically() + fadeIn(),
                                    exit = shrinkVertically() + fadeOut()
                                ) {

                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {

                                        section.items.forEach { itemSpec ->

                                            PaletteDraggableItem(
                                                label = itemSpec.label,
                                                templateFactory = itemSpec.factory,
                                                onDrop = { template, windowPos ->
                                                    clearPalettePreview()

                                                    val rect = canvasWindowRect ?: return@PaletteDraggableItem
                                                    if (!rect.contains(windowPos)) return@PaletteDraggableItem

                                                    val local = windowPos - canvasWindowOrigin

                                                    val world = screenToWorld(
                                                        screen = local,
                                                        scale = interaction.effectiveScale(state.scale),
                                                        pan = interaction.effectivePan(state.pan)
                                                    )

                                                    viewModel.addComponentAt(
                                                        componentTemplate = template,
                                                        worldPos = world,
                                                        snapTo = { SnapEngine.snapToGrid(it) },
                                                        autoConnect = autoWireOnAdd
                                                    )
                                                },
                                                onPreviewStart = { template, windowPos ->
                                                    updatePalettePreview(template, windowPos)
                                                },
                                                onPreviewMove = { template, windowPos ->
                                                    updatePalettePreview(template, windowPos)
                                                },
                                                onPreviewEnd = {
                                                    clearPalettePreview()
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item(key = "palette_footer") {
                            Column {
                                Spacer(Modifier.height(8.dp))
                                HorizontalDivider()
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    "Selecionados: ${effectiveSelectedIds.size}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(16.dp))
                            }
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.background)
                    .clipToBounds()
                    .onGloballyPositioned { coords: LayoutCoordinates ->
                        val p = coords.positionInWindow()
                        val s = coords.size
                        canvasWindowOrigin = p
                        canvasWindowRect = Rect(p.x, p.y, p.x + s.width, p.y + s.height)
                        canvasSize.value = Size(
                            width = s.width.toFloat(),
                            height = s.height.toFloat()
                        )
                    }
            ) {
                DiagramCanvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .clipToBounds()
                        .editorCanvasGestures(
                            interaction = interaction,
                            currentState = { state },
                            viewModel = viewModel
                        ),
                    state = state,
                    previewStartPort = state.connectingFrom,
                    previewEndWorld = state.previewWireWorld,
                    previewSnapTarget = state.snappedPortTarget,
                    highlights = highlights,
                    panOverride = interaction.localPan,
                    scaleOverride = interaction.localScale,
                    draggingComponentId = null,
                    draggingWorldPos = null,
                    overridePositions = interaction.overridePositions,
                    selectionBox = interaction.selectionRect(),
                    guideWorldX = interaction.guideX,
                    guideWorldY = interaction.guideY,
                    previewComponentTemplate = interaction.palettePreviewComponent,
                    previewComponentWorldPos = interaction.palettePreviewWorld,
                    visualSelectedIds = effectiveSelectedIds
                )

                EditorQuickToolbar(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .zIndex(20f)
                        .padding(top = 10.dp),
                    selectionContext = selectionContext,
                    canUndo = state.canUndo,
                    canRedo = state.canRedo,
                    onUndo = {
                        interaction.resetAfterHistoryNavigation()
                        viewModel.undo()
                    },
                    onRedo = {
                        interaction.resetAfterHistoryNavigation()
                        viewModel.redo()
                    },
                    onZoomOut = {
                        val viewport = canvasSize.value
                        if (viewport.width <= 0f || viewport.height <= 0f) return@EditorQuickToolbar
                        val newScale = (state.scale * 0.9f).coerceIn(0.4f, 3.0f)
                        if (kotlin.math.abs(newScale - state.scale) < 0.0001f) return@EditorQuickToolbar
                        val center = Point2(viewport.width / 2f, viewport.height / 2f)
                        val worldCenter = Point2(
                            (center.x - state.pan.x) / state.scale,
                            (center.y - state.pan.y) / state.scale
                        )
                        val newPan = Point2(
                            center.x - worldCenter.x * newScale,
                            center.y - worldCenter.y * newScale
                        )
                        viewModel.pushUndoStep()
                        clearTransientInteractionState()
                        viewModel.setPanAndScale(newPan, newScale)
                    },
                    onZoomIn = {
                        val viewport = canvasSize.value
                        if (viewport.width <= 0f || viewport.height <= 0f) return@EditorQuickToolbar
                        val newScale = (state.scale * 1.1f).coerceIn(0.4f, 3.0f)
                        if (kotlin.math.abs(newScale - state.scale) < 0.0001f) return@EditorQuickToolbar
                        val center = Point2(viewport.width / 2f, viewport.height / 2f)
                        val worldCenter = Point2(
                            (center.x - state.pan.x) / state.scale,
                            (center.y - state.pan.y) / state.scale
                        )
                        val newPan = Point2(
                            center.x - worldCenter.x * newScale,
                            center.y - worldCenter.y * newScale
                        )
                        viewModel.pushUndoStep()
                        clearTransientInteractionState()
                        viewModel.setPanAndScale(newPan, newScale)
                    },
                    onResetZoom = {
                        viewModel.pushUndoStep()
                        clearTransientInteractionState()
                        viewModel.setPanAndScale(state.pan, 1.0f)
                    },
                    onFitAll = {
                        val components = state.project.components
                        if (components.isEmpty()) return@EditorQuickToolbar

                        val halfWidth = 90f
                        val halfHeight = 50f

                        val minX = components.minOf { it.transform.position.x - halfWidth }
                        val minY = components.minOf { it.transform.position.y - halfHeight }
                        val maxX = components.maxOf { it.transform.position.x + halfWidth }
                        val maxY = components.maxOf { it.transform.position.y + halfHeight }

                        val contentWidth = (maxX - minX).coerceAtLeast(1f)
                        val contentHeight = (maxY - minY).coerceAtLeast(1f)

                        val viewport = canvasSize.value
                        if (viewport.width <= 0f || viewport.height <= 0f) return@EditorQuickToolbar

                        val padding = 48f
                        val scaleX = (viewport.width - padding * 2f) / contentWidth
                        val scaleY = (viewport.height - padding * 2f) / contentHeight
                        val fitScale = minOf(scaleX, scaleY).coerceIn(0.4f, 3.0f)

                        val panX = (viewport.width - contentWidth * fitScale) / 2f - minX * fitScale
                        val panY = (viewport.height - contentHeight * fitScale) / 2f - minY * fitScale

                        viewModel.pushUndoStep()
                        clearTransientInteractionState()
                        viewModel.setPanAndScale(Point2(panX, panY), fitScale)
                    },
                   // onAlignHorizontal = null,
                   // onAlignVertical = null,
                    //onDistributeHorizontal = null,
                    //onDistributeVertical = null,
                   // onDeleteSelection = null,
                   // onDuplicateSelection = null,
                    //onRotateSelection = null
                    onAlignHorizontal = if (effectiveSelectedIds.size >= 2) {
                        {
                            clearTransientInteractionState()
                            viewModel.alignSelectedHorizontally()
                        }
                    } else null,
                    onAlignVertical = if (effectiveSelectedIds.size >= 2) {
                        {
                            clearTransientInteractionState()
                            viewModel.alignSelectedVertically()
                        }
                    } else null,
                    onDistributeHorizontal = if (effectiveSelectedIds.size >= 3) {
                        {
                            clearTransientInteractionState()
                            viewModel.distributeSelectedHorizontally()
                        }
                    } else null,
                    onDistributeVertical = if (effectiveSelectedIds.size >= 3) {
                        {
                            clearTransientInteractionState()
                            viewModel.distributeSelectedVertically()
                        }
                    } else null
                )

                if (componentSelectionRect != null && effectiveSelectedIds.isNotEmpty() && state.selectedConnectionId == null) {
                    val overlayPaddingPx = with(density) { 10.dp.toPx() }
                    val toolbarGapPx = with(density) { 6.dp.toPx() }
                    val toolbarWidthPx = with(density) { 32.dp.toPx() }
                    val toolbarHeightPx = with(density) { 88.dp.toPx() }
                    val viewportPaddingPx = with(density) { 8.dp.toPx() }
                    val canvasWidth = canvasSize.value.width
                    val canvasHeight = canvasSize.value.height

                    val overlayLeft = (componentSelectionRect.left - overlayPaddingPx).coerceIn(0f, canvasWidth)
                    val overlayTop = (componentSelectionRect.top - overlayPaddingPx).coerceIn(0f, canvasHeight)
                    val overlayRight = (componentSelectionRect.right + overlayPaddingPx).coerceIn(0f, canvasWidth)
                    val overlayBottom = (componentSelectionRect.bottom + overlayPaddingPx).coerceIn(0f, canvasHeight)
                    val overlayWidth = (overlayRight - overlayLeft).coerceAtLeast(1f)
                    val overlayHeight = (overlayBottom - overlayTop).coerceAtLeast(1f)

                    fun fits(left: Float, top: Float): Boolean {
                        return left >= viewportPaddingPx &&
                                top >= viewportPaddingPx &&
                                left + toolbarWidthPx <= canvasWidth - viewportPaddingPx &&
                                top + toolbarHeightPx <= canvasHeight - viewportPaddingPx
                    }

                    val preferredTop = overlayTop.coerceIn(
                        viewportPaddingPx,
                        (canvasHeight - toolbarHeightPx - viewportPaddingPx).coerceAtLeast(viewportPaddingPx)
                    )

                    val candidates = listOf(
                        Offset(overlayRight + toolbarGapPx, preferredTop),
                        Offset(overlayLeft - toolbarWidthPx - toolbarGapPx, preferredTop),
                        Offset(
                            (overlayRight - toolbarWidthPx).coerceAtLeast(overlayLeft),
                            overlayBottom + toolbarGapPx
                        ),
                        Offset(
                            (overlayRight - toolbarWidthPx).coerceAtLeast(overlayLeft),
                            overlayTop - toolbarHeightPx - toolbarGapPx
                        )
                    )

                    val chosenToolbarOffset = candidates.firstOrNull { fits(it.x, it.y) }
                        ?: Offset(
                            x = (overlayRight + toolbarGapPx)
                                .coerceAtMost(canvasWidth - toolbarWidthPx - viewportPaddingPx)
                                .coerceAtLeast(viewportPaddingPx),
                            y = preferredTop
                        )

                    SelectionBoundsOverlay(
                        modifier = Modifier
                            .graphicsLayer {
                                translationX = overlayLeft
                                translationY = overlayTop
                            }
                            .width(with(density) { overlayWidth.toDp() })
                            .height(with(density) { overlayHeight.toDp() })
                            .zIndex(18f)
                    )

                    SelectionFloatingToolbar(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    x = chosenToolbarOffset.x.roundToInt(),
                                    y = chosenToolbarOffset.y.roundToInt()
                                )
                            }
                            .zIndex(19f),
                        onDuplicate = if (effectiveSelectedIds.isNotEmpty()) {
                            {
                                clearTransientInteractionState()
                                interaction.pinnedSelectionIds = emptySet()
                                val duplicatedIds = viewModel.duplicateSelectedComponents(
                                    offset = Point2(48f, 48f),
                                    snapTo = { SnapEngine.snapToGrid(it) }
                                )
                                if (duplicatedIds.isNotEmpty()) {
                                    viewModel.setSelectedComponents(duplicatedIds, source = SelectionSource.DUPLICATE)
                                    interaction.clearPinnedSelection()
                                }
                            }
                        } else null,
                        onDelete = when {
                            state.selectedConnectionId != null -> {
                                {
                                    clearTransientInteractionState()
                                    interaction.clearPinnedSelection()
                                    state.selectedConnectionId?.let(viewModel::deleteConnection)
                                }
                            }
                            effectiveSelectedIds.isNotEmpty() -> {
                                {
                                    clearTransientInteractionState()
                                    interaction.clearPinnedSelection()
                                    viewModel.deleteSelectedComponents()
                                }
                            }
                            else -> null
                        },
                        onRotate = if (effectiveSelectedIds.isNotEmpty() && state.selectedConnectionId == null) {
                            {
                                clearTransientInteractionState()
                                interaction.clearPinnedSelection()
                                viewModel.rotateSelectedComponentsClockwise()
                            }
                        } else null
                    )
                }
            }
        }

        val showComponentPanel =
            interaction.draggingIds.isEmpty() &&
                    interaction.overridePositions == null &&
                    !interaction.boxSelecting &&
                    state.selectionSource == SelectionSource.TAP_COMPONENT &&
                    effectiveSelectedIds.size == 1 &&
                    selectedComponent != null

        val showConnectionPanel =
            interaction.draggingIds.isEmpty() &&
                    interaction.overridePositions == null &&
                    !interaction.boxSelecting &&
                    state.selectionSource == SelectionSource.TAP_CONNECTION &&
                    state.selectedConnectionId != null &&
                    selectedConnection != null

        if (showComponentPanel && selectedComponent != null) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(290.dp)
                    .align(Alignment.TopEnd)
                    .padding(top = toolbarTopInset, end = 8.dp, bottom = 8.dp)
            ) {
                PropertiesPanel(component = selectedComponent, viewModel = viewModel)
            }
        }

        if (showConnectionPanel && selectedConnection != null) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(320.dp)
                    .align(Alignment.CenterEnd)
                    .padding(end = 8.dp)
            ) {
                ConnectionPropertiesPanel(
                    connection = selectedConnection,
                    viewModel = viewModel,
                    onClose = { viewModel.clearSelection() }
                )
            }
        }

        if (validationSummary.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = paletteWidth + 12.dp,
                        end = 12.dp,
                        bottom = 12.dp
                    )
                    .align(Alignment.BottomCenter),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Card {
                    TextButton(onClick = { validationDrawerOpen = !validationDrawerOpen }) {
                        Text(if (validationDrawerOpen) "Resumo de validação ▼" else "Resumo de validação ▲")
                    }
                }

                if (validationDrawerOpen) {
                    Spacer(Modifier.height(8.dp))
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text("Resumo de validação", style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.height(6.dp))
                            validationSummary.forEach { (title, msg) ->
                                Text(title, style = MaterialTheme.typography.bodySmall)
                                Text(msg, style = MaterialTheme.typography.bodySmall)
                                Spacer(Modifier.height(6.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
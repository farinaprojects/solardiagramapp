package br.com.solardiagram.ui.screens.editor

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.Modifier
import br.com.solardiagram.domain.model.Point2
import br.com.solardiagram.ui.viewmodel.EditorUiState
import br.com.solardiagram.ui.viewmodel.EditorViewModel

fun Modifier.editorCanvasGestures(
    interaction: EditorCanvasInteractionState,
    currentState: () -> EditorUiState,
    viewModel: EditorViewModel
): Modifier = pointerInput(viewModel, currentState().project.id) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val pointerId = down.id
        val start = down.position
        val startTime = System.currentTimeMillis()

        val stateAtDown = currentState()
        val scaleAtDown = interaction.effectiveScale(stateAtDown.scale)
        val panAtDown = interaction.effectivePan(stateAtDown.pan)
        val target = hitTestEditorTarget(
            components = stateAtDown.project.components,
            connections = stateAtDown.project.connections,
            screenPoint = start,
            scale = scaleAtDown,
            pan = panAtDown
        )

        while (true) {
            val ev = awaitPointerEvent(pass = PointerEventPass.Final)
            val pressedCount = ev.changes.count { it.pressed }

            if (pressedCount >= 2) {
                viewModel.pushUndoStep()

                var tmpScale = interaction.effectiveScale(currentState().scale)
                var tmpPan = interaction.effectivePan(currentState().pan)

                while (true) {
                    val e2 = awaitPointerEvent(pass = PointerEventPass.Final)
                    val pc = e2.changes.count { it.pressed }

                    if (pc == 0) break
                    if (pc < 2) {
                        if (e2.changes.none { it.pressed }) break
                        continue
                    }

                    val centroid = e2.calculateCentroid()
                    val pan = e2.calculatePan()
                    val zoom = e2.calculateZoom()

                    val oldScale = tmpScale
                    val oldPan = tmpPan
                    val newScale = (oldScale * zoom).coerceIn(0.4f, 3.0f)

                    val worldUnderCentroid = Point2(
                        x = (centroid.x - oldPan.x) / oldScale,
                        y = (centroid.y - oldPan.y) / oldScale
                    )
                    val anchoredPan = Point2(
                        x = centroid.x - worldUnderCentroid.x * newScale,
                        y = centroid.y - worldUnderCentroid.y * newScale
                    )
                    val finalPan = Point2(
                        x = anchoredPan.x + pan.x,
                        y = anchoredPan.y + pan.y
                    )

                    tmpScale = newScale
                    tmpPan = finalPan

                    interaction.localScale = tmpScale
                    interaction.localPan = tmpPan

                    e2.changes.forEach { it.consume() }
                }

                interaction.localScale?.let(viewModel::setScale)
                interaction.localPan?.let(viewModel::setPan)
                interaction.localScale = null
                interaction.localPan = null
                return@awaitEachGesture
            }

            val ch = ev.changes.firstOrNull { it.id == pointerId } ?: return@awaitEachGesture

            if (!ch.pressed) {
                when (target) {
                    is EditorHitTarget.Component -> { interaction.clearPinnedSelection(); viewModel.selectComponent(target.componentId) }
                    is EditorHitTarget.Connection -> { interaction.clearPinnedSelection(); viewModel.selectConnection(target.connectionId) }
                    EditorHitTarget.None -> { interaction.clearPinnedSelection(); viewModel.clearSelection() }
                    is EditorHitTarget.Port -> { interaction.clearPinnedSelection(); viewModel.selectComponent(target.componentId) }
                }
                return@awaitEachGesture
            }

            val dx = ch.position.x - start.x
            val dy = ch.position.y - start.y
            val dist2 = dx * dx + dy * dy
            val moveSlopPx = 2.5f
            val longPressMs = 260L
            val movedEnough = dist2 >= moveSlopPx * moveSlopPx
            val longPressed = (System.currentTimeMillis() - startTime) >= longPressMs

            if (target is EditorHitTarget.Port) {
                val cid = target.componentId
                val pid = target.portId

                if (stateAtDown.connectingFrom != null) {
                    viewModel.tryFinishConnection(cid, pid)
                    return@awaitEachGesture
                }

                viewModel.startConnection(cid, pid)

                dragLoopContinuous(
                    initialDown = ch,
                    onMove = { pos ->
                        val world = screenToWorld(pos, scaleAtDown, panAtDown)
                        val snap = SnapEngine.findNearestPort(
                            components = stateAtDown.project.components,
                            world = world,
                            scale = scaleAtDown,
                            thresholdPx = 22f
                        )
                        if (snap != null) {
                            viewModel.updateWirePreview(
                                world = snap.portWorld,
                                snappedTarget = snap.componentId to snap.portId
                            )
                        } else {
                            viewModel.updateWirePreview(world, snappedTarget = null)
                        }
                    },
                    onUp = { pos ->
                        val world = screenToWorld(pos, scaleAtDown, panAtDown)
                        val snap = SnapEngine.findNearestPort(
                            components = stateAtDown.project.components,
                            world = world,
                            scale = scaleAtDown,
                            thresholdPx = 22f
                        )
                        if (snap != null) {
                            viewModel.tryFinishConnection(snap.componentId, snap.portId)
                        } else {
                            val upHit = hitTestEditorTarget(
                                components = stateAtDown.project.components,
                                connections = stateAtDown.project.connections,
                                screenPoint = pos,
                                scale = scaleAtDown,
                                pan = panAtDown
                            )
                            if (upHit is EditorHitTarget.Port) viewModel.tryFinishConnection(upHit.componentId, upHit.portId)
                            else viewModel.cancelConnection()
                        }
                    }
                )
                return@awaitEachGesture
            }

            if (target is EditorHitTarget.Component) {
                if (!longPressed) continue

                val compId = target.componentId
                val currentSelection = currentState().selectedComponentIds.ifEmpty { interaction.pinnedSelectionIds }
                val movingGroup = currentSelection.isNotEmpty() && compId in currentSelection
                val groupIds = if (movingGroup) currentSelection else setOf(compId)

                if (!movingGroup) {
                    interaction.clearPinnedSelection()
                    viewModel.selectComponent(compId)
                }

                viewModel.pushUndoStep()

                val compsById = stateAtDown.project.components.associateBy { it.id }
                val downWorld = screenToWorld(start, scaleAtDown, panAtDown)
                val baseComp = compsById[compId]
                val baseStart = baseComp?.transform?.position ?: downWorld
                val grabOffset = Point2(downWorld.x - baseStart.x, downWorld.y - baseStart.y)

                val initialPositions = groupIds.associateWith { id ->
                    compsById[id]?.transform?.position ?: Point2(0f, 0f)
                }

                interaction.draggingIds = groupIds
                interaction.guideX = null
                interaction.guideY = null

                dragLoopContinuous(
                    initialDown = ch,
                    onMove = { pos ->
                        val world = screenToWorld(pos, scaleAtDown, panAtDown)
                        val baseNew = Point2(world.x - grabOffset.x, world.y - grabOffset.y)

                        val snappedVisual = SnapEngine.snapToGrid(baseNew)
                        interaction.guideX = snappedVisual.x
                        interaction.guideY = snappedVisual.y

                        val ddx = baseNew.x - baseStart.x
                        val ddy = baseNew.y - baseStart.y

                        interaction.overridePositions = groupIds.associateWith { id ->
                            val p0 = initialPositions[id] ?: Point2(0f, 0f)
                            Point2(p0.x + ddx, p0.y + ddy)
                        }
                    },
                    onUp = {
                        val currentOverrides = interaction.overridePositions
                        val finalBase = currentOverrides?.get(compId) ?: baseStart
                        val snappedBase = SnapEngine.snapToGrid(finalBase)

                        val ddx = snappedBase.x - baseStart.x
                        val ddy = snappedBase.y - baseStart.y

                        groupIds.forEach { id ->
                            val p0 = initialPositions[id] ?: return@forEach
                            viewModel.moveComponent(id, Point2(p0.x + ddx, p0.y + ddy))
                        }
                        interaction.clearPinnedSelection()
                        viewModel.setSelectedComponents(groupIds, source = br.com.solardiagram.ui.viewmodel.SelectionSource.DRAG)
                        viewModel.markSelectionAsDragged()

                        interaction.overridePositions = null
                        interaction.draggingIds = emptySet()
                        interaction.guideX = null
                        interaction.guideY = null
                    }
                )
                return@awaitEachGesture
            }

            if (target is EditorHitTarget.None || target is EditorHitTarget.Connection) {
                if (movedEnough) {
                    viewModel.pushUndoStep()
                    dragLoopContinuous(
                        initialDown = ch,
                        onMove = { pos ->
                            val delta = pos - start
                            interaction.localPan = Point2(panAtDown.x + delta.x, panAtDown.y + delta.y)
                        },
                        onUp = {
                            interaction.localPan?.let(viewModel::setPan)
                            interaction.localPan = null
                        }
                    )
                    return@awaitEachGesture
                }

                if (longPressed && !movedEnough) {
                    interaction.boxSelecting = true
                    interaction.boxStart = start
                    interaction.boxEnd = start
                    interaction.clearPinnedSelection()
                    viewModel.clearSelection()

                    dragLoopContinuous(
                        initialDown = ch,
                        onMove = { pos -> interaction.boxEnd = pos },
                        onUp = {
                            val a = interaction.boxStart
                            val b = interaction.boxEnd
                            if (a != null && b != null) {
                                val rect = screenRect(a, b)
                                val hits = stateAtDown.project.components
                                    .filter { comp ->
                                        val compRect = ComponentRenderer.componentScreenRect(comp, scaleAtDown, panAtDown)
                                        compRect.overlaps(rect)
                                    }
                                    .map { it.id }
                                    .toSet()

                                viewModel.setSelectedComponents(hits)
                            }
                            interaction.clearSelectionBox()
                        }
                    )
                    return@awaitEachGesture
                }

                continue
            }
        }
    }
}
fun screenToWorld(
    screen: Offset,
    scale: Float,
    pan: Point2
): Point2 {
    return Point2(
        x = (screen.x - pan.x) / scale,
        y = (screen.y - pan.y) / scale
    )
}

package br.com.solardiagram.ui.screens.editor

import android.graphics.Paint
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import br.com.solardiagram.domain.model.Component
import br.com.solardiagram.domain.model.ComponentType
import br.com.solardiagram.domain.model.ElectricalPhase
import br.com.solardiagram.domain.model.Point2
import br.com.solardiagram.domain.model.Port
import br.com.solardiagram.domain.model.PortKind
import br.com.solardiagram.domain.model.SystemPhase

object ComponentRenderer {

    private const val DEFAULT_W = 96f
    private const val DEFAULT_H = 40f

    private const val PV_BODY_W = 86f
    private const val PV_BODY_H = 42f

    private const val MICRO2_BODY_W = 132f
    private const val MICRO2_BODY_H = 74f

    private const val MICRO4_BODY_W = 150f
    private const val MICRO4_BODY_H = 94f

    private const val STRING_BODY_W = 130f
    private const val STRING_BODY_H = 64f

    private const val BUS_MONO_W = 74f
    private const val BUS_BI_W = 132f
    private const val BUS_TRI_W = 196f
    private const val BUS_ITEM_W = 44f
    private const val BUS_ITEM_H = 62f
    private const val BUS_ITEM_GAP = 14f

    private const val BAR_W = 52f
    private const val BAR_H = 58f

    private const val QDG_W = 100f
    private const val QDG_H = 42f

    private const val BREAKER_1P_W = 76f
    private const val BREAKER_2P_W = 98f
    private const val BREAKER_3P_W = 120f
    private const val BREAKER_H = 42f

    private const val DPS_W = 64f
    private const val DPS_H = 32f

    private const val LOAD_MONO_W = 88f
    private const val LOAD_MONO_H = 42f
    private const val LOAD_BI_W = 124f
    private const val LOAD_BI_H = 62f
    private const val LOAD_TRI_W = 136f
    private const val LOAD_TRI_H = 78f

    private const val GROUND_W = 92f
    private const val GROUND_H = 24f

    private const val DC_BOX_W = 26f
    private const val DC_BOX_H = 22f

    private const val PV_DC_BOX_W = 26f
    private const val PV_DC_BOX_H = 20f

    private const val AC_BOX_W = 72f
    private const val AC_BOX_H = 22f

    private const val PE_BOX_W = 32f
    private const val PE_BOX_H = 22f

    private const val BOX_GAP = 6f
    private const val PORT_R = 4.8f

    private val SELECTED_STROKE = Color(0xFF1565C0)
    private val SELECTED_FILL = Color(0xFFBBDEFB)
    private val DRAGGING_STROKE = Color(0xFFEF6C00)
    private val DRAGGING_FILL = Color(0xFFFFCC80)

    fun drawComponent(
        drawScope: DrawScope,
        component: Component,
        scale: Float,
        pan: Point2,
        isSelected: Boolean,
        isDragging: Boolean,
        isError: Boolean,
        isWarning: Boolean
    ) {
        val center = component.transform.position
        val pivotX = center.x * scale + pan.x
        val pivotY = center.y * scale + pan.y
        val angle = componentRotationDegrees(component)
        val canvas = drawScope.drawContext.canvas.nativeCanvas
        canvas.save()
        if (angle != 0f) {
            canvas.rotate(angle, pivotX, pivotY)
        }
        try {
            when (component.type) {
                ComponentType.PV_MODULE -> drawPvModule(
                    drawScope, component, scale, pan, isSelected, isDragging, isError, isWarning
                )

                ComponentType.MICROINVERTER -> drawMicroInverter(
                    drawScope, component, scale, pan, isSelected, isDragging, isError, isWarning
                )

                ComponentType.STRING_INVERTER -> drawStringInverter(
                    drawScope, component, scale, pan, isSelected, isDragging, isError, isWarning
                )

                ComponentType.AC_BUS -> drawAcBus(
                    drawScope, component, scale, pan, isSelected, isDragging, isError, isWarning
                )

                ComponentType.BARL,
                ComponentType.BARN,
                ComponentType.BARPE -> drawSimpleBarJunction(
                    drawScope, component, scale, pan, isSelected, isDragging, isError, isWarning
                )

                ComponentType.QDG -> drawQdg(
                    drawScope, component, scale, pan, isSelected, isDragging, isError, isWarning
                )
                ComponentType.GRID_SOURCE -> drawQdg(
                    drawScope, component, scale, pan, isSelected, isDragging, isError, isWarning
                )

                ComponentType.BREAKER -> drawBreaker(
                    drawScope, component, scale, pan, isSelected, isDragging, isError, isWarning
                )

                ComponentType.DPS -> drawDps(
                    drawScope, component, scale, pan, isSelected, isDragging, isError, isWarning
                )

                ComponentType.GROUND_BAR -> drawGroundBar(
                    drawScope, component, scale, pan, isSelected, isDragging, isError, isWarning
                )

                ComponentType.LOAD -> drawLoad(
                    drawScope, component, scale, pan, isSelected, isDragging, isError, isWarning
                )
            }
        } finally {
            canvas.restore()
        }
    }

    fun componentScreenRect(
        component: Component,
        scale: Float,
        pan: Point2
    ): Rect = EditorGeometry.componentScreenRect(component, scale, pan)

    fun portCanvasPosition(
        component: Component,
        portId: String,
        scale: Float,
        pan: Point2
    ): Offset? = EditorGeometry.portCanvasPosition(component, portId, scale, pan)

    fun portWorldPosition(
        component: Component,
        port: Port,
        index: Int
    ): Point2 = EditorGeometry.portWorldPosition(component, port, index)

    private fun portBaseWorldPosition(
        component: Component,
        port: Port
    ): Point2 = EditorGeometry.portBaseWorldPosition(component, port)

    private fun drawPvModule(
        drawScope: DrawScope,
        component: Component,
        scale: Float,
        pan: Point2,
        isSelected: Boolean,
        isDragging: Boolean,
        isError: Boolean,
        isWarning: Boolean
    ) {
        val c = component.transform.position
        val body = Rect(
            c.x - PV_BODY_W / 2f,
            c.y - PV_BODY_H / 2f,
            c.x + PV_BODY_W / 2f,
            c.y + PV_BODY_H / 2f
        )
        val posRect = Rect(
            body.center.x - 20f - PV_DC_BOX_W,
            body.bottom + BOX_GAP,
            body.center.x - 20f,
            body.bottom + BOX_GAP + PV_DC_BOX_H
        )
        val negRect = Rect(
            body.center.x + 20f,
            body.bottom + BOX_GAP,
            body.center.x + 20f + PV_DC_BOX_W,
            body.bottom + BOX_GAP + PV_DC_BOX_H
        )
        val style = styleColors(isSelected, isDragging, isError, isWarning)
        with(drawScope) {
            drawSelectionHalo(this, body, scale, pan, isSelected, isDragging)

            drawNamedBox(this, body, style.stroke, style.fill, component.name.uppercase(), scale, pan, 7.8f)
            drawSmallLabeledBox(this, posRect, style.stroke, "DC+", scale, pan)
            drawSmallLabeledBox(this, negRect, style.stroke, "DC-", scale, pan)

            component.ports.forEachIndexed { idx, port ->
                drawPortCircle(this, component, port, idx, scale, pan)
            }
        }
    }

    private fun drawMicroInverter(
        drawScope: DrawScope,
        component: Component,
        scale: Float,
        pan: Point2,
        isSelected: Boolean,
        isDragging: Boolean,
        isError: Boolean,
        isWarning: Boolean
    ) {
        val pairs = microInputPairs(component)
        val bodyW = if (pairs >= 4) MICRO4_BODY_W else MICRO2_BODY_W
        val bodyH = if (pairs >= 4) MICRO4_BODY_H else MICRO2_BODY_H
        val c = component.transform.position
        val body = Rect(
            c.x - bodyW / 2f,
            c.y - bodyH / 2f,
            c.x + bodyW / 2f,
            c.y + bodyH / 2f
        )

        val sideRects = mutableMapOf<String, Rect>()
        if (pairs >= 2) {
            sideRects["DC1"] = Rect(
                body.left - BOX_GAP - DC_BOX_W,
                body.top + 8f,
                body.left - BOX_GAP,
                body.top + 8f + DC_BOX_H
            )
            sideRects["DC2"] = Rect(
                body.right + BOX_GAP,
                body.top + 8f,
                body.right + BOX_GAP + DC_BOX_W,
                body.top + 8f + DC_BOX_H
            )
        }
        if (pairs >= 4) {
            sideRects["DC3"] = Rect(
                body.left - BOX_GAP - DC_BOX_W,
                body.top + 38f,
                body.left - BOX_GAP,
                body.top + 38f + DC_BOX_H
            )
            sideRects["DC4"] = Rect(
                body.right + BOX_GAP,
                body.top + 38f,
                body.right + BOX_GAP + DC_BOX_W,
                body.top + 38f + DC_BOX_H
            )
        }

        val totalBottomW = PE_BOX_W + 4f + AC_BOX_W
        val startX = c.x - totalBottomW / 2f
        val peRect = Rect(
            startX,
            body.bottom + BOX_GAP,
            startX + PE_BOX_W,
            body.bottom + BOX_GAP + PE_BOX_H
        )
        val acRect = Rect(
            peRect.right + 4f,
            body.bottom + BOX_GAP,
            peRect.right + 4f + AC_BOX_W,
            body.bottom + BOX_GAP + AC_BOX_H
        )

        val style = styleColors(isSelected, isDragging, isError, isWarning)
        with(drawScope) {
            drawSelectionHalo(this, body, scale, pan, isSelected, isDragging)

            drawNamedBox(this, body, style.stroke, style.fill, "MICROINVERSOR", scale, pan, if (pairs >= 4) 9.2f else 10.0f)

            listOf("DC1", "DC2", "DC3", "DC4").forEach { key ->
                sideRects[key]?.let {
                    drawSmallLabeledBox(this, it, style.stroke, key, scale, pan)
                    drawPolarityMarks(this, it, scale, pan)
                }
            }

            drawSmallLabeledBox(this, peRect, style.stroke, "PE", scale, pan)
            drawSmallLabeledBox(this, acRect, style.stroke, "AC OUT", scale, pan)
            drawAcTripletLabels(this, acRect, scale, pan)

            component.ports.forEachIndexed { idx, port ->
                drawPortCircle(this, component, port, idx, scale, pan)
            }
        }
    }

    private fun drawStringInverter(
        drawScope: DrawScope,
        component: Component,
        scale: Float,
        pan: Point2,
        isSelected: Boolean,
        isDragging: Boolean,
        isError: Boolean,
        isWarning: Boolean
    ) {
        val c = component.transform.position
        val body = Rect(
            c.x - STRING_BODY_W / 2f,
            c.y - STRING_BODY_H / 2f,
            c.x + STRING_BODY_W / 2f,
            c.y + STRING_BODY_H / 2f
        )
        val dcRect = Rect(
            body.left - BOX_GAP - DC_BOX_W,
            c.y - DC_BOX_H / 2f,
            body.left - BOX_GAP,
            c.y + DC_BOX_H / 2f
        )
        val acRect = Rect(
            body.right + BOX_GAP,
            c.y - AC_BOX_H / 2f,
            body.right + BOX_GAP + AC_BOX_W,
            c.y + AC_BOX_H / 2f
        )
        val peRect = Rect(
            c.x - PE_BOX_W / 2f,
            body.bottom + BOX_GAP,
            c.x + PE_BOX_W / 2f,
            body.bottom + BOX_GAP + PE_BOX_H
        )

        val style = styleColors(isSelected, isDragging, isError, isWarning)
        with(drawScope) {
            drawSelectionHalo(this, body, scale, pan, isSelected, isDragging)

            drawNamedBox(this, body, style.stroke, style.fill, "INVERSOR STRING", scale, pan, 9.0f)

            drawSmallLabeledBox(this, dcRect, style.stroke, "DC", scale, pan)
            drawPolarityMarks(this, dcRect, scale, pan)

            drawSmallLabeledBox(this, acRect, style.stroke, "AC OUT", scale, pan)
            drawTextCentered(this, "L", (acRect.center.x - 12f) * scale + pan.x, (acRect.center.y + 7f) * scale + pan.y, 6.5f * scale)
            drawTextCentered(this, "N", (acRect.center.x + 12f) * scale + pan.x, (acRect.center.y + 7f) * scale + pan.y, 6.5f * scale)

            drawSmallLabeledBox(this, peRect, style.stroke, "PE", scale, pan)

            component.ports.forEachIndexed { idx, port ->
                drawPortCircle(this, component, port, idx, scale, pan)
            }
        }
    }

    private fun drawAcBus(
        drawScope: DrawScope,
        component: Component,
        scale: Float,
        pan: Point2,
        isSelected: Boolean,
        isDragging: Boolean,
        isError: Boolean,
        isWarning: Boolean
    ) {
        val c = component.transform.position
        val channels = EditorGeometry.acBusChannelLayout(component)
        val overall = EditorGeometry.acBusOverallRect(component)
        val style = styleColors(isSelected, isDragging, isError, isWarning)

        with(drawScope) {
            drawSelectionHalo(this, overall, scale, pan, isSelected, isDragging)

            drawTextCentered(
                this,
                component.name.uppercase(),
                c.x * scale + pan.x,
                (overall.top - 12f) * scale + pan.y,
                6.7f * scale
            )

            channels.forEach { channel ->
                drawNamedBox(this, channel.rect, style.stroke, Color(0xFFF8FAFB), channel.centerLabel, scale, pan, 7.1f)
                drawTextCentered(this, "IN", channel.rect.center.x * scale + pan.x, (channel.rect.top + 10f) * scale + pan.y, 6.3f * scale)
                drawTextCentered(this, "OUT", channel.rect.center.x * scale + pan.x, (channel.rect.bottom - 6f) * scale + pan.y, 6.3f * scale)
            }

            drawTextCentered(
                this,
                "JUNÇÃO E SOMA DE CORRENTE POR CONDUTOR",
                c.x * scale + pan.x,
                (overall.bottom + 12f) * scale + pan.y,
                5.8f * scale
            )

            component.ports.forEachIndexed { idx, port ->
                drawPortCircle(this, component, port, idx, scale, pan)
            }
        }
    }

    private fun drawSimpleBarJunction(
        drawScope: DrawScope,
        component: Component,
        scale: Float,
        pan: Point2,
        isSelected: Boolean,
        isDragging: Boolean,
        isError: Boolean,
        isWarning: Boolean
    ) {
        val c = component.transform.position
        val body = Rect(
            c.x - BAR_W / 2f,
            c.y - BAR_H / 2f,
            c.x + BAR_W / 2f,
            c.y + BAR_H / 2f
        )
        val style = styleColors(isSelected, isDragging, isError, isWarning)
        val centerLabel = barCenterLabel(component)
        with(drawScope) {
            drawSelectionHalo(this, body, scale, pan, isSelected, isDragging)
            drawNamedBox(this, body, style.stroke, Color(0xFFF8FAFB), centerLabel, scale, pan, 7.0f)
            drawTextCentered(this, "IN", body.center.x * scale + pan.x, (body.top + 9f) * scale + pan.y, 5.6f * scale)
            drawTextCentered(this, "OUT", body.center.x * scale + pan.x, (body.bottom - 6f) * scale + pan.y, 5.6f * scale)
            component.ports.forEachIndexed { idx, port ->
                drawPortCircle(this, component, port, idx, scale, pan)
            }
        }
    }

    private fun drawQdg(
        drawScope: DrawScope,
        component: Component,
        scale: Float,
        pan: Point2,
        isSelected: Boolean,
        isDragging: Boolean,
        isError: Boolean,
        isWarning: Boolean
    ) {
        drawTwoPoleGeneric(
            drawScope, component, scale, pan, isSelected, isDragging, isError, isWarning,
            QDG_W, QDG_H, "QDG / QUADRO"
        )
    }

    private fun drawBreaker(
        drawScope: DrawScope,
        component: Component,
        scale: Float,
        pan: Point2,
        isSelected: Boolean,
        isDragging: Boolean,
        isError: Boolean,
        isWarning: Boolean
    ) {
        val width = breakerWidth(component)
        val height = BREAKER_H
        val c = component.transform.position
        val body = Rect(
            c.x - width / 2f,
            c.y - height / 2f,
            c.x + width / 2f,
            c.y + height / 2f
        )
        val style = styleColors(isSelected, isDragging, isError, isWarning)
        val pole = breakerPole(component)
        val polesLabel = when (pole) {
            br.com.solardiagram.domain.model.BreakerPole.P1 -> "1P"
            br.com.solardiagram.domain.model.BreakerPole.P2 -> "2P"
            br.com.solardiagram.domain.model.BreakerPole.P3 -> "3P"
            br.com.solardiagram.domain.model.BreakerPole.P4 -> "4P"
        }
        val separators = when (pole) {
            br.com.solardiagram.domain.model.BreakerPole.P1 -> emptyList()
            br.com.solardiagram.domain.model.BreakerPole.P2 -> listOf(body.center.x)
            br.com.solardiagram.domain.model.BreakerPole.P3 -> listOf(body.left + width / 3f, body.left + (2f * width / 3f))
            br.com.solardiagram.domain.model.BreakerPole.P4 -> listOf(body.left + width / 4f, body.left + width / 2f, body.left + (3f * width / 4f))
        }

        with(drawScope) {
            drawSelectionHalo(this, body, scale, pan, isSelected, isDragging)
            drawNamedBox(this, body, style.stroke, style.fill, component.name.uppercase(), scale, pan, 7.0f)

            separators.forEach { xLine ->
                drawLine(
                    color = style.stroke.copy(alpha = 0.28f),
                    start = Offset(xLine * scale + pan.x, (body.top + 6f) * scale + pan.y),
                    end = Offset(xLine * scale + pan.x, (body.bottom - 6f) * scale + pan.y),
                    strokeWidth = 1.0f * scale
                )
            }

            drawTextCentered(this, polesLabel, body.center.x * scale + pan.x, (body.bottom - 5f) * scale + pan.y, 6.1f * scale)
            component.ports.forEachIndexed { idx, port ->
                drawPortCircle(this, component, port, idx, scale, pan)
                drawPortConnectorLabel(this, component, port, idx, scale, pan)
            }
        }
    }

    private fun drawDps(
        drawScope: DrawScope,
        component: Component,
        scale: Float,
        pan: Point2,
        isSelected: Boolean,
        isDragging: Boolean,
        isError: Boolean,
        isWarning: Boolean
    ) {
        drawLeftTwoAndBottomOneGeneric(
            drawScope, component, scale, pan, isSelected, isDragging, isError, isWarning,
            DPS_W, DPS_H, "DPS AC"
        )
    }

    private fun drawGroundBar(
        drawScope: DrawScope,
        component: Component,
        scale: Float,
        pan: Point2,
        isSelected: Boolean,
        isDragging: Boolean,
        isError: Boolean,
        isWarning: Boolean
    ) {
        val c = component.transform.position
        val body = Rect(
            c.x - GROUND_W / 2f,
            c.y - GROUND_H / 2f,
            c.x + GROUND_W / 2f,
            c.y + GROUND_H / 2f
        )
        val style = styleColors(isSelected, isDragging, isError, isWarning)

        with(drawScope) {
            drawSelectionHalo(this, body, scale, pan, isSelected, isDragging)
            drawNamedBox(this, body, style.stroke, style.fill, "BARRAMENTO\nTERRA", scale, pan, 6.8f)
            drawRect(
                color = Color(0xFF111111),
                topLeft = Offset((body.left + 10f) * scale + pan.x, (body.center.y + 4f) * scale + pan.y),
                size = Size((body.width - 20f) * scale, 2.5f * scale),
                style = Fill
            )
            component.ports.forEachIndexed { idx, port ->
                drawPortCircle(this, component, port, idx, scale, pan)
            }
        }
    }

    private fun drawLoad(
        drawScope: DrawScope,
        component: Component,
        scale: Float,
        pan: Point2,
        isSelected: Boolean,
        isDragging: Boolean,
        isError: Boolean,
        isWarning: Boolean
    ) {
        val (width, height) = loadDimensions(component)
        val c = component.transform.position
        val body = Rect(
            c.x - width / 2f,
            c.y - height / 2f,
            c.x + width / 2f,
            c.y + height / 2f
        )
        val style = styleColors(isSelected, isDragging, isError, isWarning)
        val phaseLabel = when (loadPhase(component)) {
            br.com.solardiagram.domain.model.SystemPhase.MONO -> "MONO"
            br.com.solardiagram.domain.model.SystemPhase.BI -> "BI"
            br.com.solardiagram.domain.model.SystemPhase.TRI -> "TRI"
        }

        with(drawScope) {
            drawSelectionHalo(this, body, scale, pan, isSelected, isDragging)
            drawNamedBox(this, body, style.stroke, style.fill, component.name.uppercase(), scale, pan, 7.6f)
            drawTextCentered(this, phaseLabel, body.center.x * scale + pan.x, (body.top + 10f) * scale + pan.y, 5.8f * scale)
            drawLine(
                color = style.stroke.copy(alpha = 0.20f),
                start = Offset((body.left + 10f) * scale + pan.x, (body.bottom - 12f) * scale + pan.y),
                end = Offset((body.right - 10f) * scale + pan.x, (body.bottom - 12f) * scale + pan.y),
                strokeWidth = 1.0f * scale
            )
            component.ports.forEachIndexed { idx, port ->
                drawPortCircle(this, component, port, idx, scale, pan)
                drawPortConnectorLabel(this, component, port, idx, scale, pan)
            }
        }
    }

    private fun drawTwoPoleGeneric(
        drawScope: DrawScope,
        component: Component,
        scale: Float,
        pan: Point2,
        isSelected: Boolean,
        isDragging: Boolean,
        isError: Boolean,
        isWarning: Boolean,
        width: Float,
        height: Float,
        label: String
    ) {
        val c = component.transform.position
        val body = Rect(
            c.x - width / 2f,
            c.y - height / 2f,
            c.x + width / 2f,
            c.y + height / 2f
        )
        val style = styleColors(isSelected, isDragging, isError, isWarning)

        with(drawScope) {
            drawSelectionHalo(this, body, scale, pan, isSelected, isDragging)
            drawNamedBox(this, body, style.stroke, style.fill, label, scale, pan, 8.2f)
            component.ports.forEachIndexed { idx, port ->
                drawPortCircle(this, component, port, idx, scale, pan)
            }
        }
    }

    private fun drawLeftTwoAndBottomOneGeneric(
        drawScope: DrawScope,
        component: Component,
        scale: Float,
        pan: Point2,
        isSelected: Boolean,
        isDragging: Boolean,
        isError: Boolean,
        isWarning: Boolean,
        width: Float,
        height: Float,
        label: String
    ) {
        val c = component.transform.position
        val body = Rect(
            c.x - width / 2f,
            c.y - height / 2f,
            c.x + width / 2f,
            c.y + height / 2f
        )
        val style = styleColors(isSelected, isDragging, isError, isWarning)

        with(drawScope) {
            drawSelectionHalo(this, body, scale, pan, isSelected, isDragging)
            drawNamedBox(this, body, style.stroke, style.fill, label, scale, pan, 8.2f)
            component.ports.forEachIndexed { idx, port ->
                drawPortCircle(this, component, port, idx, scale, pan)
            }
        }
    }

    private fun breakerPole(component: Component): br.com.solardiagram.domain.model.BreakerPole {
        val specs = component.specs as? br.com.solardiagram.domain.model.ElectricalSpecs.BreakerSpecs
        return specs?.poles ?: br.com.solardiagram.domain.model.BreakerPole.P2
    }

    private fun breakerWidth(component: Component): Float = when (breakerPole(component)) {
        br.com.solardiagram.domain.model.BreakerPole.P1 -> BREAKER_1P_W
        br.com.solardiagram.domain.model.BreakerPole.P2 -> BREAKER_2P_W
        br.com.solardiagram.domain.model.BreakerPole.P3 -> BREAKER_3P_W
        br.com.solardiagram.domain.model.BreakerPole.P4 -> BREAKER_3P_W + 18f
    }

    private fun loadPhase(component: Component): br.com.solardiagram.domain.model.SystemPhase {
        val specs = component.specs as? br.com.solardiagram.domain.model.ElectricalSpecs.LoadSpecs
        return specs?.phases ?: br.com.solardiagram.domain.model.SystemPhase.MONO
    }

    private fun loadDimensions(component: Component): Pair<Float, Float> = when (loadPhase(component)) {
        br.com.solardiagram.domain.model.SystemPhase.MONO -> LOAD_MONO_W to LOAD_MONO_H
        br.com.solardiagram.domain.model.SystemPhase.BI -> LOAD_BI_W to LOAD_BI_H
        br.com.solardiagram.domain.model.SystemPhase.TRI -> LOAD_TRI_W to LOAD_TRI_H
    }

    private fun microInputPairs(component: Component): Int {
        val dcInputs = component.ports.count { it.kind == PortKind.DC_POS && (it.spec?.terminalRole?.name == "DC_INPUT" || it.direction == br.com.solardiagram.domain.model.PortDirection.INPUT) }
        return if (dcInputs >= 4) 4 else 2
    }

    private fun drawSelectionHalo(
        drawScope: DrawScope,
        rect: Rect,
        scale: Float,
        pan: Point2,
        isSelected: Boolean,
        isDragging: Boolean
    ) {
        if (!isSelected && !isDragging) return
        with(drawScope) {
            drawRect(
                color = if (isDragging) DRAGGING_STROKE.copy(alpha = 0.22f) else SELECTED_STROKE.copy(alpha = 0.20f),
                topLeft = Offset(rect.left * scale + pan.x - 5f, rect.top * scale + pan.y - 5f),
                size = Size(rect.width * scale + 10f, rect.height * scale + 10f),
                style = Stroke(width = if (isDragging) 4.0f else 3.2f)
            )
        }
    }

    private fun drawNamedBox(
        drawScope: DrawScope,
        rect: Rect,
        strokeColor: Color,
        fillColor: Color,
        text: String,
        scale: Float,
        pan: Point2,
        textSize: Float
    ) {
        with(drawScope) {
            drawRect(
                color = fillColor,
                topLeft = Offset(rect.left * scale + pan.x, rect.top * scale + pan.y),
                size = Size(rect.width * scale, rect.height * scale),
                style = Fill
            )
            drawRect(
                color = strokeColor,
                topLeft = Offset(rect.left * scale + pan.x, rect.top * scale + pan.y),
                size = Size(rect.width * scale, rect.height * scale),
                style = Stroke(width = 1.5f)
            )
            drawTextCentered(
                this,
                text,
                rect.center.x * scale + pan.x,
                (rect.center.y + 1f) * scale + pan.y,
                textSize * scale
            )
        }
    }

    private fun drawSmallLabeledBox(
        drawScope: DrawScope,
        rect: Rect,
        strokeColor: Color,
        text: String,
        scale: Float,
        pan: Point2
    ) {
        with(drawScope) {
            drawRect(
                color = Color(0xFFFDFDFD),
                topLeft = Offset(rect.left * scale + pan.x, rect.top * scale + pan.y),
                size = Size(rect.width * scale, rect.height * scale),
                style = Fill
            )
            drawRect(
                color = strokeColor,
                topLeft = Offset(rect.left * scale + pan.x, rect.top * scale + pan.y),
                size = Size(rect.width * scale, rect.height * scale),
                style = Stroke(width = 1.2f)
            )
            drawTextCentered(
                this,
                text,
                rect.center.x * scale + pan.x,
                (rect.center.y + 1f) * scale + pan.y,
                6.8f * scale
            )
        }
    }

    private fun drawPolarityMarks(
        drawScope: DrawScope,
        rect: Rect,
        scale: Float,
        pan: Point2
    ) {
        drawTextLeftAligned(
            drawScope,
            "+",
            (rect.left + 2f) * scale + pan.x,
            (rect.top + 7f) * scale + pan.y,
            6.3f * scale
        )
        drawTextLeftAligned(
            drawScope,
            "-",
            (rect.left + 2f) * scale + pan.x,
            (rect.bottom - 3f) * scale + pan.y,
            6.3f * scale
        )
    }

    private fun drawAcTripletLabels(
        drawScope: DrawScope,
        rect: Rect,
        scale: Float,
        pan: Point2
    ) {
        drawTextCentered(drawScope, "L1", (rect.left + 12f) * scale + pan.x, (rect.bottom - 3f) * scale + pan.y, 6.0f * scale)
        drawTextCentered(drawScope, "N", rect.center.x * scale + pan.x, (rect.bottom - 3f) * scale + pan.y, 6.0f * scale)
        drawTextCentered(drawScope, "L2", (rect.right - 12f) * scale + pan.x, (rect.bottom - 3f) * scale + pan.y, 6.0f * scale)
    }

    private fun drawPortCircle(
        drawScope: DrawScope,
        component: Component,
        port: Port,
        index: Int,
        scale: Float,
        pan: Point2
    ) {
        val p = portBaseWorldPosition(component, port)
        drawScope.drawCircle(
            color = colorForPort(port),
            radius = PORT_R * scale,
            center = Offset(p.x * scale + pan.x, p.y * scale + pan.y)
        )
    }

    private fun drawPortConnectorLabel(
        drawScope: DrawScope,
        component: Component,
        port: Port,
        index: Int,
        scale: Float,
        pan: Point2
    ) {
        val label = connectorLabel(component, port) ?: return
        val p = portBaseWorldPosition(component, port)
        val body = componentBaseBounds(component)
        val isLoad = component.type == ComponentType.LOAD
        val isBreaker = component.type == ComponentType.BREAKER
        val x = when (port.side) {
            br.com.solardiagram.domain.model.PortSide.LEFT -> {
                val gap = if (isLoad) 11f else if (isBreaker) 8f else 9f
                (p.x - gap) * scale + pan.x
            }
            br.com.solardiagram.domain.model.PortSide.RIGHT -> {
                val gap = if (isBreaker) 8f else 9f
                (p.x + gap) * scale + pan.x
            }
            else -> p.x * scale + pan.x
        }
        val y = when (port.side) {
            br.com.solardiagram.domain.model.PortSide.BOTTOM -> (body.bottom + if (isLoad) 13f else 12f) * scale + pan.y
            br.com.solardiagram.domain.model.PortSide.TOP -> (body.top - 8f) * scale + pan.y
            else -> p.y * scale + pan.y + 2f * scale
        }
        val textSize = when {
            isLoad -> 6.6f * scale
            isBreaker -> 6.0f * scale
            else -> 6.2f * scale
        }

        when (port.side) {
            br.com.solardiagram.domain.model.PortSide.LEFT -> drawTextRightAligned(drawScope, label, x, y, textSize)
            br.com.solardiagram.domain.model.PortSide.RIGHT -> drawTextLeftAligned(drawScope, label, x, y, textSize)
            else -> drawTextCentered(drawScope, label, x, y, textSize)
        }
    }

    private fun connectorLabel(component: Component, port: Port): String? {
        if (component.type != ComponentType.LOAD && component.type != ComponentType.BREAKER) return null
        return when (port.spec?.phase) {
            ElectricalPhase.L1 -> "L1"
            ElectricalPhase.L2 -> "L2"
            ElectricalPhase.L3 -> "L3"
            ElectricalPhase.N -> "N"
            ElectricalPhase.PE -> "PE"
            else -> when (port.name.trim().uppercase()) {
                "L1", "L2", "L3", "N", "PE" -> port.name.trim().uppercase()
                else -> null
            }
        }
    }

    private fun colorForPort(port: Port): Color {
        return when (port.kind) {
            PortKind.DC_POS -> Color(0xFFE53935)
            PortKind.DC_NEG -> Color(0xFF1E88E5)
            PortKind.AC_L -> Color(0xFFE53935)
            PortKind.AC_N -> Color(0xFF2EAD4B)
            PortKind.AC_PE, PortKind.PE -> Color(0xFF111111)
        }
    }

    private fun barCenterLabel(component: Component): String {
        val explicit = component.name.trim().uppercase()
        if (explicit.isNotBlank()) {
            return when {
                explicit.startsWith("BAR ") -> explicit.removePrefix("BAR ")
                explicit == "BARN" -> "N"
                explicit == "BARPE" -> "PE"
                explicit.startsWith("BAR") -> explicit.removePrefix("BAR")
                else -> explicit
            }.trim().ifBlank { "BAR" }
        }

        return when (component.type) {
            ComponentType.BARL -> {
                val specs = component.specs as? br.com.solardiagram.domain.model.ElectricalSpecs.AcBusSpecs
                when (specs?.phases) {
                    SystemPhase.BI -> "L2"
                    SystemPhase.TRI -> "L3"
                    else -> "L1"
                }
            }
            ComponentType.BARN -> "N"
            ComponentType.BARPE -> "PE"
            else -> "BAR"
        }
    }

    private fun componentBaseBounds(component: Component): Rect =
        EditorGeometry.componentBaseBounds(component)

    private fun componentRotationDegrees(component: Component): Float = component.transform.normalizedQuarterTurns * 90f

    private data class StyleColors(
        val stroke: Color,
        val fill: Color
    )

    private fun styleColors(
        isSelected: Boolean,
        isDragging: Boolean,
        isError: Boolean,
        isWarning: Boolean
    ): StyleColors {
        val stroke = when {
            isDragging -> DRAGGING_STROKE
            isError -> Color(0xFFD32F2F)
            isWarning -> Color(0xFFFF9800)
            isSelected -> SELECTED_STROKE
            else -> Color(0xFF263238)
        }

        val fill = when {
            isDragging -> DRAGGING_FILL
            isSelected -> SELECTED_FILL
            else -> Color(0xFFF8FAFB)
        }

        return StyleColors(stroke, fill)
    }

    private fun drawTextCentered(
        drawScope: DrawScope,
        text: String,
        x: Float,
        y: Float,
        textSize: Float
    ) {
        drawMultilineText(drawScope, text, x, y, textSize, Paint.Align.CENTER)
    }

    private fun drawTextLeftAligned(
        drawScope: DrawScope,
        text: String,
        x: Float,
        y: Float,
        textSize: Float
    ) {
        drawMultilineText(drawScope, text, x, y, textSize, Paint.Align.LEFT)
    }

    private fun drawTextRightAligned(
        drawScope: DrawScope,
        text: String,
        x: Float,
        y: Float,
        textSize: Float
    ) {
        drawMultilineText(drawScope, text, x, y, textSize, Paint.Align.RIGHT)
    }

    private fun drawMultilineText(
        drawScope: DrawScope,
        text: String,
        x: Float,
        y: Float,
        textSize: Float,
        align: Paint.Align
    ) {
        val size = textSize.coerceAtLeast(7f)
        val lines = text.split("\n")
        val paint = Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.BLACK
            this.textSize = size
            textAlign = align
        }
        val lineHeight = size * 1.05f
        val firstY = y - ((lines.size - 1) * lineHeight / 2f) + (lineHeight * 0.32f)
        lines.forEachIndexed { index, line ->
            drawScope.drawContext.canvas.nativeCanvas.drawText(line, x, firstY + index * lineHeight, paint)
        }
    }
}
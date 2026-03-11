package br.com.solardiagram.ui.screens.editor

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import br.com.solardiagram.domain.model.Component
import br.com.solardiagram.domain.model.ComponentType
import br.com.solardiagram.domain.model.ElectricalPhase
import br.com.solardiagram.domain.model.ElectricalSpecs
import br.com.solardiagram.domain.model.PhysicalTerminalRole
import br.com.solardiagram.domain.model.Point2
import br.com.solardiagram.domain.model.Port
import br.com.solardiagram.domain.model.PortDirection
import br.com.solardiagram.domain.model.PortKind
import br.com.solardiagram.domain.model.PortSide
import br.com.solardiagram.domain.model.SystemPhase
import br.com.solardiagram.domain.model.BreakerPole

object EditorGeometry {

    private const val PV_BODY_W = 86f
    private const val PV_BODY_H = 42f

    private const val MICRO2_BODY_W = 132f
    private const val MICRO2_BODY_H = 74f

    private const val MICRO4_BODY_W = 150f
    private const val MICRO4_BODY_H = 94f

    private const val STRING_BODY_W = 130f
    private const val STRING_BODY_H = 64f

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

    fun componentScreenRect(
        component: Component,
        scale: Float,
        pan: Point2
    ): Rect {
        val bounds = componentWorldBounds(component)
        return Rect(
            left = bounds.left * scale + pan.x,
            top = bounds.top * scale + pan.y,
            right = bounds.right * scale + pan.x,
            bottom = bounds.bottom * scale + pan.y
        )
    }

    fun portCanvasPosition(
        component: Component,
        portId: String,
        scale: Float,
        pan: Point2
    ): Offset? {
        val index = component.ports.indexOfFirst { it.id == portId }
        if (index < 0) return null

        val port = component.ports[index]
        val world = portWorldPosition(component, port, index)

        return Offset(
            x = world.x * scale + pan.x,
            y = world.y * scale + pan.y
        )
    }

    fun portWorldPosition(
        component: Component,
        port: Port,
        index: Int
    ): Point2 {
        val unrotated = portBaseWorldPosition(component, port)
        return rotatePointAroundComponent(component, unrotated)
    }

    fun portBaseWorldPosition(
        component: Component,
        port: Port
    ): Point2 {
        return when (component.type) {
            ComponentType.PV_MODULE -> pvPortWorldPosition(component, port)
            ComponentType.MICROINVERTER -> microPortWorldPosition(component, port)
            ComponentType.STRING_INVERTER -> stringPortWorldPosition(component, port)
            ComponentType.AC_BUS -> acBusPortWorldPosition(component, port)
            ComponentType.BARL,
            ComponentType.BARN,
            ComponentType.BARPE -> simpleBarPortWorldPosition(component, port)
            ComponentType.QDG -> qdgPortWorldPosition(component, port)
            ComponentType.BREAKER -> breakerPortWorldPosition(component, port)
            ComponentType.DPS -> dpsPortWorldPosition(component, port)
            ComponentType.GROUND_BAR -> groundPortWorldPosition(component, port)
            ComponentType.LOAD -> loadPortWorldPosition(component, port)
        }
    }

    fun componentWorldBounds(component: Component): Rect {
        val c = component.transform.position

        val base = when (component.type) {
            ComponentType.PV_MODULE -> {
                val body = Rect(
                    c.x - PV_BODY_W / 2f,
                    c.y - PV_BODY_H / 2f,
                    c.x + PV_BODY_W / 2f,
                    c.y + PV_BODY_H / 2f
                )
                Rect(
                    body.left - PORT_R,
                    body.top - PORT_R,
                    body.right + PORT_R,
                    body.bottom + BOX_GAP + PV_DC_BOX_H + PORT_R
                )
            }

            ComponentType.MICROINVERTER -> {
                val pairs = microInputPairs(component)
                val bodyW = if (pairs >= 4) MICRO4_BODY_W else MICRO2_BODY_W
                val bodyH = if (pairs >= 4) MICRO4_BODY_H else MICRO2_BODY_H
                val body = Rect(
                    c.x - bodyW / 2f,
                    c.y - bodyH / 2f,
                    c.x + bodyW / 2f,
                    c.y + bodyH / 2f
                )
                Rect(
                    body.left - BOX_GAP - DC_BOX_W - PORT_R,
                    body.top - PORT_R,
                    body.right + BOX_GAP + DC_BOX_W + PORT_R,
                    body.bottom + BOX_GAP + AC_BOX_H + PORT_R
                )
            }

            ComponentType.STRING_INVERTER -> {
                val body = Rect(
                    c.x - STRING_BODY_W / 2f,
                    c.y - STRING_BODY_H / 2f,
                    c.x + STRING_BODY_W / 2f,
                    c.y + STRING_BODY_H / 2f
                )
                Rect(
                    body.left - BOX_GAP - DC_BOX_W - PORT_R,
                    body.top - PORT_R,
                    body.right + BOX_GAP + AC_BOX_W + PORT_R,
                    body.bottom + BOX_GAP + PE_BOX_H + PORT_R
                )
            }

            ComponentType.AC_BUS -> acBusOverallRect(component)

            ComponentType.BARL,
            ComponentType.BARN,
            ComponentType.BARPE -> Rect(
                c.x - BAR_W / 2f - PORT_R,
                c.y - BAR_H / 2f - 10f - PORT_R,
                c.x + BAR_W / 2f + PORT_R,
                c.y + BAR_H / 2f + 10f + PORT_R
            )

            ComponentType.QDG -> Rect(
                c.x - QDG_W / 2f - PORT_R,
                c.y - QDG_H / 2f - PORT_R,
                c.x + QDG_W / 2f + PORT_R,
                c.y + QDG_H / 2f + PORT_R
            )

            ComponentType.BREAKER -> {
                val breakerW = breakerWidth(component)
                Rect(
                    c.x - breakerW / 2f - PORT_R,
                    c.y - BREAKER_H / 2f - PORT_R,
                    c.x + breakerW / 2f + PORT_R,
                    c.y + BREAKER_H / 2f + PORT_R
                )
            }

            ComponentType.DPS -> Rect(
                c.x - DPS_W / 2f - PORT_R,
                c.y - DPS_H / 2f - PORT_R,
                c.x + DPS_W / 2f + PORT_R,
                c.y + DPS_H / 2f + PORT_R
            )

            ComponentType.GROUND_BAR -> Rect(
                c.x - GROUND_W / 2f - PORT_R,
                c.y - GROUND_H / 2f - PORT_R,
                c.x + GROUND_W / 2f + PORT_R,
                c.y + GROUND_H / 2f + PORT_R
            )

            ComponentType.LOAD -> {
                val (loadW, loadH) = loadDimensions(component)
                Rect(
                    c.x - loadW / 2f - PORT_R,
                    c.y - loadH / 2f - PORT_R,
                    c.x + loadW / 2f + PORT_R,
                    c.y + loadH / 2f + PORT_R
                )
            }
        }

        return rotatedBoundingRect(component, base)
    }

    fun componentBaseBounds(component: Component): Rect {
        val c = component.transform.position
        return when (component.type) {
            ComponentType.PV_MODULE -> {
                val body = Rect(
                    c.x - PV_BODY_W / 2f,
                    c.y - PV_BODY_H / 2f,
                    c.x + PV_BODY_W / 2f,
                    c.y + PV_BODY_H / 2f
                )
                Rect(
                    body.left - PORT_R,
                    body.top - PORT_R,
                    body.right + PORT_R,
                    body.bottom + BOX_GAP + PV_DC_BOX_H + PORT_R
                )
            }

            ComponentType.MICROINVERTER -> {
                val pairs = microInputPairs(component)
                val bodyW = if (pairs >= 4) MICRO4_BODY_W else MICRO2_BODY_W
                val bodyH = if (pairs >= 4) MICRO4_BODY_H else MICRO2_BODY_H
                val body = Rect(
                    c.x - bodyW / 2f,
                    c.y - bodyH / 2f,
                    c.x + bodyW / 2f,
                    c.y + bodyH / 2f
                )
                Rect(
                    body.left - BOX_GAP - DC_BOX_W - PORT_R,
                    body.top - PORT_R,
                    body.right + BOX_GAP + DC_BOX_W + PORT_R,
                    body.bottom + BOX_GAP + AC_BOX_H + PORT_R
                )
            }

            ComponentType.STRING_INVERTER -> {
                val body = Rect(
                    c.x - STRING_BODY_W / 2f,
                    c.y - STRING_BODY_H / 2f,
                    c.x + STRING_BODY_W / 2f,
                    c.y + STRING_BODY_H / 2f
                )
                Rect(
                    body.left - BOX_GAP - DC_BOX_W - PORT_R,
                    body.top - PORT_R,
                    body.right + BOX_GAP + AC_BOX_W + PORT_R,
                    body.bottom + BOX_GAP + PE_BOX_H + PORT_R
                )
            }

            ComponentType.AC_BUS -> acBusOverallRect(component)

            ComponentType.BARL,
            ComponentType.BARN,
            ComponentType.BARPE -> Rect(
                c.x - BAR_W / 2f - PORT_R,
                c.y - BAR_H / 2f - 10f - PORT_R,
                c.x + BAR_W / 2f + PORT_R,
                c.y + BAR_H / 2f + 10f + PORT_R
            )

            ComponentType.QDG -> Rect(
                c.x - QDG_W / 2f - PORT_R,
                c.y - QDG_H / 2f - PORT_R,
                c.x + QDG_W / 2f + PORT_R,
                c.y + QDG_H / 2f + PORT_R
            )

            ComponentType.BREAKER -> {
                val breakerW = breakerWidth(component)
                Rect(
                    c.x - breakerW / 2f - PORT_R,
                    c.y - BREAKER_H / 2f - PORT_R,
                    c.x + breakerW / 2f + PORT_R,
                    c.y + BREAKER_H / 2f + PORT_R
                )
            }

            ComponentType.DPS -> Rect(
                c.x - DPS_W / 2f - PORT_R,
                c.y - DPS_H / 2f - PORT_R,
                c.x + DPS_W / 2f + PORT_R,
                c.y + DPS_H / 2f + PORT_R
            )

            ComponentType.GROUND_BAR -> Rect(
                c.x - GROUND_W / 2f - PORT_R,
                c.y - GROUND_H / 2f - PORT_R,
                c.x + GROUND_W / 2f + PORT_R,
                c.y + GROUND_H / 2f + PORT_R
            )

            ComponentType.LOAD -> {
                val (loadW, loadH) = loadDimensions(component)
                Rect(
                    c.x - loadW / 2f - PORT_R,
                    c.y - loadH / 2f - PORT_R,
                    c.x + loadW / 2f + PORT_R,
                    c.y + loadH / 2f + PORT_R
                )
            }
        }
    }

    private fun pvPortWorldPosition(component: Component, port: Port): Point2 {
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

        return when (port.spec?.id ?: port.name.uppercase()) {
            "pv_dc_pos", "DC+", "OUT +", "IN +" -> Point2(posRect.center.x, posRect.bottom + PORT_R)
            "pv_dc_neg", "DC-", "OUT -", "IN -" -> Point2(negRect.center.x, negRect.bottom + PORT_R)
            else -> Point2(c.x, c.y)
        }
    }

    private fun microPortWorldPosition(component: Component, port: Port): Point2 {
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

        fun leftBox(y: Float): Rect = Rect(body.left - BOX_GAP - DC_BOX_W, y, body.left - BOX_GAP, y + DC_BOX_H)
        fun rightBox(y: Float): Rect = Rect(body.right + BOX_GAP, y, body.right + BOX_GAP + DC_BOX_W, y + DC_BOX_H)

        val dc1 = leftBox(body.top + 8f)
        val dc2 = rightBox(body.top + 8f)
        val dc3 = leftBox(body.top + 38f)
        val dc4 = rightBox(body.top + 38f)

        val totalBottomW = PE_BOX_W + 4f + AC_BOX_W
        val startX = c.x - totalBottomW / 2f
        val peRect = Rect(startX, body.bottom + BOX_GAP, startX + PE_BOX_W, body.bottom + BOX_GAP + PE_BOX_H)
        val acRect = Rect(peRect.right + 4f, body.bottom + BOX_GAP, peRect.right + 4f + AC_BOX_W, body.bottom + BOX_GAP + AC_BOX_H)

        return when (port.spec?.id ?: port.name.uppercase()) {
            "dc_1_pos", "DC1+", "PV1+" -> Point2(dc1.right + PORT_R, dc1.top + 6f)
            "dc_1_neg", "DC1-", "PV1-" -> Point2(dc1.right + PORT_R, dc1.bottom - 6f)
            "dc_2_pos", "DC2+", "PV2+" -> Point2(dc2.left - PORT_R, dc2.top + 6f)
            "dc_2_neg", "DC2-", "PV2-" -> Point2(dc2.left - PORT_R, dc2.bottom - 6f)
            "dc_3_pos", "DC3+", "PV3+" -> Point2(dc3.right + PORT_R, dc3.top + 6f)
            "dc_3_neg", "DC3-", "PV3-" -> Point2(dc3.right + PORT_R, dc3.bottom - 6f)
            "dc_4_pos", "DC4+", "PV4+" -> Point2(dc4.left - PORT_R, dc4.top + 6f)
            "dc_4_neg", "DC4-", "PV4-" -> Point2(dc4.left - PORT_R, dc4.bottom - 6f)
            "pe", "PE" -> Point2(peRect.right + PORT_R, peRect.center.y)
            "ac_l1", "AC OUT L1", "L1" -> Point2(acRect.left + 12f, acRect.bottom + PORT_R)
            "ac_n", "AC OUT N", "N" -> Point2(acRect.center.x, acRect.bottom + PORT_R)
            "ac_l2", "AC OUT L2", "L2" -> Point2(acRect.right - 12f, acRect.bottom + PORT_R)
            "ac_l3", "L3" -> Point2(acRect.right - 2f, acRect.bottom + PORT_R)
            else -> Point2(c.x, c.y)
        }
    }

    private fun stringPortWorldPosition(component: Component, port: Port): Point2 {
        val c = component.transform.position
        val body = Rect(
            c.x - STRING_BODY_W / 2f,
            c.y - STRING_BODY_H / 2f,
            c.x + STRING_BODY_W / 2f,
            c.y + STRING_BODY_H / 2f
        )
        val dcRect = Rect(body.left - BOX_GAP - DC_BOX_W, c.y - DC_BOX_H / 2f, body.left - BOX_GAP, c.y + DC_BOX_H / 2f)
        val acRect = Rect(body.right + BOX_GAP, c.y - AC_BOX_H / 2f, body.right + BOX_GAP + AC_BOX_W, c.y + AC_BOX_H / 2f)
        val peRect = Rect(c.x - PE_BOX_W / 2f, body.bottom + BOX_GAP, c.x + PE_BOX_W / 2f, body.bottom + BOX_GAP + PE_BOX_H)

        return when (port.spec?.id ?: port.name.uppercase()) {
            "mppt_1_pos", "DC+", "MPPT1+" -> Point2(dcRect.right + PORT_R, dcRect.top + 6f)
            "mppt_1_neg", "DC-", "MPPT1-" -> Point2(dcRect.right + PORT_R, dcRect.top + 16f)
            "mppt_2_pos", "MPPT2+" -> Point2(dcRect.right + PORT_R, dcRect.bottom - 16f)
            "mppt_2_neg", "MPPT2-" -> Point2(dcRect.right + PORT_R, dcRect.bottom - 6f)
            "ac_l1", "L OUT", "L1" -> Point2(acRect.left - PORT_R, acRect.top + 6f)
            "ac_n", "N OUT", "N" -> Point2(acRect.left - PORT_R, acRect.bottom - 6f)
            "ac_l2", "L2" -> Point2(acRect.left - PORT_R, acRect.center.y)
            "pe", "PE" -> Point2(peRect.center.x, peRect.top - PORT_R)
            else -> Point2(c.x, c.y)
        }
    }

    private fun simpleBarPortWorldPosition(component: Component, port: Port): Point2 {
        val c = component.transform.position
        val body = Rect(c.x - BAR_W / 2f, c.y - BAR_H / 2f, c.x + BAR_W / 2f, c.y + BAR_H / 2f)
        val id = (port.spec?.id ?: port.name).lowercase()
        val isOut = id.contains("_out") || id == "out" || port.side == PortSide.BOTTOM
        return Point2(body.center.x, if (isOut) body.bottom + PORT_R else body.top - PORT_R)
    }

    private fun acBusPortWorldPosition(component: Component, port: Port): Point2 {
        val channel = acBusChannelLayout(component).firstOrNull {
            it.phase == (port.spec?.phase ?: ElectricalPhase.NONE)
        } ?: return component.transform.position

        val id = port.spec?.id ?: port.name.lowercase()
        val isOut = id.endsWith("_out") || port.name.uppercase().startsWith("OUT ") || port.side == PortSide.BOTTOM
        return Point2(channel.rect.center.x, if (isOut) channel.rect.bottom + PORT_R else channel.rect.top - PORT_R)
    }

    private data class BusChannelLayout(
        val phase: ElectricalPhase,
        val centerLabel: String,
        val rect: Rect
    )

    private fun acBusChannelLayout(component: Component): List<BusChannelLayout> {
        val c = component.transform.position
        val channels = mutableListOf<Pair<ElectricalPhase, String>>()
        when (busPhase(component)) {
            SystemPhase.MONO -> channels += ElectricalPhase.L1 to "BAR\nL1"
            SystemPhase.BI -> {
                channels += ElectricalPhase.L1 to "BAR\nL1"
                channels += ElectricalPhase.L2 to "BAR\nL2"
            }
            SystemPhase.TRI -> {
                channels += ElectricalPhase.L1 to "BAR\nL1"
                channels += ElectricalPhase.L2 to "BAR\nL2"
                channels += ElectricalPhase.L3 to "BAR\nL3"
            }
        }
        val specs = component.specs as? ElectricalSpecs.AcBusSpecs
        if (specs?.hasNeutral != false) channels += ElectricalPhase.N to "BAR\nN"
        if (specs?.hasGround == true) channels += ElectricalPhase.PE to "BAR\nPE"

        val totalW = channels.size * BUS_ITEM_W + (channels.size - 1).coerceAtLeast(0) * BUS_ITEM_GAP
        val startX = c.x - totalW / 2f

        return channels.mapIndexed { idx, (phase, label) ->
            val left = startX + idx * (BUS_ITEM_W + BUS_ITEM_GAP)
            BusChannelLayout(
                phase = phase,
                centerLabel = label,
                rect = Rect(left, c.y - BUS_ITEM_H / 2f, left + BUS_ITEM_W, c.y + BUS_ITEM_H / 2f)
            )
        }
    }

    private fun acBusOverallRect(component: Component): Rect {
        val channels = acBusChannelLayout(component)
        if (channels.isEmpty()) {
            return Rect(
                component.transform.position.x - 24f,
                component.transform.position.y - 24f,
                component.transform.position.x + 24f,
                component.transform.position.y + 24f
            )
        }
        return Rect(
            channels.first().rect.left - PORT_R,
            channels.first().rect.top - 18f,
            channels.last().rect.right + PORT_R,
            channels.first().rect.bottom + 18f
        )
    }

    private fun qdgPortWorldPosition(component: Component, port: Port): Point2 {
        val c = component.transform.position
        val body = Rect(c.x - QDG_W / 2f, c.y - QDG_H / 2f, c.x + QDG_W / 2f, c.y + QDG_H / 2f)

        return when (port.spec?.id ?: port.name.uppercase()) {
            "feed_l1", "L IN", "ALIM L1" -> Point2(body.left - PORT_R, body.top + 12f)
            "feed_n", "N IN", "ALIM N" -> Point2(body.left - PORT_R, body.bottom - 12f)
            "feed_l2", "ALIM L2" -> Point2(body.left - PORT_R, body.center.y)
            "dist_l1", "L OUT", "DIST L1" -> Point2(body.right + PORT_R, body.top + 12f)
            "dist_n", "N OUT", "DIST N" -> Point2(body.right + PORT_R, body.bottom - 12f)
            "dist_l2", "DIST L2" -> Point2(body.right + PORT_R, body.center.y)
            "pe", "PE" -> Point2(c.x, body.bottom + PORT_R)
            else -> Point2(c.x, c.y)
        }
    }

    private fun breakerPortWorldPosition(component: Component, port: Port): Point2 {
        val c = component.transform.position
        val breakerW = breakerWidth(component)
        val body = Rect(c.x - breakerW / 2f, c.y - BREAKER_H / 2f, c.x + breakerW / 2f, c.y + BREAKER_H / 2f)
        val poles = breakerPole(component)

        val yOffsets = when (poles) {
            BreakerPole.P1 -> listOf(body.center.y)
            BreakerPole.P2 -> listOf(body.top + 10.5f, body.bottom - 10.5f)
            BreakerPole.P3 -> listOf(body.top + 9f, body.center.y, body.bottom - 9f)
            BreakerPole.P4 -> listOf(body.top + 7.5f, body.top + 15.5f, body.bottom - 15.5f, body.bottom - 7.5f)
        }

        fun phaseIndex(phase: ElectricalPhase): Int = when (phase) {
            ElectricalPhase.L1 -> 0
            ElectricalPhase.L2 -> 1
            ElectricalPhase.L3 -> 2
            ElectricalPhase.N -> when (poles) {
                BreakerPole.P2 -> 1
                BreakerPole.P4 -> 3
                else -> 0
            }
            else -> 0
        }

        val phase = port.spec?.phase ?: ElectricalPhase.L1
        val y = yOffsets[phaseIndex(phase).coerceIn(0, yOffsets.lastIndex)]
        val isLoad = port.spec?.terminalRole == PhysicalTerminalRole.LOAD || (port.spec?.id ?: "").startsWith("load_")

        return Point2(if (isLoad) body.right + PORT_R else body.left - PORT_R, y)
    }

    private fun dpsPortWorldPosition(component: Component, port: Port): Point2 {
        val c = component.transform.position
        val body = Rect(c.x - DPS_W / 2f, c.y - DPS_H / 2f, c.x + DPS_W / 2f, c.y + DPS_H / 2f)

        return when (port.spec?.id ?: port.name.uppercase()) {
            "l1", "L" -> Point2(body.left - PORT_R, body.top + 9f)
            "n", "N" -> Point2(body.left - PORT_R, body.bottom - 9f)
            "pe", "PE" -> Point2(c.x, body.bottom + PORT_R)
            else -> Point2(c.x, c.y)
        }
    }

    private fun groundPortWorldPosition(component: Component, port: Port): Point2 {
        val c = component.transform.position
        val body = Rect(c.x - GROUND_W / 2f, c.y - GROUND_H / 2f, c.x + GROUND_W / 2f, c.y + GROUND_H / 2f)
        return Point2(c.x, body.top - PORT_R)
    }

    private fun loadPortWorldPosition(component: Component, port: Port): Point2 {
        val c = component.transform.position
        val (loadW, loadH) = loadDimensions(component)
        val body = Rect(c.x - loadW / 2f, c.y - loadH / 2f, c.x + loadW / 2f, c.y + loadH / 2f)

        val yMap = when (loadPhase(component)) {
            SystemPhase.MONO -> mapOf(
                ElectricalPhase.L1 to body.center.y - 9f,
                ElectricalPhase.N to body.center.y + 9f
            )
            SystemPhase.BI -> mapOf(
                ElectricalPhase.L1 to body.center.y - 18f,
                ElectricalPhase.N to body.center.y,
                ElectricalPhase.L2 to body.center.y + 18f
            )
            SystemPhase.TRI -> mapOf(
                ElectricalPhase.L1 to body.center.y - 26f,
                ElectricalPhase.L2 to body.center.y - 9f,
                ElectricalPhase.L3 to body.center.y + 9f,
                ElectricalPhase.N to body.center.y + 26f
            )
        }

        return when (val phase = port.spec?.phase) {
            ElectricalPhase.PE -> Point2(c.x, body.bottom + PORT_R)
            null -> when ((port.spec?.id ?: port.name).lowercase()) {
                "pe" -> Point2(c.x, body.bottom + PORT_R)
                else -> Point2(c.x, c.y)
            }
            else -> Point2(body.left - PORT_R, yMap[phase] ?: body.center.y)
        }
    }

    private fun breakerPole(component: Component): BreakerPole {
        val specs = component.specs as? ElectricalSpecs.BreakerSpecs
        return specs?.poles ?: BreakerPole.P2
    }

    private fun breakerWidth(component: Component): Float = when (breakerPole(component)) {
        BreakerPole.P1 -> BREAKER_1P_W
        BreakerPole.P2 -> BREAKER_2P_W
        BreakerPole.P3 -> BREAKER_3P_W
        BreakerPole.P4 -> BREAKER_3P_W + 18f
    }

    private fun loadPhase(component: Component): SystemPhase {
        val specs = component.specs as? ElectricalSpecs.LoadSpecs
        return specs?.phases ?: SystemPhase.MONO
    }

    private fun loadDimensions(component: Component): Pair<Float, Float> = when (loadPhase(component)) {
        SystemPhase.MONO -> LOAD_MONO_W to LOAD_MONO_H
        SystemPhase.BI -> LOAD_BI_W to LOAD_BI_H
        SystemPhase.TRI -> LOAD_TRI_W to LOAD_TRI_H
    }

    private fun microInputPairs(component: Component): Int {
        val dcInputs = component.ports.count {
            it.kind == PortKind.DC_POS &&
                    (it.spec?.terminalRole?.name == "DC_INPUT" || it.direction == PortDirection.INPUT)
        }
        return if (dcInputs >= 4) 4 else 2
    }

    private fun busPhase(component: Component): SystemPhase {
        val specs = component.specs as? ElectricalSpecs.AcBusSpecs
        return specs?.phases ?: when {
            component.ports.any { it.name.equals("L3", ignoreCase = true) } -> SystemPhase.TRI
            component.ports.any { it.name.equals("L2", ignoreCase = true) } -> SystemPhase.BI
            else -> SystemPhase.MONO
        }
    }

    private fun rotatePointAroundComponent(component: Component, point: Point2): Point2 {
        val center = component.transform.position
        val dx = point.x - center.x
        val dy = point.y - center.y
        return when (component.transform.normalizedQuarterTurns) {
            1 -> Point2(center.x - dy, center.y + dx)
            2 -> Point2(center.x - dx, center.y - dy)
            3 -> Point2(center.x + dy, center.y - dx)
            else -> point
        }
    }

    private fun rotatedBoundingRect(component: Component, rect: Rect): Rect {
        return when (component.transform.normalizedQuarterTurns) {
            1, 3 -> {
                val center = rect.center
                val halfWidth = rect.height / 2f
                val halfHeight = rect.width / 2f
                Rect(
                    center.x - halfWidth,
                    center.y - halfHeight,
                    center.x + halfWidth,
                    center.y + halfHeight
                )
            }
            else -> rect
        }
    }
}
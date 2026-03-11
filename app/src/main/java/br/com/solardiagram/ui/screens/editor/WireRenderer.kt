package br.com.solardiagram.ui.screens.editor

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import br.com.solardiagram.domain.model.Component
import br.com.solardiagram.domain.model.Connection
import br.com.solardiagram.domain.model.Point2
import br.com.solardiagram.domain.model.PortKind
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object WireRenderer {

    private const val SEGMENT_EPS = 0.5f
    private const val LANE_SPACING = 7f
    private const val JUMP_RADIUS = 8f
    private const val JUMP_HEIGHT = 7f

    private fun colorFor(kind: PortKind): Color = when (kind) {
        PortKind.AC_L, PortKind.DC_POS -> Color(0xFFD32F2F)
        PortKind.AC_N -> Color(0xFF2E7D32)
        PortKind.PE, PortKind.AC_PE -> Color(0xFF212121)
        PortKind.DC_NEG -> Color(0xFF1565C0)
    }

    private fun biasFor(kind: PortKind): Float = when (kind) {
        PortKind.DC_POS -> -8f
        PortKind.DC_NEG -> 8f
        PortKind.AC_L -> -6f
        PortKind.AC_N -> 6f
        PortKind.PE, PortKind.AC_PE -> 12f
    }

    fun drawWires(
        drawScope: DrawScope,
        connections: List<Connection>,
        components: List<Component>,
        scale: Float,
        pan: Point2,
        errorConnectionIds: Set<String>,
        warnConnectionIds: Set<String>,
        selectedConnectionId: String?
    ) {
        val compsById = components.associateBy { it.id }
        val routed = mutableListOf<WireRoute>()

        connections.forEachIndexed { index, conn ->
            val fromComp = compsById[conn.fromComponentId] ?: return@forEachIndexed
            val toComp = compsById[conn.toComponentId] ?: return@forEachIndexed
            val a = ComponentRenderer.portCanvasPosition(fromComp, conn.fromPortId, scale, pan) ?: return@forEachIndexed
            val b = ComponentRenderer.portCanvasPosition(toComp, conn.toPortId, scale, pan) ?: return@forEachIndexed
            val fromPort = fromComp.portById(conn.fromPortId) ?: return@forEachIndexed

            val isErr = conn.id in errorConnectionIds
            val isWarn = conn.id in warnConnectionIds
            val isSel = conn.id == selectedConnectionId
            val color = when {
                isErr -> Color(0xFFD32F2F)
                isWarn -> Color(0xFFF9A825)
                isSel -> Color(0xFF1E88E5)
                else -> colorFor(fromPort.kind)
            }
            val stroke = when {
                isErr || isWarn -> 4.6f
                isSel -> 4.2f
                else -> 3.2f
            }
            routed += WireRoute(
                connectionId = conn.id,
                color = color,
                stroke = stroke,
                original = OrthogonalRouter.route(a, b, biasFor(fromPort.kind)),
                order = index
            )
        }

        val adjusted = applyLaneSeparation(routed)

        with(drawScope) {
            val alreadyDrawnSegments = mutableListOf<DrawSegment>()
            adjusted.forEach { route ->
                drawAdjustedRoute(this, route, alreadyDrawnSegments)
            }
        }
    }

    fun drawPreview(drawScope: DrawScope, from: Offset, to: Offset, highlight: Boolean) {
        with(drawScope) {
            val pts = OrthogonalRouter.route(from, to, 0f)
            val path = Path().apply {
                moveTo(pts.first().x, pts.first().y)
                for (i in 1 until pts.size) lineTo(pts[i].x, pts[i].y)
            }
            drawPath(
                path = path,
                color = if (highlight) Color(0xFF43A047) else Color(0xFF78909C),
                style = Stroke(width = if (highlight) 4f else 3f)
            )
        }
    }

    private fun applyLaneSeparation(routes: List<WireRoute>): List<AdjustedWireRoute> {
        val baseSegments = routes.flatMap { route ->
            toSegments(route.original).mapIndexed { segIndex, seg ->
                BaseSegment(route.order, segIndex, route.connectionId, seg)
            }
        }

        val grouped = mutableSetOf<String>()
        val laneOffsets = mutableMapOf<Pair<Int, Int>, Float>()

        baseSegments.forEach { seed ->
            val key = "${seed.routeOrder}:${seed.segmentIndex}"
            if (!grouped.add(key)) return@forEach

            val cluster = mutableListOf(seed)
            var changed = true
            while (changed) {
                changed = false
                baseSegments.forEach { candidate ->
                    val ck = "${candidate.routeOrder}:${candidate.segmentIndex}"
                    if (grouped.contains(ck)) return@forEach
                    if (cluster.any { it.segment.canShareLaneGroupWith(candidate.segment) }) {
                        grouped.add(ck)
                        cluster += candidate
                        changed = true
                    }
                }
            }

            if (cluster.size <= 1) {
                laneOffsets[seed.routeOrder to seed.segmentIndex] = 0f
            } else {
                val ordered = cluster.sortedWith(
                    compareBy<BaseSegment>({ it.segment.axisStart() }, { it.routeOrder }, { it.segmentIndex })
                )
                val center = (ordered.size - 1) / 2f
                ordered.forEachIndexed { idx, item ->
                    laneOffsets[item.routeOrder to item.segmentIndex] = (idx - center) * LANE_SPACING
                }
            }
        }

        return routes.map { route ->
            val segs = toSegments(route.original)
            val shifted = segs.mapIndexed { segIndex, seg ->
                seg.shifted(laneOffsets[route.order to segIndex] ?: 0f)
            }
            AdjustedWireRoute(
                connectionId = route.connectionId,
                color = route.color,
                stroke = route.stroke,
                points = rebuildPoints(route.original, shifted),
                order = route.order
            )
        }
    }

    private fun rebuildPoints(original: List<Offset>, shifted: List<Segment>): List<Offset> {
        if (original.size <= 1 || shifted.isEmpty()) return original
        val out = mutableListOf(original.first())
        var current = original.first()
        shifted.forEachIndexed { idx, seg ->
            val target = when (seg.orientation) {
                Orientation.HORIZONTAL -> Offset(original[idx + 1].x, seg.fixed)
                Orientation.VERTICAL -> Offset(seg.fixed, original[idx + 1].y)
            }
            if (!current.approxOn(seg)) {
                current = when (seg.orientation) {
                    Orientation.HORIZONTAL -> Offset(current.x, seg.fixed)
                    Orientation.VERTICAL -> Offset(seg.fixed, current.y)
                }
                if (!current.approxEquals(out.last())) out += current
            }
            if (!target.approxEquals(out.last())) out += target
            current = target
        }
        if (!out.last().approxEquals(original.last())) out += original.last()
        return simplifyPoints(out)
    }

    private fun drawAdjustedRoute(
        drawScope: DrawScope,
        route: AdjustedWireRoute,
        alreadyDrawnSegments: MutableList<DrawSegment>
    ) {
        val segments = toSegments(route.points)
        segments.forEach { seg ->
            when (seg.orientation) {
                Orientation.VERTICAL -> {
                    drawScope.drawLine(
                        color = route.color,
                        start = Offset(seg.fixed, seg.variableStart),
                        end = Offset(seg.fixed, seg.variableEnd),
                        strokeWidth = route.stroke
                    )
                }
                Orientation.HORIZONTAL -> {
                    drawHorizontalWithJumps(drawScope, seg, route, alreadyDrawnSegments)
                }
            }
            alreadyDrawnSegments += DrawSegment(route.connectionId, seg)
        }
    }

    private fun drawHorizontalWithJumps(
        drawScope: DrawScope,
        seg: Segment,
        route: AdjustedWireRoute,
        alreadyDrawnSegments: List<DrawSegment>
    ) {
        val y = seg.fixed
        val x1 = seg.variableStart
        val x2 = seg.variableEnd
        val dir = if (x2 >= x1) 1f else -1f
        val minX = min(x1, x2)
        val maxX = max(x1, x2)

        val jumps = alreadyDrawnSegments
            .asSequence()
            .filter { it.connectionId != route.connectionId && it.segment.orientation == Orientation.VERTICAL }
            .mapNotNull { prev ->
                val v = prev.segment
                val ix = v.fixed
                val crosses = ix > minX + JUMP_RADIUS && ix < maxX - JUMP_RADIUS &&
                    y > min(v.variableStart, v.variableEnd) + JUMP_RADIUS &&
                    y < max(v.variableStart, v.variableEnd) - JUMP_RADIUS
                if (crosses) ix else null
            }
            .distinct()
            .sorted()
            .toList()

        if (jumps.isEmpty()) {
            drawScope.drawLine(
                color = route.color,
                start = Offset(x1, y),
                end = Offset(x2, y),
                strokeWidth = route.stroke
            )
            return
        }

        val path = Path().apply {
            moveTo(x1, y)
            jumps.forEach { ix ->
                val before = ix - (JUMP_RADIUS * dir)
                val after = ix + (JUMP_RADIUS * dir)
                lineTo(before, y)
                quadraticBezierTo(ix, y - JUMP_HEIGHT, after, y)
            }
            lineTo(x2, y)
        }
        drawScope.drawPath(path, color = route.color, style = Stroke(width = route.stroke))
    }

    private fun simplifyPoints(points: List<Offset>): List<Offset> {
        if (points.size <= 2) return points
        val out = mutableListOf(points.first())
        for (i in 1 until points.size - 1) {
            val p0 = out.last()
            val p1 = points[i]
            val p2 = points[i + 1]
            val colinear = (approxEq(p0.x, p1.x) && approxEq(p1.x, p2.x)) ||
                (approxEq(p0.y, p1.y) && approxEq(p1.y, p2.y))
            if (!colinear && !p1.approxEquals(p0)) out += p1
        }
        if (!points.last().approxEquals(out.last())) out += points.last()
        return out
    }

    private fun toSegments(points: List<Offset>): List<Segment> {
        if (points.size < 2) return emptyList()
        val out = mutableListOf<Segment>()
        for (i in 0 until points.size - 1) {
            val a = points[i]
            val b = points[i + 1]
            if (approxEq(a.y, b.y) && !approxEq(a.x, b.x)) {
                out += Segment(Orientation.HORIZONTAL, a.y, a.x, b.x)
            } else if (approxEq(a.x, b.x) && !approxEq(a.y, b.y)) {
                out += Segment(Orientation.VERTICAL, a.x, a.y, b.y)
            }
        }
        return out
    }

    private fun approxEq(a: Float, b: Float): Boolean = abs(a - b) <= SEGMENT_EPS

    private data class WireRoute(
        val connectionId: String,
        val color: Color,
        val stroke: Float,
        val original: List<Offset>,
        val order: Int
    )

    private data class AdjustedWireRoute(
        val connectionId: String,
        val color: Color,
        val stroke: Float,
        val points: List<Offset>,
        val order: Int
    )

    private data class BaseSegment(
        val routeOrder: Int,
        val segmentIndex: Int,
        val connectionId: String,
        val segment: Segment
    )

    private data class DrawSegment(
        val connectionId: String,
        val segment: Segment
    )

    private enum class Orientation { HORIZONTAL, VERTICAL }

    private data class Segment(
        val orientation: Orientation,
        val fixed: Float,
        val variableStart: Float,
        val variableEnd: Float
    ) {
        fun axisStart(): Float = min(variableStart, variableEnd)
        fun axisEnd(): Float = max(variableStart, variableEnd)

        fun shifted(delta: Float): Segment = when (orientation) {
            Orientation.HORIZONTAL -> copy(fixed = fixed + delta)
            Orientation.VERTICAL -> copy(fixed = fixed + delta)
        }

        fun canShareLaneGroupWith(other: Segment): Boolean {
            if (orientation != other.orientation) return false
            if (!approxEq(fixed, other.fixed)) return false
            return overlapAmount(other) > 10f
        }

        fun overlapAmount(other: Segment): Float {
            val start = max(axisStart(), other.axisStart())
            val end = min(axisEnd(), other.axisEnd())
            return end - start
        }
    }

    private fun Offset.approxEquals(other: Offset): Boolean = approxEq(x, other.x) && approxEq(y, other.y)

    private fun Offset.approxOn(seg: Segment): Boolean = when (seg.orientation) {
        Orientation.HORIZONTAL -> approxEq(y, seg.fixed)
        Orientation.VERTICAL -> approxEq(x, seg.fixed)
    }
}

private object OrthogonalRouter {
    fun route(a: Offset, b: Offset, bias: Float): List<Offset> {
        val preferX = abs(a.x - b.x) >= abs(a.y - b.y)
        val path = if (preferX) {
            val midX = ((a.x + b.x) / 2f) + bias
            listOf(a, Offset(midX, a.y), Offset(midX, b.y), b)
        } else {
            val midY = ((a.y + b.y) / 2f) + bias
            listOf(a, Offset(a.x, midY), Offset(b.x, midY), b)
        }
        return simplify(path)
    }

    private fun simplify(points: List<Offset>): List<Offset> {
        if (points.size <= 2) return points
        val out = mutableListOf(points.first())
        for (i in 1 until points.size - 1) {
            val p0 = out.last()
            val p1 = points[i]
            val p2 = points[i + 1]
            val colinear = (p0.x == p1.x && p1.x == p2.x) || (p0.y == p1.y && p1.y == p2.y)
            if (!colinear) out.add(p1)
        }
        out.add(points.last())
        return out
    }
}

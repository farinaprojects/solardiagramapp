package br.com.solardiagram.ui.screens.editor

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import br.com.solardiagram.domain.model.Component
import kotlin.math.abs

object AutoWireRouter {
    private const val CLEARANCE = 18f

    fun route(
        start: Offset,
        end: Offset,
        components: List<Component>,
        scale: Float = 1f,
        pan: br.com.solardiagram.domain.model.Point2 = br.com.solardiagram.domain.model.Point2(0f, 0f),
        ignoreComponentIds: Set<String> = emptySet()
    ): List<Offset> {
        val obstacles = components
            .filterNot { it.id in ignoreComponentIds }
            .map { ComponentRenderer.componentScreenRect(it, scale, pan) }

        val candidates = mutableListOf<List<Offset>>()

        fun addCandidate(points: List<Offset>) {
            val cleaned = simplify(points)
            if (cleaned.size >= 2) candidates += cleaned
        }

        val midX = (start.x + end.x) / 2f
        val midY = (start.y + end.y) / 2f
        addCandidate(listOf(start, Offset(midX, start.y), Offset(midX, end.y), end))
        addCandidate(listOf(start, Offset(start.x, midY), Offset(end.x, midY), end))

        val xs = buildList {
            add(start.x)
            add(end.x)
            obstacles.forEach {
                add(it.left - CLEARANCE)
                add(it.right + CLEARANCE)
            }
        }.distinct()

        val ys = buildList {
            add(start.y)
            add(end.y)
            obstacles.forEach {
                add(it.top - CLEARANCE)
                add(it.bottom + CLEARANCE)
            }
        }.distinct()

        xs.forEach { x -> addCandidate(listOf(start, Offset(x, start.y), Offset(x, end.y), end)) }
        ys.forEach { y -> addCandidate(listOf(start, Offset(start.x, y), Offset(end.x, y), end)) }
        xs.forEach { x -> ys.forEach { y -> addCandidate(listOf(start, Offset(x, start.y), Offset(x, y), Offset(end.x, y), end)) } }

        return candidates.minByOrNull { score(it, obstacles) } ?: simplify(listOf(start, end))
    }

    fun simplify(points: List<Offset>): List<Offset> {
        if (points.size <= 2) return points
        val out = mutableListOf(points.first())
        for (i in 1 until points.lastIndex) {
            val a = out.last()
            val b = points[i]
            val c = points[i + 1]
            val colinearX = abs(a.x - b.x) < 0.5f && abs(b.x - c.x) < 0.5f
            val colinearY = abs(a.y - b.y) < 0.5f && abs(b.y - c.y) < 0.5f
            if (!colinearX && !colinearY) out += b
        }
        out += points.last()
        return out.distinct()
    }

    private fun score(points: List<Offset>, obstacles: List<Rect>): Float {
        var len = 0f
        var bends = 0f
        var collisions = 0f
        for (i in 0 until points.lastIndex) {
            val a = points[i]
            val b = points[i + 1]
            len += abs(a.x - b.x) + abs(a.y - b.y)
            collisions += segmentPenalty(a, b, obstacles)
        }
        bends = (points.size - 2) * 80f
        return collisions * 100000f + bends + len
    }

    private fun segmentPenalty(a: Offset, b: Offset, obstacles: List<Rect>): Float {
        var penalty = 0f
        obstacles.forEach { r ->
            if (segmentIntersectsRect(a, b, r)) penalty += 1f
        }
        return penalty
    }

    private fun segmentIntersectsRect(a: Offset, b: Offset, r: Rect): Boolean {
        if (abs(a.x - b.x) < 0.5f) {
            val x = a.x
            val top = minOf(a.y, b.y)
            val bottom = maxOf(a.y, b.y)
            return x in r.left..r.right && bottom >= r.top && top <= r.bottom
        }
        if (abs(a.y - b.y) < 0.5f) {
            val y = a.y
            val left = minOf(a.x, b.x)
            val right = maxOf(a.x, b.x)
            return y in r.top..r.bottom && right >= r.left && left <= r.right
        }
        return false
    }
}

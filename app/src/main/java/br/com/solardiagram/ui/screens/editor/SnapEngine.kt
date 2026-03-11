package br.com.solardiagram.ui.screens.editor

import br.com.solardiagram.domain.model.Component
import br.com.solardiagram.domain.model.DiagramProject
import br.com.solardiagram.domain.model.Point2
import br.com.solardiagram.domain.model.PortKind
import br.com.solardiagram.domain.rules.ConnectionCompatibility
import kotlin.math.round
import kotlin.math.sqrt

object SnapEngine {
    private const val GRID_STEP = 32f

    fun snapToGrid(world: Point2): Point2 {
        val x = round(world.x / GRID_STEP) * GRID_STEP
        val y = round(world.y / GRID_STEP) * GRID_STEP
        return Point2(x, y)
    }

    data class PortSnap(
        val componentId: String,
        val portId: String,
        val portWorld: Point2,
        val distanceWorld: Float
    )

    /**
     * Procura porta mais próxima em um raio (threshold em PX, convertido para mundo via scale).
     */
    fun findNearestPort(
        components: List<Component>,
        world: Point2,
        scale: Float,
        thresholdPx: Float
    ): PortSnap? {
        val thresholdWorld = thresholdPx / scale
        var best: PortSnap? = null

        components.forEach { c ->
            c.ports.forEachIndexed { idx, p ->
                val pw = ComponentRenderer.portWorldPosition(c, p, idx)
                val dx = pw.x - world.x
                val dy = pw.y - world.y
                val dist = sqrt(dx * dx + dy * dy)
                if (dist <= thresholdWorld) {
                    val cand = PortSnap(c.id, p.id, pw, dist)
                    if (best == null || cand.distanceWorld < best!!.distanceWorld) best = cand
                }
            }
        }
        return best
    }

    /**
     * ✅ Função que o EditorScreen está chamando.
     *
     * - Retorna a porta mais próxima dentro de um threshold (em px), opcionalmente:
     *   - ignorando um componente (ex: o componente de origem)
     *   - filtrando por compatibilidade elétrica (se fromPortKind informado)
     *
     * Observação: mesmo se você não passar fromPortKind, o snap ainda funciona
     * (a validação final continua sendo feita no ViewModel).
     */
    fun findNearestCompatiblePort(
        project: DiagramProject,
        world: Point2,
        excludeComponentId: String? = null,
        fromPortKind: PortKind? = null,
        scale: Float = 1f,
        thresholdPx: Float = 28f
    ): PortSnap? {
        val thresholdWorld = thresholdPx / scale
        var best: PortSnap? = null

        project.components.forEach { c ->
            if (excludeComponentId != null && c.id == excludeComponentId) return@forEach

            c.ports.forEachIndexed { idx, p ->
                // Se foi informado um tipo de porta de origem, filtra compatibilidade
                if (fromPortKind != null && !ConnectionCompatibility.isCompatible(fromPortKind, p.kind)) return@forEachIndexed

                val pw = ComponentRenderer.portWorldPosition(c, p, idx)
                val dx = pw.x - world.x
                val dy = pw.y - world.y
                val dist = sqrt(dx * dx + dy * dy)

                if (dist <= thresholdWorld) {
                    val cand = PortSnap(c.id, p.id, pw, dist)
                    if (best == null || cand.distanceWorld < best!!.distanceWorld) best = cand
                }
            }
        }
        return best
    }
}
package br.com.solardiagram.domain.engine

import br.com.solardiagram.domain.model.Component
import br.com.solardiagram.domain.model.ComponentType
import br.com.solardiagram.domain.model.Connection
import br.com.solardiagram.domain.model.DiagramProject
import br.com.solardiagram.domain.model.ElectricalPhase
import br.com.solardiagram.domain.model.PhysicalTerminalRole
import br.com.solardiagram.domain.model.Point2
import br.com.solardiagram.domain.model.Port
import br.com.solardiagram.domain.model.PortKind
import br.com.solardiagram.util.Ids
import kotlin.math.hypot

class AutoBusConnectionEngine(
    private val maxDistanceWorld: Float = 260f
) {
    fun apply(project: DiagramProject): DiagramProject {
        val buses = project.components.filter { it.type in setOf(ComponentType.BARL, ComponentType.BARN, ComponentType.BARPE) }
        if (buses.isEmpty()) return project

        val existingKeys = project.connections.flatMap { conn ->
            listOf(
                edgeKey(conn.fromComponentId, conn.fromPortId, conn.toComponentId, conn.toPortId),
                edgeKey(conn.toComponentId, conn.toPortId, conn.fromComponentId, conn.fromPortId)
            )
        }.toMutableSet()

        val existingCounts = mutableMapOf<String, Int>()
        project.connections.forEach { conn ->
            existingCounts[portRef(conn.fromComponentId, conn.fromPortId)] = (existingCounts[portRef(conn.fromComponentId, conn.fromPortId)] ?: 0) + 1
            existingCounts[portRef(conn.toComponentId, conn.toPortId)] = (existingCounts[portRef(conn.toComponentId, conn.toPortId)] ?: 0) + 1
        }

        val newConnections = mutableListOf<Connection>()

        project.components
            .filterNot { it.type in setOf(ComponentType.BARL, ComponentType.BARN, ComponentType.BARPE) }
            .forEach { component ->
                component.ports.forEach { port ->
                    val spec = port.spec ?: return@forEach
                    val phase = spec.phase
                    if (phase == ElectricalPhase.NONE) return@forEach
                    if (spec.terminalRole == PhysicalTerminalRole.DC_INPUT) return@forEach
                    if (port.kind !in setOf(PortKind.AC_L, PortKind.AC_N, PortKind.PE, PortKind.AC_PE)) return@forEach

                    val fromRef = portRef(component.id, port.id)
                    val maxConnections = spec.maxConnections
                    if ((existingCounts[fromRef] ?: 0) >= maxConnections) return@forEach

                    val busCandidate = buses
                        .asSequence()
                        .mapNotNull { bus -> bestBusPort(bus, phase, port.kind)?.let { bus to it } }
                        .filter { (bus, busPort) ->
                            val busRef = portRef(bus.id, busPort.id)
                            val maxBusConnections = busPort.spec?.maxConnections ?: 1
                            (existingCounts[busRef] ?: 0) < maxBusConnections
                        }
                        .map { (bus, busPort) -> Triple(bus, busPort, distance(component.transform.position, bus.transform.position)) }
                        .filter { it.third <= maxDistanceWorld }
                        .minByOrNull { it.third }
                        ?: return@forEach

                    val bus = busCandidate.first
                    val busPort = busCandidate.second
                    val key = edgeKey(component.id, port.id, bus.id, busPort.id)
                    if (key in existingKeys) return@forEach

                    val connection = Connection(
                        id = Ids.newId(),
                        fromComponentId = component.id,
                        fromPortId = port.id,
                        toComponentId = bus.id,
                        toPortId = busPort.id
                    )
                    newConnections += connection
                    existingKeys += key
                    existingKeys += edgeKey(bus.id, busPort.id, component.id, port.id)
                    existingCounts[fromRef] = (existingCounts[fromRef] ?: 0) + 1
                    val busRef = portRef(bus.id, busPort.id)
                    existingCounts[busRef] = (existingCounts[busRef] ?: 0) + 1
                }
            }

        return if (newConnections.isEmpty()) project else project.copy(connections = project.connections + newConnections)
    }

    private fun bestBusPort(bus: Component, phase: ElectricalPhase, kind: PortKind): Port? {
        val matching = bus.ports.filter { port ->
            val spec = port.spec ?: return@filter false
            spec.terminalRole == PhysicalTerminalRole.BUSBAR &&
                spec.phase == phase &&
                when (kind) {
                    PortKind.AC_L -> port.kind == PortKind.AC_L
                    PortKind.AC_N -> port.kind == PortKind.AC_N
                    PortKind.PE, PortKind.AC_PE -> port.kind == PortKind.PE || port.kind == PortKind.AC_PE
                    else -> false
                }
        }
        return matching.firstOrNull { it.spec?.name == "OUT" } ?: matching.firstOrNull()
    }

    private fun edgeKey(aComp: String, aPort: String, bComp: String, bPort: String): String = "$aComp|$aPort|$bComp|$bPort"
    private fun portRef(componentId: String, portId: String): String = "$componentId|$portId"
    private fun distance(a: Point2, b: Point2): Float = hypot(a.x - b.x, a.y - b.y)
}

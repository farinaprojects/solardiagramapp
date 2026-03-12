package br.com.solardiagram.domain.model

object PortSpecCatalog {

    fun pvModulePorts(): List<PortTemplate> = listOf(
        PortTemplate(
            logicalId = "pv_dc_pos",
            displayName = "DC+",
            kind = PortKind.DC_POS,
            direction = PortDirection.BIDIRECTIONAL,
            side = PortSide.BOTTOM,
            slot = 0,
            spec = ComponentPortSpec(
                id = "pv_dc_pos",
                name = "DC+",
                type = PortElectricalType.DC,
                polarity = PortPolarity.POSITIVE,
                terminalRole = PhysicalTerminalRole.TERMINAL,
                maxConnections = 1,
                required = true,
                supportsSeriesLink = true,
                notes = "Terminal positivo real do módulo FV"
            )
        ),
        PortTemplate(
            logicalId = "pv_dc_neg",
            displayName = "DC-",
            kind = PortKind.DC_NEG,
            direction = PortDirection.BIDIRECTIONAL,
            side = PortSide.BOTTOM,
            slot = 1,
            spec = ComponentPortSpec(
                id = "pv_dc_neg",
                name = "DC-",
                type = PortElectricalType.DC,
                polarity = PortPolarity.NEGATIVE,
                terminalRole = PhysicalTerminalRole.TERMINAL,
                maxConnections = 1,
                required = true,
                supportsSeriesLink = true,
                notes = "Terminal negativo real do módulo FV"
            )
        )
    )

    fun microInverterPorts(
        dcInputPairs: Int,
        phases: SystemPhase = SystemPhase.BI
    ): List<PortTemplate> {
        val ports = mutableListOf<PortTemplate>()

        repeat(dcInputPairs) { idx ->
            val ch = idx + 1

            ports += PortTemplate(
                logicalId = "dc_${ch}_pos",
                displayName = "PV${ch}+",
                kind = PortKind.DC_POS,
                direction = PortDirection.INPUT,
                side = if (idx % 2 == 0) PortSide.LEFT else PortSide.RIGHT,
                slot = idx * 2,
                spec = ComponentPortSpec(
                    id = "dc_${ch}_pos",
                    name = "PV${ch}+",
                    type = PortElectricalType.DC,
                    polarity = PortPolarity.POSITIVE,
                    terminalRole = PhysicalTerminalRole.DC_INPUT,
                    maxConnections = 1,
                    required = true,
                    notes = "Entrada DC positiva do canal $ch"
                )
            )

            ports += PortTemplate(
                logicalId = "dc_${ch}_neg",
                displayName = "PV${ch}-",
                kind = PortKind.DC_NEG,
                direction = PortDirection.INPUT,
                side = if (idx % 2 == 0) PortSide.LEFT else PortSide.RIGHT,
                slot = idx * 2 + 1,
                spec = ComponentPortSpec(
                    id = "dc_${ch}_neg",
                    name = "PV${ch}-",
                    type = PortElectricalType.DC,
                    polarity = PortPolarity.NEGATIVE,
                    terminalRole = PhysicalTerminalRole.DC_INPUT,
                    maxConnections = 1,
                    required = true,
                    notes = "Entrada DC negativa do canal $ch"
                )
            )
        }

        val acPhases = when (phases) {
            SystemPhase.MONO -> listOf(ElectricalPhase.L1, ElectricalPhase.N)
            SystemPhase.BI -> listOf(ElectricalPhase.L1, ElectricalPhase.N, ElectricalPhase.L2)
            SystemPhase.TRI -> listOf(ElectricalPhase.L1, ElectricalPhase.L2, ElectricalPhase.L3, ElectricalPhase.N)
        }

        var acSlot = 0
        acPhases.forEach { phase ->
            ports += PortTemplate(
                logicalId = "ac_${phase.name.lowercase()}",
                displayName = phase.name,
                kind = if (phase == ElectricalPhase.N) PortKind.AC_N else PortKind.AC_L,
                direction = PortDirection.OUTPUT,
                side = PortSide.BOTTOM,
                slot = acSlot++,
                spec = ComponentPortSpec(
                    id = "ac_${phase.name.lowercase()}",
                    name = phase.name,
                    type = PortElectricalType.AC,
                    phase = phase,
                    terminalRole = PhysicalTerminalRole.AC_OUTPUT,
                    maxConnections = 1,
                    required = true
                )
            )
        }

        ports += PortTemplate(
            logicalId = "pe",
            displayName = "PE",
            kind = PortKind.PE,
            direction = PortDirection.BIDIRECTIONAL,
            side = PortSide.BOTTOM,
            slot = acSlot,
            spec = ComponentPortSpec(
                id = "pe",
                name = "PE",
                type = PortElectricalType.GROUND,
                phase = ElectricalPhase.PE,
                terminalRole = PhysicalTerminalRole.PROTECTIVE_EARTH,
                maxConnections = 1,
                required = false
            )
        )

        return ports
    }

    fun stringInverterPorts(
        mpptCount: Int,
        phases: SystemPhase = SystemPhase.BI
    ): List<PortTemplate> {
        val ports = mutableListOf<PortTemplate>()

        repeat(mpptCount) { idx ->
            val mppt = idx + 1

            ports += PortTemplate(
                logicalId = "mppt_${mppt}_pos",
                displayName = "MPPT${mppt}+",
                kind = PortKind.DC_POS,
                direction = PortDirection.INPUT,
                side = PortSide.LEFT,
                slot = idx * 2,
                spec = ComponentPortSpec(
                    id = "mppt_${mppt}_pos",
                    name = "MPPT${mppt}+",
                    type = PortElectricalType.DC,
                    polarity = PortPolarity.POSITIVE,
                    terminalRole = PhysicalTerminalRole.DC_INPUT,
                    maxConnections = 1,
                    required = true
                )
            )

            ports += PortTemplate(
                logicalId = "mppt_${mppt}_neg",
                displayName = "MPPT${mppt}-",
                kind = PortKind.DC_NEG,
                direction = PortDirection.INPUT,
                side = PortSide.LEFT,
                slot = idx * 2 + 1,
                spec = ComponentPortSpec(
                    id = "mppt_${mppt}_neg",
                    name = "MPPT${mppt}-",
                    type = PortElectricalType.DC,
                    polarity = PortPolarity.NEGATIVE,
                    terminalRole = PhysicalTerminalRole.DC_INPUT,
                    maxConnections = 1,
                    required = true
                )
            )
        }

        var slot = 0

        fun addAc(phase: ElectricalPhase) {
            ports += PortTemplate(
                logicalId = "ac_${phase.name.lowercase()}",
                displayName = phase.name,
                kind = if (phase == ElectricalPhase.N) PortKind.AC_N else PortKind.AC_L,
                direction = PortDirection.OUTPUT,
                side = PortSide.RIGHT,
                slot = slot++,
                spec = ComponentPortSpec(
                    id = "ac_${phase.name.lowercase()}",
                    name = phase.name,
                    type = PortElectricalType.AC,
                    phase = phase,
                    terminalRole = PhysicalTerminalRole.AC_OUTPUT,
                    maxConnections = 1,
                    required = true
                )
            )
        }

        when (phases) {
            SystemPhase.MONO -> {
                addAc(ElectricalPhase.L1)
                addAc(ElectricalPhase.N)
            }
            SystemPhase.BI -> {
                addAc(ElectricalPhase.L1)
                addAc(ElectricalPhase.N)
                addAc(ElectricalPhase.L2)
            }
            SystemPhase.TRI -> {
                addAc(ElectricalPhase.L1)
                addAc(ElectricalPhase.L2)
                addAc(ElectricalPhase.L3)
                addAc(ElectricalPhase.N)
            }
        }

        ports += PortTemplate(
            logicalId = "pe",
            displayName = "PE",
            kind = PortKind.PE,
            direction = PortDirection.BIDIRECTIONAL,
            side = PortSide.BOTTOM,
            slot = 0,
            spec = ComponentPortSpec(
                id = "pe",
                name = "PE",
                type = PortElectricalType.GROUND,
                phase = ElectricalPhase.PE,
                terminalRole = PhysicalTerminalRole.PROTECTIVE_EARTH,
                maxConnections = 1,
                required = false
            )
        )

        return ports
    }

    fun acBusPorts(
        phases: SystemPhase,
        hasNeutral: Boolean = true,
        hasGround: Boolean = true
    ): List<PortTemplate> {
        val ports = mutableListOf<PortTemplate>()

        val linePhases = when (phases) {
            SystemPhase.MONO -> listOf(ElectricalPhase.L1)
            SystemPhase.BI -> listOf(ElectricalPhase.L1, ElectricalPhase.L2)
            SystemPhase.TRI -> listOf(ElectricalPhase.L1, ElectricalPhase.L2, ElectricalPhase.L3)
        }

        fun addJunction(
            tag: String,
            displayName: String,
            kind: PortKind,
            phase: ElectricalPhase,
            slot: Int,
            notesPrefix: String
        ) {
            val type = if (kind == PortKind.PE) PortElectricalType.GROUND else PortElectricalType.AC
            ports += PortTemplate(
                logicalId = "${tag}_in",
                displayName = "IN $displayName",
                kind = kind,
                direction = PortDirection.BIDIRECTIONAL,
                side = PortSide.TOP,
                slot = slot,
                spec = ComponentPortSpec(
                    id = "${tag}_in",
                    name = "IN $displayName",
                    type = type,
                    phase = phase,
                    terminalRole = PhysicalTerminalRole.BUSBAR,
                    maxConnections = 8,
                    required = false,
                    notes = "$notesPrefix • entrada/junção"
                )
            )
            ports += PortTemplate(
                logicalId = "${tag}_out",
                displayName = "OUT $displayName",
                kind = kind,
                direction = PortDirection.BIDIRECTIONAL,
                side = PortSide.BOTTOM,
                slot = slot,
                spec = ComponentPortSpec(
                    id = "${tag}_out",
                    name = "OUT $displayName",
                    type = type,
                    phase = phase,
                    terminalRole = PhysicalTerminalRole.BUSBAR,
                    maxConnections = 8,
                    required = false,
                    notes = "$notesPrefix • saída consolidada"
                )
            )
        }

        linePhases.forEachIndexed { idx, phase ->
            addJunction(
                tag = phase.name.lowercase(),
                displayName = phase.name,
                kind = PortKind.AC_L,
                phase = phase,
                slot = idx,
                notesPrefix = "Junção da barra ${phase.name}"
            )
        }

        if (hasNeutral) {
            addJunction(
                tag = "n",
                displayName = "N",
                kind = PortKind.AC_N,
                phase = ElectricalPhase.N,
                slot = linePhases.size,
                notesPrefix = "Junção da barra N"
            )
        }

        if (hasGround) {
            addJunction(
                tag = "pe",
                displayName = "PE",
                kind = PortKind.PE,
                phase = ElectricalPhase.PE,
                slot = linePhases.size + if (hasNeutral) 1 else 0,
                notesPrefix = "Junção da barra PE"
            )
        }

        return ports
    }


    fun barLPorts(phase: ElectricalPhase = ElectricalPhase.L1): List<PortTemplate> = listOf(
        PortTemplate(
            logicalId = "line_in",
            displayName = "IN",
            kind = PortKind.AC_L,
            direction = PortDirection.BIDIRECTIONAL,
            side = PortSide.TOP,
            slot = 0,
            spec = ComponentPortSpec(
                id = "line_in",
                name = "IN",
                type = PortElectricalType.AC,
                phase = phase,
                terminalRole = PhysicalTerminalRole.BUSBAR,
                maxConnections = 8,
                required = false,
                notes = "Entrada/junção da fase ${phase.name}"
            )
        ),
        PortTemplate(
            logicalId = "line_out",
            displayName = "OUT",
            kind = PortKind.AC_L,
            direction = PortDirection.BIDIRECTIONAL,
            side = PortSide.BOTTOM,
            slot = 0,
            spec = ComponentPortSpec(
                id = "line_out",
                name = "OUT",
                type = PortElectricalType.AC,
                phase = phase,
                terminalRole = PhysicalTerminalRole.BUSBAR,
                maxConnections = 8,
                required = false,
                notes = "Saída consolidada da fase ${phase.name}"
            )
        )
    )

    fun barNPorts(): List<PortTemplate> = listOf(
        PortTemplate(
            logicalId = "neutral_in",
            displayName = "IN",
            kind = PortKind.AC_N,
            direction = PortDirection.BIDIRECTIONAL,
            side = PortSide.TOP,
            slot = 0,
            spec = ComponentPortSpec(
                id = "neutral_in",
                name = "IN",
                type = PortElectricalType.AC,
                phase = ElectricalPhase.N,
                terminalRole = PhysicalTerminalRole.BUSBAR,
                maxConnections = 8,
                required = false,
                notes = "Entrada/junção do neutro"
            )
        ),
        PortTemplate(
            logicalId = "neutral_out",
            displayName = "OUT",
            kind = PortKind.AC_N,
            direction = PortDirection.BIDIRECTIONAL,
            side = PortSide.BOTTOM,
            slot = 0,
            spec = ComponentPortSpec(
                id = "neutral_out",
                name = "OUT",
                type = PortElectricalType.AC,
                phase = ElectricalPhase.N,
                terminalRole = PhysicalTerminalRole.BUSBAR,
                maxConnections = 8,
                required = false,
                notes = "Saída consolidada do neutro"
            )
        )
    )

    fun barPePorts(): List<PortTemplate> = listOf(
        PortTemplate(
            logicalId = "pe_in",
            displayName = "IN",
            kind = PortKind.PE,
            direction = PortDirection.BIDIRECTIONAL,
            side = PortSide.TOP,
            slot = 0,
            spec = ComponentPortSpec(
                id = "pe_in",
                name = "IN",
                type = PortElectricalType.GROUND,
                phase = ElectricalPhase.PE,
                terminalRole = PhysicalTerminalRole.BUSBAR,
                maxConnections = 8,
                required = false,
                notes = "Entrada/junção do PE"
            )
        ),
        PortTemplate(
            logicalId = "pe_out",
            displayName = "OUT",
            kind = PortKind.PE,
            direction = PortDirection.BIDIRECTIONAL,
            side = PortSide.BOTTOM,
            slot = 0,
            spec = ComponentPortSpec(
                id = "pe_out",
                name = "OUT",
                type = PortElectricalType.GROUND,
                phase = ElectricalPhase.PE,
                terminalRole = PhysicalTerminalRole.BUSBAR,
                maxConnections = 8,
                required = false,
                notes = "Saída consolidada do PE"
            )
        )
    )

    fun breakerAcPorts(poles: BreakerPole): List<PortTemplate> {
        val phaseSequence = when (poles) {
            BreakerPole.P1 -> listOf(ElectricalPhase.L1)
            BreakerPole.P2 -> listOf(ElectricalPhase.L1, ElectricalPhase.L2)
            BreakerPole.P3 -> listOf(ElectricalPhase.L1, ElectricalPhase.L2, ElectricalPhase.L3)
            BreakerPole.P4 -> listOf(ElectricalPhase.L1, ElectricalPhase.L2, ElectricalPhase.L3, ElectricalPhase.N)
        }

        val ports = mutableListOf<PortTemplate>()

        phaseSequence.forEachIndexed { idx, phase ->
            val kind = if (phase == ElectricalPhase.N) PortKind.AC_N else PortKind.AC_L

            ports += PortTemplate(
                logicalId = "line_${phase.name.lowercase()}",
                displayName = "LINE ${phase.name}",
                kind = kind,
                direction = PortDirection.INPUT,
                side = PortSide.LEFT,
                slot = idx,
                spec = ComponentPortSpec(
                    id = "line_${phase.name.lowercase()}",
                    name = "LINE ${phase.name}",
                    type = PortElectricalType.AC,
                    phase = phase,
                    terminalRole = PhysicalTerminalRole.LINE,
                    maxConnections = 1,
                    required = true
                )
            )

            ports += PortTemplate(
                logicalId = "load_${phase.name.lowercase()}",
                displayName = "LOAD ${phase.name}",
                kind = kind,
                direction = PortDirection.OUTPUT,
                side = PortSide.RIGHT,
                slot = idx,
                spec = ComponentPortSpec(
                    id = "load_${phase.name.lowercase()}",
                    name = "LOAD ${phase.name}",
                    type = PortElectricalType.AC,
                    phase = phase,
                    terminalRole = PhysicalTerminalRole.LOAD,
                    maxConnections = 1,
                    required = true
                )
            )
        }

        return ports
    }


    fun gridSourcePorts(phases: SystemPhase): List<PortTemplate> {
        val ports = mutableListOf<PortTemplate>()

        val linePhases = when (phases) {
            SystemPhase.MONO -> listOf(ElectricalPhase.L1, ElectricalPhase.N)
            SystemPhase.BI -> listOf(ElectricalPhase.L1, ElectricalPhase.N, ElectricalPhase.L2)
            SystemPhase.TRI -> listOf(ElectricalPhase.L1, ElectricalPhase.L2, ElectricalPhase.L3, ElectricalPhase.N)
        }

        linePhases.forEachIndexed { idx, phase ->
            val kind = if (phase == ElectricalPhase.N) PortKind.AC_N else PortKind.AC_L

            ports += PortTemplate(
                logicalId = "grid_${phase.name.lowercase()}",
                displayName = "REDE ${phase.name}",
                kind = kind,
                direction = PortDirection.OUTPUT,
                side = PortSide.RIGHT,
                slot = idx,
                spec = ComponentPortSpec(
                    id = "grid_${phase.name.lowercase()}",
                    name = "REDE ${phase.name}",
                    type = PortElectricalType.AC,
                    phase = phase,
                    terminalRole = PhysicalTerminalRole.LINE,
                    maxConnections = 1,
                    required = true
                )
            )
        }

        ports += PortTemplate(
            logicalId = "pe",
            displayName = "PE",
            kind = PortKind.PE,
            direction = PortDirection.BIDIRECTIONAL,
            side = PortSide.BOTTOM,
            slot = 0,
            spec = ComponentPortSpec(
                id = "pe",
                name = "PE",
                type = PortElectricalType.GROUND,
                phase = ElectricalPhase.PE,
                terminalRole = PhysicalTerminalRole.PROTECTIVE_EARTH,
                maxConnections = 1,
                required = false
            )
        )

        return ports
    }

    fun qdgPorts(phases: SystemPhase): List<PortTemplate> {
        val ports = mutableListOf<PortTemplate>()

        val linePhases = when (phases) {
            SystemPhase.MONO -> listOf(ElectricalPhase.L1, ElectricalPhase.N)
            SystemPhase.BI -> listOf(ElectricalPhase.L1, ElectricalPhase.N, ElectricalPhase.L2)
            SystemPhase.TRI -> listOf(ElectricalPhase.L1, ElectricalPhase.L2, ElectricalPhase.L3, ElectricalPhase.N)
        }

        linePhases.forEachIndexed { idx, phase ->
            val kind = if (phase == ElectricalPhase.N) PortKind.AC_N else PortKind.AC_L

            ports += PortTemplate(
                logicalId = "feed_${phase.name.lowercase()}",
                displayName = "ALIM ${phase.name}",
                kind = kind,
                direction = PortDirection.INPUT,
                side = PortSide.LEFT,
                slot = idx,
                spec = ComponentPortSpec(
                    id = "feed_${phase.name.lowercase()}",
                    name = "ALIM ${phase.name}",
                    type = PortElectricalType.AC,
                    phase = phase,
                    terminalRole = PhysicalTerminalRole.LINE,
                    maxConnections = 1,
                    required = true
                )
            )

            ports += PortTemplate(
                logicalId = "dist_${phase.name.lowercase()}",
                displayName = "DIST ${phase.name}",
                kind = kind,
                direction = PortDirection.OUTPUT,
                side = PortSide.RIGHT,
                slot = idx,
                spec = ComponentPortSpec(
                    id = "dist_${phase.name.lowercase()}",
                    name = "DIST ${phase.name}",
                    type = PortElectricalType.AC,
                    phase = phase,
                    terminalRole = PhysicalTerminalRole.LOAD,
                    maxConnections = 1,
                    required = false
                )
            )
        }

        ports += PortTemplate(
            logicalId = "pe",
            displayName = "PE",
            kind = PortKind.PE,
            direction = PortDirection.BIDIRECTIONAL,
            side = PortSide.BOTTOM,
            slot = 0,
            spec = ComponentPortSpec(
                id = "pe",
                name = "PE",
                type = PortElectricalType.GROUND,
                phase = ElectricalPhase.PE,
                terminalRole = PhysicalTerminalRole.BUSBAR,
                maxConnections = 8,
                required = false
            )
        )

        return ports
    }

    fun dpsAcPorts(phases: SystemPhase = SystemPhase.MONO): List<PortTemplate> {
        val ports = mutableListOf<PortTemplate>()

        val linePhases = when (phases) {
            SystemPhase.MONO -> listOf(ElectricalPhase.L1)
            SystemPhase.BI -> listOf(ElectricalPhase.L1, ElectricalPhase.L2)
            SystemPhase.TRI -> listOf(ElectricalPhase.L1, ElectricalPhase.L2, ElectricalPhase.L3)
        }

        linePhases.forEachIndexed { idx, phase ->
            ports += PortTemplate(
                logicalId = phase.name.lowercase(),
                displayName = phase.name,
                kind = PortKind.AC_L,
                direction = PortDirection.BIDIRECTIONAL,
                side = PortSide.LEFT,
                slot = idx,
                spec = ComponentPortSpec(
                    id = phase.name.lowercase(),
                    name = phase.name,
                    type = PortElectricalType.AC,
                    phase = phase,
                    terminalRole = PhysicalTerminalRole.SURGE_REFERENCE,
                    maxConnections = 1,
                    required = false
                )
            )
        }

        ports += PortTemplate(
            logicalId = "n",
            displayName = "N",
            kind = PortKind.AC_N,
            direction = PortDirection.BIDIRECTIONAL,
            side = PortSide.LEFT,
            slot = linePhases.size,
            spec = ComponentPortSpec(
                id = "n",
                name = "N",
                type = PortElectricalType.AC,
                phase = ElectricalPhase.N,
                terminalRole = PhysicalTerminalRole.SURGE_REFERENCE,
                maxConnections = 1,
                required = false
            )
        )

        ports += PortTemplate(
            logicalId = "pe",
            displayName = "PE",
            kind = PortKind.PE,
            direction = PortDirection.BIDIRECTIONAL,
            side = PortSide.BOTTOM,
            slot = 0,
            spec = ComponentPortSpec(
                id = "pe",
                name = "PE",
                type = PortElectricalType.GROUND,
                phase = ElectricalPhase.PE,
                terminalRole = PhysicalTerminalRole.PROTECTIVE_EARTH,
                maxConnections = 1,
                required = true
            )
        )

        return ports
    }

    fun groundBarPorts(): List<PortTemplate> = listOf(
        PortTemplate(
            logicalId = "pe_bus",
            displayName = "PE BUS",
            kind = PortKind.PE,
            direction = PortDirection.BIDIRECTIONAL,
            side = PortSide.TOP,
            slot = 0,
            spec = ComponentPortSpec(
                id = "pe_bus",
                name = "PE BUS",
                type = PortElectricalType.GROUND,
                phase = ElectricalPhase.PE,
                terminalRole = PhysicalTerminalRole.BUSBAR,
                maxConnections = 16,
                required = false
            )
        )
    )

    fun loadPorts(phases: SystemPhase = SystemPhase.BI): List<PortTemplate> {
        val ports = mutableListOf<PortTemplate>()

        val seq = when (phases) {
            SystemPhase.MONO -> listOf(ElectricalPhase.L1, ElectricalPhase.N)
            SystemPhase.BI -> listOf(ElectricalPhase.L1, ElectricalPhase.N, ElectricalPhase.L2)
            SystemPhase.TRI -> listOf(ElectricalPhase.L1, ElectricalPhase.L2, ElectricalPhase.L3, ElectricalPhase.N)
        }

        seq.forEachIndexed { idx, phase ->
            ports += PortTemplate(
                logicalId = "load_${phase.name.lowercase()}",
                displayName = phase.name,
                kind = if (phase == ElectricalPhase.N) PortKind.AC_N else PortKind.AC_L,
                direction = PortDirection.INPUT,
                side = PortSide.LEFT,
                slot = idx,
                spec = ComponentPortSpec(
                    id = "load_${phase.name.lowercase()}",
                    name = phase.name,
                    type = PortElectricalType.AC,
                    phase = phase,
                    terminalRole = PhysicalTerminalRole.LINE,
                    maxConnections = 1,
                    required = true
                )
            )
        }

        ports += PortTemplate(
            logicalId = "pe",
            displayName = "PE",
            kind = PortKind.PE,
            direction = PortDirection.BIDIRECTIONAL,
            side = PortSide.BOTTOM,
            slot = 0,
            spec = ComponentPortSpec(
                id = "pe",
                name = "PE",
                type = PortElectricalType.GROUND,
                phase = ElectricalPhase.PE,
                terminalRole = PhysicalTerminalRole.PROTECTIVE_EARTH,
                maxConnections = 1,
                required = false
            )
        )

        return ports
    }
}

data class PortTemplate(
    val logicalId: String,
    val displayName: String,
    val kind: PortKind,
    val direction: PortDirection,
    val side: PortSide,
    val slot: Int,
    val spec: ComponentPortSpec
)
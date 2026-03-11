package br.com.solardiagram.domain.model

import br.com.solardiagram.util.Ids

object ComponentFactory {

    private fun port(template: PortTemplate) = Port(
        id = Ids.newId(),
        name = template.displayName,
        kind = template.kind,
        direction = template.direction,
        side = template.side,
        slot = template.slot,
        spec = template.spec
    )

    fun pvModule(name: String = "Módulo FV"): Component {
        return Component(
            id = Ids.newId(),
            type = ComponentType.PV_MODULE,
            name = name,
            ports = PortSpecCatalog.pvModulePorts().map(::port),
            specs = ElectricalSpecs.PvModuleSpecs(
                pMaxW = 550.0,
                vMpV = 41.0,
                iMpA = 13.4,
                vOcV = 49.5,
                iScA = 14.1
            )
        )
    }

    fun microInverter(name: String = "Microinversor", acVoltageV: Double = 220.0): Component =
        microInverter2(name = name, acVoltageV = acVoltageV)

    fun microInverter2(name: String = "Microinversor 2", acVoltageV: Double = 220.0): Component =
        microInverterWithInputs(name = name, acVoltageV = acVoltageV, dcInputPairs = 2)

    fun microInverter4(name: String = "Microinversor 4", acVoltageV: Double = 220.0): Component =
        microInverterWithInputs(name = name, acVoltageV = acVoltageV, dcInputPairs = 4)

    private fun microInverterWithInputs(
        name: String,
        acVoltageV: Double,
        dcInputPairs: Int
    ): Component {
        return Component(
            id = Ids.newId(),
            type = ComponentType.MICROINVERTER,
            name = name,
            ports = PortSpecCatalog.microInverterPorts(dcInputPairs = dcInputPairs, phases = SystemPhase.BI).map(::port),
            specs = ElectricalSpecs.MicroInverterSpecs(
                maxDcPowerW = if (dcInputPairs == 4) 1800.0 else 900.0,
                maxDcVoltageV = 60.0,
                maxDcCurrentA = 15.0,
                acNominalPowerW = if (dcInputPairs == 4) 1600.0 else 800.0,
                acVoltageV = acVoltageV,
                maxAcCurrentA = if (dcInputPairs == 4) 8.0 else 4.0,
                phases = SystemPhase.BI,
                dcInputPairs = dcInputPairs
            )
        )
    }

    fun breakerMono(ratedA: Double = 32.0): Component = breakerAc(ratedA = ratedA, poles = BreakerPole.P1, name = "Disjuntor Mono ${ratedA.toInt()}A")

    fun breakerBi(ratedA: Double = 32.0): Component = breakerAc(ratedA = ratedA, poles = BreakerPole.P2, name = "Disjuntor Bi ${ratedA.toInt()}A")

    fun breakerTri(ratedA: Double = 32.0): Component = breakerAc(ratedA = ratedA, poles = BreakerPole.P3, name = "Disjuntor Tri ${ratedA.toInt()}A")

    fun breakerAc(
        ratedA: Double = 32.0,
        poles: BreakerPole = BreakerPole.P2,
        name: String = "Disjuntor ${ratedA.toInt()}A"
    ): Component {
        return Component(
            id = Ids.newId(),
            type = ComponentType.BREAKER,
            name = name,
            ports = PortSpecCatalog.breakerAcPorts(poles).map(::port),
            specs = ElectricalSpecs.BreakerSpecs(
                ratedCurrentA = ratedA,
                curve = BreakerCurve.C,
                poles = poles,
                applicableTo = CurrentKind.AC
            )
        )
    }

    fun groundBar(): Component {
        return Component(
            id = Ids.newId(),
            type = ComponentType.GROUND_BAR,
            name = "Barramento Terra",
            ports = PortSpecCatalog.groundBarPorts().map(::port),
            specs = ElectricalSpecs.GroundingSpecs(isMainEarthPoint = true)
        )
    }

    fun stringInverter(name: String = "Inversor String", acVoltageV: Double = 220.0): Component {
        val phases = SystemPhase.BI
        val mpptCount = 2
        return Component(
            id = Ids.newId(),
            type = ComponentType.STRING_INVERTER,
            name = name,
            ports = PortSpecCatalog.stringInverterPorts(mpptCount = mpptCount, phases = phases).map(::port),
            specs = ElectricalSpecs.StringInverterSpecs(
                maxDcPowerW = 6000.0,
                maxDcVoltageV = 600.0,
                maxDcCurrentA = 16.0,
                acNominalPowerW = 5000.0,
                acVoltageV = acVoltageV,
                maxAcCurrentA = 25.0,
                mpptCount = mpptCount,
                phases = phases
            )
        )
    }

    fun acBus(
        name: String = "Barramento AC Bifásico",
        phases: SystemPhase = SystemPhase.BI,
        ratedCurrentA: Double = 100.0
    ): Component {
        return Component(
            id = Ids.newId(),
            type = ComponentType.AC_BUS,
            name = name,
            ports = PortSpecCatalog.acBusPorts(phases = phases, hasNeutral = true, hasGround = true).map(::port),
            specs = ElectricalSpecs.AcBusSpecs(
                maxBusCurrentA = ratedCurrentA,
                phases = phases,
                hasNeutral = true,
                hasGround = true
            )
        )
    }


    fun barL(name: String = "BAR L", phase: ElectricalPhase = ElectricalPhase.L1, ratedCurrentA: Double = 100.0): Component {
        return Component(
            id = Ids.newId(),
            type = ComponentType.BARL,
            name = name,
            ports = PortSpecCatalog.barLPorts(phase).map(::port),
            specs = ElectricalSpecs.AcBusSpecs(
                maxBusCurrentA = ratedCurrentA,
                phases = when (phase) {
                    ElectricalPhase.L1 -> SystemPhase.MONO
                    ElectricalPhase.L2 -> SystemPhase.BI
                    ElectricalPhase.L3 -> SystemPhase.TRI
                    else -> SystemPhase.MONO
                },
                hasNeutral = false,
                hasGround = false
            )
        )
    }

    fun barN(name: String = "BAR N", ratedCurrentA: Double = 100.0): Component {
        return Component(
            id = Ids.newId(),
            type = ComponentType.BARN,
            name = name,
            ports = PortSpecCatalog.barNPorts().map(::port),
            specs = ElectricalSpecs.AcBusSpecs(
                maxBusCurrentA = ratedCurrentA,
                phases = SystemPhase.MONO,
                hasNeutral = true,
                hasGround = false
            )
        )
    }

    fun barPe(name: String = "BAR PE", ratedCurrentA: Double = 100.0): Component {
        return Component(
            id = Ids.newId(),
            type = ComponentType.BARPE,
            name = name,
            ports = PortSpecCatalog.barPePorts().map(::port),
            specs = ElectricalSpecs.AcBusSpecs(
                maxBusCurrentA = ratedCurrentA,
                phases = SystemPhase.MONO,
                hasNeutral = false,
                hasGround = true
            )
        )
    }

    fun qdg(name: String = "QDG / Quadro"): Component {
        val phases = SystemPhase.BI
        return Component(
            id = Ids.newId(),
            type = ComponentType.QDG,
            name = name,
            ports = PortSpecCatalog.qdgPorts(phases).map(::port),
            specs = ElectricalSpecs.QdgSpecs(
                maxBusCurrentA = 100.0,
                phases = phases
            )
        )
    }

    fun dpsAc(name: String = "DPS AC", maxVoltageV: Double = 275.0): Component {
        return Component(
            id = Ids.newId(),
            type = ComponentType.DPS,
            name = name,
            ports = PortSpecCatalog.dpsAcPorts(SystemPhase.MONO).map(::port),
            specs = ElectricalSpecs.DpsSpecs(
                maxVoltageV = maxVoltageV,
                applicableTo = CurrentKind.AC
            )
        )
    }

    fun loadMono(name: String = "Carga Mono", powerW: Double = 1500.0, voltageV: Double = 127.0): Component =
        load(name = name, powerW = powerW, voltageV = voltageV, phases = SystemPhase.MONO)

    fun loadBi(name: String = "Carga Bi", powerW: Double = 3000.0, voltageV: Double = 220.0): Component =
        load(name = name, powerW = powerW, voltageV = voltageV, phases = SystemPhase.BI)

    fun loadTri(name: String = "Carga Tri", powerW: Double = 5000.0, voltageV: Double = 380.0): Component =
        load(name = name, powerW = powerW, voltageV = voltageV, phases = SystemPhase.TRI)

    fun load(
        name: String = "Carga Mono",
        powerW: Double = 1500.0,
        voltageV: Double = 127.0,
        phases: SystemPhase = SystemPhase.MONO
    ): Component {
        return Component(
            id = Ids.newId(),
            type = ComponentType.LOAD,
            name = name,
            ports = PortSpecCatalog.loadPorts(phases).map(::port),
            specs = ElectricalSpecs.LoadSpecs(
                label = name,
                powerW = powerW,
                voltageV = voltageV,
                phases = phases
            )
        )
    }
}

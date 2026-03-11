package br.com.solardiagram.data.local.dto

import br.com.solardiagram.domain.model.*
import br.com.solardiagram.util.Ids

object ProjectMapperV1 {

    fun toDomain(p: ProjectDtoV1): DiagramProject {
        return DiagramProject(
            id = p.id,
            name = p.name,
            location = p.location,
            createdAtEpochMs = p.createdAtEpochMs,
            updatedAtEpochMs = p.updatedAtEpochMs,
            components = p.components.map { c ->
                val type = ComponentType.valueOf(c.type)
                val ports = c.ports.map {
                    Port(
                        id = it.id,
                        name = it.name,
                        kind = PortKind.valueOf(it.kind),
                        direction = runCatching { PortDirection.valueOf(it.direction ?: "BIDIRECTIONAL") }.getOrDefault(PortDirection.BIDIRECTIONAL),
                        side = runCatching { PortSide.valueOf(it.side ?: "RIGHT") }.getOrDefault(PortSide.RIGHT),
                        slot = it.slot,
                        spec = portSpecFromDto(type, it)
                    )
                }
                val specs = toSpecs(type, c.specs)
                Component(
                    id = c.id,
                    type = type,
                    name = c.name,
                    ports = normalizePorts(type = type, name = c.name, ports = ports, specs = specs),
                    specs = specs,
                    transform = Transform2(position = Point2(c.x, c.y), rotationQuarterTurns = c.rotationQuarterTurns)
                )
            },
            connections = p.connections.map { dto ->
                Connection(
                    id = dto.id,
                    fromComponentId = dto.fromComponentId,
                    fromPortId = dto.fromPortId,
                    toComponentId = dto.toComponentId,
                    toPortId = dto.toPortId,
                    meta = ConnectionMeta(
                        lengthMeters = dto.lengthMeters,
                        overrideCableMm2 = dto.overrideCableMm2,
                        label = dto.label,
                        conductorMaterial = runCatching { ConductorMaterial.valueOf(dto.material ?: "CU") }.getOrDefault(ConductorMaterial.CU),
                        installationMethod = runCatching { InstallationMethod.valueOf(dto.install ?: "CONDUIT_EXPOSED") }.getOrDefault(InstallationMethod.CONDUIT_EXPOSED),
                        insulation = runCatching { InsulationClass.valueOf(dto.insulation ?: "V750") }.getOrDefault(InsulationClass.V750),
                        ambientTempC = dto.tempC ?: 30.0,
                        grouping = runCatching { CircuitGrouping.valueOf(dto.grouping ?: "G1") }.getOrDefault(CircuitGrouping.G1),
                        phases = runCatching { SystemPhase.valueOf(dto.phases ?: "MONO") }.getOrDefault(SystemPhase.MONO)
                    )
                )
            }
        )
    }

    fun fromDomain(project: DiagramProject): ProjectDtoV1 {
        return ProjectDtoV1(
            id = project.id,
            name = project.name,
            location = project.location,
            createdAtEpochMs = project.createdAtEpochMs,
            updatedAtEpochMs = project.updatedAtEpochMs,
            components = project.components.map { c ->
                ComponentDtoV1(
                    id = c.id,
                    type = c.type.name,
                    name = c.name,
                    ports = c.ports.map {
                        PortDtoV1(
                            id = it.id,
                            name = it.name,
                            kind = it.kind.name,
                            direction = it.direction.name,
                            side = it.side.name,
                            slot = it.slot,
                            specId = it.spec?.id,
                            electricalType = it.spec?.type?.name,
                            phase = it.spec?.phase?.name,
                            polarity = it.spec?.polarity?.name,
                            terminalRole = it.spec?.terminalRole?.name,
                            maxConnections = it.spec?.maxConnections,
                            required = it.spec?.required,
                            supportsSeries = it.spec?.supportsSeriesLink,
                            specNotes = it.spec?.notes
                        )
                    },
                    specs = fromSpecs(c.specs),
                    x = c.transform.position.x,
                    y = c.transform.position.y,
                    rotationQuarterTurns = c.transform.normalizedQuarterTurns
                )
            },
            connections = project.connections.map { c ->
                ConnectionDtoV1(
                    id = c.id,
                    fromComponentId = c.fromComponentId,
                    fromPortId = c.fromPortId,
                    toComponentId = c.toComponentId,
                    toPortId = c.toPortId,
                    lengthMeters = c.meta.lengthMeters,
                    overrideCableMm2 = c.meta.overrideCableMm2,
                    label = c.meta.label,
                    material = c.meta.conductorMaterial.name,
                    install = c.meta.installationMethod.name,
                    insulation = c.meta.insulation.name,
                    tempC = c.meta.ambientTempC,
                    grouping = c.meta.grouping.name,
                    phases = c.meta.phases.name
                )
            }
        )
    }


    private fun normalizePorts(
        type: ComponentType,
        name: String,
        ports: List<Port>,
        specs: ElectricalSpecs
    ): List<Port> {
        return when (type) {
            ComponentType.LOAD -> normalizeLoadPorts(name, ports, specs as? ElectricalSpecs.LoadSpecs)
            else -> ports
        }
    }

    private fun normalizeLoadPorts(
        name: String,
        ports: List<Port>,
        specs: ElectricalSpecs.LoadSpecs?
    ): List<Port> {
        val phases = specs?.phases ?: SystemPhase.MONO
        val templates = PortSpecCatalog.loadPorts(phases)

        fun samePhase(existing: Port, template: PortTemplate): Boolean {
            val existingPhase = existing.spec?.phase
            val templatePhase = template.spec.phase
            if (existingPhase != null && templatePhase != null && existingPhase == templatePhase) return true

            val existingName = existing.name.trim().uppercase()
            val expected = template.spec.name.trim().uppercase()
            return existingName == expected || existingName.endsWith(" ${expected}")
        }

        val consumed = mutableSetOf<String>()
        val normalized = templates.map { template ->
            val match = ports.firstOrNull { it.id !in consumed && samePhase(it, template) }
            if (match != null) {
                consumed += match.id
                match.copy(
                    name = template.displayName,
                    kind = template.kind,
                    direction = template.direction,
                    side = template.side,
                    slot = template.slot,
                    spec = template.spec.copy(id = match.spec?.id ?: template.spec.id)
                )
            } else {
                Port(
                    id = Ids.newId(),
                    name = template.displayName,
                    kind = template.kind,
                    direction = template.direction,
                    side = template.side,
                    slot = template.slot,
                    spec = template.spec
                )
            }
        }.toMutableList()

        ports.filter { it.id !in consumed && it.kind == PortKind.PE }.forEach { existingPe ->
            val peIdx = normalized.indexOfFirst { it.spec?.phase == ElectricalPhase.PE || it.name.equals("PE", ignoreCase = true) }
            if (peIdx >= 0) {
                val template = normalized[peIdx]
                normalized[peIdx] = existingPe.copy(
                    name = template.name,
                    kind = template.kind,
                    direction = template.direction,
                    side = template.side,
                    slot = template.slot,
                    spec = template.spec?.copy(id = existingPe.spec?.id ?: template.spec?.id.orEmpty())
                )
            }
        }

        return normalized
    }

    private fun toSpecs(type: ComponentType, s: SpecsDtoV1): ElectricalSpecs {
        fun d(key: String, default: String) = s.data[key] ?: default
        return when (type) {
            ComponentType.PV_MODULE -> ElectricalSpecs.PvModuleSpecs(
                pMaxW = d("pMaxW","550").toDouble(),
                vMpV = d("vMpV","41").toDouble(),
                iMpA = d("iMpA","13.4").toDouble(),
                vOcV = d("vOcV","49.5").toDouble(),
                iScA = d("iScA","14.1").toDouble()
            )
            ComponentType.MICROINVERTER -> ElectricalSpecs.MicroInverterSpecs(
                maxDcPowerW = d("maxDcPowerW","450").toDouble(),
                maxDcVoltageV = d("maxDcVoltageV","60").toDouble(),
                maxDcCurrentA = d("maxDcCurrentA","15").toDouble(),
                acNominalPowerW = d("acNominalPowerW","450").toDouble(),
                acVoltageV = d("acVoltageV","220").toDouble(),
                maxAcCurrentA = d("maxAcCurrentA","2.5").toDouble(),
                phases = SystemPhase.valueOf(d("phases","BI")),
                dcInputPairs = d("dcInputPairs", "2").toInt()
            )
            ComponentType.STRING_INVERTER -> ElectricalSpecs.StringInverterSpecs(
                maxDcPowerW = d("maxDcPowerW","5000").toDouble(),
                maxDcVoltageV = d("maxDcVoltageV","600").toDouble(),
                maxDcCurrentA = d("maxDcCurrentA","12").toDouble(),
                acNominalPowerW = d("acNominalPowerW","5000").toDouble(),
                acVoltageV = d("acVoltageV","220").toDouble(),
                maxAcCurrentA = d("maxAcCurrentA","25").toDouble(),
                mpptCount = d("mpptCount","2").toInt(),
                phases = SystemPhase.valueOf(d("phases","BI"))
            )
            ComponentType.AC_BUS,
            ComponentType.BARL,
            ComponentType.BARN,
            ComponentType.BARPE -> ElectricalSpecs.AcBusSpecs(
                maxBusCurrentA = d("maxBusCurrentA","100").toDouble(),
                phases = SystemPhase.valueOf(d("phases","BI")),
                hasNeutral = d("hasNeutral","true").toBoolean(),
                hasGround = d("hasGround","true").toBoolean()
            )
            ComponentType.QDG -> ElectricalSpecs.QdgSpecs(
                maxBusCurrentA = d("maxBusCurrentA","80").toDouble(),
                phases = SystemPhase.valueOf(d("phases","BI"))
            )
            ComponentType.BREAKER -> ElectricalSpecs.BreakerSpecs(
                ratedCurrentA = d("ratedCurrentA","32").toDouble(),
                curve = BreakerCurve.valueOf(d("curve","C")),
                poles = BreakerPole.valueOf(d("poles","P2")),
                applicableTo = CurrentKind.valueOf(d("applicableTo","AC"))
            )
            ComponentType.DPS -> ElectricalSpecs.DpsSpecs(
                maxVoltageV = d("maxVoltageV","275").toDouble(),
                applicableTo = CurrentKind.valueOf(d("applicableTo","AC"))
            )
            ComponentType.GROUND_BAR -> ElectricalSpecs.GroundingSpecs(
                isMainEarthPoint = d("isMainEarthPoint","true").toBoolean()
            )
            ComponentType.LOAD -> ElectricalSpecs.LoadSpecs(
                label = d("label","Carga"),
                powerW = d("powerW","1500").toDouble(),
                voltageV = d("voltageV","220").toDouble(),
                phases = SystemPhase.valueOf(d("phases","MONO"))
            )
        }
    }

    private fun fromSpecs(s: ElectricalSpecs): SpecsDtoV1 {
        return when (s) {
            is ElectricalSpecs.PvModuleSpecs -> SpecsDtoV1("PvModuleSpecs", mapOf(
                "pMaxW" to s.pMaxW.toString(),
                "vMpV" to s.vMpV.toString(),
                "iMpA" to s.iMpA.toString(),
                "vOcV" to s.vOcV.toString(),
                "iScA" to s.iScA.toString()
            ))
            is ElectricalSpecs.MicroInverterSpecs -> SpecsDtoV1("MicroInverterSpecs", mapOf(
                "maxDcPowerW" to s.maxDcPowerW.toString(),
                "maxDcVoltageV" to s.maxDcVoltageV.toString(),
                "maxDcCurrentA" to s.maxDcCurrentA.toString(),
                "acNominalPowerW" to s.acNominalPowerW.toString(),
                "acVoltageV" to s.acVoltageV.toString(),
                "maxAcCurrentA" to s.maxAcCurrentA.toString(),
                "phases" to s.phases.name,
                "dcInputPairs" to s.dcInputPairs.toString()
            ))
            is ElectricalSpecs.StringInverterSpecs -> SpecsDtoV1("StringInverterSpecs", mapOf(
                "maxDcPowerW" to s.maxDcPowerW.toString(),
                "maxDcVoltageV" to s.maxDcVoltageV.toString(),
                "maxDcCurrentA" to s.maxDcCurrentA.toString(),
                "acNominalPowerW" to s.acNominalPowerW.toString(),
                "acVoltageV" to s.acVoltageV.toString(),
                "maxAcCurrentA" to s.maxAcCurrentA.toString(),
                "mpptCount" to s.mpptCount.toString(),
                "phases" to s.phases.name
            ))
            is ElectricalSpecs.AcBusSpecs -> SpecsDtoV1("AcBusSpecs", mapOf(
                "maxBusCurrentA" to s.maxBusCurrentA.toString(),
                "phases" to s.phases.name,
                "hasNeutral" to s.hasNeutral.toString(),
                "hasGround" to s.hasGround.toString()
            ))
            is ElectricalSpecs.QdgSpecs -> SpecsDtoV1("QdgSpecs", mapOf(
                "maxBusCurrentA" to s.maxBusCurrentA.toString(),
                "phases" to s.phases.name
            ))
            is ElectricalSpecs.BreakerSpecs -> SpecsDtoV1("BreakerSpecs", mapOf(
                "ratedCurrentA" to s.ratedCurrentA.toString(),
                "curve" to s.curve.name,
                "poles" to s.poles.name,
                "applicableTo" to s.applicableTo.name
            ))
            is ElectricalSpecs.DpsSpecs -> SpecsDtoV1("DpsSpecs", mapOf(
                "maxVoltageV" to s.maxVoltageV.toString(),
                "applicableTo" to s.applicableTo.name
            ))
            is ElectricalSpecs.GroundingSpecs -> SpecsDtoV1("GroundingSpecs", mapOf(
                "isMainEarthPoint" to s.isMainEarthPoint.toString()
            ))
            is ElectricalSpecs.LoadSpecs -> SpecsDtoV1("LoadSpecs", mapOf(
                "label" to s.label,
                "powerW" to s.powerW.toString(),
                "voltageV" to s.voltageV.toString(),
                "phases" to s.phases.name
            ))
        }
    }

    private fun portSpecFromDto(type: ComponentType, dto: PortDtoV1): ComponentPortSpec? {
        if (dto.specId != null || dto.electricalType != null || dto.phase != null || dto.polarity != null || dto.terminalRole != null) {
            return ComponentPortSpec(
                id = dto.specId ?: dto.name.lowercase().replace(' ', '_'),
                name = dto.name,
                type = runCatching { PortElectricalType.valueOf(dto.electricalType ?: inferElectricalType(dto.kind).name) }.getOrDefault(inferElectricalType(dto.kind)),
                phase = runCatching { ElectricalPhase.valueOf(dto.phase ?: inferPhase(dto.name, dto.kind).name) }.getOrDefault(inferPhase(dto.name, dto.kind)),
                polarity = runCatching { PortPolarity.valueOf(dto.polarity ?: inferPolarity(dto.kind).name) }.getOrDefault(inferPolarity(dto.kind)),
                terminalRole = runCatching { PhysicalTerminalRole.valueOf(dto.terminalRole ?: inferRole(type, dto.name).name) }.getOrDefault(inferRole(type, dto.name)),
                maxConnections = dto.maxConnections ?: inferMaxConnections(type, dto.kind),
                required = dto.required ?: inferRequired(type, dto.kind, dto.name),
                supportsSeriesLink = dto.supportsSeries ?: (type == ComponentType.PV_MODULE && (dto.kind == "DC_POS" || dto.kind == "DC_NEG")),
                notes = dto.specNotes
            )
        }
        return inferLegacyPortSpec(type, dto)
    }

    private fun inferLegacyPortSpec(type: ComponentType, dto: PortDtoV1): ComponentPortSpec? {
        return ComponentPortSpec(
            id = dto.name.lowercase().replace(' ', '_').replace('+', 'p').replace('-', 'm'),
            name = dto.name,
            type = inferElectricalType(dto.kind),
            phase = inferPhase(dto.name, dto.kind),
            polarity = inferPolarity(dto.kind),
            terminalRole = inferRole(type, dto.name),
            maxConnections = inferMaxConnections(type, dto.kind),
            required = inferRequired(type, dto.kind, dto.name),
            supportsSeriesLink = type == ComponentType.PV_MODULE && (dto.kind == "DC_POS" || dto.kind == "DC_NEG")
        )
    }

    private fun inferElectricalType(kind: String): PortElectricalType = when (runCatching { PortKind.valueOf(kind) }.getOrNull()) {
        PortKind.DC_POS, PortKind.DC_NEG -> PortElectricalType.DC
        PortKind.AC_L, PortKind.AC_N, PortKind.AC_PE -> PortElectricalType.AC
        PortKind.PE -> PortElectricalType.GROUND
        null -> PortElectricalType.SIGNAL
    }

    private fun inferPhase(name: String, kind: String): ElectricalPhase {
        val n = name.uppercase()
        return when {
            kind == "PE" -> ElectricalPhase.PE
            kind == "AC_N" || n.contains(" N") || n == "N" -> ElectricalPhase.N
            n.contains("L1") -> ElectricalPhase.L1
            n.contains("L2") -> ElectricalPhase.L2
            n.contains("L3") -> ElectricalPhase.L3
            else -> ElectricalPhase.NONE
        }
    }

    private fun inferPolarity(kind: String): PortPolarity = when (kind) {
        "DC_POS" -> PortPolarity.POSITIVE
        "DC_NEG" -> PortPolarity.NEGATIVE
        else -> PortPolarity.NONE
    }

    private fun inferRole(type: ComponentType, name: String): PhysicalTerminalRole {
        val n = name.uppercase()
        return when (type) {
            ComponentType.PV_MODULE -> PhysicalTerminalRole.TERMINAL
            ComponentType.MICROINVERTER, ComponentType.STRING_INVERTER -> when {
                n.contains("PE") -> PhysicalTerminalRole.PROTECTIVE_EARTH
                n.contains("AC") || n == "L1" || n == "L2" || n == "L3" || n == "N" -> PhysicalTerminalRole.AC_OUTPUT
                else -> PhysicalTerminalRole.DC_INPUT
            }
            ComponentType.AC_BUS, ComponentType.BARL, ComponentType.BARN, ComponentType.BARPE -> PhysicalTerminalRole.BUSBAR
            ComponentType.BREAKER -> if (n.contains("IN") || n.contains("LINE")) PhysicalTerminalRole.LINE else PhysicalTerminalRole.LOAD
            ComponentType.QDG -> if (n.contains("IN") || n.contains("ALIM")) PhysicalTerminalRole.LINE else if (n.contains("PE")) PhysicalTerminalRole.BUSBAR else PhysicalTerminalRole.LOAD
            ComponentType.DPS -> if (n.contains("PE")) PhysicalTerminalRole.PROTECTIVE_EARTH else PhysicalTerminalRole.SURGE_REFERENCE
            ComponentType.GROUND_BAR -> PhysicalTerminalRole.BUSBAR
            ComponentType.LOAD -> if (n.contains("PE")) PhysicalTerminalRole.PROTECTIVE_EARTH else PhysicalTerminalRole.LINE
        }
    }

    private fun inferMaxConnections(type: ComponentType, kind: String): Int = when (type) {
        ComponentType.AC_BUS, ComponentType.BARL, ComponentType.BARN, ComponentType.BARPE -> 8
        ComponentType.GROUND_BAR -> 16
        else -> 1
    }

    private fun inferRequired(type: ComponentType, kind: String, name: String): Boolean = when (type) {
        ComponentType.PV_MODULE -> kind == PortKind.DC_POS.name || kind == PortKind.DC_NEG.name
        ComponentType.MICROINVERTER,
        ComponentType.STRING_INVERTER,
        ComponentType.BREAKER,
        ComponentType.QDG,
        ComponentType.LOAD -> !name.uppercase().contains("PE")
        ComponentType.AC_BUS, ComponentType.BARL, ComponentType.BARN, ComponentType.BARPE, ComponentType.DPS, ComponentType.GROUND_BAR -> false
        else -> false
    }
}

package br.com.solardiagram.domain.model

data class Connection(
    val id: String,
    val fromComponentId: String,
    val fromPortId: String,
    val toComponentId: String,
    val toPortId: String,
    val meta: ConnectionMeta = ConnectionMeta()
)

data class ConnectionMeta(
    val lengthMeters: Double? = null,
    val overrideCableMm2: Double? = null,
    val conductorMaterial: ConductorMaterial = ConductorMaterial.CU,
    val installationMethod: InstallationMethod = InstallationMethod.CONDUIT_EXPOSED,
    val insulation: InsulationClass = InsulationClass.V750,
    val ambientTempC: Double = 30.0,
    val grouping: CircuitGrouping = CircuitGrouping.G1,
    val phases: SystemPhase = SystemPhase.MONO,
    val label: String? = null
)

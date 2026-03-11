package br.com.solardiagram.domain.model

enum class PortElectricalType {
    AC,
    DC,
    GROUND,
    SIGNAL
}

enum class ElectricalPhase {
    L1,
    L2,
    L3,
    N,
    PE,
    NONE
}

enum class PortPolarity {
    POSITIVE,
    NEGATIVE,
    NONE
}

enum class PhysicalTerminalRole {
    TERMINAL,
    DC_INPUT,
    AC_OUTPUT,
    BUSBAR,
    LINE,
    LOAD,
    PROTECTIVE_EARTH,
    SURGE_REFERENCE
}

data class ComponentPortSpec(
    val id: String,
    val name: String,
    val type: PortElectricalType,
    val phase: ElectricalPhase = ElectricalPhase.NONE,
    val polarity: PortPolarity = PortPolarity.NONE,
    val terminalRole: PhysicalTerminalRole = PhysicalTerminalRole.TERMINAL,
    val maxConnections: Int = 1,
    val required: Boolean = false,
    val supportsSeriesLink: Boolean = false,
    val notes: String? = null
)

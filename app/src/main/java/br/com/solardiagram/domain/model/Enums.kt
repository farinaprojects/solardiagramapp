package br.com.solardiagram.domain.model

enum class SystemPhase { MONO, BI, TRI }

enum class PortKind { DC_POS, DC_NEG, AC_L, AC_N, AC_PE, PE }

enum class PortDirection { INPUT, OUTPUT, BIDIRECTIONAL }

enum class PortSide { LEFT, RIGHT, TOP, BOTTOM }

enum class CurrentKind { AC, DC }

enum class ComponentType {
    PV_MODULE,
    MICROINVERTER,
    STRING_INVERTER,
    AC_BUS,
    BARL,
    BARN,
    BARPE,
    QDG,
    BREAKER,
    DPS,
    GROUND_BAR,
    LOAD
}

enum class BreakerCurve { B, C, D }
enum class BreakerPole { P1, P2, P3, P4 }

enum class ConductorMaterial { CU, AL }
enum class InstallationMethod { CONDUIT_EXPOSED, CONDUIT_EMBEDDED, TRAY, FREE_AIR }
enum class InsulationClass { V750, V1000 }
enum class CircuitGrouping { G1, G2, G3, G4PLUS }

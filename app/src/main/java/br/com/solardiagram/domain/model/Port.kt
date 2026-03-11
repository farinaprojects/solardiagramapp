package br.com.solardiagram.domain.model

data class Port(
    val id: String,
    val name: String,
    val kind: PortKind,
    val direction: PortDirection = PortDirection.BIDIRECTIONAL,
    val side: PortSide = PortSide.RIGHT,
    val slot: Int = 0,
    val spec: ComponentPortSpec? = null
)

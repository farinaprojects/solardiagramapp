package br.com.solardiagram.domain.model

data class Component(
    val id: String,
    val type: ComponentType,
    val name: String,
    val ports: List<Port>,
    val specs: ElectricalSpecs,
    val transform: Transform2 = Transform2()
) {
    fun portById(id: String): Port? = ports.firstOrNull { it.id == id }
}

package br.com.solardiagram.domain.electrical

data class ElectricalInstallation(
    val centralQdgComponentId: String? = null,
    val qdgComponentIds: List<String> = emptyList(),
    val mainSupplyFlows: List<ElectricalFlow> = emptyList(),
    val branchFlows: List<ElectricalFlow> = emptyList(),
    val generationFlows: List<ElectricalFlow> = emptyList(),
    val exportFlows: List<ElectricalFlow> = emptyList(),
    val detachedFlows: List<ElectricalFlow> = emptyList()
) {
    val hasCentralQdg: Boolean
        get() = !centralQdgComponentId.isNullOrBlank()

    val loadServingFlows: List<ElectricalFlow>
        get() = branchFlows.filter { it.destinationType != null }
}

package br.com.solardiagram.domain.electrical

data class ElectricalEdge(
    val fromNodeId: String,
    val toNodeId: String,
    val connectionId: String? = null
) {
    val fromComponentId: String
        get() = fromNodeId.substringBefore("::").substringBefore(":")

    val fromPortId: String
        get() = if ("::" in fromNodeId) fromNodeId.substringAfter("::") else fromNodeId.substringAfter(":", "")

    val toComponentId: String
        get() = toNodeId.substringBefore("::").substringBefore(":")

    val toPortId: String
        get() = if ("::" in toNodeId) toNodeId.substringAfter("::") else toNodeId.substringAfter(":", "")
}
package br.com.solardiagram.domain.electrical

enum class ElectricalProtectionRole {
    MAIN_INCOMING,
    LOAD_BRANCH,
    GENERATION_BRANCH,
    MIXED_BRANCH,
    UNCLASSIFIED
}

data class ElectricalProtection(
    val breakerComponentId: String,
    val breakerName: String,
    val role: ElectricalProtectionRole,
    val protectedFlowIds: List<String> = emptyList(),
    val upstreamComponentIds: List<String> = emptyList(),
    val downstreamComponentIds: List<String> = emptyList(),
    val protectedComponentIds: List<String> = emptyList(),
    val servesCentralQdg: Boolean = false,
    val isMainProtection: Boolean = false
)

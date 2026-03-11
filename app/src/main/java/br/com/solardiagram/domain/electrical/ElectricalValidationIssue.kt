package br.com.solardiagram.domain.electrical

enum class ElectricalIssueSeverity {
    WARNING,
    ERROR
}

data class ElectricalValidationIssue(
    val severity: ElectricalIssueSeverity,
    val message: String,
    val componentId: String? = null,
    val portId: String? = null
)
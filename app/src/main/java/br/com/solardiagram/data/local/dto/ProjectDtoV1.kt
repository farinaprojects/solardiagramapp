package br.com.solardiagram.data.local.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProjectDtoV1(
    val version: Int = 1,
    val id: String,
    val name: String,
    val location: String? = null,
    @SerialName("created_at") val createdAtEpochMs: Long,
    @SerialName("updated_at") val updatedAtEpochMs: Long,
    val components: List<ComponentDtoV1>,
    val connections: List<ConnectionDtoV1>
)

@Serializable
data class ComponentDtoV1(
    val id: String,
    val type: String,
    val name: String,
    val ports: List<PortDtoV1>,
    val specs: SpecsDtoV1,
    val x: Float,
    val y: Float,
    @SerialName("rotation_q") val rotationQuarterTurns: Int = 0
)

@Serializable
data class PortDtoV1(
    val id: String,
    val name: String,
    val kind: String,
    val direction: String? = null,
    val side: String? = null,
    val slot: Int = 0,
    @SerialName("spec_id") val specId: String? = null,
    @SerialName("electrical_type") val electricalType: String? = null,
    val phase: String? = null,
    val polarity: String? = null,
    @SerialName("terminal_role") val terminalRole: String? = null,
    @SerialName("max_connections") val maxConnections: Int? = null,
    val required: Boolean? = null,
    @SerialName("supports_series") val supportsSeries: Boolean? = null,
    @SerialName("spec_notes") val specNotes: String? = null
)

@Serializable
data class SpecsDtoV1(
    val kind: String,
    val data: Map<String, String>
)

@Serializable
data class ConnectionDtoV1(
    val id: String,
    @SerialName("from_component_id") val fromComponentId: String,
    @SerialName("from_port_id") val fromPortId: String,
    @SerialName("to_component_id") val toComponentId: String,
    @SerialName("to_port_id") val toPortId: String,

    @SerialName("length_m") val lengthMeters: Double? = null,
    @SerialName("override_mm2") val overrideCableMm2: Double? = null,
    val label: String? = null,

    @SerialName("material") val material: String? = null,
    @SerialName("install") val install: String? = null,
    @SerialName("insulation") val insulation: String? = null,
    @SerialName("temp_c") val tempC: Double? = null,
    @SerialName("grouping") val grouping: String? = null,
    @SerialName("phases") val phases: String? = null
)

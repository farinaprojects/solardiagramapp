package br.com.solardiagram.domain.model

data class DiagramProject(
    val id: String,
    val name: String,
    val location: String? = null,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val updatedAtEpochMs: Long = System.currentTimeMillis(),
    val components: List<Component> = emptyList(),
    val connections: List<Connection> = emptyList()
)

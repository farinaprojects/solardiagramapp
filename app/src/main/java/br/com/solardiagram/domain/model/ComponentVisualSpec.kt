
package br.com.solardiagram.domain.model

data class ComponentVisualSpec(
    val width: Float,
    val height: Float,
    val portSpacing: Float = 12f,
    val labelOffset: Float = 8f
)

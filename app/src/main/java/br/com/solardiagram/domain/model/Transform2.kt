package br.com.solardiagram.domain.model

data class Transform2(
    val position: Point2 = Point2(0f, 0f),
    val rotationQuarterTurns: Int = 0
) {
    val normalizedQuarterTurns: Int
        get() = ((rotationQuarterTurns % 4) + 4) % 4
}

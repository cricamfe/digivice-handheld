package dev.cricamfe.digivice.core.memory.resources

/** Port of `Backend/Memory/Resources/PlayerResource.cs`. */
data class PlayerResource(
    val bits: Int = 0,
    val mapId: Short = 0,
    val previousMapId: Short = 0,
    val seabedRoute: UByte = 0u,
    val mapVariant: UByte = 0u,
    val positionX: Int = 0,
    val positionY: Int = 0,
    val extraChapter: Boolean = false,
)

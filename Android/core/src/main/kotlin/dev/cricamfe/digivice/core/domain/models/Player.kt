package dev.cricamfe.digivice.core.domain.models

/** Port of `Backend/Domain/Models/Player.cs`; C# `byte?` is [UByte]?. */
data class Player(
    var bits: Int = 0,
    var mapId: String = "",
    var previousMapId: String = "",
    var seabedRoute: UByte? = null,
    var mapVariant: UByte? = null,
    var positionX: Int = 0,
    var positionY: Int = 0,
    var extraChapter: Boolean = false,
)

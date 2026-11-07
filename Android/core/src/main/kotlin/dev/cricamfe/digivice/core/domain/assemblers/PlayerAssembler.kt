package dev.cricamfe.digivice.core.domain.assemblers

import dev.cricamfe.digivice.core.domain.models.Player
import dev.cricamfe.digivice.core.memory.resources.PlayerResource

/** Port of `Backend/Domain/Assemblers/PlayerAssembler.cs`. */
object PlayerAssembler {
    fun assemble(resource: PlayerResource): Player = Player(
        bits = resource.bits,
        mapId = formatMapId(resource.mapId),
        previousMapId = formatMapId(resource.previousMapId),
        seabedRoute = resource.seabedRoute.takeIf { it.toInt() != 0 },
        mapVariant = resource.mapVariant.takeIf { it.toInt() != 0 },
        positionX = resource.positionX,
        positionY = resource.positionY,
        extraChapter = resource.extraChapter,
    )

    /** C# `short.ToString("X4")`: the 16-bit pattern in four upper-case hex digits, so a negative id never gets a sign. */
    fun formatMapId(mapId: Short): String = (mapId.toInt() and 0xFFFF).toString(16).uppercase().padStart(4, '0')
}

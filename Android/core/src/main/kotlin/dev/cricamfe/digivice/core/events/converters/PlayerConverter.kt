package dev.cricamfe.digivice.core.events.converters

import dev.cricamfe.digivice.core.domain.models.Player
import dev.cricamfe.digivice.core.events.dto.PlayerDto
import dev.cricamfe.digivice.core.events.dto.shared.Optional

/** Port of `Backend/Events/Converters/PlayerConverter.cs`. Reads the domain model, never changes it. */
object PlayerConverter {
    fun toDto(player: Player): PlayerDto = PlayerDto(
        bits = Optional.Present(player.bits),
        mapId = Optional.Present(player.mapId),
        previousMapId = Optional.Present(player.previousMapId),
        seabedRoute = Optional.Present(player.seabedRoute),
        mapVariant = Optional.Present(player.mapVariant),
        positionX = Optional.Present(player.positionX),
        positionY = Optional.Present(player.positionY),
        extraChapter = if (player.extraChapter) Optional.Present(true) else Optional.Empty,
    )
}

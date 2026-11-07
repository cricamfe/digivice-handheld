package dev.cricamfe.digivice.core.events.diffing

import dev.cricamfe.digivice.core.domain.models.Player
import dev.cricamfe.digivice.core.events.converters.PlayerConverter
import dev.cricamfe.digivice.core.events.diffing.extensions.changed
import dev.cricamfe.digivice.core.events.diffing.extensions.hasNoChanges
import dev.cricamfe.digivice.core.events.dto.PlayerDto

/** Port of `Backend/Events/Diffing/PlayerDiffer.cs`. Only reads the players. */
object PlayerDiffer {
    fun diff(previousPlayer: Player?, newPlayer: Player): PlayerDto {
        if (newPlayer.hasNoChanges(previousPlayer)) return PlayerDto()
        if (previousPlayer == null) return PlayerConverter.toDto(newPlayer)

        return PlayerDto(
            bits = changed(previousPlayer.bits, newPlayer.bits),
            mapId = changed(previousPlayer.mapId, newPlayer.mapId),
            previousMapId = changed(previousPlayer.previousMapId, newPlayer.previousMapId),
            seabedRoute = changed(previousPlayer.seabedRoute, newPlayer.seabedRoute),
            mapVariant = changed(previousPlayer.mapVariant, newPlayer.mapVariant),
            positionX = changed(previousPlayer.positionX, newPlayer.positionX),
            positionY = changed(previousPlayer.positionY, newPlayer.positionY),
            extraChapter = changed(previousPlayer.extraChapter, newPlayer.extraChapter),
        )
    }
}

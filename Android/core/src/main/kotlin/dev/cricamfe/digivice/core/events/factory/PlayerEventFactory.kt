package dev.cricamfe.digivice.core.events.factory

import dev.cricamfe.digivice.core.domain.models.State
import dev.cricamfe.digivice.core.events.Event
import dev.cricamfe.digivice.core.events.diffing.PlayerDiffer
import dev.cricamfe.digivice.core.events.dto.extensions.isNotEmpty
import dev.cricamfe.digivice.core.events.dto.PlayerDto
import dev.cricamfe.digivice.core.events.models.EventType

/** Port of `Backend/Events/Factory/PlayerEventFactory.cs`: the `PlayerChanged` event, or none when the section did not change. */
object PlayerEventFactory {
    fun create(previousState: State, newState: State): List<Event> {
        val dto = PlayerDiffer.diff(previousState.player, newState.player)

        return if (dto.isNotEmpty(PlayerDto())) listOf(Event(EventType.PlayerChanged, dto)) else emptyList()
    }
}

package dev.cricamfe.digivice.core.events.factory

import dev.cricamfe.digivice.core.domain.models.State
import dev.cricamfe.digivice.core.events.Event
import dev.cricamfe.digivice.core.events.diffing.NpcsDiffer
import dev.cricamfe.digivice.core.events.dto.extensions.isNotEmpty
import dev.cricamfe.digivice.core.events.dto.NpcsDto
import dev.cricamfe.digivice.core.events.models.EventType

/** Port of `Backend/Events/Factory/NpcsEventFactory.cs`: the `NpcsChanged` event, or none when the section did not change. */
object NpcsEventFactory {
    fun create(previousState: State, newState: State): List<Event> {
        val dto = NpcsDiffer.diff(previousState.npcs, newState.npcs)

        return if (dto.isNotEmpty(NpcsDto())) listOf(Event(EventType.NpcsChanged, dto)) else emptyList()
    }
}

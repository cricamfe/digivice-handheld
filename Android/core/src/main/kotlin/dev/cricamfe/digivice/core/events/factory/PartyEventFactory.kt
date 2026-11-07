package dev.cricamfe.digivice.core.events.factory

import dev.cricamfe.digivice.core.domain.models.State
import dev.cricamfe.digivice.core.events.Event
import dev.cricamfe.digivice.core.events.diffing.PartyDiffer
import dev.cricamfe.digivice.core.events.dto.extensions.isNotEmpty
import dev.cricamfe.digivice.core.events.dto.PartyDto
import dev.cricamfe.digivice.core.events.models.EventType

/** Port of `Backend/Events/Factory/PartyEventFactory.cs`: the `PartyChanged` event, or none when the section did not change. */
object PartyEventFactory {
    fun create(previousState: State, newState: State): List<Event> {
        val dto = PartyDiffer.diff(previousState.party, newState.party)

        return if (dto.isNotEmpty(PartyDto())) listOf(Event(EventType.PartyChanged, dto)) else emptyList()
    }
}

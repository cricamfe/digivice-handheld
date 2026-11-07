package dev.cricamfe.digivice.core.events.factory

import dev.cricamfe.digivice.core.domain.models.State
import dev.cricamfe.digivice.core.events.Event
import dev.cricamfe.digivice.core.events.diffing.JournalDiffer
import dev.cricamfe.digivice.core.events.dto.extensions.isNotEmpty
import dev.cricamfe.digivice.core.events.dto.JournalDto
import dev.cricamfe.digivice.core.events.models.EventType

/** Port of `Backend/Events/Factory/JournalEventFactory.cs`: the `JournalChanged` event, or none when the section did not change. */
object JournalEventFactory {
    fun create(previousState: State, newState: State): List<Event> {
        val dto = JournalDiffer.diff(previousState.journal, newState.journal)

        return if (dto.isNotEmpty(JournalDto())) listOf(Event(EventType.JournalChanged, dto)) else emptyList()
    }
}

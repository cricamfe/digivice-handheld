package dev.cricamfe.digivice.core.events.factory

import dev.cricamfe.digivice.core.domain.models.State
import dev.cricamfe.digivice.core.events.Event
import dev.cricamfe.digivice.core.events.diffing.ImportantItemsDiffer
import dev.cricamfe.digivice.core.events.dto.extensions.isNotEmpty
import dev.cricamfe.digivice.core.events.dto.ImportantItemsDto
import dev.cricamfe.digivice.core.events.models.EventType

/** Port of `Backend/Events/Factory/ImportantItemsEventFactory.cs`: the `ImportantItemsChanged` event, or none when the section did not change. */
object ImportantItemsEventFactory {
    fun create(previousState: State, newState: State): List<Event> {
        val dto = ImportantItemsDiffer.diff(previousState.importantItems, newState.importantItems)

        return if (dto.isNotEmpty(ImportantItemsDto())) listOf(Event(EventType.ImportantItemsChanged, dto)) else emptyList()
    }
}

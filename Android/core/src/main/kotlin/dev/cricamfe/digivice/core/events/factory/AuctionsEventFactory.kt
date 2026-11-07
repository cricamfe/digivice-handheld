package dev.cricamfe.digivice.core.events.factory

import dev.cricamfe.digivice.core.domain.models.State
import dev.cricamfe.digivice.core.events.Event
import dev.cricamfe.digivice.core.events.diffing.AuctionsDiffer
import dev.cricamfe.digivice.core.events.dto.extensions.isNotEmpty
import dev.cricamfe.digivice.core.events.dto.AuctionsDto
import dev.cricamfe.digivice.core.events.models.EventType

/** Port of `Backend/Events/Factory/AuctionsEventFactory.cs`: the `AuctionsChanged` event, or none when the section did not change. */
object AuctionsEventFactory {
    fun create(previousState: State, newState: State): List<Event> {
        val dto = AuctionsDiffer.diff(previousState.auctions, newState.auctions)

        return if (dto.isNotEmpty(AuctionsDto())) listOf(Event(EventType.AuctionsChanged, dto)) else emptyList()
    }
}

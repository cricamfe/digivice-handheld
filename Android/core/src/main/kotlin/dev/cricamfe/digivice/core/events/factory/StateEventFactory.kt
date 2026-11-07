package dev.cricamfe.digivice.core.events.factory

import dev.cricamfe.digivice.core.domain.models.State
import dev.cricamfe.digivice.core.events.Event
import dev.cricamfe.digivice.core.events.converters.StateConverter
import dev.cricamfe.digivice.core.events.diffing.extensions.hasNoChanges
import dev.cricamfe.digivice.core.events.models.EventType

/** Port of `Backend/Events/Factory/StateEventFactory.cs`. Only reads the states. */
object StateEventFactory {
    /**
     * The events that take a client from [previousState] to [newState]: none when nothing changed, one `InitialState`
     * with the whole state when there is no previous state, otherwise the `...Changed` events of the sections that
     * changed, in the order of the state (player, important items, party, digimon battle, card battle, auctions, npcs,
     * journal).
     */
    fun create(previousState: State?, newState: State): List<Event> {
        if (newState.hasNoChanges(previousState)) return emptyList()
        if (previousState == null) return listOf(Event(EventType.InitialState, StateConverter.toDto(newState)))

        return PlayerEventFactory.create(previousState, newState) +
            ImportantItemsEventFactory.create(previousState, newState) +
            PartyEventFactory.create(previousState, newState) +
            DigimonBattleEventFactory.create(previousState, newState) +
            CardBattleEventFactory.create(previousState, newState) +
            AuctionsEventFactory.create(previousState, newState) +
            NpcsEventFactory.create(previousState, newState) +
            JournalEventFactory.create(previousState, newState)
    }
}

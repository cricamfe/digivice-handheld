package dev.cricamfe.digivice.core.events.converters

import dev.cricamfe.digivice.core.domain.models.State
import dev.cricamfe.digivice.core.events.dto.StateDto

/** Port of `Backend/Events/Converters/StateConverter.cs`. Reads the domain model, never changes it. */
object StateConverter {
    fun toDto(state: State): StateDto = StateDto(
        player = PlayerConverter.toDto(state.player),
        importantItems = ImportantItemsConverter.toDto(state.importantItems),
        party = PartyConverter.toDto(state.party),
        digimonBattle = DigimonBattleConverter.toDto(state.digimonBattle),
        cardBattle = CardBattleConverter.toDto(state.cardBattle),
        auctions = AuctionsConverter.toDto(state.auctions),
        npcs = NpcsConverter.toDto(state.npcs),
        journal = JournalConverter.toDto(state.journal),
    )
}

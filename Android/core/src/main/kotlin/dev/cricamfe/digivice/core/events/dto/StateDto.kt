package dev.cricamfe.digivice.core.events.dto

import dev.cricamfe.digivice.core.events.dto.interfaces.IDto
import kotlinx.serialization.Serializable

/** Port of `Backend/Events/DTO/StateDTO.cs`: the eight sections are required, as `required` in C#. */
@Serializable
data class StateDto(
    val player: PlayerDto,
    val importantItems: ImportantItemsDto,
    val party: PartyDto,
    val digimonBattle: DigimonBattleDto,
    val cardBattle: CardBattleDto,
    val auctions: AuctionsDto,
    val npcs: NpcsDto,
    val journal: JournalDto,
) : IDto

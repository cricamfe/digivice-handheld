package dev.cricamfe.digivice.core.events.converters

import dev.cricamfe.digivice.core.domain.models.CardBattle
import dev.cricamfe.digivice.core.events.dto.CardBattleDto
import dev.cricamfe.digivice.core.events.dto.shared.Optional

/** Port of `Backend/Events/Converters/CardBattleConverter.cs`. Reads the domain model, never changes it. */
object CardBattleConverter {
    fun toDto(cardBattle: CardBattle): CardBattleDto = CardBattleDto(
        id = Optional.Present(cardBattle.id),
    )
}

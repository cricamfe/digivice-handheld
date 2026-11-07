package dev.cricamfe.digivice.core.events.diffing

import dev.cricamfe.digivice.core.domain.models.CardBattle
import dev.cricamfe.digivice.core.events.converters.CardBattleConverter
import dev.cricamfe.digivice.core.events.diffing.extensions.changed
import dev.cricamfe.digivice.core.events.diffing.extensions.hasNoChanges
import dev.cricamfe.digivice.core.events.dto.CardBattleDto

/** Port of `Backend/Events/Diffing/CardBattleDiffer.cs`. Only reads the card battles. */
object CardBattleDiffer {
    fun diff(previousCardBattle: CardBattle?, newCardBattle: CardBattle): CardBattleDto {
        if (newCardBattle.hasNoChanges(previousCardBattle)) return CardBattleDto()
        if (previousCardBattle == null) return CardBattleConverter.toDto(newCardBattle)

        return CardBattleDto(id = changed(previousCardBattle.id, newCardBattle.id))
    }
}

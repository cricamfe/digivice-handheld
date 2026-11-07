package dev.cricamfe.digivice.core.domain.assemblers

import dev.cricamfe.digivice.core.domain.models.CardBattle
import dev.cricamfe.digivice.core.memory.resources.CardBattleResource

/** Port of `Backend/Domain/Assemblers/CardBattleAssembler.cs`. */
object CardBattleAssembler {
    fun assemble(resource: CardBattleResource): CardBattle = CardBattle(
        id = if (resource.id <= 0) null else resource.id,
    )
}

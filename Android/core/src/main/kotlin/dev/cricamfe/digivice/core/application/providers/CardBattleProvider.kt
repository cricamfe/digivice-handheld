package dev.cricamfe.digivice.core.application.providers

import dev.cricamfe.digivice.core.application.loaders.interfaces.ICardBattleLoader
import dev.cricamfe.digivice.core.application.providers.interfaces.ICardBattleProvider
import dev.cricamfe.digivice.core.domain.assemblers.CardBattleAssembler
import dev.cricamfe.digivice.core.domain.models.CardBattle

/** Port of `Backend/Application/Providers/CardBattleProvider.cs`. */
class CardBattleProvider(private val cardBattleLoader: ICardBattleLoader) : ICardBattleProvider {
    override fun get(): CardBattle = CardBattleAssembler.assemble(cardBattleLoader.load())
}

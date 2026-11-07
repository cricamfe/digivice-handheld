package dev.cricamfe.digivice.core.application.providers.interfaces

import dev.cricamfe.digivice.core.domain.models.CardBattle

/** Port of `Backend/Application/Providers/Interfaces/ICardBattleProvider.cs`. */
interface ICardBattleProvider {
    fun get(): CardBattle
}

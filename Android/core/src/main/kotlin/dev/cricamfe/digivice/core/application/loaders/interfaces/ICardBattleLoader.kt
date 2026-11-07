package dev.cricamfe.digivice.core.application.loaders.interfaces

import dev.cricamfe.digivice.core.memory.resources.CardBattleResource

/** Port of `Backend/Application/Loaders/Interfaces/ICardBattleLoader.cs`. */
interface ICardBattleLoader {
    fun load(): CardBattleResource
}

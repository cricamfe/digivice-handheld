package dev.cricamfe.digivice.core.memory.readers.interfaces

import dev.cricamfe.digivice.core.memory.addresses.CardBattleAddresses
import dev.cricamfe.digivice.core.memory.resources.CardBattleResource

/** Port of `Backend/Memory/Readers/Interfaces/ICardBattleReader.cs`. */
interface ICardBattleReader {
    fun read(addresses: CardBattleAddresses): CardBattleResource
}

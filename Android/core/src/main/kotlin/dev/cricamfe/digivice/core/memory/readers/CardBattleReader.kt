package dev.cricamfe.digivice.core.memory.readers

import dev.cricamfe.digivice.core.memory.addresses.CardBattleAddresses
import dev.cricamfe.digivice.core.memory.readers.interfaces.ICardBattleReader
import dev.cricamfe.digivice.core.memory.readers.interfaces.IMemoryReader
import dev.cricamfe.digivice.core.memory.resources.CardBattleResource

/** Port of `Backend/Memory/Readers/CardBattleReader.cs`. */
class CardBattleReader(private val memoryReader: IMemoryReader) : ICardBattleReader {
    override fun read(addresses: CardBattleAddresses): CardBattleResource {
        return CardBattleResource(
            id = memoryReader.readInt32(addresses.id),
        )
    }
}

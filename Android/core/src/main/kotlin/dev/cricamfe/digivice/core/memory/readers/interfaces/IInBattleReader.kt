package dev.cricamfe.digivice.core.memory.readers.interfaces

import dev.cricamfe.digivice.core.memory.addresses.parties.InBattleAddresses
import dev.cricamfe.digivice.core.memory.resources.parties.InBattleResource

/** Port of `Backend/Memory/Readers/Interfaces/IInBattleReader.cs`. */
interface IInBattleReader {
    fun read(addresses: InBattleAddresses, partySlotIndex: Int): InBattleResource
}

package dev.cricamfe.digivice.core.memory.readers.interfaces

import dev.cricamfe.digivice.core.memory.addresses.PartyAddresses
import dev.cricamfe.digivice.core.memory.resources.PartyResource

/** Port of `Backend/Memory/Readers/Interfaces/IPartyReader.cs`. */
interface IPartyReader {
    fun read(addresses: PartyAddresses): PartyResource
}

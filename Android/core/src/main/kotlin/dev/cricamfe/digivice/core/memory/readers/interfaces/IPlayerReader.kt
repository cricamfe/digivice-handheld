package dev.cricamfe.digivice.core.memory.readers.interfaces

import dev.cricamfe.digivice.core.memory.addresses.PlayerAddresses
import dev.cricamfe.digivice.core.memory.resources.PlayerResource

/** Port of `Backend/Memory/Readers/Interfaces/IPlayerReader.cs`. */
interface IPlayerReader {
    fun read(addresses: PlayerAddresses): PlayerResource
}

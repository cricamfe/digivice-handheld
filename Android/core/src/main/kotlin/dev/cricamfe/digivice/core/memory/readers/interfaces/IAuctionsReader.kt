package dev.cricamfe.digivice.core.memory.readers.interfaces

import dev.cricamfe.digivice.core.memory.addresses.AuctionsAddresses
import dev.cricamfe.digivice.core.memory.resources.AuctionsResource

/** Port of `Backend/Memory/Readers/Interfaces/IAuctionsReader.cs`. */
interface IAuctionsReader {
    fun read(addresses: AuctionsAddresses): AuctionsResource
}

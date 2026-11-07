package dev.cricamfe.digivice.core.memory.readers.interfaces

import dev.cricamfe.digivice.core.memory.addresses.NpcsAddresses
import dev.cricamfe.digivice.core.memory.resources.NpcsResource

/** Port of `Backend/Memory/Readers/Interfaces/INpcsReader.cs`. */
interface INpcsReader {
    fun read(addresses: NpcsAddresses): NpcsResource
}

package dev.cricamfe.digivice.core.memory.readers.interfaces

import dev.cricamfe.digivice.core.memory.MemoryBlockReader
import dev.cricamfe.digivice.core.memory.addresses.parties.digimons.DigievolutionsAddresses
import dev.cricamfe.digivice.core.memory.resources.parties.digimons.StoredDigievolutionResource

/** Port of `Backend/Memory/Readers/Interfaces/IStoredDigievolutionReader.cs`. */
interface IStoredDigievolutionReader {
    fun read(
        memoryBlockReader: MemoryBlockReader,
        digievolutionsAddresses: DigievolutionsAddresses,
    ): List<StoredDigievolutionResource>
}

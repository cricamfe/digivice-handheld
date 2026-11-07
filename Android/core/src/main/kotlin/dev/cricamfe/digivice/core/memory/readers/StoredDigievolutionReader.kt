package dev.cricamfe.digivice.core.memory.readers

import dev.cricamfe.digivice.core.memory.MemoryBlockReader
import dev.cricamfe.digivice.core.memory.addresses.parties.digimons.DigievolutionsAddresses
import dev.cricamfe.digivice.core.memory.readers.interfaces.IStoredDigievolutionReader
import dev.cricamfe.digivice.core.memory.resources.parties.digimons.StoredDigievolutionResource

/** Port of `Backend/Memory/Readers/StoredDigievolutionReader.cs`. */
class StoredDigievolutionReader : IStoredDigievolutionReader {
    override fun read(
        memoryBlockReader: MemoryBlockReader,
        digievolutionsAddresses: DigievolutionsAddresses,
    ): List<StoredDigievolutionResource> {
        val unlockedDigievolutionsStart = digievolutionsAddresses.unlockedDigievolutionsStart
        val unlockedDigievolutionEntryStride = digievolutionsAddresses.unlockedDigievolutionEntryStride
        val maxUnlockedDigievolutions = digievolutionsAddresses.maxUnlockedDigievolutions

        val storedDigievolutions = mutableListOf<StoredDigievolutionResource>()

        for (i in 0 until maxUnlockedDigievolutions) {
            val offset = unlockedDigievolutionsStart + (i * unlockedDigievolutionEntryStride)
            val digievolutionId: Int = memoryBlockReader.readInt16(offset + digievolutionsAddresses.id).toInt()

            if (digievolutionId <= 0) {
                break
            }

            storedDigievolutions.add(
                StoredDigievolutionResource(
                    digievolutionId = digievolutionId,
                    level = memoryBlockReader.readInt16(offset + digievolutionsAddresses.level).toInt(),
                    dvexp = memoryBlockReader.readInt32(offset + digievolutionsAddresses.dvexp),
                ),
            )
        }

        return storedDigievolutions
    }
}

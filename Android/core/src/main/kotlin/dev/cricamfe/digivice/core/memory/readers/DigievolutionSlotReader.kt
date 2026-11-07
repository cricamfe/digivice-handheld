package dev.cricamfe.digivice.core.memory.readers

import dev.cricamfe.digivice.core.memory.MemoryBlockReader
import dev.cricamfe.digivice.core.memory.addresses.parties.SlotAddresses
import dev.cricamfe.digivice.core.memory.readers.interfaces.IDigievolutionSlotReader
import dev.cricamfe.digivice.core.memory.resources.parties.digimons.DigievolutionSlotResource

/** Port of `Backend/Memory/Readers/DigievolutionSlotReader.cs`. */
class DigievolutionSlotReader : IDigievolutionSlotReader {
    override fun read(memoryBlockReader: MemoryBlockReader, slotAddresses: SlotAddresses): DigievolutionSlotResource {
        val digievolutionId: Int = memoryBlockReader.readInt16(slotAddresses.address.toInt()).toInt()

        return DigievolutionSlotResource(
            index = slotAddresses.index,
            digievolutionId = digievolutionId,
        )
    }
}

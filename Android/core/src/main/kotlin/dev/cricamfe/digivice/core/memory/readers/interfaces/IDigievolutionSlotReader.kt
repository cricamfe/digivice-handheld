package dev.cricamfe.digivice.core.memory.readers.interfaces

import dev.cricamfe.digivice.core.memory.MemoryBlockReader
import dev.cricamfe.digivice.core.memory.addresses.parties.SlotAddresses
import dev.cricamfe.digivice.core.memory.resources.parties.digimons.DigievolutionSlotResource

/** Port of `Backend/Memory/Readers/Interfaces/IDigievolutionSlotReader.cs`. */
interface IDigievolutionSlotReader {
    fun read(memoryBlockReader: MemoryBlockReader, slotAddresses: SlotAddresses): DigievolutionSlotResource
}

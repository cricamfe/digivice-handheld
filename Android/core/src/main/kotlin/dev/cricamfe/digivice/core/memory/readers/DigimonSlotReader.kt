package dev.cricamfe.digivice.core.memory.readers

import dev.cricamfe.digivice.core.memory.addresses.parties.SlotAddresses
import dev.cricamfe.digivice.core.memory.readers.interfaces.IDigimonSlotReader
import dev.cricamfe.digivice.core.memory.readers.interfaces.IMemoryReader
import dev.cricamfe.digivice.core.memory.resources.parties.DigimonSlotResource

/** Port of `Backend/Memory/Readers/DigimonSlotReader.cs`. */
class DigimonSlotReader(private val memoryReader: IMemoryReader) : IDigimonSlotReader {
    override fun read(addresses: SlotAddresses): DigimonSlotResource {
        return DigimonSlotResource(
            index = addresses.index,
            // A C# byte widens to int as 0..255 (the empty slot sentinel is 0xFF), hence UByte.toInt().
            digimonId = memoryReader.readByte(addresses.address).toInt(),
        )
    }
}

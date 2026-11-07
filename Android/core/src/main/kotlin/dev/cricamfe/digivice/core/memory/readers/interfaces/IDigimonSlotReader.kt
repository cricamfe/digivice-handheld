package dev.cricamfe.digivice.core.memory.readers.interfaces

import dev.cricamfe.digivice.core.memory.addresses.parties.SlotAddresses
import dev.cricamfe.digivice.core.memory.resources.parties.DigimonSlotResource

/** Port of `Backend/Memory/Readers/Interfaces/IDigimonSlotReader.cs`. */
interface IDigimonSlotReader {
    fun read(addresses: SlotAddresses): DigimonSlotResource
}

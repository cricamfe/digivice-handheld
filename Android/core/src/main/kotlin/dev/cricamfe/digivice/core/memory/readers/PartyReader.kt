package dev.cricamfe.digivice.core.memory.readers

import dev.cricamfe.digivice.core.memory.addresses.PartyAddresses
import dev.cricamfe.digivice.core.memory.readers.interfaces.IDigimonSlotReader
import dev.cricamfe.digivice.core.memory.readers.interfaces.IPartyReader
import dev.cricamfe.digivice.core.memory.resources.PartyResource

/** Port of `Backend/Memory/Readers/PartyReader.cs`. */
class PartyReader(private val slotReader: IDigimonSlotReader) : IPartyReader {
    override fun read(addresses: PartyAddresses): PartyResource {
        return PartyResource(
            slotsResource = addresses.slots.map { slot -> slotReader.read(slot) },
        )
    }
}

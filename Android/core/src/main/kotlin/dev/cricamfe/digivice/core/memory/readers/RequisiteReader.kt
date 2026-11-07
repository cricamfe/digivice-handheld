package dev.cricamfe.digivice.core.memory.readers

import dev.cricamfe.digivice.core.memory.addresses.journals.quests.RequisiteAddresses
import dev.cricamfe.digivice.core.memory.readers.interfaces.IMemoryReader
import dev.cricamfe.digivice.core.memory.readers.interfaces.IRequisiteReader
import dev.cricamfe.digivice.core.memory.resources.journals.quests.RequisiteResource

/** Port of `Backend/Memory/Readers/RequisiteReader.cs`. */
class RequisiteReader(private val memoryReader: IMemoryReader) : IRequisiteReader {
    override fun read(addresses: RequisiteAddresses): RequisiteResource {
        return RequisiteResource(
            id = addresses.id,
            value = readBitMaskedValue(memoryReader, addresses.address, addresses.bitMasks),
        )
    }
}

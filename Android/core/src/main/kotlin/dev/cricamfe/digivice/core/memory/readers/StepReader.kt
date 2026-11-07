package dev.cricamfe.digivice.core.memory.readers

import dev.cricamfe.digivice.core.memory.addresses.journals.quests.StepAddresses
import dev.cricamfe.digivice.core.memory.readers.interfaces.IMemoryReader
import dev.cricamfe.digivice.core.memory.readers.interfaces.IRequisiteReader
import dev.cricamfe.digivice.core.memory.readers.interfaces.IStepReader
import dev.cricamfe.digivice.core.memory.resources.journals.quests.StepResource

/** Port of `Backend/Memory/Readers/StepReader.cs`. */
class StepReader(
    private val memoryReader: IMemoryReader,
    private val requisiteReader: IRequisiteReader,
) : IStepReader {
    override fun read(addresses: StepAddresses): StepResource {
        return StepResource(
            number = addresses.number,
            value = readValue(addresses),
            requisites = addresses.requisites.map(requisiteReader::read),
        )
    }

    private fun readValue(addresses: StepAddresses): UByte {
        val atLeast = addresses.atLeast
        if (atLeast != null) {
            return if (memoryReader.readByte(addresses.address).toInt() >= atLeast) 1u else 0u
        }

        val value = readBitMaskedValue(memoryReader, addresses.address, addresses.bitMasks)
        if (value.toInt() != 0) return value

        return addresses.alternatives
            .map { readBitMaskedValue(memoryReader, it.address, it.bitMasks) }
            .firstOrNull { it.toInt() != 0 } ?: 0u
    }
}

package dev.cricamfe.digivice.core.memory.readers

import dev.cricamfe.digivice.core.memory.addresses.parties.InBattleAddresses
import dev.cricamfe.digivice.core.memory.readers.interfaces.IInBattleReader
import dev.cricamfe.digivice.core.memory.readers.interfaces.IMemoryReader
import dev.cricamfe.digivice.core.memory.resources.parties.InBattleResource
import dev.cricamfe.digivice.core.memory.resources.parties.digimons.VitalResource

/** Port of `Backend/Memory/Readers/InBattleReader.cs`. */
class InBattleReader(private val memoryReader: IMemoryReader) : IInBattleReader {
    override fun read(addresses: InBattleAddresses, partySlotIndex: Int): InBattleResource {
        val slotBase = addresses.allySlotBase + ((partySlotIndex - 1) * addresses.slotStride)

        return InBattleResource(
            formId = memoryReader.readInt16(slotBase + addresses.id).toInt(),
            condition = memoryReader.readByte(slotBase + addresses.condition).toInt(),
            strength = memoryReader.readInt16(slotBase + addresses.strength).toInt(),
            defense = memoryReader.readInt16(slotBase + addresses.defense).toInt(),
            speed = memoryReader.readInt16(slotBase + addresses.speed).toInt(),
            hp = VitalResource(
                max = memoryReader.readInt16(slotBase + addresses.hp.max).toInt(),
                current = memoryReader.readInt16(slotBase + addresses.hp.current).toInt(),
            ),
            mp = VitalResource(
                max = memoryReader.readInt16(slotBase + addresses.mp.max).toInt(),
                current = memoryReader.readInt16(slotBase + addresses.mp.current).toInt(),
            ),
        )
    }
}

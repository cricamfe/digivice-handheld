package dev.cricamfe.digivice.core.memory.readers

import dev.cricamfe.digivice.core.memory.addresses.EnemyAddresses
import dev.cricamfe.digivice.core.memory.readers.interfaces.IEnemyReader
import dev.cricamfe.digivice.core.memory.readers.interfaces.IMemoryReader
import dev.cricamfe.digivice.core.memory.resources.battles.EnemyResource
import dev.cricamfe.digivice.core.memory.resources.parties.digimons.VitalResource

/** Port of `Backend/Memory/Readers/EnemyReader.cs`. */
class EnemyReader(private val memoryReader: IMemoryReader) : IEnemyReader {
    override fun read(addresses: EnemyAddresses): EnemyResource {
        val slotIndex = resolveActiveEnemySlotIndex(addresses)
        val slotBase = addresses.enemySlotBase + (slotIndex * addresses.slotStride)

        return EnemyResource(
            id = memoryReader.readInt16(slotBase + addresses.id).toInt(),
            groupId = memoryReader.readInt16(addresses.groupId).toInt(),
            condition = memoryReader.readByte(slotBase + addresses.condition).toInt(),
            strength = memoryReader.readInt16(slotBase + addresses.strength).toInt(),
            defense = memoryReader.readInt16(slotBase + addresses.defense).toInt(),
            speed = memoryReader.readInt16(slotBase + addresses.speed).toInt(),
            hp = VitalResource(
                max = memoryReader.readInt16(slotBase + addresses.hp.max).toInt(),
                current = memoryReader.readInt16(slotBase + addresses.hp.current).toInt(),
            ),
        )
    }

    private fun resolveActiveEnemySlotIndex(addresses: EnemyAddresses): Int {
        val activeEnemySlotIndex: Int = memoryReader.readInt16(addresses.activeEnemySlotIndex).toInt()

        if (activeEnemySlotIndex >= 0 && activeEnemySlotIndex < addresses.slotCount) {
            val activeSlotBase = addresses.enemySlotBase + (activeEnemySlotIndex * addresses.slotStride)
            val activeSlotId = memoryReader.readInt16(activeSlotBase + addresses.id).toInt()

            if (activeSlotId != 0) {
                return activeEnemySlotIndex
            }
        }

        for (slotIndex in 0 until addresses.slotCount) {
            val slotBase = addresses.enemySlotBase + (slotIndex * addresses.slotStride)
            val slotId = memoryReader.readInt16(slotBase + addresses.id).toInt()
            val currentHp = memoryReader.readInt16(slotBase + addresses.hp.current).toInt()

            if (slotId != 0 && currentHp > 0) {
                return slotIndex
            }
        }

        for (slotIndex in addresses.slotCount - 1 downTo 0) {
            val slotBase = addresses.enemySlotBase + (slotIndex * addresses.slotStride)
            val slotId = memoryReader.readInt16(slotBase + addresses.id).toInt()

            if (slotId != 0) {
                return slotIndex
            }
        }

        return 0
    }
}

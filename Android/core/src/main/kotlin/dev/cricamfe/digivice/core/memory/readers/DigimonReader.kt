package dev.cricamfe.digivice.core.memory.readers

import dev.cricamfe.digivice.core.memory.MemoryBlockReader
import dev.cricamfe.digivice.core.memory.addresses.parties.DigimonAddress
import dev.cricamfe.digivice.core.memory.addresses.parties.DigimonStatusAddresses
import dev.cricamfe.digivice.core.memory.addresses.parties.InBattleAddresses
import dev.cricamfe.digivice.core.memory.readers.interfaces.IDigievolutionSlotReader
import dev.cricamfe.digivice.core.memory.readers.interfaces.IDigimonReader
import dev.cricamfe.digivice.core.memory.readers.interfaces.IInBattleReader
import dev.cricamfe.digivice.core.memory.readers.interfaces.IMemoryReader
import dev.cricamfe.digivice.core.memory.readers.interfaces.IStoredDigievolutionReader
import dev.cricamfe.digivice.core.memory.resources.parties.DigimonResource
import dev.cricamfe.digivice.core.memory.resources.parties.digimons.AttributesResource
import dev.cricamfe.digivice.core.memory.resources.parties.digimons.ElementsResource
import dev.cricamfe.digivice.core.memory.resources.parties.digimons.EquipmentsResource
import dev.cricamfe.digivice.core.memory.resources.parties.digimons.VitalResource

/** Port of `Backend/Memory/Readers/DigimonReader.cs`. */
class DigimonReader(
    private val memoryReader: IMemoryReader,
    private val digievolutionSlotReader: IDigievolutionSlotReader,
    private val storedDigievolutionReader: IStoredDigievolutionReader,
    private val digimonInBattleReader: IInBattleReader,
    private val memoryBlockSize: Int = 1500,
) : IDigimonReader {

    override fun read(
        digimonAddress: DigimonAddress,
        digimonStatusAddresses: DigimonStatusAddresses,
        inBattleAddresses: InBattleAddresses,
        partySlotIndex: Int,
    ): DigimonResource? {
        val memoryBlock = memoryReader.readBytes(digimonAddress.memoryBlockAddress, memoryBlockSize)
        if (memoryBlock.size < memoryBlockSize) {
            return null
        }

        val memoryBlockReader = MemoryBlockReader(memoryBlock)
        val digievolutionsSlots = digimonStatusAddresses.digievolutions.slots
            .map { slot -> digievolutionSlotReader.read(memoryBlockReader, slot) }

        val activeDigievolutionId = memoryReader.readInt16(
            digimonAddress.memoryBlockAddress + digimonStatusAddresses.digievolutions.activeDigievolution,
        ).toInt()

        return DigimonResource(
            activeDigievolutionId = activeDigievolutionId,
            experience = memoryBlockReader.readInt32(digimonStatusAddresses.experience),
            level = memoryBlockReader.readInt16(digimonStatusAddresses.level).toInt(),
            tp = memoryBlockReader.readInt16(digimonStatusAddresses.tp).toInt(),
            blast = memoryReader.readInt16(digimonAddress.blastAddress).toInt(),
            hp = VitalResource(
                current = memoryBlockReader.readInt16(digimonStatusAddresses.hp.current).toInt(),
                max = memoryBlockReader.readInt16(digimonStatusAddresses.hp.max).toInt(),
            ),
            mp = VitalResource(
                current = memoryBlockReader.readInt16(digimonStatusAddresses.mp.current).toInt(),
                max = memoryBlockReader.readInt16(digimonStatusAddresses.mp.max).toInt(),
            ),
            inBattle = digimonInBattleReader.read(inBattleAddresses, partySlotIndex),
            attributes = AttributesResource(
                strength = memoryBlockReader.readInt16(digimonStatusAddresses.attributes.strength).toInt(),
                defense = memoryBlockReader.readInt16(digimonStatusAddresses.attributes.defense).toInt(),
                spirit = memoryBlockReader.readInt16(digimonStatusAddresses.attributes.spirit).toInt(),
                wisdom = memoryBlockReader.readInt16(digimonStatusAddresses.attributes.wisdom).toInt(),
                speed = memoryBlockReader.readInt16(digimonStatusAddresses.attributes.speed).toInt(),
                charisma = memoryBlockReader.readInt16(digimonStatusAddresses.attributes.charisma).toInt(),
            ),
            elements = ElementsResource(
                fire = memoryBlockReader.readInt16(digimonStatusAddresses.elements.fire).toInt(),
                water = memoryBlockReader.readInt16(digimonStatusAddresses.elements.water).toInt(),
                ice = memoryBlockReader.readInt16(digimonStatusAddresses.elements.ice).toInt(),
                wind = memoryBlockReader.readInt16(digimonStatusAddresses.elements.wind).toInt(),
                thunder = memoryBlockReader.readInt16(digimonStatusAddresses.elements.thunder).toInt(),
                machine = memoryBlockReader.readInt16(digimonStatusAddresses.elements.machine).toInt(),
                dark = memoryBlockReader.readInt16(digimonStatusAddresses.elements.dark).toInt(),
            ),
            equipments = EquipmentsResource(
                head = memoryBlockReader.readInt16(digimonStatusAddresses.equipments.head).toInt(),
                body = memoryBlockReader.readInt16(digimonStatusAddresses.equipments.body).toInt(),
                right = memoryBlockReader.readInt16(digimonStatusAddresses.equipments.right).toInt(),
                left = memoryBlockReader.readInt16(digimonStatusAddresses.equipments.left).toInt(),
                accessory1 = memoryBlockReader.readInt16(digimonStatusAddresses.equipments.accessory1).toInt(),
                accessory2 = memoryBlockReader.readInt16(digimonStatusAddresses.equipments.accessory2).toInt(),
            ),
            digievolutions = digievolutionsSlots,
            storedDigievolutions = storedDigievolutionReader.read(
                memoryBlockReader,
                digimonStatusAddresses.digievolutions,
            ),
        )
    }
}

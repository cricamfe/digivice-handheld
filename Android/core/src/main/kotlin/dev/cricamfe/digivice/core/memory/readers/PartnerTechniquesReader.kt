package dev.cricamfe.digivice.core.memory.readers

import dev.cricamfe.digivice.core.memory.readers.interfaces.IMemoryReader
import dev.cricamfe.digivice.core.memory.repositories.IAddressesRepository
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * The techniques each partner of the party really has, from its `PartnerStats` in the RAM (facts from the matching
 * decompilation of the European version, ReGame-Labs/dw3_decomp, MIT): the three Digimon in its slots, each with the
 * `PartnerEntry.skills` the game makes its battle menu from (`listSlotTechs`). Nothing here depends on which Digimon it is.
 */
class PartnerTechniquesReader(
    private val memoryReader: IMemoryReader,
    private val addressesRepository: IAddressesRepository,
) {
    data class Form(val id: Int, val skills: List<Int>)

    data class Partner(val digimonId: Int, val forms: List<Form>)

    fun read(): List<Partner> {
        val party = addressesRepository.getPartyAddresses()
        val digimons = addressesRepository.getDigimonsAddresses()
        val addresses = addressesRepository.getDigimonStatusAddresses().digievolutions
        val entries = addresses.unlockedDigievolutionsStart

        return party.slots.mapNotNull { slot ->
            val digimonId = memoryReader.readByte(slot.address).toInt()
            val digimon = digimons[digimonId]
            if (digimonId == party.emptySlotId || digimon == null) return@mapNotNull null

            val size = entries + addresses.maxUnlockedDigievolutions * addresses.unlockedDigievolutionEntryStride
            val block = ByteBuffer.wrap(memoryReader.readBytes(digimon.memoryBlockAddress, size)).order(ByteOrder.LITTLE_ENDIAN)
            val slots = addresses.slots.map { block.getShort(it.address.toInt()).toInt() }.filter { it >= FIRST_ENTRY_ID }
            val forms = (0 until addresses.maxUnlockedDigievolutions).mapNotNull { index ->
                val entry = entries + index * addresses.unlockedDigievolutionEntryStride
                val id = block.getShort(entry + addresses.id).toInt()
                if (id !in slots) return@mapNotNull null
                Form(id, List(SKILLS) { block.getShort(entry + SKILLS_OFFSET + it * Short.SIZE_BYTES).toInt() and 0xFFFF })
            }
            Partner(digimonId, forms)
        }
    }

    private companion object {
        /** `FIRST_ENTRY_ID`: a slot under it holds nothing. */
        const val FIRST_ENTRY_ID = 3

        /** `PartnerEntry.skills`: six s16, with `SKILL_ID` and the flags. */
        const val SKILLS_OFFSET = 8
        const val SKILLS = 6
    }
}

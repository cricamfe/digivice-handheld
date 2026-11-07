package dev.cricamfe.digivice.core.memory.readers

import dev.cricamfe.digivice.core.memory.FakeDigievolutionSlotReader
import dev.cricamfe.digivice.core.memory.FakeInBattleReader
import dev.cricamfe.digivice.core.memory.FakeMemoryReader
import dev.cricamfe.digivice.core.memory.FakeStoredDigievolutionReader
import dev.cricamfe.digivice.core.memory.addresses.parties.DigimonAddress
import dev.cricamfe.digivice.core.memory.addresses.parties.DigimonStatusAddresses
import dev.cricamfe.digivice.core.memory.addresses.parties.InBattleAddresses
import dev.cricamfe.digivice.core.memory.addresses.parties.SlotAddresses
import dev.cricamfe.digivice.core.memory.addresses.parties.digimons.AttributesAddresses
import dev.cricamfe.digivice.core.memory.addresses.parties.digimons.DigievolutionsAddresses
import dev.cricamfe.digivice.core.memory.addresses.parties.digimons.ElementsAddresses
import dev.cricamfe.digivice.core.memory.addresses.parties.digimons.EquipmentsAddresses
import dev.cricamfe.digivice.core.memory.addresses.parties.digimons.VitalAddresses
import dev.cricamfe.digivice.core.memory.resources.parties.InBattleResource
import dev.cricamfe.digivice.core.memory.resources.parties.digimons.DigievolutionSlotResource
import dev.cricamfe.digivice.core.memory.resources.parties.digimons.StoredDigievolutionResource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class DigimonReaderTests {
    private companion object {
        const val BLAST_ADDRESS = 0x42B76L
    }

    private fun writeInt16(block: ByteArray, offset: Int, value: Short) {
        ByteBuffer.wrap(block).order(ByteOrder.LITTLE_ENDIAN).putShort(offset, value)
    }

    private fun writeInt32(block: ByteArray, offset: Int, value: Int) {
        ByteBuffer.wrap(block).order(ByteOrder.LITTLE_ENDIAN).putInt(offset, value)
    }

    @Test
    fun `read should return null when memory block is too short`() {
        val address = DigimonAddress(memoryBlockAddress = 0x800100)
        val statusAddresses = DigimonStatusAddresses()
        val memoryReader = FakeMemoryReader().apply { blocks[0x800100L to 1500] = ByteArray(1000) }

        val reader = DigimonReader(
            memoryReader,
            FakeDigievolutionSlotReader(),
            FakeStoredDigievolutionReader(),
            FakeInBattleReader(),
        )

        val result = reader.read(address, statusAddresses, InBattleAddresses(), 1)

        assertNull(result)
    }

    @Test
    fun `read should correctly map digimon fields when block is fully loaded`() {
        val address = DigimonAddress(memoryBlockAddress = 0x800100, blastAddress = BLAST_ADDRESS)
        val statusAddresses = DigimonStatusAddresses(
            experience = 10,
            level = 14,
            tp = 16,
            hp = VitalAddresses(current = 18, max = 20),
            mp = VitalAddresses(current = 22, max = 24),
            attributes = AttributesAddresses(
                strength = 26, defense = 28, spirit = 30, wisdom = 32, speed = 34, charisma = 36,
            ),
            elements = ElementsAddresses(
                fire = 38, water = 40, ice = 42, wind = 44, thunder = 46, machine = 48, dark = 50,
            ),
            equipments = EquipmentsAddresses(
                head = 52, body = 54, right = 56, left = 58, accessory1 = 60, accessory2 = 62,
            ),
            digievolutions = DigievolutionsAddresses(activeDigievolution = 200, slots = emptyList()),
        )

        val block = ByteArray(1500)
        writeInt32(block, 10, 85000) // Experience
        writeInt16(block, 14, 45) // Level
        writeInt16(block, 16, 32) // TP

        // HP / MP
        writeInt16(block, 18, 500) // HP Current
        writeInt16(block, 20, 1000) // HP Max
        writeInt16(block, 22, 200) // MP Current
        writeInt16(block, 24, 400) // MP Max

        // Attributes
        writeInt16(block, 26, 80) // Strength
        writeInt16(block, 28, 75) // Defense
        writeInt16(block, 30, 60) // Spirit
        writeInt16(block, 32, 95) // Wisdom
        writeInt16(block, 34, 110) // Speed
        writeInt16(block, 36, 40) // Charisma

        // Elements
        writeInt16(block, 38, 10) // Fire
        writeInt16(block, 40, 20) // Water
        writeInt16(block, 42, 30) // Ice
        writeInt16(block, 44, 40) // Wind
        writeInt16(block, 46, 50) // Thunder
        writeInt16(block, 48, 60) // Machine
        writeInt16(block, 50, 70) // Dark

        // Equipments
        writeInt16(block, 52, 101) // Head
        writeInt16(block, 54, 102) // Body
        writeInt16(block, 56, 103) // Right
        writeInt16(block, 58, 104) // Left
        writeInt16(block, 60, 105) // Accessory1
        writeInt16(block, 62, 106) // Accessory2

        val memoryReader = FakeMemoryReader().apply {
            blocks[0x800100L to 1500] = block
            int16[0x800100L + 200] = 3 // ActiveDigievolutionId
            int16[BLAST_ADDRESS] = 750
        }

        val reader = DigimonReader(
            memoryReader,
            FakeDigievolutionSlotReader(),
            FakeStoredDigievolutionReader(),
            FakeInBattleReader(),
        )

        val result = reader.read(address, statusAddresses, InBattleAddresses(), 1)

        assertNotNull(result)
        result!!
        assertEquals(3, result.activeDigievolutionId)
        assertEquals(85000, result.experience)
        assertEquals(45, result.level)
        assertEquals(32, result.tp)
        assertEquals(750, result.blast)

        // HP / MP
        assertEquals(500, result.hp.current)
        assertEquals(1000, result.hp.max)
        assertEquals(200, result.mp.current)
        assertEquals(400, result.mp.max)

        // Attributes
        assertEquals(80, result.attributes.strength)
        assertEquals(75, result.attributes.defense)
        assertEquals(60, result.attributes.spirit)
        assertEquals(95, result.attributes.wisdom)
        assertEquals(110, result.attributes.speed)
        assertEquals(40, result.attributes.charisma)

        // Elements
        assertEquals(10, result.elements.fire)
        assertEquals(20, result.elements.water)
        assertEquals(30, result.elements.ice)
        assertEquals(40, result.elements.wind)
        assertEquals(50, result.elements.thunder)
        assertEquals(60, result.elements.machine)
        assertEquals(70, result.elements.dark)

        // Equipments
        assertEquals(101, result.equipments.head)
        assertEquals(102, result.equipments.body)
        assertEquals(103, result.equipments.right)
        assertEquals(104, result.equipments.left)
        assertEquals(105, result.equipments.accessory1)
        assertEquals(106, result.equipments.accessory2)
    }

    @Test
    fun `read should use zero active digievolution id when memory reader returns zero`() {
        val address = DigimonAddress(memoryBlockAddress = 0x800100, blastAddress = BLAST_ADDRESS)
        val statusAddresses = DigimonStatusAddresses(
            experience = 10,
            level = 14,
            hp = VitalAddresses(),
            mp = VitalAddresses(),
            attributes = AttributesAddresses(),
            elements = ElementsAddresses(),
            equipments = EquipmentsAddresses(),
            digievolutions = DigievolutionsAddresses(activeDigievolution = 200, slots = emptyList()),
        )
        val memoryReader = FakeMemoryReader().apply {
            blocks[0x800100L to 1500] = ByteArray(1500)
            int16[0x800100L + 200] = 0
            int16[BLAST_ADDRESS] = 0
        }

        val reader = DigimonReader(
            memoryReader,
            FakeDigievolutionSlotReader(),
            FakeStoredDigievolutionReader(),
            FakeInBattleReader(),
        )

        val result = reader.read(address, statusAddresses, InBattleAddresses(), 1)

        assertNotNull(result)
        assertEquals(0, result!!.activeDigievolutionId)
    }

    @Test
    fun `read should map evolution slots and stored digievolutions when slots are present`() {
        val address = DigimonAddress(memoryBlockAddress = 0x800100, blastAddress = BLAST_ADDRESS)
        val slotAddress1 = SlotAddresses(index = 0, address = 300)
        val slotAddress2 = SlotAddresses(index = 1, address = 304)
        val statusAddresses = DigimonStatusAddresses(
            experience = 10,
            level = 14,
            hp = VitalAddresses(),
            mp = VitalAddresses(),
            attributes = AttributesAddresses(),
            elements = ElementsAddresses(),
            equipments = EquipmentsAddresses(),
            digievolutions = DigievolutionsAddresses(
                activeDigievolution = 200,
                slots = listOf(slotAddress1, slotAddress2),
            ),
        )
        val memoryReader = FakeMemoryReader().apply {
            blocks[0x800100L to 1500] = ByteArray(1500)
            int16[0x800100L + 200] = 5
            int16[BLAST_ADDRESS] = 200
        }
        val slotResource1 = DigievolutionSlotResource(index = 0, digievolutionId = 12)
        val slotResource2 = DigievolutionSlotResource(index = 1, digievolutionId = 15)
        val slotReader = FakeDigievolutionSlotReader(
            mapOf(slotAddress1 to slotResource1, slotAddress2 to slotResource2),
        )
        val storedDigievolutions = listOf(
            StoredDigievolutionResource(digievolutionId = 12, level = 10, dvexp = 500),
            StoredDigievolutionResource(digievolutionId = 15, level = 25, dvexp = 1200),
            StoredDigievolutionResource(digievolutionId = 99, level = 5),
        )

        val reader = DigimonReader(
            memoryReader,
            slotReader,
            FakeStoredDigievolutionReader(storedDigievolutions),
            FakeInBattleReader(),
        )

        val result = reader.read(address, statusAddresses, InBattleAddresses(), 1)

        assertNotNull(result)
        result!!
        assertEquals(2, result.digievolutions.size)
        assertEquals(slotResource1, result.digievolutions[0])
        assertEquals(slotResource2, result.digievolutions[1])
        assertEquals(3, result.storedDigievolutions.size)
        assertEquals(1200, result.storedDigievolutions[1].dvexp)
        assertEquals(99, result.storedDigievolutions[2].digievolutionId)
        assertEquals(5, result.storedDigievolutions[2].level)
    }

    @Test
    fun `read should pass party slot index to in battle reader`() {
        val address = DigimonAddress(memoryBlockAddress = 0x800100, blastAddress = BLAST_ADDRESS)
        val statusAddresses = DigimonStatusAddresses(
            digievolutions = DigievolutionsAddresses(activeDigievolution = 200, slots = emptyList()),
        )
        val inBattleAddresses = InBattleAddresses()
        val inBattleResource = InBattleResource()
        val memoryReader = FakeMemoryReader().apply { blocks[0x800100L to 1500] = ByteArray(1500) }
        val inBattleReader = FakeInBattleReader(inBattleResource)

        val reader = DigimonReader(
            memoryReader,
            FakeDigievolutionSlotReader(),
            FakeStoredDigievolutionReader(),
            inBattleReader,
        )

        val result = reader.read(address, statusAddresses, inBattleAddresses, 2)

        assertNotNull(result)
        assertSame(inBattleResource, result!!.inBattle)
        assertEquals(1, inBattleReader.calls.size)
        assertSame(inBattleAddresses, inBattleReader.calls.single().first)
        assertEquals(2, inBattleReader.calls.single().second)
    }
}

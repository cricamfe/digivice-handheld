package dev.cricamfe.digivice.core.application.loaders

import dev.cricamfe.digivice.core.memory.FakeMemoryReader
import dev.cricamfe.digivice.core.memory.MemoryReadException
import dev.cricamfe.digivice.core.memory.readers.DigievolutionSlotReader
import dev.cricamfe.digivice.core.memory.readers.DigimonReader
import dev.cricamfe.digivice.core.memory.readers.DigimonSlotReader
import dev.cricamfe.digivice.core.memory.readers.InBattleReader
import dev.cricamfe.digivice.core.memory.readers.PartyReader
import dev.cricamfe.digivice.core.memory.readers.StoredDigievolutionReader
import dev.cricamfe.digivice.core.memory.readers.interfaces.IMemoryReader
import dev.cricamfe.digivice.core.memory.repositories.IAddressesRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PartyLoaderTests : LoaderIntegrationTestBase() {
    private companion object {
        const val EMPTY_SLOT_ID = 0xFF
    }

    @Test
    fun `load should integrate pipeline and skip empty slots`() {
        val addressesRepository = createAddressesRepository()

        // Party slot ids at 0x00048DA4 / 0x00048DA8 / 0x00048DAC; Kumamon (id 1) block at 0x00049878
        val memoryReader = FakeMemoryReader().apply {
            bytes[0x00048DA4L] = 1u
            bytes[0x00048DA8L] = EMPTY_SLOT_ID.toUByte()
            bytes[0x00048DACL] = EMPTY_SLOT_ID.toUByte()
        }

        // Offsets from DigimonStatusAddresses.json
        val fakeMemoryBlock = ByteArray(1500)
        writeInt32(fakeMemoryBlock, 0x18, 1500)
        writeInt16(fakeMemoryBlock, 0x1C, 12)
        writeInt16(fakeMemoryBlock, 0x20, 450)
        writeInt16(fakeMemoryBlock, 0x22, 500)
        writeInt16(fakeMemoryBlock, 0x28, 42)
        writeInt16(fakeMemoryBlock, 0x48, 5)
        writeInt16(fakeMemoryBlock, 0x4A, 10)
        writeInt16(fakeMemoryBlock, 0x50, 5)
        writeInt16(fakeMemoryBlock, 0x52, 3)

        memoryReader.blocks[0x00049878L to 1500] = fakeMemoryBlock
        memoryReader.int16[0x00049878L - 4] = 5
        memoryReader.int16[0x00042B76L] = 0

        val partyLoader = createPartyLoader(addressesRepository, memoryReader)

        val partyResource = partyLoader.load()

        assertEquals(3, partyResource.slotsResource.size)

        val slot0 = partyResource.slotsResource[0]
        assertEquals(1, slot0.index)
        val kumamon = slot0.digimonResource
        assertNotNull(kumamon)
        kumamon!!
        assertEquals(5, kumamon.activeDigievolutionId)
        assertEquals(1500, kumamon.experience)
        assertEquals(12, kumamon.level)
        assertEquals(450, kumamon.hp.current)
        assertEquals(500, kumamon.hp.max)
        assertEquals(42, kumamon.attributes.strength)

        assertEquals(3, kumamon.digievolutions.size)

        val evolutionSlot1 = kumamon.digievolutions[0]
        assertEquals(5, evolutionSlot1.digievolutionId)

        val evolutionSlot2 = kumamon.digievolutions[1]
        assertEquals(10, evolutionSlot2.digievolutionId)

        val storedDigievolution = kumamon.storedDigievolutions.single()
        assertEquals(5, storedDigievolution.digievolutionId)
        assertEquals(3, storedDigievolution.level)

        val slot1 = partyResource.slotsResource[1]
        assertEquals(2, slot1.index)
        assertEquals(EMPTY_SLOT_ID, slot1.digimonId)
        assertNull(slot1.digimonResource)

        val slot2 = partyResource.slotsResource[2]
        assertEquals(3, slot2.index)
        assertEquals(EMPTY_SLOT_ID, slot2.digimonId)
        assertNull(slot2.digimonResource)

        assertTrue(memoryReader.readBytesCalls.none { (address, _) -> address != 0x00049878L })
    }

    @Test
    fun `load should throw memory read exception when slot cannot be read`() {
        val addressesRepository = createAddressesRepository()
        val memoryReader = FakeMemoryReader().apply {
            byteFailures[0x00048DA4L] = MemoryReadException(0x00048DA4, "Memory session is not connected.")
            bytes[0x00048DA8L] = 2u
            bytes[0x00048DACL] = EMPTY_SLOT_ID.toUByte()
        }

        val partyLoader = createPartyLoader(addressesRepository, memoryReader)

        assertThrows(MemoryReadException::class.java) { partyLoader.load() }
    }

    @Test
    fun `load should load only occupied slots`() {
        val addressesRepository = createAddressesRepository()
        val memoryReader = FakeMemoryReader().apply {
            bytes[0x00048DA4L] = EMPTY_SLOT_ID.toUByte()
            bytes[0x00048DA8L] = 2u
            bytes[0x00048DACL] = EMPTY_SLOT_ID.toUByte()
        }

        // Monmon (id 2) block at 0x00049C54
        val fakeMemoryBlock = ByteArray(1500)
        writeInt16(fakeMemoryBlock, 0x1C, 8)

        memoryReader.blocks[0x00049C54L to 1500] = fakeMemoryBlock
        memoryReader.int16[0x00049C54L - 4] = 2
        memoryReader.int16[0x00042B78L] = 0

        val partyLoader = createPartyLoader(addressesRepository, memoryReader)

        val partyResource = partyLoader.load()

        assertEquals(3, partyResource.slotsResource.size)

        val slot0 = partyResource.slotsResource[0]
        assertEquals(EMPTY_SLOT_ID, slot0.digimonId)
        assertNull(slot0.digimonResource)

        val slot1 = partyResource.slotsResource[1]
        assertEquals(2, slot1.digimonId)
        assertNotNull(slot1.digimonResource)
        assertEquals(8, slot1.digimonResource!!.level)

        val slot2 = partyResource.slotsResource[2]
        assertEquals(EMPTY_SLOT_ID, slot2.digimonId)
        assertNull(slot2.digimonResource)
    }

    @Test
    fun `load should not load digimon resource when slot contains unknown digimon id`() {
        val addressesRepository = createAddressesRepository()
        val memoryReader = FakeMemoryReader().apply {
            bytes[0x00048DA4L] = 99u
            bytes[0x00048DA8L] = EMPTY_SLOT_ID.toUByte()
            bytes[0x00048DACL] = EMPTY_SLOT_ID.toUByte()
        }

        val partyLoader = createPartyLoader(addressesRepository, memoryReader)

        val partyResource = partyLoader.load()

        assertEquals(99, partyResource.slotsResource[0].digimonId)
        assertNull(partyResource.slotsResource[0].digimonResource)
        assertTrue(memoryReader.readBytesCalls.isEmpty())
    }

    @Test
    fun `load should not load digimon resource when later slot contains unknown digimon id`() {
        val addressesRepository = createAddressesRepository()
        val memoryReader = FakeMemoryReader().apply {
            bytes[0x00048DA4L] = 1u
            bytes[0x00048DA8L] = 99u
            bytes[0x00048DACL] = EMPTY_SLOT_ID.toUByte()
        }

        val fakeMemoryBlock = ByteArray(1500)
        writeInt16(fakeMemoryBlock, 28, 12)

        memoryReader.blocks[0x00049878L to 1500] = fakeMemoryBlock
        memoryReader.int16[0x00049878L - 4] = 5
        memoryReader.int16[0x00042B76L] = 0

        val partyLoader = createPartyLoader(addressesRepository, memoryReader)

        val partyResource = partyLoader.load()

        assertEquals(1, partyResource.slotsResource[0].digimonId)
        assertNotNull(partyResource.slotsResource[0].digimonResource)
        assertEquals(99, partyResource.slotsResource[1].digimonId)
        assertNull(partyResource.slotsResource[1].digimonResource)
        assertEquals(1, memoryReader.readBytesCalls.count { it == (0x00049878L to 1500) })
    }

    @Test
    fun `load should load kotemon when slot contains digimon id zero`() {
        val kotemonMemoryBlockAddress = 0x0004949CL
        val kotemonBlastAddress = 0x00042B74L

        val addressesRepository = createAddressesRepository()
        val memoryReader = FakeMemoryReader().apply {
            bytes[0x00048DA4L] = 0u
            bytes[0x00048DA8L] = EMPTY_SLOT_ID.toUByte()
            bytes[0x00048DACL] = EMPTY_SLOT_ID.toUByte()
        }

        val fakeMemoryBlock = ByteArray(1500)
        writeInt16(fakeMemoryBlock, 0x1C, 7)

        memoryReader.blocks[kotemonMemoryBlockAddress to 1500] = fakeMemoryBlock
        memoryReader.int16[kotemonBlastAddress] = 420

        val partyLoader = createPartyLoader(addressesRepository, memoryReader)

        val partyResource = partyLoader.load()

        val kotemonSlot = partyResource.slotsResource[0]
        assertEquals(0, kotemonSlot.digimonId)
        val kotemon = kotemonSlot.digimonResource
        assertNotNull(kotemon)
        kotemon!!
        assertEquals(7, kotemon.level)
        assertEquals(420, kotemon.blast)
    }

    @Test
    fun `load should return all empty slots when all slots are empty`() {
        val addressesRepository = createAddressesRepository()
        val memoryReader = FakeMemoryReader().apply {
            bytes[0x00048DA4L] = EMPTY_SLOT_ID.toUByte()
            bytes[0x00048DA8L] = EMPTY_SLOT_ID.toUByte()
            bytes[0x00048DACL] = EMPTY_SLOT_ID.toUByte()
        }

        val partyLoader = createPartyLoader(addressesRepository, memoryReader)

        val partyResource = partyLoader.load()

        assertEquals(3, partyResource.slotsResource.size)
        for (slot in partyResource.slotsResource) {
            assertEquals(EMPTY_SLOT_ID, slot.digimonId)
            assertNull(slot.digimonResource)
        }

        assertTrue(memoryReader.readBytesCalls.isEmpty())
    }

    @Test
    fun `load should keep persistent vitals and populate in battle`() {
        val addressesRepository = createAddressesRepository()
        val memoryReader = FakeMemoryReader().apply {
            bytes[0x00048DA4L] = 1u
            bytes[0x00048DA8L] = EMPTY_SLOT_ID.toUByte()
            bytes[0x00048DACL] = EMPTY_SLOT_ID.toUByte()
        }

        val fakeMemoryBlock = ByteArray(1500)
        writeInt16(fakeMemoryBlock, 0x20, 450)
        writeInt16(fakeMemoryBlock, 0x22, 500)
        writeInt16(fakeMemoryBlock, 0x24, 200)
        writeInt16(fakeMemoryBlock, 0x26, 300)

        memoryReader.blocks[0x00049878L to 1500] = fakeMemoryBlock
        memoryReader.int16[0x00049878L - 4] = 5
        memoryReader.int16[0x00042B76L] = 0

        memoryReader.int16[0x000A4470L + 0x06] = 1850
        memoryReader.int16[0x000A4470L + 0x08] = 1400
        memoryReader.int16[0x000A4470L + 0x0A] = 1140
        memoryReader.int16[0x000A4470L + 0x0C] = 900
        memoryReader.int16[0x000A4470L + 0x10] = 252
        memoryReader.int16[0x000A4470L + 0x12] = 185
        memoryReader.int16[0x000A4470L + 0x14] = 84
        memoryReader.bytes[0x000A4470L + 0x1C] = 0x04u

        val partyLoader = createPartyLoader(addressesRepository, memoryReader)
        val partyResource = partyLoader.load()

        assertNotNull(partyResource.slotsResource[0].digimonResource)
        val digimon = partyResource.slotsResource[0].digimonResource!!
        assertEquals(450, digimon.hp.current)
        assertEquals(500, digimon.hp.max)
        assertEquals(200, digimon.mp.current)
        assertEquals(300, digimon.mp.max)
        assertEquals(1400, digimon.inBattle.hp.current)
        assertEquals(1850, digimon.inBattle.hp.max)
        assertEquals(900, digimon.inBattle.mp.current)
        assertEquals(1140, digimon.inBattle.mp.max)
        assertEquals(0x04, digimon.inBattle.condition)
        assertEquals(252, digimon.inBattle.strength)
        assertEquals(185, digimon.inBattle.defense)
        assertEquals(84, digimon.inBattle.speed)
    }

    private fun createPartyLoader(addressesRepository: IAddressesRepository, memoryReader: IMemoryReader): PartyLoader {
        val digimonReader = DigimonReader(
            memoryReader,
            DigievolutionSlotReader(),
            StoredDigievolutionReader(),
            InBattleReader(memoryReader),
        )
        val partyReader = PartyReader(DigimonSlotReader(memoryReader))
        val digimonLoader = DigimonLoader(addressesRepository, digimonReader)
        return PartyLoader(addressesRepository, partyReader, digimonLoader)
    }
}

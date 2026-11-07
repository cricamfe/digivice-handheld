package dev.cricamfe.digivice.core.application.loaders

import dev.cricamfe.digivice.core.memory.FakeMemoryReader
import dev.cricamfe.digivice.core.memory.MemoryReadException
import dev.cricamfe.digivice.core.memory.readers.DigievolutionSlotReader
import dev.cricamfe.digivice.core.memory.readers.DigimonReader
import dev.cricamfe.digivice.core.memory.readers.InBattleReader
import dev.cricamfe.digivice.core.memory.readers.StoredDigievolutionReader
import dev.cricamfe.digivice.core.memory.readers.interfaces.IMemoryReader
import dev.cricamfe.digivice.core.memory.repositories.IAddressesRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DigimonLoaderTests : LoaderIntegrationTestBase() {
    @Test
    fun `load should integrate digimon addresses status and reader pipeline`() {
        val addressesRepository = createAddressesRepository()
        val memoryBlock = ByteArray(1500)

        writeInt32(memoryBlock, 0x18, 85000)
        writeInt16(memoryBlock, 0x1C, 45)

        writeInt16(memoryBlock, 0x20, 500)
        writeInt16(memoryBlock, 0x22, 1000)
        writeInt16(memoryBlock, 0x24, 200)
        writeInt16(memoryBlock, 0x26, 400)

        writeInt16(memoryBlock, 0x28, 80)
        writeInt16(memoryBlock, 0x2A, 75)
        writeInt16(memoryBlock, 0x2C, 60)
        writeInt16(memoryBlock, 0x2E, 95)
        writeInt16(memoryBlock, 0x30, 110)
        writeInt16(memoryBlock, 0x32, 40)

        writeInt16(memoryBlock, 0x34, 10)
        writeInt16(memoryBlock, 0x36, 20)
        writeInt16(memoryBlock, 0x38, 30)
        writeInt16(memoryBlock, 0x3A, 40)
        writeInt16(memoryBlock, 0x3C, 50)
        writeInt16(memoryBlock, 0x3E, 60)
        writeInt16(memoryBlock, 0x40, 70)

        writeInt16(memoryBlock, 0x3C0, 101)
        writeInt16(memoryBlock, 0x3C2, 102)
        writeInt16(memoryBlock, 0x3C4, 103)
        writeInt16(memoryBlock, 0x3C6, 104)
        writeInt16(memoryBlock, 0x3C8, 105)
        writeInt16(memoryBlock, 0x3CA, 106)

        writeInt16(memoryBlock, 0x48, 5)
        writeInt16(memoryBlock, 0x4A, 10)
        writeInt16(memoryBlock, 0x4C, 0)
        writeInt16(memoryBlock, 0x50, 5)
        writeInt16(memoryBlock, 0x52, 3)

        val memoryReader = FakeMemoryReader().apply {
            blocks[0x00049878L to 1500] = memoryBlock
            int16[0x00049878L - 4] = 5
            int16[0x00042B76L] = 400
        }

        val digimonLoader = createDigimonLoader(addressesRepository, memoryReader)

        val digimonResource = digimonLoader.load(1, 1)

        assertNotNull(digimonResource)
        digimonResource!!
        assertEquals(5, digimonResource.activeDigievolutionId)
        assertEquals(85000, digimonResource.experience)
        assertEquals(45, digimonResource.level)
        assertEquals(400, digimonResource.blast)
        assertEquals(500, digimonResource.hp.current)
        assertEquals(1000, digimonResource.hp.max)
        assertEquals(200, digimonResource.mp.current)
        assertEquals(400, digimonResource.mp.max)
        assertEquals(80, digimonResource.attributes.strength)
        assertEquals(75, digimonResource.attributes.defense)
        assertEquals(60, digimonResource.attributes.spirit)
        assertEquals(95, digimonResource.attributes.wisdom)
        assertEquals(110, digimonResource.attributes.speed)
        assertEquals(40, digimonResource.attributes.charisma)
        assertEquals(10, digimonResource.elements.fire)
        assertEquals(20, digimonResource.elements.water)
        assertEquals(30, digimonResource.elements.ice)
        assertEquals(40, digimonResource.elements.wind)
        assertEquals(50, digimonResource.elements.thunder)
        assertEquals(60, digimonResource.elements.machine)
        assertEquals(70, digimonResource.elements.dark)
        assertEquals(101, digimonResource.equipments.head)
        assertEquals(102, digimonResource.equipments.body)
        assertEquals(103, digimonResource.equipments.right)
        assertEquals(104, digimonResource.equipments.left)
        assertEquals(105, digimonResource.equipments.accessory1)
        assertEquals(106, digimonResource.equipments.accessory2)
        assertEquals(3, digimonResource.digievolutions.size)
        assertEquals(5, digimonResource.digievolutions[0].digievolutionId)
        assertEquals(10, digimonResource.digievolutions[1].digievolutionId)
        assertEquals(0, digimonResource.digievolutions[2].digievolutionId)
        val storedDigievolution = digimonResource.storedDigievolutions.single()
        assertEquals(5, storedDigievolution.digievolutionId)
        assertEquals(3, storedDigievolution.level)
    }

    @Test
    fun `load should integrate another valid digimon address`() {
        val addressesRepository = createAddressesRepository()
        val memoryBlock = ByteArray(1500)
        writeInt32(memoryBlock, 0x18, 12000)
        writeInt16(memoryBlock, 0x1C, 18)

        val memoryReader = FakeMemoryReader().apply {
            blocks[0x0004AFA0L to 1500] = memoryBlock
            int16[0x0004AFA0L - 4] = 7
            int16[0x00042B82L] = 999
        }

        val digimonLoader = createDigimonLoader(addressesRepository, memoryReader)

        val digimonResource = digimonLoader.load(7, 1)

        assertNotNull(digimonResource)
        digimonResource!!
        assertEquals(7, digimonResource.activeDigievolutionId)
        assertEquals(12000, digimonResource.experience)
        assertEquals(18, digimonResource.level)
        assertEquals(999, digimonResource.blast)
    }

    @Test
    fun `load should throw memory read exception when digimon memory block cannot be read`() {
        val addressesRepository = createAddressesRepository()
        val memoryReader = FakeMemoryReader().apply {
            blockFailures[0x00049878L to 1500] = MemoryReadException(0x00049878, "Memory session is not connected.")
        }

        val digimonLoader = createDigimonLoader(addressesRepository, memoryReader)

        assertThrows(MemoryReadException::class.java) { digimonLoader.load(1, 1) }
    }

    @Test
    fun `load should return null when digimon memory block is too short`() {
        val addressesRepository = createAddressesRepository()
        val memoryReader = FakeMemoryReader().apply { blocks[0x00049878L to 1500] = ByteArray(1499) }

        val digimonLoader = createDigimonLoader(addressesRepository, memoryReader)

        val digimonResource = digimonLoader.load(1, 1)

        assertNull(digimonResource)
    }

    @Test
    fun `load should return null when digimon id is not registered`() {
        val addressesRepository = createAddressesRepository()
        val memoryReader = FakeMemoryReader()
        val digimonLoader = createDigimonLoader(addressesRepository, memoryReader)

        val digimonResource = digimonLoader.load(99, 1)

        assertNull(digimonResource)
        assertTrue(memoryReader.readBytesCalls.isEmpty())
    }

    private fun createDigimonLoader(addressesRepository: IAddressesRepository, memoryReader: IMemoryReader): DigimonLoader {
        val digimonReader = DigimonReader(
            memoryReader,
            DigievolutionSlotReader(),
            StoredDigievolutionReader(),
            InBattleReader(memoryReader),
        )
        return DigimonLoader(addressesRepository, digimonReader)
    }
}

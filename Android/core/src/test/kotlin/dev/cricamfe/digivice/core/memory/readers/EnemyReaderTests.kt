package dev.cricamfe.digivice.core.memory.readers

import dev.cricamfe.digivice.core.memory.FakeMemoryReader
import dev.cricamfe.digivice.core.memory.addresses.EnemyAddresses
import dev.cricamfe.digivice.core.memory.addresses.parties.digimons.VitalAddresses
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EnemyReaderTests {
    private companion object {
        const val ENEMY_SLOT_BASE = 0x000A44D0L
        const val ACTIVE_ENEMY_SLOT_INDEX_ADDRESS = 0x000A446CL
        const val GROUP_ID_ADDRESS = 0x00042B2CL
        const val SLOT_STRIDE = 0x20
    }

    @Test
    fun `read should read slot zero when only first slot has live enemy`() {
        val memoryReader = createMemoryReader()
        setupEmptyEnemySlot(memoryReader, slotIndex = 1)
        setupEmptyEnemySlot(memoryReader, slotIndex = 2)
        setupEnemySlot(memoryReader, slotIndex = 0, id = 122, maxHp = 672, currentHp = 600, condition = 0x01, speed = 84)
        memoryReader.int16[ACTIVE_ENEMY_SLOT_INDEX_ADDRESS] = 0
        memoryReader.int16[GROUP_ID_ADDRESS] = 201

        val result = EnemyReader(memoryReader).read(createAddresses())

        assertEquals(122, result.id)
        assertEquals(201, result.groupId)
        assertEquals(600, result.hp.current)
        assertEquals(672, result.hp.max)
        assertEquals(0x01, result.condition)
        assertEquals(84, result.speed)
    }

    @Test
    fun `read should read second slot when first slot is ko`() {
        val memoryReader = createMemoryReader()
        setupEnemySlot(memoryReader, slotIndex = 0, id = 100, maxHp = 500, currentHp = 0, condition = 0, speed = 0)
        setupEnemySlot(memoryReader, slotIndex = 1, id = 200, maxHp = 800, currentHp = 400, condition = 0, speed = 90)
        setupEmptyEnemySlot(memoryReader, slotIndex = 2)

        val result = EnemyReader(memoryReader).read(createAddresses())

        assertEquals(200, result.id)
        assertEquals(400, result.hp.current)
        assertEquals(800, result.hp.max)
        assertEquals(90, result.speed)
    }

    @Test
    fun `read should prefer active enemy slot index when multiple live enemy slots exist`() {
        val memoryReader = createMemoryReader()
        setupEnemySlot(memoryReader, slotIndex = 0, id = 100, maxHp = 500, currentHp = 300, condition = 0, speed = 70)
        setupEnemySlot(memoryReader, slotIndex = 1, id = 200, maxHp = 800, currentHp = 400, condition = 0, speed = 90)
        setupEmptyEnemySlot(memoryReader, slotIndex = 2)
        memoryReader.int16[ACTIVE_ENEMY_SLOT_INDEX_ADDRESS] = 1

        val result = EnemyReader(memoryReader).read(createAddresses())

        assertEquals(200, result.id)
        assertEquals(400, result.hp.current)
    }

    @Test
    fun `read should read last occupied slot when all enemy slots are ko`() {
        val memoryReader = createMemoryReader()
        setupEnemySlot(memoryReader, slotIndex = 0, id = 100, maxHp = 500, currentHp = 0, condition = 0, speed = 70)
        setupEnemySlot(memoryReader, slotIndex = 1, id = 200, maxHp = 800, currentHp = 0, condition = 0, speed = 90)
        setupEmptyEnemySlot(memoryReader, slotIndex = 2)

        val result = EnemyReader(memoryReader).read(createAddresses())

        assertEquals(200, result.id)
        assertEquals(0, result.hp.current)
    }

    @Test
    fun `read should stay on ko enemy when active enemy slot index still points to them`() {
        val memoryReader = createMemoryReader()
        setupEnemySlot(memoryReader, slotIndex = 0, id = 100, maxHp = 500, currentHp = 0, condition = 0, speed = 0)
        setupEnemySlot(memoryReader, slotIndex = 1, id = 200, maxHp = 800, currentHp = 400, condition = 0, speed = 90)
        setupEmptyEnemySlot(memoryReader, slotIndex = 2)
        memoryReader.int16[ACTIVE_ENEMY_SLOT_INDEX_ADDRESS] = 0

        val result = EnemyReader(memoryReader).read(createAddresses())

        assertEquals(100, result.id)
        assertEquals(0, result.hp.current)
    }

    @Test
    fun `read should read first live slot when active enemy slot index points to empty slot`() {
        val memoryReader = createMemoryReader()
        setupEnemySlot(memoryReader, slotIndex = 0, id = 100, maxHp = 500, currentHp = 0, condition = 0, speed = 0)
        setupEnemySlot(memoryReader, slotIndex = 1, id = 200, maxHp = 800, currentHp = 400, condition = 0, speed = 90)
        setupEmptyEnemySlot(memoryReader, slotIndex = 2)
        memoryReader.int16[ACTIVE_ENEMY_SLOT_INDEX_ADDRESS] = 2

        val result = EnemyReader(memoryReader).read(createAddresses())

        assertEquals(200, result.id)
        assertEquals(400, result.hp.current)
    }

    @Test
    fun `read should read first live slot when active enemy slot index is out of range`() {
        val memoryReader = createMemoryReader()
        setupEnemySlot(memoryReader, slotIndex = 0, id = 100, maxHp = 500, currentHp = 0, condition = 0, speed = 0)
        setupEnemySlot(memoryReader, slotIndex = 1, id = 200, maxHp = 800, currentHp = 400, condition = 0, speed = 90)
        setupEmptyEnemySlot(memoryReader, slotIndex = 2)
        memoryReader.int16[ACTIVE_ENEMY_SLOT_INDEX_ADDRESS] = 3

        val result = EnemyReader(memoryReader).read(createAddresses())

        assertEquals(200, result.id)
        assertEquals(400, result.hp.current)
    }

    @Test
    fun `read should read third slot when all three enemies are ko`() {
        val memoryReader = createMemoryReader()
        setupEnemySlot(memoryReader, slotIndex = 0, id = 100, maxHp = 500, currentHp = 0, condition = 0, speed = 70)
        setupEnemySlot(memoryReader, slotIndex = 1, id = 200, maxHp = 800, currentHp = 0, condition = 0, speed = 90)
        setupEnemySlot(memoryReader, slotIndex = 2, id = 300, maxHp = 600, currentHp = 0, condition = 0, speed = 80)

        val result = EnemyReader(memoryReader).read(createAddresses())

        assertEquals(300, result.id)
        assertEquals(0, result.hp.current)
    }

    @Test
    fun `read should read third slot when active enemy slot index points to front and prior enemies are alive`() {
        val memoryReader = createMemoryReader()
        setupEnemySlot(memoryReader, slotIndex = 0, id = 197, maxHp = 528, currentHp = 0, condition = 0, speed = 0)
        setupEnemySlot(memoryReader, slotIndex = 1, id = 132, maxHp = 528, currentHp = 222, condition = 0, speed = 0)
        setupEnemySlot(memoryReader, slotIndex = 2, id = 110, maxHp = 552, currentHp = 552, condition = 0, speed = 0)
        memoryReader.int16[ACTIVE_ENEMY_SLOT_INDEX_ADDRESS] = 2

        val result = EnemyReader(memoryReader).read(createAddresses())

        assertEquals(110, result.id)
        assertEquals(552, result.hp.current)
        assertEquals(552, result.hp.max)
    }

    @Test
    fun `read should return enemy with id zero when all enemy slots are empty`() {
        val memoryReader = createMemoryReader()
        setupEmptyEnemySlot(memoryReader, slotIndex = 0)
        setupEmptyEnemySlot(memoryReader, slotIndex = 1)
        setupEmptyEnemySlot(memoryReader, slotIndex = 2)

        val result = EnemyReader(memoryReader).read(createAddresses())

        assertEquals(0, result.id)
        assertEquals(0, result.hp.current)
        assertEquals(0, result.hp.max)
    }

    @Test
    fun `read should sign extend negative hp and keep condition unsigned`() {
        val memoryReader = createMemoryReader()
        setupEnemySlot(memoryReader, slotIndex = 0, id = 100, maxHp = 500, currentHp = -5, condition = 0xFF, speed = 0)
        setupEmptyEnemySlot(memoryReader, slotIndex = 1)
        setupEmptyEnemySlot(memoryReader, slotIndex = 2)

        val result = EnemyReader(memoryReader).read(createAddresses())

        // Slot 0 is live only through the "last occupied slot" fallback because hp <= 0.
        assertEquals(-5, result.hp.current)
        assertEquals(255, result.condition)
    }

    private fun createMemoryReader() = FakeMemoryReader().apply {
        int16[GROUP_ID_ADDRESS] = 0
        int16[ACTIVE_ENEMY_SLOT_INDEX_ADDRESS] = -1
    }

    private fun createAddresses() = EnemyAddresses(
        enemySlotBase = ENEMY_SLOT_BASE,
        slotStride = SLOT_STRIDE,
        slotCount = 3,
        activeEnemySlotIndex = ACTIVE_ENEMY_SLOT_INDEX_ADDRESS,
        groupId = GROUP_ID_ADDRESS,
        id = 0x00,
        condition = 0x1C,
        strength = 0x10,
        defense = 0x12,
        speed = 0x14,
        hp = VitalAddresses(max = 0x06, current = 0x08),
    )

    private fun getSlotBase(slotIndex: Int): Long = ENEMY_SLOT_BASE + (slotIndex * SLOT_STRIDE)

    private fun setupEmptyEnemySlot(memoryReader: FakeMemoryReader, slotIndex: Int) =
        setupEnemySlot(memoryReader, slotIndex, id = 0, maxHp = 0, currentHp = 0, condition = 0, speed = 0)

    private fun setupEnemySlot(
        memoryReader: FakeMemoryReader,
        slotIndex: Int,
        id: Short,
        maxHp: Short,
        currentHp: Short,
        condition: Int,
        speed: Short,
    ) {
        val slotBase = getSlotBase(slotIndex)
        memoryReader.int16[slotBase + 0x00] = id
        memoryReader.int16[slotBase + 0x06] = maxHp
        memoryReader.int16[slotBase + 0x08] = currentHp
        memoryReader.int16[slotBase + 0x10] = 0
        memoryReader.int16[slotBase + 0x12] = 0
        memoryReader.int16[slotBase + 0x14] = speed
        memoryReader.bytes[slotBase + 0x1C] = condition.toUByte()
    }
}

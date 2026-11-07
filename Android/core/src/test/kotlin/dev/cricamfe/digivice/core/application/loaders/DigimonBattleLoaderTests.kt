package dev.cricamfe.digivice.core.application.loaders

import dev.cricamfe.digivice.core.memory.FakeMemoryReader
import dev.cricamfe.digivice.core.memory.readers.DigimonBattleReader
import dev.cricamfe.digivice.core.memory.readers.EnemyReader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DigimonBattleLoaderTests : LoaderIntegrationTestBase() {
    private companion object {
        const val ENEMY_SLOT_BASE = 0x000A44D0L
        const val ACTIVE_ENEMY_SLOT_INDEX_ADDRESS = 0x000A446CL
        const val SLOT_STRIDE = 0x20
    }

    @Test
    fun `load should read enemy from enemy addresses`() {
        val addressesRepository = createAddressesRepository()
        val memoryReader = FakeMemoryReader()

        setupEmptyEnemySlot(memoryReader, slotIndex = 1)
        setupEmptyEnemySlot(memoryReader, slotIndex = 2)
        setupEnemySlot(memoryReader, slotIndex = 0, id = 122, maxHp = 672, currentHp = 600, condition = 0x01, speed = 84)
        memoryReader.int16[ACTIVE_ENEMY_SLOT_INDEX_ADDRESS] = 0
        memoryReader.bytes[0x000A4530L] = 0x02u

        val digimonBattleReader = DigimonBattleReader(memoryReader, EnemyReader(memoryReader))
        val loader = DigimonBattleLoader(addressesRepository, digimonBattleReader)
        val resource = loader.load()

        assertEquals(0x02.toUByte(), resource.field)
        assertEquals(122, resource.enemy.id)
        assertEquals(0x01, resource.enemy.condition)
        assertEquals(84, resource.enemy.speed)
        assertEquals(600, resource.enemy.hp.current)
        assertEquals(672, resource.enemy.hp.max)
    }

    @Test
    fun `load should return enemy with id zero when out of battle`() {
        val addressesRepository = createAddressesRepository()
        val memoryReader = FakeMemoryReader()

        setupEmptyEnemySlot(memoryReader, slotIndex = 0)
        setupEmptyEnemySlot(memoryReader, slotIndex = 1)
        setupEmptyEnemySlot(memoryReader, slotIndex = 2)
        memoryReader.int16[ACTIVE_ENEMY_SLOT_INDEX_ADDRESS] = -1
        memoryReader.bytes[0x000A4530L] = 0x00u

        val digimonBattleReader = DigimonBattleReader(memoryReader, EnemyReader(memoryReader))
        val loader = DigimonBattleLoader(addressesRepository, digimonBattleReader)
        val resource = loader.load()

        assertEquals(0.toUByte(), resource.field)
        assertEquals(0, resource.enemy.id)
    }

    private fun getSlotBase(slotIndex: Int): Long = ENEMY_SLOT_BASE + slotIndex * SLOT_STRIDE

    private fun setupEmptyEnemySlot(memoryReader: FakeMemoryReader, slotIndex: Int) {
        setupEnemySlot(memoryReader, slotIndex, id = 0, maxHp = 0, currentHp = 0, condition = 0, speed = 0)
    }

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

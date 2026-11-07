package dev.cricamfe.digivice.core.memory.readers

import dev.cricamfe.digivice.core.memory.FakeEnemyReader
import dev.cricamfe.digivice.core.memory.FakeMemoryReader
import dev.cricamfe.digivice.core.memory.addresses.DigimonBattleAddresses
import dev.cricamfe.digivice.core.memory.addresses.EnemyAddresses
import dev.cricamfe.digivice.core.memory.resources.battles.EnemyResource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class DigimonBattleReaderTests {
    @Test
    fun `read should map field and delegate enemy`() {
        val memoryReader = FakeMemoryReader().apply { bytes[0x000A4530] = 0x02u }
        val enemyResource = EnemyResource(id = 122, groupId = 201)
        val enemyReader = FakeEnemyReader(enemyResource)
        val enemyAddresses = EnemyAddresses()
        val digimonBattleAddresses = DigimonBattleAddresses(field = 0x000A4530)

        val result = DigimonBattleReader(memoryReader, enemyReader).read(digimonBattleAddresses, enemyAddresses)

        assertEquals(0x02.toUByte(), result.field)
        assertSame(enemyResource, result.enemy)
        assertEquals(1, enemyReader.calls.size)
        assertSame(enemyAddresses, enemyReader.calls.single())
    }

    @Test
    fun `read should take the field's boost and the active fighter`() {
        val memoryReader = FakeMemoryReader().apply {
            bytes[0x000A4530] = 0x02u
            int16[0x000A4532] = 127          // the most a boost gives: 127 of 128ths
            int32[0x000A4468] = 2            // the third fighter of the party
            int32[0x00042B28] = 15           // the stage the battle started on
        }
        val addresses = DigimonBattleAddresses(
            field = 0x000A4530,
            fieldAmount = 0x000A4532,
            activeFighter = 0x000A4468,
            stage = 0x00042B28,
        )

        val result = DigimonBattleReader(memoryReader, FakeEnemyReader(EnemyResource())).read(addresses, EnemyAddresses())

        assertEquals(127, result.fieldAmount)
        assertEquals(2, result.activeFighter)
        assertEquals(15, result.stage)
    }
}

package dev.cricamfe.digivice.core.events.converters

import dev.cricamfe.digivice.core.domain.models.DigimonBattle
import dev.cricamfe.digivice.core.domain.models.battles.Enemy
import dev.cricamfe.digivice.core.domain.models.parties.digimons.Vital
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Port of `Tests/Unit/Events/Converters/DigimonBattleConverterTests.cs`. */
class DigimonBattleConverterTests {
    @Test
    fun `toDto maps all digimon battle fields`() {
        val digimonBattle = DigimonBattle(
            field = 0x02u,
            enemy = Enemy(
                id = 122,
                groupId = 201,
                condition = 0x01,
                strength = 0,
                defense = 0,
                speed = 84,
                hp = Vital(current = 600, max = 672),
            ),
        )

        val dto = DigimonBattleConverter.toDto(digimonBattle)

        assertTrue(dto.field.hasValue)
        assertEquals(0x02.toUByte(), dto.field.value)
        assertTrue(dto.enemy.hasValue)
        assertEquals(201, dto.enemy.value!!.groupId.value)
        assertEquals(122, dto.enemy.value!!.id.value)
    }

    @Test
    fun `toDto sends an explicit null enemy when the enemy is null`() {
        val dto = DigimonBattleConverter.toDto(DigimonBattle(field = 0x00u, enemy = null))

        assertTrue(dto.enemy.hasValue)
        assertNull(dto.enemy.value)
    }
}

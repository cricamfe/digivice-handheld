package dev.cricamfe.digivice.core.domain.assemblers

import dev.cricamfe.digivice.core.memory.resources.DigimonBattleResource
import dev.cricamfe.digivice.core.memory.resources.battles.EnemyResource
import dev.cricamfe.digivice.core.memory.resources.parties.digimons.VitalResource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class DigimonBattleAssemblerTests {
    @Test
    fun `assemble should map enemy fields`() {
        val resource = DigimonBattleResource(
            field = 0x02u,
            enemy = EnemyResource(
                id = 122,
                groupId = 201,
                condition = 0x01,
                strength = 95,
                defense = 63,
                speed = 84,
                hp = VitalResource(current = 600, max = 672),
            ),
        )

        val result = DigimonBattleAssembler.assemble(resource)

        assertEquals(0x02.toUByte(), result.field)
        val enemy = result.enemy
        assertNotNull(enemy)
        enemy!!
        assertEquals(201, enemy.groupId)
        assertEquals(122, enemy.id)
        assertEquals(0x01, enemy.condition)
        assertEquals(95, enemy.strength)
        assertEquals(63, enemy.defense)
        assertEquals(84, enemy.speed)
        assertEquals(600, enemy.hp.current)
        assertEquals(672, enemy.hp.max)
    }

    @ParameterizedTest
    @ValueSource(ints = [0, -1])
    fun `assemble should return null enemy when enemy id is not positive`(rawEnemyId: Int) {
        val resource = DigimonBattleResource(
            field = 0x00u,
            enemy = EnemyResource(
                id = rawEnemyId,
                groupId = 201,
                speed = 84,
                hp = VitalResource(current = 600, max = 672),
            ),
        )

        val result = DigimonBattleAssembler.assemble(resource)

        assertEquals(0x00.toUByte(), result.field)
        assertNull(result.enemy)
    }
}

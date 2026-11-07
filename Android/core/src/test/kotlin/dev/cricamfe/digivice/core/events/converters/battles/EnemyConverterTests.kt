package dev.cricamfe.digivice.core.events.converters.battles

import dev.cricamfe.digivice.core.domain.models.battles.Enemy
import dev.cricamfe.digivice.core.domain.models.parties.digimons.Vital
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Port of `Tests/Unit/Events/Converters/Battles/EnemyConverterTests.cs`. */
class EnemyConverterTests {
    @Test
    fun `toDto maps every field`() {
        val enemy = Enemy(
            id = 122,
            groupId = 201,
            condition = 0x01,
            strength = 95,
            defense = 63,
            speed = 84,
            hp = Vital(current = 600, max = 672),
        )

        val result = EnemyConverter.toDto(enemy)

        assertEquals(122, result.id.value)
        assertEquals(201, result.groupId.value)
        assertEquals(0x01, result.condition.value)
        assertEquals(95, result.strength.value)
        assertEquals(63, result.defense.value)
        assertEquals(84, result.speed.value)
        assertEquals(600, result.hp.value.current.value)
        assertEquals(672, result.hp.value.max.value)
    }

    @Test
    fun `toDto keeps zero values present`() {
        val result = EnemyConverter.toDto(Enemy(id = 122))

        assertTrue(result.condition.hasValue)
        assertEquals(0, result.condition.value)
        assertTrue(result.strength.hasValue)
        assertTrue(result.defense.hasValue)
        assertTrue(result.hp.value.current.hasValue)
    }
}

package dev.cricamfe.digivice.core.events.diffing.battles

import dev.cricamfe.digivice.core.domain.models.battles.Enemy
import dev.cricamfe.digivice.core.domain.models.parties.digimons.Vital
import dev.cricamfe.digivice.core.events.assertAbsent
import dev.cricamfe.digivice.core.events.assertPresent
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/** Port of `Tests/Unit/Events/Diffing/Battles/EnemyDifferTests.cs`. */
class EnemyDifferTests {
    private fun baseEnemy() = Enemy(
        id = 122,
        groupId = 201,
        condition = 0,
        strength = 0,
        defense = 0,
        speed = 0,
        hp = Vital(current = 672, max = 672),
    )

    @Test
    fun `diff returns null when there are no changes`() {
        assertNull(EnemyDiffer.diff(baseEnemy(), baseEnemy()))
    }

    @Test
    fun `diff returns the full dto when the previous is null`() {
        val new = baseEnemy().also { it.speed = 84 }

        val result = EnemyDiffer.diff(null, new)

        assertNotNull(result)
        assertPresent(122, result!!.id)
        assertPresent(84, result.speed)
        assertPresent(672, result.hp.value.current)
    }

    @Test
    fun `diff returns only the speed when only the speed changes`() {
        val new = baseEnemy().also { it.speed = 84 }

        val result = EnemyDiffer.diff(baseEnemy(), new)!!

        assertPresent(84, result.speed)
        assertAbsent(result.id)
        assertAbsent(result.groupId)
        assertAbsent(result.condition)
        assertAbsent(result.strength)
        assertAbsent(result.defense)
        assertAbsent(result.hp)
    }

    @Test
    fun `diff returns the group id delta when only the group id changes`() {
        val new = baseEnemy().also { it.groupId = 272 }

        val result = EnemyDiffer.diff(baseEnemy(), new)!!

        assertPresent(272, result.groupId)
        assertAbsent(result.id)
        assertAbsent(result.speed)
    }

    @Test
    fun `diff returns the hp delta when only the hp changes`() {
        val new = baseEnemy().also { it.hp.current = 500 }

        val result = EnemyDiffer.diff(baseEnemy(), new)!!

        assertPresent(500, result.hp.value.current)
        assertAbsent(result.hp.value.max)
        assertAbsent(result.speed)
    }
}

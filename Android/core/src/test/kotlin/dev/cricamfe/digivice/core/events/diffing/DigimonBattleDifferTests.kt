package dev.cricamfe.digivice.core.events.diffing

import dev.cricamfe.digivice.core.domain.models.DigimonBattle
import dev.cricamfe.digivice.core.domain.models.battles.Enemy
import dev.cricamfe.digivice.core.domain.models.parties.digimons.Vital
import dev.cricamfe.digivice.core.events.assertAbsent
import dev.cricamfe.digivice.core.events.assertPresent
import dev.cricamfe.digivice.core.events.presentValue
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Port of `Tests/Unit/Events/Diffing/DigimonBattleDifferTests.cs`. */
class DigimonBattleDifferTests {
    private fun baseDigimonBattle() = DigimonBattle(
        field = 0x00u,
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

    @Test
    fun `diff returns an empty dto when there are no changes`() {
        val result = DigimonBattleDiffer.diff(baseDigimonBattle(), baseDigimonBattle())

        assertAbsent(result.field)
        assertAbsent(result.enemy)
    }

    @Test
    fun `diff returns the full dto when the previous is null`() {
        val new = baseDigimonBattle().also { it.field = 0x02u }

        val result = DigimonBattleDiffer.diff(null, new)

        assertPresent(0x02.toUByte(), result.field)
        val enemy = presentValue(result.enemy)
        assertPresent(122, enemy.id)
        assertPresent(201, enemy.groupId)
    }

    @Test
    fun `diff returns the field delta when only the field changed`() {
        val new = baseDigimonBattle().also { it.field = 0x03u }

        val result = DigimonBattleDiffer.diff(baseDigimonBattle(), new)

        assertPresent(0x03.toUByte(), result.field)
        assertAbsent(result.enemy)
    }

    @Test
    fun `diff returns both deltas when the field and the enemy changed`() {
        val new = baseDigimonBattle().also {
            it.field = 0x04u
            it.enemy!!.speed = 90
        }

        val result = DigimonBattleDiffer.diff(baseDigimonBattle(), new)

        assertPresent(0x04.toUByte(), result.field)
        assertPresent(90, presentValue(result.enemy).speed)
    }

    @Test
    fun `diff returns an explicit null enemy when the enemy becomes null`() {
        val new = baseDigimonBattle().also { it.enemy = null }

        val result = DigimonBattleDiffer.diff(baseDigimonBattle(), new)

        assertPresent(null, result.enemy)
        assertAbsent(result.field)
    }

    @Test
    fun `diff returns an empty dto when the enemy stays null`() {
        val previous = baseDigimonBattle().also { it.enemy = null }
        val new = baseDigimonBattle().also { it.enemy = null }

        val result = DigimonBattleDiffer.diff(previous, new)

        assertAbsent(result.enemy)
        assertAbsent(result.field)
    }

    @Test
    fun `diff returns the full enemy when the enemy appears`() {
        val previous = baseDigimonBattle().also { it.enemy = null }

        val result = DigimonBattleDiffer.diff(previous, baseDigimonBattle())

        val enemy = presentValue(result.enemy)
        assertPresent(122, enemy.id)
        assertPresent(201, enemy.groupId)
        assertTrue(enemy.hp.hasValue)
    }

    @Test
    fun `diff returns only the active fighter when the partner is switched`() {
        val previous = DigimonBattle(field = 2u, fieldAmount = 64, activeFighter = 0)
        val new = DigimonBattle(field = 2u, fieldAmount = 64, activeFighter = 1)

        val result = DigimonBattleDiffer.diff(previous, new)

        assertPresent(1, result.activeFighter)
        assertAbsent(result.field)
        assertAbsent(result.fieldAmount)
        assertAbsent(result.enemy)
    }

    @Test
    fun `diff returns only the field's boost when it changes`() {
        val result = DigimonBattleDiffer.diff(DigimonBattle(field = 2u, fieldAmount = 64), DigimonBattle(field = 2u, fieldAmount = 127))

        assertPresent(127, result.fieldAmount)
        assertAbsent(result.activeFighter)
    }

    @Test
    fun `diff returns only the stage when the battle moves to another`() {
        val result = DigimonBattleDiffer.diff(DigimonBattle(stage = 12), DigimonBattle(stage = 22))

        assertPresent(22, result.stage)
        assertAbsent(result.field)
        assertAbsent(result.activeFighter)
    }
}

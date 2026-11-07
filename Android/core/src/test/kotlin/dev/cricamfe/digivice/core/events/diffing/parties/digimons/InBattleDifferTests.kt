package dev.cricamfe.digivice.core.events.diffing.parties.digimons

import dev.cricamfe.digivice.core.domain.models.parties.digimons.InBattle
import dev.cricamfe.digivice.core.domain.models.parties.digimons.Vital
import dev.cricamfe.digivice.core.events.assertAbsent
import dev.cricamfe.digivice.core.events.assertPresent
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/** Port of `Tests/Unit/Events/Diffing/Parties/Digimons/InBattleDifferTests.cs`. */
class InBattleDifferTests {
    private fun baseInBattle() = InBattle(
        condition = 0,
        strength = 0,
        defense = 0,
        speed = 0,
        hp = Vital(current = 100, max = 100),
        mp = Vital(current = 50, max = 50),
    )

    @Test
    fun `diff returns null when there are no changes`() {
        assertNull(InBattleDiffer.diff(baseInBattle(), baseInBattle()))
    }

    @Test
    fun `diff returns the full dto when the previous is null`() {
        val new = baseInBattle().also {
            it.condition = 0x04
            it.speed = 84
        }

        val result = InBattleDiffer.diff(null, new)

        assertNotNull(result)
        assertPresent(0x04, result!!.condition)
        assertPresent(0, result.strength)
        assertPresent(0, result.defense)
        assertPresent(84, result.speed)
        assertPresent(100, result.hp.value.current)
    }

    @Test
    fun `diff returns only the changed fields when there are partial changes`() {
        val new = baseInBattle().also {
            it.condition = 0x01
            it.hp.current = 50
        }

        val result = InBattleDiffer.diff(baseInBattle(), new)!!

        assertPresent(0x01, result.condition)
        assertPresent(50, result.hp.value.current)
        assertAbsent(result.hp.value.max)
        assertAbsent(result.mp)
        assertAbsent(result.strength)
        assertAbsent(result.defense)
        assertAbsent(result.speed)
    }

    @Test
    fun `diff returns only the speed when only the speed changes`() {
        val new = baseInBattle().also { it.speed = 84 }

        val result = InBattleDiffer.diff(baseInBattle(), new)!!

        assertPresent(84, result.speed)
        assertAbsent(result.condition)
        assertAbsent(result.strength)
        assertAbsent(result.defense)
        assertAbsent(result.hp)
        assertAbsent(result.mp)
    }

    @Test
    fun `diff returns only the form when the fighter digivolved`() {
        val previous = baseInBattle().also { it.formId = 383 }
        val new = baseInBattle().also { it.formId = 389 }

        val result = InBattleDiffer.diff(previous, new)!!

        assertPresent(389, result.formId)
        assertAbsent(result.condition)
        assertAbsent(result.hp)
        assertAbsent(result.speed)
    }

    @Test
    fun `diff sends the form in the full dto`() {
        val result = InBattleDiffer.diff(null, baseInBattle().also { it.formId = 389 })!!

        assertPresent(389, result.formId)
    }
}

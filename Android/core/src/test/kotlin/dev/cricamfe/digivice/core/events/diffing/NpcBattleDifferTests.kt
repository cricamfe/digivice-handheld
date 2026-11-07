package dev.cricamfe.digivice.core.events.diffing

import dev.cricamfe.digivice.core.domain.models.NpcBattle
import dev.cricamfe.digivice.core.events.assertPresent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/** Port of `Tests/Unit/Events/Diffing/NpcBattleDifferTests.cs`. */
class NpcBattleDifferTests {
    @Test
    fun `diff returns null when the battles are equal`() {
        assertNull(NpcBattleDiffer.diff(NpcBattle(id = "first", won = true), NpcBattle(id = "first", won = true)))
    }

    @Test
    fun `diff returns the full dto when the previous is null`() {
        val result = NpcBattleDiffer.diff(null, NpcBattle(id = "first", won = true))

        assertNotNull(result)
        assertEquals("first", result!!.id)
        assertPresent(true, result.won)
    }

    @Test
    fun `diff returns the won delta when won changed`() {
        val result = NpcBattleDiffer.diff(NpcBattle(id = "first", won = false), NpcBattle(id = "first", won = true))

        assertNotNull(result)
        assertEquals("first", result!!.id)
        assertPresent(true, result.won)
    }
}

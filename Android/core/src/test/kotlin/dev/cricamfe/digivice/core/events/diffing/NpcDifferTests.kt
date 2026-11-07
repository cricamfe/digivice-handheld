package dev.cricamfe.digivice.core.events.diffing

import dev.cricamfe.digivice.core.domain.models.Npc
import dev.cricamfe.digivice.core.domain.models.NpcBattle
import dev.cricamfe.digivice.core.events.assertPresent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/** Port of `Tests/Unit/Events/Diffing/NpcDifferTests.cs`. */
class NpcDifferTests {
    private fun npc(firstWon: Boolean, secondWon: Boolean) = Npc(
        battles = listOf(
            NpcBattle(id = "first", won = firstWon),
            NpcBattle(id = "second", won = secondWon),
        ),
    )

    @Test
    fun `diff returns null when there are no changes`() {
        assertNull(NpcDiffer.diff(npc(true, false), npc(true, false)))
    }

    @Test
    fun `diff returns the full dto when the previous is null`() {
        val result = NpcDiffer.diff(null, npc(true, false))

        assertNotNull(result)
        val battles = result!!.battles.value
        assertEquals(2, battles.size)
        assertEquals("first", battles[0].id)
        assertPresent(true, battles[0].won)
        assertEquals("second", battles[1].id)
        assertPresent(false, battles[1].won)
    }

    @Test
    fun `diff returns only the changed battle when one battle is won`() {
        val result = NpcDiffer.diff(npc(true, false), npc(true, true))

        assertNotNull(result)
        val battle = result!!.battles.value.single()
        assertEquals("second", battle.id)
        assertPresent(true, battle.won)
    }
}

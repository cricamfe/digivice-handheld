package dev.cricamfe.digivice.core.events.diffing

import dev.cricamfe.digivice.core.domain.models.Npc
import dev.cricamfe.digivice.core.domain.models.NpcBattle
import dev.cricamfe.digivice.core.domain.models.Npcs
import dev.cricamfe.digivice.core.events.assertAbsent
import dev.cricamfe.digivice.core.events.assertPresent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Port of `Tests/Unit/Events/Diffing/NpcsDifferTests.cs`. */
class NpcsDifferTests {
    private fun baseNpcs() = Npcs(
        genji = Npc(battles = listOf(NpcBattle(id = "first", won = false), NpcBattle(id = "second", won = false))),
        natsumi = Npc(battles = listOf(NpcBattle(id = "first", won = false))),
    )

    @Test
    fun `diff returns an empty dto when there are no changes`() {
        val result = NpcsDiffer.diff(baseNpcs(), baseNpcs())

        assertAbsent(result.genji)
        assertAbsent(result.natsumi)
    }

    @Test
    fun `diff returns the full dto when the previous is null`() {
        val current = baseNpcs().also { it.genji.battles[0].won = true }

        val result = NpcsDiffer.diff(null, current)

        assertTrue(result.genji.hasValue)
        assertTrue(result.genji.value.battles.hasValue)
        assertPresent(true, result.genji.value.battles.value[0].won)
        assertTrue(result.natsumi.hasValue)
    }

    @Test
    fun `diff returns the genji delta when only genji changed`() {
        val current = baseNpcs().also { it.genji.battles[0].won = true }

        val result = NpcsDiffer.diff(baseNpcs(), current)

        assertTrue(result.genji.value.battles.hasValue)
        assertEquals("first", result.genji.value.battles.value[0].id)
        assertPresent(true, result.genji.value.battles.value[0].won)
        assertAbsent(result.natsumi)
    }
}

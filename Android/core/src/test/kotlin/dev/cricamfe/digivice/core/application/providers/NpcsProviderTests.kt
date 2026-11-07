package dev.cricamfe.digivice.core.application.providers

import dev.cricamfe.digivice.core.application.FakeNpcsLoader
import dev.cricamfe.digivice.core.memory.resources.NpcBattleResource
import dev.cricamfe.digivice.core.memory.resources.NpcResource
import dev.cricamfe.digivice.core.memory.resources.NpcsResource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class NpcsProviderTests {
    @Test
    fun `get should load resource and apply npcs assembler`() {
        val resource = NpcsResource(
            genji = NpcResource(
                battles = listOf(
                    NpcBattleResource(id = "first", value = 0x20u),
                    NpcBattleResource(id = "second", value = 0u),
                ),
            ),
        )
        val loader = FakeNpcsLoader(resource)

        val result = NpcsProvider(loader).get()

        assertEquals(2, result.genji.battles.size)
        assertTrue(result.genji.battles[0].won)
        assertFalse(result.genji.battles[1].won)
        assertTrue(result.natsumi.battles.isEmpty())
        assertEquals(1, loader.calls)
    }
}

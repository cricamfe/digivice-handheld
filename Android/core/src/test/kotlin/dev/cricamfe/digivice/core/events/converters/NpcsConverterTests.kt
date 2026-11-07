package dev.cricamfe.digivice.core.events.converters

import dev.cricamfe.digivice.core.domain.models.Npc
import dev.cricamfe.digivice.core.domain.models.NpcBattle
import dev.cricamfe.digivice.core.domain.models.Npcs
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Port of `Tests/Unit/Events/Converters/NpcsConverterTests.cs`. */
class NpcsConverterTests {
    @Test
    fun `toDto maps npc battles`() {
        val npcs = Npcs(
            genji = Npc(battles = listOf(NpcBattle(id = "first", won = true), NpcBattle(id = "second", won = true))),
            natsumi = Npc(battles = listOf(NpcBattle(id = "first", won = false))),
        )

        val dto = NpcsConverter.toDto(npcs)

        assertTrue(dto.genji.hasValue)
        assertTrue(dto.genji.value.battles.hasValue)
        assertEquals(2, dto.genji.value.battles.value.size)
        assertEquals("first", dto.genji.value.battles.value[0].id)
        assertTrue(dto.genji.value.battles.value[0].won.value)
        assertTrue(dto.natsumi.hasValue)
        assertFalse(dto.natsumi.value.battles.value.single().won.value)
    }

    @Test
    fun `toDto keeps an npc without battles present`() {
        val dto = NpcsConverter.toDto(Npcs())

        assertTrue(dto.aoaAttacker.hasValue)
        assertEquals(emptyList<Any>(), dto.aoaAttacker.value.battles.value)
    }
}

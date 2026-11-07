package dev.cricamfe.digivice.core.events.factory

import dev.cricamfe.digivice.core.domain.models.Npc
import dev.cricamfe.digivice.core.domain.models.NpcBattle
import dev.cricamfe.digivice.core.domain.models.Npcs
import dev.cricamfe.digivice.core.domain.models.State
import dev.cricamfe.digivice.core.events.dto.NpcsDto
import dev.cricamfe.digivice.core.events.models.EventType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Port of `Tests/Unit/Events/Factory/NpcsEventFactoryTests.cs`. */
class NpcsEventFactoryTests {
    private fun state(npcs: Npcs) = State(npcs = npcs)

    private fun baseNpcs() = Npcs(genji = Npc(battles = listOf(NpcBattle(id = "first", won = false))))

    @Test
    fun `create returns no events when the npcs have no changes`() {
        assertTrue(NpcsEventFactory.create(state(baseNpcs()), state(baseNpcs())).isEmpty())
    }

    @Test
    fun `create returns an npcs changed event when the npcs change`() {
        val newNpcs = baseNpcs().also { it.genji.battles[0].won = true }

        val event = NpcsEventFactory.create(state(baseNpcs()), state(newNpcs)).single()

        assertEquals(EventType.NpcsChanged, event.type)
        assertTrue(assertInstanceOf(NpcsDto::class.java, event.payload).genji.hasValue)
    }
}

package dev.cricamfe.digivice.core.events.factory

import dev.cricamfe.digivice.core.domain.models.Player
import dev.cricamfe.digivice.core.domain.models.State
import dev.cricamfe.digivice.core.events.assertPresent
import dev.cricamfe.digivice.core.events.dto.PlayerDto
import dev.cricamfe.digivice.core.events.models.EventType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Port of `Tests/Unit/Events/Factory/PlayerEventFactoryTests.cs`. */
class PlayerEventFactoryTests {
    private fun state(player: Player) = State(player = player)

    @Test
    fun `create returns no events when the player has no changes`() {
        val previous = state(Player(bits = 100, mapId = "0001"))
        val new = state(Player(bits = 100, mapId = "0001"))

        assertTrue(PlayerEventFactory.create(previous, new).isEmpty())
    }

    @Test
    fun `create returns a player changed event when the player changes`() {
        val previous = state(Player(bits = 100, mapId = "0001"))
        val new = state(Player(bits = 200, mapId = "0001"))

        val event = PlayerEventFactory.create(previous, new).single()

        assertEquals(EventType.PlayerChanged, event.type)
        val dto = assertInstanceOf(PlayerDto::class.java, event.payload)
        assertPresent(200, dto.bits)
    }
}

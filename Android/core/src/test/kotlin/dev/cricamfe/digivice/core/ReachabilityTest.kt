package dev.cricamfe.digivice.core

import dev.cricamfe.digivice.core.events.Event
import dev.cricamfe.digivice.core.events.dto.HealthDto
import dev.cricamfe.digivice.core.events.dto.PlayerDto
import dev.cricamfe.digivice.core.events.models.EventType
import dev.cricamfe.digivice.core.events.models.HealthStatus
import dev.cricamfe.digivice.core.retroarch.ConnectionCodes
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ReachabilityTest {
    private fun health(status: HealthStatus, code: String? = null) =
        Event(EventType.HealthChanged, HealthDto(status, code, null))

    @Test
    fun `not being on the game takes the overlay away`() {
        val away = listOf(ConnectionCodes.RETROARCH_UNREACHABLE, ConnectionCodes.NO_CONTENT, ConnectionCodes.OTHER_CONSOLE)
        for (code in away) assertEquals(false, Reachability.of(health(HealthStatus.Error, code)), code)
    }

    @Test
    fun `every other error keeps the overlay`() {
        val others = listOf(
            ConnectionCodes.WRONG_GAME,
            ConnectionCodes.WAITING_FOR_SAVE,
            ConnectionCodes.MEMORY_UNAVAILABLE,
            ConnectionCodes.MEMORY_READ_FAILED,
            ConnectionCodes.STATE_COMPOSE_FAILED,
        )
        for (code in others) assertEquals(true, Reachability.of(health(HealthStatus.Error, code)), code)
    }

    @Test
    fun `healthy and loading are reachable`() {
        assertEquals(true, Reachability.of(health(HealthStatus.Healthy)))
        assertEquals(true, Reachability.of(health(HealthStatus.Loading)))
    }

    @Test
    fun `an event that is not about health says nothing`() {
        assertNull(Reachability.of(Event(EventType.PlayerChanged, PlayerDto())))
    }
}

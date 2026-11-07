package dev.cricamfe.digivice.core.events.factory

import dev.cricamfe.digivice.core.domain.models.Auctions
import dev.cricamfe.digivice.core.domain.models.State
import dev.cricamfe.digivice.core.events.assertPresent
import dev.cricamfe.digivice.core.events.dto.AuctionsDto
import dev.cricamfe.digivice.core.events.models.EventType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Port of `Tests/Unit/Events/Factory/AuctionsEventFactoryTests.cs`. */
class AuctionsEventFactoryTests {
    private fun state(auctions: Auctions) = State(auctions = auctions)

    @Test
    fun `create returns no events when the auctions have no changes`() {
        val previous = state(Auctions(divineBarrier = true))
        val new = state(Auctions(divineBarrier = true))

        assertTrue(AuctionsEventFactory.create(previous, new).isEmpty())
    }

    @Test
    fun `create returns an auctions changed event when the auctions change`() {
        val previous = state(Auctions(divineBarrier = false))
        val new = state(Auctions(divineBarrier = true))

        val event = AuctionsEventFactory.create(previous, new).single()

        assertEquals(EventType.AuctionsChanged, event.type)
        val dto = assertInstanceOf(AuctionsDto::class.java, event.payload)
        assertPresent(true, dto.divineBarrier)
    }
}

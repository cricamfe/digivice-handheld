package dev.cricamfe.digivice.core.events

import dev.cricamfe.digivice.core.domain.models.State
import dev.cricamfe.digivice.core.events.models.HealthStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

/** Port of `Tests/Unit/Events/States/GameStateStoreTests.cs`. */
class GameStateStoreTests {
    @Test
    fun `updateState updates the current state`() {
        val store = GameStateStore()
        val expected = State()

        store.updateState(expected)

        assertNotNull(store.currentState)
        assertSame(expected, store.currentState)
    }

    @Test
    fun `clearState resets the current state without changing the status`() {
        val store = GameStateStore()
        store.updateState(State())
        store.status = HealthStatus.Healthy

        store.clearState()

        assertNull(store.currentState)
        assertEquals(HealthStatus.Healthy, store.status)
    }

    @Test
    fun `the default status is loading`() {
        assertEquals(HealthStatus.Loading, GameStateStore().status)
    }
}

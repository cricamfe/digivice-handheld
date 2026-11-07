package dev.cricamfe.digivice.core.events.factory

import dev.cricamfe.digivice.core.domain.models.State
import dev.cricamfe.digivice.core.events.GameStateStore
import dev.cricamfe.digivice.core.events.dto.HealthDto
import dev.cricamfe.digivice.core.events.models.EventType
import dev.cricamfe.digivice.core.events.models.HealthStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Port of `Tests/Unit/Events/Factory/HealthEventFactoryTests.cs` (`CreateSuccess` is `success`, `CreateError` is `error`). */
class HealthEventFactoryTests {
    @Test
    fun `success returns no events when already loading without state`() {
        val store = GameStateStore()

        val result = HealthEventFactory.success(store)

        assertTrue(result.isEmpty())
        assertEquals(HealthStatus.Loading, store.status)
    }

    @Test
    fun `success sets healthy when the state exists`() {
        val store = GameStateStore()
        store.updateState(State())

        val result = HealthEventFactory.success(store)

        assertEquals(HealthStatus.Healthy, store.status)
        assertNull(store.lastErrorCode)
        assertNull(store.lastErrorDetail)

        val event = result.single()
        assertEquals(EventType.HealthChanged, event.type)
        val dto = assertInstanceOf(HealthDto::class.java, event.payload)
        assertEquals(HealthStatus.Healthy, dto.status)
        assertNull(dto.errorCode)
        assertNull(dto.errorDetail)
    }

    @Test
    fun `success transitions from error to loading when there is no state`() {
        val store = GameStateStore().apply {
            status = HealthStatus.Error
            lastErrorCode = "retroarch_unreachable"
            lastErrorDetail = "detail"
        }

        val result = HealthEventFactory.success(store)

        assertEquals(HealthStatus.Loading, store.status)
        assertNull(store.lastErrorCode)
        assertNull(store.lastErrorDetail)
        val dto = assertInstanceOf(HealthDto::class.java, result.single().payload)
        assertEquals(HealthStatus.Loading, dto.status)
    }

    @Test
    fun `error clears the state and returns an event when leaving a non error status`() {
        val store = GameStateStore().apply { status = HealthStatus.Loading }

        val result = HealthEventFactory.error(store, "retroarch_unreachable")

        assertNull(store.currentState)
        assertEquals(HealthStatus.Error, store.status)
        assertEquals("retroarch_unreachable", store.lastErrorCode)

        val event = result.single()
        assertEquals(EventType.HealthChanged, event.type)
        val dto = assertInstanceOf(HealthDto::class.java, event.payload)
        assertEquals(HealthStatus.Error, dto.status)
        assertEquals("retroarch_unreachable", dto.errorCode)
    }

    @Test
    fun `error returns no events when the store was already in error without state`() {
        val store = GameStateStore().apply {
            status = HealthStatus.Error
            lastErrorCode = "retroarch_unreachable"
        }

        assertTrue(HealthEventFactory.error(store, "retroarch_unreachable").isEmpty())
    }

    @Test
    fun `error announces a change of code while already in error without state`() {
        val store = GameStateStore().apply {
            status = HealthStatus.Error
            lastErrorCode = "retroarch_unreachable"
            lastErrorDetail = "first detail"
        }

        val result = HealthEventFactory.error(store, "no_content", "second detail")

        assertEquals("no_content", store.lastErrorCode)
        assertEquals("second detail", store.lastErrorDetail)
        val dto = assertInstanceOf(HealthDto::class.java, result.single().payload)
        assertEquals(HealthStatus.Error, dto.status)
        assertEquals("no_content", dto.errorCode)
        assertEquals("second detail", dto.errorDetail)
    }

    @Test
    fun `error with the same code and a different detail emits nothing but records the detail`() {
        val store = GameStateStore().apply {
            status = HealthStatus.Error
            lastErrorCode = "wrong_game"
            lastErrorDetail = "first detail"
        }

        assertTrue(HealthEventFactory.error(store, "wrong_game", "second detail").isEmpty())
        assertEquals("second detail", store.lastErrorDetail)
    }

    @Test
    fun `error returns an event when already in error but the state exists`() {
        val store = GameStateStore().apply { status = HealthStatus.Error }
        store.updateState(State())

        val result = HealthEventFactory.error(store, "retroarch_unreachable")

        assertNull(store.currentState)
        assertEquals(HealthStatus.Error, store.status)
        val dto = assertInstanceOf(HealthDto::class.java, result.single().payload)
        assertEquals(HealthStatus.Error, dto.status)
        assertEquals("retroarch_unreachable", dto.errorCode)
    }

    @Test
    fun `error persists the error detail when provided`() {
        val store = GameStateStore().apply { status = HealthStatus.Healthy }

        val result = HealthEventFactory.error(store, "memory_read_failed", "Failed to read player data")

        assertEquals("memory_read_failed", store.lastErrorCode)
        assertEquals("Failed to read player data", store.lastErrorDetail)
        val dto = assertInstanceOf(HealthDto::class.java, result.single().payload)
        assertEquals(HealthStatus.Error, dto.status)
        assertEquals("memory_read_failed", dto.errorCode)
        assertEquals("Failed to read player data", dto.errorDetail)
    }
}

package dev.cricamfe.digivice.core

import dev.cricamfe.digivice.core.events.Event
import dev.cricamfe.digivice.core.domain.models.State
import dev.cricamfe.digivice.core.events.converters.StateConverter
import dev.cricamfe.digivice.core.events.models.EventType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class GameSessionTests {
    private class Loop(private val emit: (Event) -> Unit) : GameSessionLoop {
        val calls = mutableListOf<String>()
        val initial = Event(EventType.InitialState, StateConverter.toDto(State()))
        var emitOnStop = false
        override fun start(scope: CoroutineScope) { calls += "start" }
        override fun stop() {
            calls += "stop"
            if (emitOnStop) emit(initial)
        }
        override fun requestInitialEvents() {
            calls += "initial"
            emit(initial)
        }
        fun emitLate() = emit(initial)
    }

    private val scope = CoroutineScope(Dispatchers.Unconfined)

    @Test
    fun `suspension can resume the same source and replay its current state`() {
        for (source in GameSource.entries) {
            lateinit var loop: Loop
            val events = mutableListOf<Event>()
            val session = GameSession(source, { false }, { event, _ -> events += event }) { emit ->
                Loop(emit).also { loop = it }
            }
            session.start(scope)
            session.stop()
            session.start(scope)
            session.requestInitialEvents()
            assertEquals(listOf("start", "stop", "start", "initial"), loop.calls)
            assertSame(loop.initial, events.single())
            assertEquals(false, session.portClosed)
            session.close()
        }
    }

    @Test
    fun `retiring a source closes it once and rejects callbacks and restart`() {
        for (source in GameSource.entries) {
            lateinit var loop: Loop
            val events = mutableListOf<Event>()
            val session = GameSession(source, { null }, { event, _ -> events += event }) { emit ->
                Loop(emit).also { loop = it; it.emitOnStop = true }
            }
            session.start(scope)
            session.close()
            session.close()
            loop.emitLate()
            session.start(scope)
            session.stop()
            session.requestInitialEvents()
            assertEquals(listOf("start", "stop"), loop.calls)
            assertTrue(events.isEmpty(), "A retired $source must not publish even while stop closes its request")
        }
    }

    @Test
    fun `changing source keeps queued callbacks identifiable and initial replay belongs to the new session`() {
        for (source in GameSource.entries) {
            val queued = mutableListOf<Pair<Event, GameSession>>()
            val loops = mutableListOf<Loop>()
            fun create(kind: GameSource) = GameSession(kind, { false }, { event, origin -> queued += event to origin }) { emit ->
                Loop(emit).also { loops += it }
            }
            val previous = create(source)
            previous.requestInitialEvents()
            previous.close()
            val current = create(source)
            loops.first().emitLate()
            current.requestInitialEvents()

            // The Android main-thread callback uses the same identity check after changing source.
            val delivered = queued.filter { (_, origin) -> origin === current }.map { it.first }
            assertEquals(2, queued.size)
            assertSame(loops.last().initial, delivered.single())
            assertNotSame(loops.first().initial, delivered.single())
            current.close()
        }
    }
}

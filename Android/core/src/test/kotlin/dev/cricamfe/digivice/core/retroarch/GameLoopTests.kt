// advanceTimeBy and the scheduler's currentTime are still experimental in kotlinx-coroutines-test.
@file:OptIn(ExperimentalCoroutinesApi::class)

package dev.cricamfe.digivice.core.retroarch

import kotlinx.coroutines.ExperimentalCoroutinesApi
import dev.cricamfe.digivice.core.CoreModule
import dev.cricamfe.digivice.core.GameLoop
import dev.cricamfe.digivice.core.events.Event
import dev.cricamfe.digivice.core.events.GameStateStore
import dev.cricamfe.digivice.core.events.EventSerializer
import dev.cricamfe.digivice.core.events.dto.FieldActorDto
import dev.cricamfe.digivice.core.events.dto.FieldActorsDto
import dev.cricamfe.digivice.core.events.dto.FieldChangingDto
import dev.cricamfe.digivice.core.events.dto.HealthDto
import dev.cricamfe.digivice.core.events.dto.PlayerPositionDto
import dev.cricamfe.digivice.core.events.models.EventType
import dev.cricamfe.digivice.core.events.models.HealthStatus
import dev.cricamfe.digivice.core.memory.TestEnvironment
import dev.cricamfe.digivice.core.memory.readers.FieldTaskProbe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext

/**
 * The loop against a fake RetroArch over real UDP sockets on loopback, with short timeouts, serving [SyntheticRam]: no
 * dump of the game is needed. The loop's time is a [TestClock] that only moves when a test moves it, so the spacing of
 * the readings never depends on how fast the machine runs the test; the scheduling tests run [start][GameLoop.start] in
 * virtual time. Only the tests of the threads of start and stop use the real clock.
 */
class GameLoopTests {
    private class Rig(
        ram: ByteArray,
        timeoutMs: Int = 300,
        intervalMs: Long = 500,
        readDefinition: (String) -> String = TestEnvironment.definitions::readText,
        onEvent: (Event) -> Unit = {},
        positionWanted: () -> Boolean = { false },
        actorsWanted: () -> Boolean = { true },
        /** The time of the loop and of the fake; null is the real time. */
        val clock: TestClock? = TestClock(),
        context: CoroutineContext = Dispatchers.IO,
    ) {
        val fake = FakeRetroArch(ram.copyOf()).also { it.clock = clock }
        val client = RetroArchClient("127.0.0.1", fake.port, timeoutMs)
        val accessor = PagedMemoryAccessor(client)
        val module = CoreModule(accessor, readDefinition, TestEnvironment.definitions::listFiles)
        val connector = RetroArchConnector(client, accessor, module.addressesRepository)
        val store = GameStateStore()
        val events = CopyOnWriteArrayList<Event>()
        val loop = GameLoop(
            connector, module, store, { events += it; onEvent(it) }, intervalMs, positionWanted, actorsWanted,
            nanoTime = clock?.let { it::nanoTime } ?: System::nanoTime,
            context = context,
        )

        /** Runs one tick and returns the events it dispatched. */
        fun tick(): List<Event> {
            val before = events.size
            loop.tick()
            return events.drop(before)
        }

        fun close() {
            loop.stop()
            client.close()
            fake.close()
        }

        fun signatureReads() = fake.commands.count { it.startsWith("READ_CORE_MEMORY 10105 ", ignoreCase = true) }
    }

    private val rigs = mutableListOf<Rig>()

    private fun rig(
        ram: ByteArray = SyntheticRam.game(),
        timeoutMs: Int = 300,
        intervalMs: Long = 500,
        readDefinition: (String) -> String = TestEnvironment.definitions::readText,
        onEvent: (Event) -> Unit = {},
        positionWanted: () -> Boolean = { false },
        actorsWanted: () -> Boolean = { true },
        clock: TestClock? = TestClock(),
        context: CoroutineContext = Dispatchers.IO,
    ) = Rig(ram, timeoutMs, intervalMs, readDefinition, onEvent, positionWanted, actorsWanted, clock, context)
        .also { rigs += it }

    /** A rig whose loop runs on the scheduler of the test, with its virtual time as the clock of the loop and the fake. */
    private fun TestScope.virtualRig(
        intervalMs: Long,
        onEvent: (Event) -> Unit = {},
        positionWanted: () -> Boolean = { false },
    ) = rig(
        timeoutMs = 2000,
        intervalMs = intervalMs,
        onEvent = onEvent,
        positionWanted = positionWanted,
        clock = TestClock(testScheduler),
        context = StandardTestDispatcher(testScheduler),
    )

    /**
     * Runs the loop until the rig's clock reaches [ms]. The clock is the scheduler's time plus the latency of the fake,
     * which only the fake's thread knows, so the scheduler is moved a millisecond at a time.
     */
    private fun TestScope.advanceClockTo(rig: Rig, ms: Long) {
        while (rig.clock!!.millis() < ms) advanceTimeBy(1)
    }

    /** When each command [command] was received, in ms of the rig's clock. */
    private fun Rig.times(command: String): List<Long> =
        fake.received.filter { it.command == command }.map { it.nanos / 1_000_000 }

    @AfterEach
    fun tearDown() = rigs.forEach { it.close() }

    /** The types of the state and health events; the position and the characters of the map are asserted apart. */
    private fun List<Event>.types() =
        map { it.type }.filter {
            it != EventType.FieldActorsChanged && it != EventType.PartnerTechniquesChanged && it != EventType.QuestOffersChanged
        }

    private fun List<Event>.health() = filter { it.type == EventType.HealthChanged }.map { it.payload as HealthDto }

    private fun assertError(rig: Rig, code: String, events: List<Event>) {
        assertEquals(listOf(EventType.HealthChanged), events.types())
        val health = events.health().single()
        assertEquals(HealthStatus.Error, health.status)
        assertEquals(code, health.errorCode)
        assertEquals(HealthStatus.Error, rig.store.status)
        assertEquals(code, rig.store.lastErrorCode)
        assertNull(rig.store.currentState)
    }

    // ---- connection outcomes ----------------------------------------------------------------------------------

    @Test
    fun `a game in progress is read and announced`() {
        val rig = rig()

        val events = rig.tick()

        assertEquals(listOf(EventType.InitialState, EventType.HealthChanged), events.types())
        assertEquals(HealthStatus.Healthy, events.health().single().status)
        assertEquals(HealthStatus.Healthy, rig.store.status)
        assertNotNull(rig.store.currentState)
        assertEquals(1, rig.signatureReads())
        assertTrue(rig.accessor.requestsInCycle in 1..10, "requests in the cycle: ${rig.accessor.requestsInCycle}")
        assertEquals(emptyList<Event>(), rig.tick(), "nothing changed, nothing to say")
        assertEquals(1, rig.signatureReads(), "the signature is not checked again while the title stays")
    }

    @Test
    fun `no reply to VERSION is retroarch_unreachable`() {
        val rig = rig(timeoutMs = 40)
        rig.fake.silent = true

        assertError(rig, ConnectionCodes.RETROARCH_UNREACHABLE, rig.tick())
        assertEquals(emptyList<Event>(), rig.tick(), "the same error is not announced twice")
    }

    @Test
    fun `nothing listening is retroarch_unreachable`() {
        val rig = rig(timeoutMs = 2000)
        rig.fake.close()

        assertError(rig, ConnectionCodes.RETROARCH_UNREACHABLE, rig.tick())
    }

    @Test
    fun `CONTENTLESS is no_content`() {
        val rig = rig()
        rig.fake.state = "CONTENTLESS"

        assertError(rig, ConnectionCodes.NO_CONTENT, rig.tick())
        assertEquals(0, rig.signatureReads())
    }

    @Test
    fun `no memory map with a game loaded is memory_unavailable`() {
        val rig = rig()
        rig.fake.memoryMap = false

        val events = rig.tick()

        assertError(rig, ConnectionCodes.MEMORY_UNAVAILABLE, events)
        assertTrue(events.health().single().errorDetail!!.contains("no memory map defined"))
    }

    @Test
    fun `a game of another console is other_console, even when its core refuses reads`() {
        val rig = rig()
        rig.fake.title = "game_boy_advance,Pokemon - Emerald Version (USA, Europe)"
        rig.fake.memoryMap = false

        assertError(rig, ConnectionCodes.OTHER_CONSOLE, rig.tick())
    }

    @Test
    fun `a wrong signature is wrong_game`() {
        val rig = rig(ram = ByteArray(FakeRetroArch.RAM_SIZE))

        assertError(rig, ConnectionCodes.WRONG_GAME, rig.tick())
    }

    @Test
    fun `wrong_game is not re-read every cycle but is checked again later`() {
        val rig = rig(ram = ByteArray(FakeRetroArch.RAM_SIZE))
        assertError(rig, ConnectionCodes.WRONG_GAME, rig.tick())

        // The game finished booting: same title, now the right RAM.
        rig.fake.ram = SyntheticRam.game()
        repeat(RetroArchConnector.WRONG_GAME_RECHECK_CYCLES - 1) {
            assertEquals(emptyList<Event>(), rig.tick())
        }
        assertEquals(1, rig.signatureReads(), "no re-read inside the wait")

        val events = rig.tick()

        assertEquals(2, rig.signatureReads())
        assertEquals(listOf(EventType.HealthChanged, EventType.InitialState, EventType.HealthChanged), events.types())
        assertEquals(HealthStatus.Healthy, rig.store.status)
    }

    @Test
    fun `while the game boots a zeroed party is waiting_for_save, not three Kotemon`() {
        val rig = rig(ram = SyntheticRam.booting())

        assertError(rig, ConnectionCodes.WAITING_FOR_SAVE, rig.tick())
    }

    @Test
    fun `the right signature with an empty party is waiting_for_save and a clean state`() {
        val rig = rig(ram = SyntheticRam.title())

        assertError(rig, ConnectionCodes.WAITING_FOR_SAVE, rig.tick())
        assertEquals(emptyList<Event>(), rig.tick())
        assertEquals(1, rig.signatureReads())
    }

    @Test
    fun `each change of error code is announced once`() {
        val rig = rig(timeoutMs = 40)

        rig.fake.silent = true
        assertError(rig, ConnectionCodes.RETROARCH_UNREACHABLE, rig.tick())
        assertEquals(emptyList<Event>(), rig.tick(), "the same code again says nothing")

        rig.fake.silent = false
        rig.fake.state = "CONTENTLESS"
        assertError(rig, ConnectionCodes.NO_CONTENT, rig.tick())

        rig.fake.state = "PLAYING"
        rig.fake.ram = ByteArray(FakeRetroArch.RAM_SIZE)
        assertError(rig, ConnectionCodes.WRONG_GAME, rig.tick())

        rig.fake.title = "playstation,Digimon World 2003 (Europe) (Disc 2)"
        rig.fake.ram = SyntheticRam.title()
        assertError(rig, ConnectionCodes.WAITING_FOR_SAVE, rig.tick())
        assertEquals(emptyList<Event>(), rig.tick())

        rig.fake.memoryMap = false
        rig.fake.title = "playstation,Another"
        assertError(rig, ConnectionCodes.MEMORY_UNAVAILABLE, rig.tick())
    }

    // ---- pause, title screen, reconnection --------------------------------------------------------------------

    @Test
    fun `PAUSED keeps the last state with no error and no events and reads nothing`() {
        val rig = rig()
        rig.tick()
        val state = rig.store.currentState
        rig.fake.state = "PAUSED"
        rig.fake.clearLog()

        assertEquals(emptyList<Event>(), rig.tick())

        assertSame(state, rig.store.currentState)
        assertEquals(HealthStatus.Healthy, rig.store.status)
        assertNull(rig.store.lastErrorCode)
        assertEquals(listOf("GET_STATUS"), rig.fake.commands)

        rig.fake.state = "PLAYING"
        assertEquals(emptyList<Event>(), rig.tick(), "resuming an unchanged game says nothing")
    }

    @Test
    fun `PAUSED before anything was read neither reads nor errors`() {
        val rig = rig()
        rig.fake.state = "PAUSED"

        assertEquals(emptyList<Event>(), rig.tick())

        assertEquals(HealthStatus.Loading, rig.store.status)
        assertEquals(listOf("VERSION", "GET_STATUS"), rig.fake.commands)
    }

    @Test
    fun `a game and then the title screen clears the state and waits for the save`() {
        val rig = rig()
        rig.tick()
        assertNotNull(rig.store.currentState)

        rig.fake.ram = SyntheticRam.title()

        assertError(rig, ConnectionCodes.WAITING_FOR_SAVE, rig.tick())
        assertEquals(1, rig.signatureReads(), "the title screen keeps the signature, no new check")

        rig.fake.ram = SyntheticRam.game()
        val events = rig.tick()

        assertEquals(listOf(EventType.HealthChanged, EventType.InitialState, EventType.HealthChanged), events.types())
        assertEquals(HealthStatus.Healthy, rig.store.status)
        assertNull(rig.store.lastErrorCode)
    }

    @Test
    fun `a change of title checks the signature again`() {
        val rig = rig()
        rig.tick()
        assertEquals(1, rig.signatureReads())

        rig.fake.title = "playstation,Digimon World 2003 (Europe) (Disc 2)"
        assertEquals(emptyList<Event>(), rig.tick())
        assertEquals(2, rig.signatureReads())

        rig.fake.title = "playstation,Another Game"
        rig.fake.ram = ByteArray(FakeRetroArch.RAM_SIZE)
        assertError(rig, ConnectionCodes.WRONG_GAME, rig.tick())
        assertEquals(3, rig.signatureReads())
    }

    // ---- lost replies -----------------------------------------------------------------------------------------

    @Test
    fun `a lost reply skips the cycle and keeps the state`() {
        val rig = rig(timeoutMs = 40)
        rig.tick()
        val state = rig.store.currentState
        rig.fake.dropNextReads(2)

        assertEquals(emptyList<Event>(), rig.tick())

        assertSame(state, rig.store.currentState)
        assertEquals(HealthStatus.Healthy, rig.store.status)
        assertNull(rig.store.lastErrorCode)
        assertEquals(emptyList<Event>(), rig.tick(), "the next cycle works as if nothing happened")
    }

    @Test
    fun `a lost status reply skips the cycle too`() {
        val rig = rig(timeoutMs = 40)
        rig.tick()
        val state = rig.store.currentState
        rig.fake.silent = true

        assertEquals(emptyList<Event>(), rig.tick())

        assertSame(state, rig.store.currentState)
        assertEquals(HealthStatus.Healthy, rig.store.status)
    }

    @Test
    fun `isolated losses never add up to an error`() {
        val rig = rig(timeoutMs = 30)
        rig.tick()
        rig.fake.clearLog()

        repeat(6) {
            rig.fake.dropNextReads(2)
            assertEquals(emptyList<Event>(), rig.tick())
            assertEquals(emptyList<Event>(), rig.tick())
        }

        assertEquals(HealthStatus.Healthy, rig.store.status)
        assertEquals(0, rig.fake.commands.count { it == "VERSION" }, "no probe: the losses were never consecutive")
    }

    @Test
    fun `losses with a silent VERSION end in retroarch_unreachable and the next connection starts over`() {
        val rig = rig(timeoutMs = 30)
        rig.tick()
        rig.fake.silent = true

        repeat(2) { assertEquals(emptyList<Event>(), rig.tick(), "lost cycle ${it + 1} is only skipped") }
        assertNotNull(rig.store.currentState)

        assertError(rig, ConnectionCodes.RETROARCH_UNREACHABLE, rig.tick())

        rig.fake.silent = false
        val events = rig.tick()

        assertEquals(listOf(EventType.HealthChanged, EventType.InitialState, EventType.HealthChanged), events.types())
        assertEquals(2, rig.signatureReads(), "a reconnection checks the signature again")
    }

    @Test
    fun `losses while VERSION still answers end in memory_read_failed`() {
        val rig = rig(timeoutMs = 25)
        rig.tick()
        rig.fake.dropNextReads(Int.MAX_VALUE)
        rig.fake.clearLog()

        repeat(GameLoop.LOST_CYCLES_BEFORE_FAILURE - 1) { assertEquals(emptyList<Event>(), rig.tick()) }
        assertNotNull(rig.store.currentState, "the state is kept until the loop gives up")
        assertTrue(rig.fake.commands.contains("VERSION"), "RetroArch was probed on the way")

        val events = rig.tick()

        assertError(rig, ConnectionCodes.MEMORY_READ_FAILED, events)

        rig.fake.dropNextReads(0)
        val recovered = rig.tick()
        assertEquals(listOf(EventType.HealthChanged, EventType.InitialState, EventType.HealthChanged), recovered.types())
    }

    // ---- failures while composing -------------------------------------------------------------------------------

    @Test
    fun `a read refused while composing is memory_read_failed`() {
        val rig = rig()
        rig.tick()
        // The party page of the connection check is the first read; the next page the composer needs is refused.
        rig.fake.refuseReadsAfter(1)

        val events = rig.tick()

        assertError(rig, ConnectionCodes.MEMORY_READ_FAILED, events)
        assertTrue(events.health().single().errorDetail!!.contains("no descriptor for address"))
    }

    @Test
    fun `an unexpected failure while composing is state_compose_failed`() {
        var broken = false
        val rig = rig(
            readDefinition = { path ->
                if (broken && path == "PlayerAddresses.json") throw IllegalStateException("boom")
                TestEnvironment.definitions.readText(path)
            },
        )
        broken = true

        val events = rig.tick()

        assertError(rig, ConnectionCodes.STATE_COMPOSE_FAILED, events)
        assertEquals("boom", events.health().single().errorDetail)
    }

    // ---- protocol, ordering, concurrency ------------------------------------------------------------------------

    @Test
    fun `never sends write commands`() {
        val rig = rig(timeoutMs = 30)
        rig.tick()                                  // connection + game
        rig.fake.ram = SyntheticRam.title(); rig.tick()       // title screen
        rig.fake.ram = SyntheticRam.game(); rig.tick()
        rig.fake.state = "PAUSED"; rig.tick()
        rig.fake.state = "CONTENTLESS"; rig.tick()
        rig.fake.state = "PLAYING"; rig.fake.title = "playstation,Other"; rig.tick()
        rig.fake.ram = ByteArray(FakeRetroArch.RAM_SIZE); rig.fake.title = "playstation,Third"; rig.tick()
        rig.fake.memoryMap = false; rig.fake.title = "playstation,Fourth"; rig.tick()
        rig.fake.memoryMap = true; rig.fake.ram = SyntheticRam.game(); rig.fake.dropNextReads(3)
        repeat(3) { rig.tick() }
        rig.fake.silent = true
        repeat(4) { rig.tick() }

        val commands = rig.fake.commands
        assertTrue(commands.size > 30, "the scenario sent ${commands.size} commands")
        val allowed = setOf("VERSION", "GET_STATUS", "READ_CORE_MEMORY")
        val offending = commands.filter { it.substringBefore(' ') !in allowed }
        assertEquals(emptyList<String>(), offending)
        val malformed = commands.filter {
            it.startsWith("READ_CORE_MEMORY") && !Regex("""READ_CORE_MEMORY [0-9A-Fa-f]+ \d+""").matches(it)
        }
        assertEquals(emptyList<String>(), malformed)
    }

    @Test
    fun `dispatch gets the events in the order the original loop emits them and holds no lock`() {
        val held = CopyOnWriteArrayList<Boolean>()
        lateinit var rig: Rig
        rig = rig(onEvent = {
            held += Thread.holdsLock(rig.loop) || Thread.holdsLock(rig.connector) || Thread.holdsLock(rig.client)
        })

        val events = rig.tick()

        assertEquals(listOf(EventType.InitialState, EventType.HealthChanged), events.types())
        assertEquals(HealthStatus.Healthy, events.health().single().status)
        assertEquals(listOf(false, false, false, false, false), held, "the state, the characters, the techniques, the offers and the health")
    }

    // ---- requestInitialEvents ---------------------------------------------------------------------------------

    @Test
    fun `requestInitialEvents re-emits the state and the health at the next tick`() {
        val rig = rig()
        val first = rig.tick()
        val initial = first.first { it.type == EventType.InitialState }

        rig.loop.requestInitialEvents()
        val events = rig.tick()

        assertEquals(listOf(EventType.InitialState, EventType.HealthChanged), events.types())
        assertEquals(initial, events[0], "the same whole state a new client needs")
        assertEquals(HealthDto(HealthStatus.Healthy), events.health().single())
    }

    @Test
    fun `requestInitialEvents emits nothing until the next tick, and only once`() {
        val rig = rig()
        rig.tick()
        val before = rig.events.size

        rig.loop.requestInitialEvents()
        assertEquals(before, rig.events.size, "the request itself dispatches nothing: only a tick does")

        assertEquals(listOf(EventType.InitialState, EventType.FieldActorsChanged, EventType.PartnerTechniquesChanged, EventType.QuestOffersChanged, EventType.HealthChanged), rig.tick().map { it.type })
        assertEquals(emptyList<Event>(), rig.tick(), "the request was consumed")
    }

    @Test
    fun `requestInitialEvents without a state sends only the current error`() {
        val rig = rig(ram = SyntheticRam.title())
        rig.tick()
        assertNull(rig.store.currentState)

        rig.loop.requestInitialEvents()
        val events = rig.tick()

        assertEquals(listOf(EventType.HealthChanged), events.types())
        assertEquals(
            HealthDto(HealthStatus.Error, ConnectionCodes.WAITING_FOR_SAVE, null),
            events.health().single(),
        )
    }

    @Test
    fun `requestInitialEvents before anything was read reports Loading`() {
        val rig = rig()
        rig.fake.state = "PAUSED"

        rig.loop.requestInitialEvents()
        val events = rig.tick()

        assertEquals(listOf(HealthDto(HealthStatus.Loading)), events.health())
        assertEquals(listOf(EventType.HealthChanged), events.types())
    }

    @Test
    fun `requestInitialEvents is answered while the game is paused`() {
        val rig = rig()
        rig.tick()
        rig.fake.state = "PAUSED"
        rig.events.clear()

        rig.loop.requestInitialEvents()
        val events = rig.tick()

        assertEquals(listOf(EventType.InitialState, EventType.HealthChanged), events.types())
    }

    @Test
    fun `requestInitialEvents sends the state before the changes of the same tick`() {
        val rig = rig()
        rig.tick()

        rig.loop.requestInitialEvents()
        rig.fake.ram = SyntheticRam.title()
        val events = rig.tick()

        // The snapshot the client asked for first, then what this tick found.
        assertEquals(listOf(EventType.InitialState, EventType.HealthChanged, EventType.HealthChanged), events.types())
        val (snapshotHealth, errorHealth) = events.health()
        assertEquals(HealthStatus.Healthy, snapshotHealth.status)
        assertEquals(ConnectionCodes.WAITING_FOR_SAVE, errorHealth.errorCode)
    }

    // ---- start / stop -----------------------------------------------------------------------------------------

    private fun awaitCondition(timeoutMs: Long = 3000, condition: () -> Boolean) {
        val deadline = System.nanoTime() + timeoutMs * 1_000_000
        while (!condition()) {
            check(System.nanoTime() < deadline) { "condition not reached in $timeoutMs ms" }
            Thread.sleep(2)
        }
    }

    private fun scope() = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Test
    fun `start polls in the background and stop leaves no socket and no more traffic`() {
        val rig = rig(intervalMs = 40, clock = null)
        val scope = scope()
        try {
            rig.loop.start(scope)
            awaitCondition { rig.events.any { it.type == EventType.InitialState } }
            awaitCondition { rig.fake.commands.count { it == "GET_STATUS" } >= 3 }

            rig.loop.stop()
            val stopped = System.nanoTime()

            assertEquals(0, rig.client.openSockets)
            Thread.sleep(150)
            // A datagram sent just before stop() may be logged by the fake a moment later; nothing goes out after it.
            val late = rig.fake.received.filter { it.nanos - stopped > 50_000_000 }.map { it.command }
            assertEquals(emptyList<String>(), late, "the loop kept polling after stop()")
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun `stop is prompt even while a request waits for a reply`() {
        val rig = rig(timeoutMs = 5000, intervalMs = 40, clock = null)
        rig.fake.silent = true
        val scope = scope()
        try {
            rig.loop.start(scope)
            awaitCondition { rig.fake.commands.isNotEmpty() }

            val started = System.nanoTime()
            rig.loop.stop()
            val elapsedMs = (System.nanoTime() - started) / 1_000_000

            assertTrue(elapsedMs < 500, "stop() took $elapsedMs ms")
            assertEquals(0, rig.client.openSockets)
            Thread.sleep(100)
            assertEquals(emptyList<Event>(), rig.events, "nothing is dispatched by a stopped loop")
            assertEquals(HealthStatus.Loading, rig.store.status)
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun `the loop can be started again after stop`() {
        val rig = rig(intervalMs = 40, clock = null)
        val scope = scope()
        try {
            rig.loop.start(scope)
            awaitCondition { rig.store.currentState != null }
            rig.loop.stop()
            rig.events.clear()
            rig.fake.ram = SyntheticRam.title()

            rig.loop.start(scope)
            awaitCondition { rig.store.lastErrorCode == ConnectionCodes.WAITING_FOR_SAVE }
            rig.loop.stop()

            assertEquals(0, rig.client.openSockets)
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun `a restart waits for the tick of the old run instead of overlapping it`() {
        val inDispatch = AtomicInteger(0)
        val maxInDispatch = AtomicInteger(0)
        val release = CountDownLatch(1)
        val firstDispatch = CountDownLatch(1)
        val rig = rig(intervalMs = 20, clock = null, onEvent = {
            maxInDispatch.accumulateAndGet(inDispatch.incrementAndGet(), ::maxOf)
            firstDispatch.countDown()
            release.await(5, TimeUnit.SECONDS)
            inDispatch.decrementAndGet()
        })
        val scope = scope()
        try {
            rig.loop.start(scope)
            assertTrue(firstDispatch.await(3, TimeUnit.SECONDS))

            // The old tick is stuck inside dispatch; stop() cannot interrupt that, and start() must not run beside it.
            rig.loop.stop()
            rig.loop.start(scope)
            val statusesBefore = rig.fake.commands.count { it == "GET_STATUS" }
            Thread.sleep(250)
            assertEquals(statusesBefore, rig.fake.commands.count { it == "GET_STATUS" }, "the new run polled early")

            release.countDown()
            awaitCondition { rig.fake.commands.count { it == "GET_STATUS" } > statusesBefore }
            rig.loop.stop()

            assertEquals(1, maxInDispatch.get(), "two ticks were dispatching at once")
            assertEquals(0, rig.client.openSockets)
        } finally {
            release.countDown()
            scope.cancel()
        }
    }

    @Test
    fun `ticks start every interval, measured start to start`() = runTest {
        val rig = virtualRig(intervalMs = 100)
        rig.fake.statusDelayMs = 30

        rig.loop.start(backgroundScope)
        advanceClockTo(rig, 800)
        rig.loop.stop()

        // End to start would put them 130 ms apart: the status alone takes 30 ms before the page reads.
        assertEquals((0L until 800L step 100).toList(), rig.times("GET_STATUS").filter { it < 800 })
    }

    @Test
    fun `missed ticks are skipped instead of burst`() = runTest {
        val rig = virtualRig(intervalMs = 100)
        rig.fake.statusDelayMs = 150      // a tick lasts more than one interval

        rig.loop.start(backgroundScope)
        advanceClockTo(rig, 1000)
        rig.loop.stop()

        // Catching up would start the next tick as soon as the last ended (150 ms later); skipping waits for the next
        // multiple of the interval.
        assertEquals(listOf(0L, 200, 400, 600, 800), rig.times("GET_STATUS").filter { it < 1000 })
    }

    @Test
    fun `a pause during a change of field keeps the pace of the cycles`() = runTest {
        lateinit var rig: Rig
        var pausedAt = -1L
        rig = virtualRig(intervalMs = 100, positionWanted = { true }, onEvent = { event ->
            // RetroArch's menu opens as soon as the field starts to close.
            if (pausedAt < 0 && event.type == EventType.FieldChanging && (event.payload as FieldChangingDto).changing) {
                rig.fake.state = "PAUSED"
                pausedAt = rig.clock!!.millis()
            }
        })
        rig.fake.latencyMs = 20

        rig.loop.start(backgroundScope)
        advanceClockTo(rig, 250)
        val closedAt = rig.clock!!.millis()
        rig.closeFieldFor(0x0212)
        advanceClockTo(rig, 1500)
        rig.loop.stop()

        // The first reading of the field's task after it closed finds the change, as soon as its reply arrives (20 ms).
        val fieldRead = "READ_CORE_MEMORY ${rig.fieldTask().toString(16).uppercase()} ${FieldTaskProbe.SIZE}"
        assertEquals(rig.times(fieldRead).first { it >= closedAt } + 20, pausedAt)
        // While the field changes the cycles go back to back, so one comes at once; it finds the game paused, and from
        // then on they keep to one every 100 ms, start to start.
        val statuses = rig.times("GET_STATUS").filter { it < 1500 }
        assertEquals((pausedAt until 1500L step 100).toList(), statuses.filter { it >= pausedAt })
    }

    @Test
    fun `the loop runs on the IO dispatcher`() {
        val thread = java.util.concurrent.atomic.AtomicReference<String>()
        val first = CountDownLatch(1)
        val rig = rig(
            intervalMs = 40,
            clock = null,
            onEvent = { thread.compareAndSet(null, Thread.currentThread().name); first.countDown() },
        )
        val scope = scope()
        try {
            rig.loop.start(scope)
            assertTrue(first.await(3, TimeUnit.SECONDS))
            rig.loop.stop()

            assertTrue(thread.get().startsWith("DefaultDispatcher-worker"), thread.get())
        } finally {
            scope.cancel()
        }
    }

    // ---- the position between cycles --------------------------------------------------------------------------------

    /** The player's actor in the synthetic game (the third entry of the task registry) and the bytes of a quick read of it. */
    private val actor = SyntheticRam.PLAYER_ACTOR.toInt()
    private val quickSize = 0x5C + 4
    private val quickRead = "READ_CORE_MEMORY ${actor.toString(16).uppercase()} $quickSize"

    private fun Rig.putInt(address: Int, value: Int) {
        java.nio.ByteBuffer.wrap(fake.ram).order(java.nio.ByteOrder.LITTLE_ENDIAN).putInt(address, value)
    }

    private fun Rig.walkTo(x: Int, y: Int) {
        putInt(actor + 0x58, x)
        putInt(actor + 0x5C, y)
    }

    private fun Rig.requests() = fake.commands.filter { it.startsWith("READ_CORE_MEMORY") || it == "GET_STATUS" }

    private fun List<Event>.positions() =
        filter { it.type == EventType.PlayerPositionChanged }.map { it.payload as PlayerPositionDto }

    /** The field's main task, found by the id the registry has it with. */
    private fun Rig.fieldTask(): Int = module.fieldTask.address!!.toInt()

    private fun List<Event>.changes() = filter { it.type == EventType.FieldChanging }.map { it.payload as FieldChangingDto }

    private fun Rig.closeFieldFor(mode: Int) {
        putInt(fieldTask() + 0x5C, mode)
        putInt(fieldTask() + 0x0C, 2)
    }

    @Test
    fun `a field tick is one read and says nothing while the field runs`() {
        val rig = rig(positionWanted = { true })
        rig.tick()
        rig.fake.clearLog()
        val before = rig.events.size

        rig.loop.fieldTick()

        assertEquals(listOf("READ_CORE_MEMORY ${rig.fieldTask().toString(16).uppercase()} 108"), rig.fake.commands)
        assertEquals(before, rig.events.size)
    }

    @Test
    fun `a field that starts to close is told once, with where it goes, and the actor is not read any more`() {
        val rig = rig(positionWanted = { true })
        rig.tick()
        val before = rig.events.size

        rig.closeFieldFor(0x0212)
        rig.loop.fieldTick()
        rig.fake.clearLog()
        rig.walkTo(530, 640)
        rig.loop.fieldTick()
        rig.loop.fastTick()

        assertEquals(listOf(FieldChangingDto(true, "0212")), rig.events.drop(before).changes())
        assertEquals(emptyList<PlayerPositionDto>(), rig.events.drop(before).positions())
        assertEquals(emptyList<String>(), rig.fake.commands)
    }

    @Test
    fun `a field waiting for its delay is still running`() {
        val rig = rig(positionWanted = { true })
        rig.tick()
        val before = rig.events.size

        rig.putInt(rig.fieldTask() + 0x68, 30)
        rig.closeFieldFor(0x0212)
        rig.loop.fieldTick()

        assertEquals(emptyList<FieldChangingDto>(), rig.events.drop(before).changes())
    }

    @Test
    fun `the cycle that finds the next field running ends the change, after its map and position`() {
        val rig = rig(positionWanted = { true })
        rig.tick()
        val task = rig.fieldTask()
        rig.closeFieldFor(0x0212)
        rig.loop.fieldTick()
        rig.tick()
        val before = rig.events.size

        rig.putInt(TestEnvironment.createAddressesRepository().getPlayerAddresses().mapId.toInt(), 0x0212)
        rig.walkTo(530, 640)
        rig.putInt(task + 0x0C, 1)
        rig.tick()

        val events = rig.events.drop(before)
        assertEquals(listOf(FieldChangingDto(false, null)), events.changes())
        val end = events.indexOfFirst { it.type == EventType.FieldChanging }
        assertTrue(events.indexOfFirst { it.type == EventType.PlayerChanged } in 0 until end)
        assertTrue(events.indexOfFirst { it.type == EventType.PlayerPositionChanged } in 0 until end)
        assertEquals(PlayerPositionDto(530, 640), events.positions().last())
    }

    /** Adds the area name banner to a free entry of the registry. */
    private fun Rig.showBanner(state: Int, substate: Int) {
        val banner = 0x100000
        val registry = 0x4B610
        val buffer = java.nio.ByteBuffer.wrap(fake.ram).order(java.nio.ByteOrder.LITTLE_ENDIAN)
        val free = (0 until 100).first { buffer.getInt(registry + it * 4) == 0 || buffer.getInt(registry + it * 4) == (0x80000000.toInt() + banner) }
        putInt(registry + free * 4, 0x80000000.toInt() + banner)
        putInt(banner, 9)
        putInt(banner + 0x0C, state)
        putInt(banner + 0x10, substate)
    }

    @Test
    fun `a change lasts while the area name banner covers the running field, and ends when it opens`() {
        val rig = rig(positionWanted = { true })
        rig.tick()
        val task = rig.fieldTask()
        rig.closeFieldFor(0x0212)
        rig.loop.fieldTick()
        rig.putInt(task + 0x0C, 1)
        rig.showBanner(state = 2, substate = 1)
        val before = rig.events.size

        rig.tick()

        assertEquals(emptyList<FieldChangingDto>(), rig.events.drop(before).changes())

        rig.showBanner(state = 2, substate = 3)
        rig.tick()

        assertEquals(listOf(FieldChangingDto(false, null)), rig.events.drop(before).changes())
    }

    @Test
    fun `a change is sent again to a client that connects meanwhile`() {
        val rig = rig(positionWanted = { true })
        rig.tick()
        rig.closeFieldFor(0x0212)
        rig.loop.fieldTick()
        rig.loop.requestInitialEvents()
        val before = rig.events.size

        rig.tick()

        assertEquals(FieldChangingDto(true, "0212"), rig.events.drop(before).changes().first())
    }

    @Test
    fun `a quick tick is one read of the actor and nothing else`() {
        val rig = rig(positionWanted = { true })
        rig.tick()
        rig.fake.clearLog()

        rig.walkTo(530, 640)
        val before = rig.events.size
        rig.loop.fastTick()

        assertEquals(listOf("READ_CORE_MEMORY ${actor.toString(16).uppercase()} $quickSize"), rig.fake.commands)
        assertEquals(listOf(PlayerPositionDto(530, 640)), rig.events.drop(before).positions())
        assertEquals(1, rig.events.drop(before).size, "only the position goes out, no state and no health")
    }

    @Test
    fun `a quick tick with the position unchanged says nothing`() {
        val rig = rig(positionWanted = { true })
        rig.tick()
        val before = rig.events.size

        rig.loop.fastTick()
        rig.loop.fastTick()

        assertEquals(before, rig.events.size)
        assertEquals(2, rig.fake.commands.count { it.startsWith("READ_CORE_MEMORY ${actor.toString(16).uppercase()} ") })
    }

    @Test
    fun `a quick tick says each change once`() {
        val rig = rig(positionWanted = { true })
        rig.tick()
        val before = rig.events.size

        rig.walkTo(530, 640)
        rig.loop.fastTick()
        rig.loop.fastTick()
        rig.walkTo(534, 641)
        rig.loop.fastTick()

        assertEquals(listOf(PlayerPositionDto(530, 640), PlayerPositionDto(534, 641)), rig.events.drop(before).positions())
    }

    @Test
    fun `a quick tick leaves the state and the health alone`() {
        val rig = rig(positionWanted = { true })
        rig.tick()
        val state = rig.store.currentState

        rig.walkTo(1, 2)
        rig.loop.fastTick()

        assertSame(state, rig.store.currentState)
        assertEquals(HealthStatus.Healthy, rig.store.status)
    }

    @Test
    fun `bytes that are not the player's actor say nothing and the next cycle finds it again`() {
        val rig = rig(positionWanted = { true })
        rig.tick()
        val before = rig.events.size

        rig.putInt(actor, 16)                         // the task was freed and the memory reused
        rig.walkTo(999, 999)
        rig.fake.clearLog()
        rig.loop.fastTick()

        assertEquals(1, rig.fake.commands.size)
        assertEquals(before, rig.events.size)

        rig.putInt(actor, 5)                          // a follower, kind 2, is not the player either
        rig.putInt(actor + 8, 2)
        rig.loop.fastTick()
        assertEquals(before, rig.events.size)
    }

    @Test
    fun `without an actor a quick tick asks for nothing`() {
        val rig = rig(positionWanted = { true })

        rig.loop.fastTick()                           // before the first cycle
        assertEquals(emptyList<String>(), rig.fake.commands)

        rig.tick()
        rig.fake.ram = SyntheticRam.outOfField() // no field: no player
        rig.tick()
        rig.fake.clearLog()
        rig.loop.fastTick()
        assertEquals(emptyList<String>(), rig.fake.commands)
    }

    @Test
    fun `an error or a pause makes the loop forget the actor`() {
        val rig = rig(positionWanted = { true })
        rig.tick()
        rig.fake.state = "PAUSED"
        rig.tick()
        rig.fake.clearLog()

        rig.loop.fastTick()

        assertEquals(emptyList<String>(), rig.fake.commands)
    }

    @Test
    fun `a quick tick asks for nothing while no page could take the position`() {
        var page = false
        val rig = rig(positionWanted = { page })
        rig.tick()
        rig.fake.clearLog()

        rig.loop.fastTick()
        assertEquals(emptyList<String>(), rig.fake.commands)

        page = true
        rig.loop.fastTick()
        assertEquals(1, rig.fake.commands.size)
    }

    @Test
    fun `lost replies on quick ticks are ignored, one request each with no retry and no health event`() {
        val rig = rig(timeoutMs = 10_000, positionWanted = { true })
        rig.tick()
        val before = rig.events.size
        rig.fake.silent = true
        rig.fake.clearLog()

        val started = System.nanoTime()
        repeat(20) { rig.loop.fastTick() }
        val elapsedMs = (System.nanoTime() - started) / 1_000_000

        assertEquals(20, rig.fake.commands.size, "one request each, never the retry")
        assertEquals(before, rig.events.size)
        assertEquals(HealthStatus.Healthy, rig.store.status)
        // Twenty quick reads of 60 ms each; one wait of the client's own timeout would be 10 s.
        assertTrue(elapsedMs < 10_000, "each lost quick read waits ${RetroArchConnector.QUICK_READ_TIMEOUT_MS} ms, not the client's timeout: $elapsedMs ms")

        // The full cycle's own count of lost cycles starts from zero: three lost cycles are needed, not twenty.
        rig.fake.silent = false
        assertEquals(emptyList<EventType>(), rig.tick().types())
    }

    @Test
    fun `a refused quick read is ignored too`() {
        val rig = rig(positionWanted = { true })
        rig.tick()
        val before = rig.events.size
        rig.fake.refuseReadsAfter(0)

        rig.loop.fastTick()

        assertEquals(before, rig.events.size)
        assertEquals(HealthStatus.Healthy, rig.store.status)
    }

    @Test
    fun `the position the cycle sees is told even if a quick tick had told another`() {
        val rig = rig(positionWanted = { true })
        rig.tick()

        rig.walkTo(530, 640)
        rig.loop.fastTick()                           // the page now has (530, 640); the state still has the old one
        rig.walkTo(SyntheticRam.START_X, SyntheticRam.START_Y) // back where the state has it before the next cycle reads it
        val events = rig.tick()

        assertEquals(listOf(PlayerPositionDto(SyntheticRam.START_X, SyntheticRam.START_Y)), events.positions())
        assertEquals(emptyList<EventType>(), events.types().filter { it == EventType.PlayerChanged })
    }

    @Test
    fun `an overlay that connects later starts from the state and then follows the quick ticks`() {
        val rig = rig(positionWanted = { true })
        rig.tick()
        rig.loop.requestInitialEvents()
        rig.tick()
        val before = rig.events.size

        rig.walkTo(540, 650)
        rig.loop.fastTick()

        assertEquals(listOf(PlayerPositionDto(540, 650)), rig.events.drop(before).positions())
    }

    @Test
    fun `started with no latency, a full cycle comes every interval and the readings go between them`() = runTest {
        val rig = virtualRig(intervalMs = 250, positionWanted = { true })     // a reading every 50 ms

        rig.loop.start(backgroundScope)
        advanceClockTo(rig, 1000)
        rig.loop.stop()

        // Every request answers at once, so a cycle takes no time: it starts on its interval and the position is read on
        // every slot, the slot of a cycle inside it (before its first page, after its status).
        assertEquals(listOf(0L, 250, 500, 750), rig.times("GET_STATUS"))
        assertEquals((0L until 1000L step 50).toList(), rig.times(quickRead))
        for (status in listOf(250L, 500, 750)) {
            val inCycle = rig.fake.received.filter { it.nanos / 1_000_000 == status }.map { it.command }
            assertEquals(listOf("GET_STATUS", quickRead), inCycle.take(2), "the cycle at $status")
        }
    }

    @Test
    fun `readings are not bunched, one right after another does nothing`() {
        val rig = rig(intervalMs = 500, positionWanted = { true })      // a reading every 100 ms
        rig.tick()
        rig.fake.clearLog()

        rig.loop.fastTick(minGapMs = 50)
        rig.loop.fastTick(minGapMs = 50)         // a moment later: the reading before is less than 50 ms old

        assertEquals(1, rig.fake.commands.size)
    }

    @Test
    fun `started with a frame of latency, the position is read every 100 ms and also during the full cycle`() = runTest {
        val rig = virtualRig(intervalMs = 500, positionWanted = { true })
        rig.fake.latencyMs = 20

        rig.loop.start(backgroundScope)
        advanceClockTo(rig, 3000)
        rig.loop.stop()

        // Every request takes 20 ms. The first cycle asks VERSION before its status, then checks the signature (11
        // reads) and reads 6 pages; its state is ready at 380 ms, and the position is read from then on, one slot of
        // 100 ms apart, with the field's task (one request more) after each reading.
        val statuses = rig.times("GET_STATUS").filter { it < 3000 }
        val readings = rig.times(quickRead).filter { it < 3000 }
        assertEquals((380L until 3000L step 100).toList(), readings)
        // A cycle comes due at 500 ms, when the reading of 480 ms and the field's at 500 are out: it waits for them,
        // 20 ms, and the next ones keep to the interval from start to start.
        assertEquals(listOf(20L, 520, 1020, 1520, 2020, 2520), statuses)
        // A cycle lasts 220 ms (status, party page, 6 pages, 2 readings with the field's after each): the position is
        // still read twice inside it, between its pages.
        for (status in statuses.drop(1)) {
            val cycle = rig.fake.received.map { it.nanos / 1_000_000 to it.command }.filter { it.first in status..status + 220 }
            val pages = cycle.filter { it.second.endsWith(" ${RetroArchClient.MAX_READ_BYTES}") }
            assertEquals(listOf(status + 60, status + 160), readings.filter { it in status..pages.last().first }, "the cycle at $status")
        }
    }

    @Test
    fun `a position read after the state's sample is not followed by the older one`() {
        val rig = rig(intervalMs = 100, positionWanted = { true })      // slots of 20 ms
        rig.tick()
        val before = rig.events.size
        // The player moves right after the page with the actor is loaded: the state keeps the position it has in that page.
        rig.fake.afterRead = { address ->
            if (address == 0xAC000L) {
                rig.walkTo(700, 701)
                rig.clock!!.advance(30)                // so the next reading is due
                rig.fake.afterRead = null
            }
        }

        val events = rig.tick()

        assertEquals(listOf(PlayerPositionDto(700, 701)), events.positions(), "the newest reading, and the cycle's older one not after it")
        assertEquals(emptyList<EventType>(), events.map { it.type }.filter { it == EventType.PlayerChanged })
        assertEquals(SyntheticRam.START_X, rig.store.currentState!!.player.positionX, "the state keeps what its own page said")
        assertTrue(rig.events.size > before)
    }

    @Test
    fun `a lost reply during the cycle does not change health nor stretch the cycle past its patience`() {
        val rig = rig(intervalMs = 100, timeoutMs = 10_000, positionWanted = { true })
        rig.tick()
        // The reading before the first page is the one to lose; the cycle's own requests answer.
        rig.fake.dropNextReads(1)

        val started = System.nanoTime()
        val events = rig.tick()
        val elapsedMs = (System.nanoTime() - started) / 1_000_000

        assertTrue(events.health().none { it.status == HealthStatus.Error }, "no error event")
        assertEquals(HealthStatus.Healthy, rig.store.status)
        assertEquals(1, rig.fake.commands.count { it.endsWith(" $quickSize") }, "asked once")
        assertTrue(elapsedMs < 10_000, "the lost reading waits its 60 ms, not the client's 10 s: $elapsedMs ms")
    }

    @Test
    fun `no actor known, no reading during the cycle`() {
        val rig = rig(ram = SyntheticRam.outOfField(), positionWanted = { true })
        rig.tick()
        rig.fake.clearLog()
        rig.tick()

        assertTrue(rig.fake.commands.none { it.endsWith(" $quickSize") }, "no quick read: ${rig.fake.commands}")
    }

    // ---- the characters of the map ----------------------------------------------------------------------------------

    /** The first character of the map in the synthetic game (key 0x21, at 144, 647) and its address. */
    private val npc = SyntheticRam.FIRST_CHARACTER.toInt()

    private fun List<Event>.actors() =
        filter { it.type == EventType.FieldActorsChanged }.map { (it.payload as FieldActorsDto).actors }

    private val mapActors = SyntheticRam.CHARACTERS.map { FieldActorDto(it.key1, it.x, it.y, it.dir) }

    @Test
    fun `the first cycle sends the characters of the map, apart from the state`() {
        val rig = rig()

        val events = rig.tick()

        assertEquals(listOf(mapActors), events.actors())
        val state = events.first { it.type == EventType.InitialState }
        assertTrue(EventSerializer.payloadJson(state).contains("\"player\""))
        assertTrue(!EventSerializer.payloadJson(state).contains("1072"), "the characters (one at x 1072) are not in the state")
    }

    @Test
    fun `an unchanged list sends nothing`() {
        val rig = rig()
        rig.tick()

        assertEquals(emptyList<Event>(), rig.tick())
        assertEquals(emptyList<Event>(), rig.tick())
    }

    @Test
    fun `a character that walks sends the whole list again`() {
        val rig = rig()
        rig.tick()

        rig.putInt(npc + 0x58, 150)
        rig.putInt(npc + 0x60, 6)
        val events = rig.tick()

        assertEquals(listOf(EventType.FieldActorsChanged), events.map { it.type }, "only the list goes out, no state event")
        assertEquals(listOf(listOf(FieldActorDto(0x21, 150, 647, 6)) + mapActors.drop(1)), events.actors())
    }

    @Test
    fun `a character the game does not draw is left out`() {
        val rig = rig()
        rig.tick()

        rig.putInt(npc + 0x9C, 0)                      // no animation file
        val events = rig.tick()

        assertEquals(listOf(mapActors.drop(1)), events.actors())
    }

    @Test
    fun `out of the field the list is empty, and it is sent once when it empties`() {
        val rig = rig()
        rig.tick()

        rig.fake.ram = SyntheticRam.outOfField()   // no field
        assertEquals(listOf(emptyList<FieldActorDto>()), rig.tick().actors())
        assertEquals(emptyList<List<FieldActorDto>>(), rig.tick().actors())
    }

    /** The reads of a steady full cycle (the second) of [ram]. */
    private fun steadyReads(ram: ByteArray, actorsWanted: Boolean): List<String> {
        val rig = rig(ram = ram, actorsWanted = { actorsWanted })
        rig.tick()
        rig.fake.clearLog()
        rig.tick()
        return rig.fake.commands.filter { it.startsWith("READ_CORE_MEMORY") }
    }

    private fun page(address: Long) = "READ_CORE_MEMORY ${address.toString(16).uppercase()} ${RetroArchClient.MAX_READ_BYTES}"

    @Test
    fun `out of the field the registry's tasks are not read for the characters`() {
        val pages = steadyReads(SyntheticRam.outOfField(), actorsWanted = true)

        // The walk for the player visits every task of the registry; the characters add nothing to it.
        assertEquals(pages.distinct(), pages, "no page is asked twice")
        assertTrue(pages.containsAll(SyntheticRam.OTHER_TASKS.map { page(it / 0x4000 * 0x4000) }), "$pages")
        assertEquals(steadyReads(SyntheticRam.outOfField(), actorsWanted = false), pages)
    }

    @Test
    fun `an overlay that connects later gets the characters with the state`() {
        val rig = rig()
        rig.tick()

        rig.loop.requestInitialEvents()
        val events = rig.tick()

        assertEquals(listOf(EventType.InitialState, EventType.FieldActorsChanged), events.map { it.type }.take(2))
        assertEquals(listOf(mapActors), events.actors())
    }

    @Test
    fun `after an error the list is sent again with the next state`() {
        val rig = rig()
        rig.tick()
        rig.fake.ram = SyntheticRam.title()
        rig.tick()                                      // waiting_for_save: the state is lost
        rig.fake.ram = SyntheticRam.game()
        val events = rig.tick()

        assertEquals(listOf(mapActors), events.actors())
    }

    @Test
    fun `a full cycle in the field asks for at most 3 pages more than before the characters`() {
        val reads = steadyReads(SyntheticRam.game(), actorsWanted = true)
        val without = steadyReads(SyntheticRam.game(), actorsWanted = false)

        assertTrue(reads.size in 5..8, "reads in a steady full cycle: $reads")
        assertTrue(reads.size - without.size in 0..3, "with the characters $reads, without $without")
        // The look for the area name banner already visits every task of the registry, and with it the page of the
        // characters: they cost no page more here.
        assertEquals(without, reads)
    }
}

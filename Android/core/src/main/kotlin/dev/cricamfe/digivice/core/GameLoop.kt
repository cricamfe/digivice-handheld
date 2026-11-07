package dev.cricamfe.digivice.core

import dev.cricamfe.digivice.core.events.Event
import dev.cricamfe.digivice.core.events.GameStateStore
import dev.cricamfe.digivice.core.events.converters.StateConverter
import dev.cricamfe.digivice.core.events.dto.FieldActorDto
import dev.cricamfe.digivice.core.events.dto.FieldActorsDto
import dev.cricamfe.digivice.core.events.dto.FieldChangingDto
import dev.cricamfe.digivice.core.events.dto.PlayerDto
import dev.cricamfe.digivice.core.events.dto.extensions.isNotEmpty
import dev.cricamfe.digivice.core.events.dto.shared.Optional
import dev.cricamfe.digivice.core.events.dto.QuestOffersDto
import dev.cricamfe.digivice.core.events.dto.PartnerTechniquesDto
import dev.cricamfe.digivice.core.events.dto.PartnerTechniquesFormDto
import dev.cricamfe.digivice.core.events.dto.PartnerTechniquesPartnerDto
import dev.cricamfe.digivice.core.events.dto.PlayerPositionDto
import dev.cricamfe.digivice.core.events.factory.HealthEventFactory
import dev.cricamfe.digivice.core.events.factory.StateEventFactory
import dev.cricamfe.digivice.core.domain.models.Player
import dev.cricamfe.digivice.core.domain.models.State
import dev.cricamfe.digivice.core.events.models.EventType
import dev.cricamfe.digivice.core.memory.MemoryReadException
import dev.cricamfe.digivice.core.memory.readers.FieldActorsReader
import dev.cricamfe.digivice.core.memory.readers.FieldTaskProbe
import dev.cricamfe.digivice.core.memory.readers.PartnerTechniquesReader
import dev.cricamfe.digivice.core.memory.readers.PlayerPositionProbe
import dev.cricamfe.digivice.core.retroarch.ConnectionCodes
import dev.cricamfe.digivice.core.retroarch.ConnectionResult
import dev.cricamfe.digivice.core.retroarch.MemoryReadLost
import dev.cricamfe.digivice.core.retroarch.RetroArchConnector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import java.util.concurrent.atomic.AtomicBoolean
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Port of `Backend/Application/GameLoopService.cs` over RetroArch: every cycle it connects, composes the state, and
 * hands the resulting events to [dispatch] in the order the original emits them (health after the connection, the state
 * events, the new state stored, health again). A failure goes out as an error event that also clears [store], so a
 * reconnection starts again with an `InitialState`.
 *
 * What the original did not have is a lost UDP reply, and RetroArch's pause:
 *  - `GET_STATUS PAUSED`: nothing is read, composed or emitted; the state stays as it is, with no error.
 *  - A lost reply (a status, a read that failed twice) skips the cycle, keeps the state, emits nothing.
 *  - After [LOST_CYCLES_BEFORE_PROBE] cycles in a row without a usable reply, `VERSION` is asked each cycle: if nobody
 *    answers either, RetroArch is gone and the error is `retroarch_unreachable`.
 *  - If `VERSION` still answers, the loop keeps skipping until [LOST_CYCLES_BEFORE_FAILURE] cycles in a row, which is
 *    `memory_read_failed`: RetroArch lives but the memory does not reach us, and stale data must not pass for fresh.
 *  Any cycle that gets a reply (even a pause or an error) resets the count.
 *
 * [tick] is one synchronous cycle and is what the tests drive. [start] runs it every [intervalMs] on
 * `Dispatchers.IO`, measured from start to start; a tick that overruns skips the ticks it missed instead of bursting.
 * No lock is held while [dispatch] runs. [dispatch] is called on the loop's thread, one event at a time.
 *
 * The position of the player is read apart from the cycle, every slot (`intervalMs / 5`, 100 ms by default), in the same
 * thread and in the same order of requests: [fastTick] is one request, the bytes of the player's actor at the address
 * the last cycle found, which are checked as the registry walk checks them and give the position; if it changed,
 * `PlayerPositionChanged` goes out, apart from the state and its diff. [start] wakes at the next full cycle (every
 * `intervalMs`) or a slot after the last reading, whichever is first, so the readings stay evenly spaced whatever the
 * cycle did to the clock. Nothing is asked when no actor is known (before the first cycle, or after a pause, a lost
 * reply or an error), when the page has no state yet, when no page could take the event ([positionWanted]), or while
 * the loop is stopped (the policy stops it with the display). A lost or refused reply, or bytes that are not the
 * player's any more, change nothing: health is the full cycle's business, and the next cycle finds the actor again.
 *
 * A full cycle takes about 150 ms of requests, which would leave the position unread for that long. So the loop also
 * reads it between the requests of a cycle ([PagedMemoryAccessor.beforeRequest]): when a slot has
 * passed since the last reading and an actor is known, one quick read goes out before the next page, on the same socket
 * and thread, one request at a time. It does not touch the pages the cycle is composing the state from. The state's own
 * sample of the position is taken early in the cycle, so a diff at its end could carry a position older than a quick
 * reading sent meanwhile: the state's diff therefore never carries the position (`PlayerPositionChanged` does, and
 * the state in the store keeps its own), and the cycle sends the state's position only when no quick reading was made in
 * it. The page then never gets an older position after a newer one.
 *
 * The field's main task is read apart from the cycle too, every slot ([readField]). It says when the game starts to close the
 * field, where it is going, and when the next field opens, which is what the page times its fade with. From the first
 * sign, `FieldChanging` goes out and the old actor is not read again; the state then comes in full cycles, one after the
 * other, until a cycle finds the new field running. That cycle ends the change, after the map, position and characters of
 * the same sample, so the page never opens on a mix of two places.
 *
 * A client that connects after the events went out (the overlay's page, opened later) calls [requestInitialEvents] from
 * any thread. The next tick starts by sending it the state and the health as they are, and goes on as usual. The store
 * is only ever touched by the loop's thread.
 *
 * [start] after [stop] never overlaps: the new run waits for the tick of the old one to end before it opens the
 * connector again, so a tick that was interrupted cannot see the loop as running and report its own interruption.
 */
/** The field is closing or not open yet; [to] is the map it leaves for, when known. */
private data class FieldChanging(val to: String?)

class GameLoop(
    private val connector: RetroArchConnector,
    private val module: CoreModule,
    private val store: GameStateStore,
    private val dispatch: (Event) -> Unit,
    private val intervalMs: Long = POLL_INTERVAL_MS,
    /** Whether a page is there to take the position; asked from the loop's thread before each quick read. */
    private val positionWanted: () -> Boolean = { true },
    /** Whether the page draws the characters of the field; asked by each full cycle, for the reads they take. */
    private val actorsWanted: () -> Boolean = { true },
    /** The clock the loop spaces its cycles and readings by; the tests give it a virtual one. */
    private val nanoTime: () -> Long = System::nanoTime,
    /** Where [start] runs the loop; the tests give it a dispatcher of virtual time. */
    private val context: CoroutineContext = Dispatchers.IO,
) : GameSessionLoop {
    private var lostCycles = 0
    /** A cycle found the game paused: a change of field does not end while it lasts, so nothing to ask for it at once. */
    private var gamePaused = false

    /** The characters of the map the page was last told; null when it has been told nothing (or the state was lost). */
    private var sentActors: List<FieldActorsReader.Actor>? = null

    /** The techniques of the party the page was last told; null when it has been told none. */
    private var sentTechniques: List<PartnerTechniquesReader.Partner>? = null
    private var sentOffers: List<String>? = null

    /** The position the page was last told, by the state or by a quick read; null when it has none. */
    private var sentPosition: PlayerPositionProbe.Position? = null

    private var loggedFieldPhase: String? = null

    /** When the field's task was last asked for, on [nanoTime]'s clock. */
    private var lastFieldReadNanos = Long.MIN_VALUE / 2

    /** When the position was last asked for (any quick read, answered or lost) was sent, on [nanoTime]'s clock. */
    private var lastPositionReadNanos = Long.MIN_VALUE / 2

    /** A full cycle is running: the requests it makes are the moments to read the position. */
    private var inCycle = false

    /** A quick read gave a position during this cycle, newer than anything the cycle's own state sample has. */
    private var positionReadInCycle = false

    /** A quick read started a change of field during this cycle. */
    private var fieldChangeFoundInCycle = false

    /** The change of field the page was last told of; null when the field is running, or there is none. */
    private var fieldChange: FieldChanging? = null

    private val positionSlotNanos = maxOf(1L, intervalMs / FAST_TICKS_PER_CYCLE) * NANOS_PER_MS

    init {
        connector.betweenRequests = {
            if (inCycle) {
                readPosition(positionSlotNanos)
                readField()
            }
        }
    }

    /** Set by [stop] so a tick that was interrupted does not report its own interruption as an error. */
    @Volatile private var stopping = false

    private var job: Job? = null

    /** The job [stop] cancelled last: its tick may still be running when [start] is called again. */
    private var retired: Job? = null

    private val initialEventsRequested = AtomicBoolean(false)

    /**
     * Asks for the state (if there is one) and the health to be sent again at the start of the next tick, as a client
     * that has just connected needs them. Thread safe; asking twice before the tick is the same as asking once.
     */
    override fun requestInitialEvents() {
        initialEventsRequested.set(true)
    }

    /** One cycle of the loop. */
    fun tick() {
        inCycle = true
        positionReadInCycle = false
        fieldChangeFoundInCycle = false
        try {
            runCycle()
        } finally {
            inCycle = false
        }
    }

    private fun runCycle() {
        try {
            if (!stopping && initialEventsRequested.getAndSet(false)) {
                store.currentState?.let {
                    dispatch(Event(EventType.InitialState, StateConverter.toDto(it)))
                    sentPosition = positionOf(it.player)
                    sentActors?.takeIf { actors -> actors.isNotEmpty() }?.let { actors -> dispatch(actorsEvent(actors)) }
                    fieldChange?.let { change -> dispatch(fieldChangeEvent(change)) }
                    sentTechniques?.let { techniques -> dispatch(techniquesEvent(techniques)) }
                    sentOffers?.let { offers -> dispatch(offersEvent(offers)) }
                }
                dispatch(HealthEventFactory.current(store))
            }
            gamePaused = false
            when (val connection = connector.beginCycle()) {
                is ConnectionResult.Lost -> return onLostCycle(connection.detail)
                is ConnectionResult.Paused -> {
                    lostCycles = 0
                    gamePaused = true
                    module.playerPosition.forget()
                    return
                }
                is ConnectionResult.Failed -> {
                    lostCycles = 0
                    return fail(connection.code, connection.detail)
                }
                ConnectionResult.Ready -> Unit
            }
            if (stopping) return

            emit(HealthEventFactory.success(store))

            val newState = module.stateComposer.compose()
            // The characters of the map are only looked for in the field, where the walk found the player's actor.
            val actors = if (module.playerPosition.actor != null && actorsWanted()) module.fieldActors.read() else emptyList()
            if (stopping) return
            val previousState = store.currentState
            val techniques = module.partnerTechniques.read()
            val offers = module.questConditions.read()
            val field = module.fieldTask.read()
            logFieldPhase(field)
            // A change the cycle is first to see goes out before the map it brings.
            if (fieldChange == null) sendFieldChange(changeOf(field, newState.player.mapId))
            emit(withoutPosition(StateEventFactory.create(previousState, newState)))
            syncPosition(previousState, newState)
            syncActors(actors)
            syncTechniques(techniques)
            syncOffers(offers)
            // A change a quick read found during this cycle is newer than its sample, which still has the field running.
            if (!fieldChangeFoundInCycle) sendFieldChange(changeOf(field, newState.player.mapId))

            store.updateState(newState)
            emit(HealthEventFactory.success(store))
            lostCycles = 0
        } catch (e: MemoryReadLost) {
            onLostCycle(e.message ?: "A read was lost.")
        } catch (e: MemoryReadException) {
            connector.reset()
            lostCycles = 0
            fail(ConnectionCodes.MEMORY_READ_FAILED, e.message)
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Error processing game state in GameLoop.", e)
            connector.reset()
            lostCycles = 0
            fail(ConnectionCodes.STATE_COMPOSE_FAILED, e.message)
        }
    }

    /** The state's diff without the position: it travels as `PlayerPositionChanged`; see the class comment. */
    private fun withoutPosition(events: List<Event>): List<Event> = events.mapNotNull { event ->
        val player = event.payload as? PlayerDto
        if (event.type != EventType.PlayerChanged || player == null) return@mapNotNull event
        player.copy(positionX = Optional.Empty, positionY = Optional.Empty)
            .takeIf { it.isNotEmpty(PlayerDto()) }
            ?.let { Event(event.type, it) }
    }

    /**
     * The first state carries the position with it. After that the position goes out by itself: the state's sample, if
     * no quick reading was made in this cycle (any that was is newer than the sample, which was taken early on) and it
     * is not the one the page has. A sample that brings a new map always goes out: a quick reading of this cycle was of
     * the actor of the old one.
     */
    private fun syncPosition(previous: State?, new: State) {
        val position = positionOf(new.player)
        val trusted = !positionReadInCycle || previous?.player?.mapId != new.player.mapId
        if (previous != null && trusted && position != sentPosition) {
            dispatch(Event(EventType.PlayerPositionChanged, PlayerPositionDto(position.x, position.y)))
        }
        if (previous == null || trusted) sentPosition = position
    }

    /** The list goes out apart from the state, and only when it is not the one the page has. */
    private fun syncActors(actors: List<FieldActorsReader.Actor>) {
        if (actors == sentActors) return
        sentActors = actors
        dispatch(actorsEvent(actors))
    }

    /** The same for the techniques of the party. */
    private fun syncTechniques(techniques: List<PartnerTechniquesReader.Partner>) {
        if (techniques == sentTechniques) return
        sentTechniques = techniques
        dispatch(techniquesEvent(techniques))
    }

    /** The same for the quests the game would start now. */
    private fun syncOffers(offers: List<String>) {
        if (offers == sentOffers) return
        sentOffers = offers
        dispatch(offersEvent(offers))
    }

    private fun offersEvent(offers: List<String>) = Event(EventType.QuestOffersChanged, QuestOffersDto(offers))

    private fun techniquesEvent(techniques: List<PartnerTechniquesReader.Partner>) = Event(
        EventType.PartnerTechniquesChanged,
        PartnerTechniquesDto(
            techniques.map { partner ->
                PartnerTechniquesPartnerDto(partner.digimonId, partner.forms.map { PartnerTechniquesFormDto(it.id, it.skills) })
            },
        ),
    )

    private fun actorsEvent(actors: List<FieldActorsReader.Actor>) =
        Event(EventType.FieldActorsChanged, FieldActorsDto(actors.map { FieldActorDto(it.key1, it.x, it.y, it.dir) }))

    private fun positionOf(player: Player) = PlayerPositionProbe.Position(player.positionX, player.positionY)

    /**
     * One quick tick: one request for the bytes of the player's actor, and `PlayerPositionChanged` if its position is
     * not the one the page has. Never connects, never walks the registry, never touches the state or the health. With
     * [minGapMs] it does nothing when the position was read less than that ago, so readings are not bunched together.
     */
    fun fastTick(minGapMs: Long = 0) = readPosition(minGapMs * NANOS_PER_MS)

    /**
     * Reads the position if [minGapNanos] have passed since the last time, an actor is known and the page has a state to
     * put it in (before that it would get a position ahead of the state). A failure to dispatch is logged: this runs
     * between the requests of a cycle and must not end it.
     */
    private fun readPosition(minGapNanos: Long) {
        try {
            if (stopping) return
            val probe = module.playerPosition
            val actor = probe.actor ?: return
            if (sentPosition == null || fieldChange != null || !positionWanted()) return
            if (nanoTime() - lastPositionReadNanos < minGapNanos) return

            // The spacing counts from when the request goes out, which is what the page's arrivals are spaced by.
            lastPositionReadNanos = nanoTime()
            val bytes = connector.readQuick(actor, probe.size)
            val position = bytes?.let { probe.position(it) } ?: return
            if (inCycle) positionReadInCycle = true
            if (stopping || position == sentPosition) return
            sentPosition = position
            dispatch(Event(EventType.PlayerPositionChanged, PlayerPositionDto(position.x, position.y)))
        } catch (e: Exception) {
            logger.log(Level.WARNING, "Reading the position failed.", e)
        }
    }

    /** One quick read of the field's main task. */
    fun fieldTick() = readField()

    /**
     * Reads the field's task, if the last cycle found one and no change is known yet: from then on the cycles have it. Only
     * ever starts a change, which a cycle ends. See [readPosition] for the conditions.
     */
    private fun readField() {
        try {
            if (stopping || fieldChange != null || sentPosition == null || !positionWanted()) return
            val probe = module.fieldTask
            val address = probe.address ?: return
            if (nanoTime() - lastFieldReadNanos < positionSlotNanos) return

            lastFieldReadNanos = nanoTime()
            val bytes = connector.readQuick(address, FieldTaskProbe.SIZE) ?: return
            val change = changeOf(probe.parse(bytes), null)
            if (stopping) return
            if (inCycle && change != null) fieldChangeFoundInCycle = true
            sendFieldChange(change)
        } catch (e: Exception) {
            logger.log(Level.WARNING, "Reading the field failed.", e)
        }
    }

    /**
     * What a reading of the field's task says, or null when there is no change going on. With no task, in a mode that is
     * a field ([mapId]; null is a task that was there a moment ago) one is on its way; any other mode has no field.
     */
    private fun changeOf(field: FieldTaskProbe.Reading?, mapId: String?): FieldChanging? = when {
        field == null -> FieldChanging(fieldChange?.to).takeIf { mapId == null || FieldTaskProbe.isFieldMode(mapId) }
        field.isRunning -> null
        else -> FieldChanging(field.leavingFor ?: fieldChange?.to)
    }

    private fun sendFieldChange(change: FieldChanging?) {
        if (change == fieldChange) return
        val started = fieldChange == null
        fieldChange = change
        if (started) module.playerPosition.forget()
        logger.log(Level.FINE, "Field change: $change")
        dispatch(fieldChangeEvent(change))
    }

    /** The steps the game goes through in a change of field, to time the page's fade with what it really does. */
    private fun logFieldPhase(field: FieldTaskProbe.Reading?) {
        val phase = field?.let { "field ${it.state}.${it.substate} covered=${it.bannerCovers}" } ?: "no field"
        if (phase != loggedFieldPhase) logger.log(Level.FINE, "Field phase: $phase")
        loggedFieldPhase = phase
    }

    private fun fieldChangeEvent(change: FieldChanging?) =
        Event(EventType.FieldChanging, FieldChangingDto(changing = change != null, to = change?.to))

    private fun onLostCycle(detail: String) {
        if (stopping) return
        module.playerPosition.forget()
        lostCycles++
        logger.log(Level.FINE, "Cycle skipped ($lostCycles in a row): $detail")

        if (lostCycles >= LOST_CYCLES_BEFORE_PROBE && !connector.isRetroArchAlive()) {
            if (stopping) return
            val lost = lostCycles
            connector.reset()
            lostCycles = 0
            fail(ConnectionCodes.RETROARCH_UNREACHABLE, "No reply from RetroArch for $lost cycles in a row ($detail)")
        } else if (lostCycles >= LOST_CYCLES_BEFORE_FAILURE) {
            val lost = lostCycles
            connector.reset()
            lostCycles = 0
            fail(
                ConnectionCodes.MEMORY_READ_FAILED,
                "RetroArch answers VERSION but $lost cycles in a row lost a reply ($detail)",
            )
        }
    }

    private fun fail(code: String, detail: String?) {
        if (stopping) return
        module.playerPosition.forget()
        sentPosition = null
        sentActors = null
        sentTechniques = null
        fieldChange = null
        module.fieldTask.forget()
        emit(HealthEventFactory.error(store, code, detail))
    }

    private fun emit(events: List<Event>) {
        for (event in events) dispatch(event)
    }

    /**
     * Starts polling on [scope] with [context] (`Dispatchers.IO`). Does nothing if the loop is already running. After a
     * [stop] the run begins once the tick of the previous one has ended.
     */
    @OptIn(DelicateCoroutinesApi::class)
    @Synchronized
    override fun start(scope: CoroutineScope) {
        if (job?.isActive == true) return
        val previous = retired
        // ATOMIC: even a run that is stopped before it gets going must take its turn in the line behind [previous].
        job = scope.launch(context, CoroutineStart.ATOMIC) {
            withContext(NonCancellable) { previous?.join() }
            synchronized(this@GameLoop) {
                // stop() holds this lock when it cancels, so a run stopped meanwhile does not undo its `stopping`.
                ensureActive()
                stopping = false
                connector.open()
            }
            val intervalNanos = intervalMs * NANOS_PER_MS
            var nextCycle = nanoTime()
            while (isActive) {
                try {
                    if (nanoTime() >= nextCycle) {
                        tick()
                        // The cycle may have ended a slot or more after the last reading: no waiting for the next one.
                        readPosition(positionSlotNanos)
                        readField()
                        nextCycle += intervalNanos
                        val now = nanoTime()
                        if (now >= nextCycle) nextCycle += ((now - nextCycle) / intervalNanos + 1) * intervalNanos
                        // A change of field is over when a cycle sees it: the page is waiting for that one.
                        if (fieldChange != null && !gamePaused) nextCycle = now
                    } else {
                        readPosition(positionSlotNanos)
                        readField()
                        if (fieldChange != null && !gamePaused) nextCycle = nanoTime()
                    }
                } catch (e: Exception) {
                    logger.log(Level.SEVERE, "A tick of GameLoop failed.", e)
                }
                // The next reading is a slot after the last one, wherever the cycle's requests put it, so the readings
                // keep an even spacing; with nothing to read the conditions are looked at again a slot later.
                val now = nanoTime()
                val positionDue = lastPositionReadNanos + positionSlotNanos
                val wake = minOf(nextCycle, if (positionDue > now) positionDue else now + positionSlotNanos)
                delay(maxOf(0L, (wake - now + NANOS_PER_MS - 1) / NANOS_PER_MS))
            }
        }
    }

    /**
     * Stops polling and closes the sockets. Returns at once: a request that is waiting for a reply is interrupted, and a
     * tick that was running ends without emitting anything.
     */
    @Synchronized
    override fun stop() {
        stopping = true
        job?.let {
            it.cancel()
            retired = it
        }
        job = null
        connector.close()
    }

    companion object {
        const val POLL_INTERVAL_MS = 500L

        /** Cycles in a row without a usable reply before `VERSION` is asked each cycle (1.5 s at the default cadence). */
        const val LOST_CYCLES_BEFORE_PROBE = 3

        /** Cycles in a row without a usable reply, with RetroArch still answering `VERSION`, before `memory_read_failed`. */
        const val LOST_CYCLES_BEFORE_FAILURE = 10

        /** Slots per full cycle: the position is read this many times as often as the state. */
        const val FAST_TICKS_PER_CYCLE = 5

        private const val NANOS_PER_MS = 1_000_000L
        private val logger = Logger.getLogger(GameLoop::class.java.name)
    }
}

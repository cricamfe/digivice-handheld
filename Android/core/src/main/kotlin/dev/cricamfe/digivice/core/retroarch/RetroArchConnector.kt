package dev.cricamfe.digivice.core.retroarch

import dev.cricamfe.digivice.core.memory.MemoryReadException
import dev.cricamfe.digivice.core.memory.MemoryReader
import dev.cricamfe.digivice.core.memory.repositories.IAddressesRepository
import java.util.logging.Level
import java.util.logging.Logger

/** How the start of a cycle went; the Kotlin counterpart of the C# `ConnectionAttemptResult`. */
sealed interface ConnectionResult {
    /** RetroArch runs Digimon World 2003 with a save loaded: the state can be read. */
    data object Ready : ConnectionResult

    /** The game is paused (or RetroArch's menu is open): keep the last state, read nothing. */
    data object Paused : ConnectionResult

    /** A reply did not arrive. Not an error by itself; see [GameLoop] for what repeated losses mean. */
    data class Lost(val detail: String) : ConnectionResult

    /** The game cannot be read, for the reason in [code] (one of [ConnectionCodes]). */
    data class Failed(val code: String, val detail: String?) : ConnectionResult
}

/**
 * Decides, at the start of every cycle, whether the game can be read: the counterpart of `IDuckstationConnector`.
 *
 * It asks `VERSION` only when it has no verified session (one command less per cycle while playing), `GET_STATUS` every
 * cycle, and checks [GameSignature] when it connects and whenever the title in `GET_STATUS` changes. A rejected title
 * is checked again every [WRONG_GAME_RECHECK_CYCLES] cycles, because RetroArch reports the title as soon as the content
 * loads, when the game may still be booting and its RAM does not carry the signature yet.
 *
 * It also starts the page cache of [accessor] for the cycle, and tells the title screen from a save by the party slots.
 * Not thread safe: the loop that owns it calls it one cycle at a time.
 */
class RetroArchConnector(
    private val client: RetroArchClient,
    private val accessor: PagedMemoryAccessor,
    private val addressesRepository: IAddressesRepository,
) {
    private val reader = MemoryReader(accessor)

    private var cycle = 0L
    private var verifiedTitle: String? = null
    private var rejectedTitle: String? = null
    private var rejectedAtCycle = 0L

    fun beginCycle(): ConnectionResult {
        accessor.beginCycle()
        cycle++

        if (verifiedTitle == null && client.version() == null) {
            reset()
            return ConnectionResult.Failed(
                ConnectionCodes.RETROARCH_UNREACHABLE,
                "No reply to VERSION from ${client.endpoint}.",
            )
        }

        val title = when (val status = client.status()) {
            RetroArchStatus.Unreachable -> return ConnectionResult.Lost("No reply to GET_STATUS.")
            RetroArchStatus.Contentless -> {
                reset()
                return ConnectionResult.Failed(ConnectionCodes.NO_CONTENT, null)
            }
            is RetroArchStatus.Paused -> return ConnectionResult.Paused
            is RetroArchStatus.Playing -> status.title
        }

        // Another console's game: no need to read anything, and its core may not hand out the memory at all.
        if (title.contains(',') && title.substringBefore(',') != PLAYSTATION_SYSTEM) {
            reset()
            return ConnectionResult.Failed(ConnectionCodes.OTHER_CONSOLE, "The content '$title' is of another console.")
        }

        try {
            if (title != verifiedTitle) {
                verifySignature(title)?.let { return it }
            }
            if (isPartyEmpty()) return ConnectionResult.Failed(ConnectionCodes.WAITING_FOR_SAVE, null)
        } catch (e: MemoryReadLost) {
            return ConnectionResult.Lost(e.message ?: "A read was lost.")
        } catch (e: MemoryReadRefused) {
            // Verified or not, RetroArch will not hand out the memory: the next cycle tries again from scratch.
            return ConnectionResult.Failed(ConnectionCodes.MEMORY_UNAVAILABLE, e.reason)
        }
        return ConnectionResult.Ready
    }

    /** Null when [title] carries the game's signature (and is now verified), or the failure. */
    private fun verifySignature(title: String): ConnectionResult.Failed? {
        if (title == rejectedTitle && cycle - rejectedAtCycle < WRONG_GAME_RECHECK_CYCLES) return wrongGame(title)

        if (GameSignature.matches(client)) {
            verifiedTitle = title
            rejectedTitle = null
            return null
        }
        verifiedTitle = null
        rejectedTitle = title
        rejectedAtCycle = cycle
        logger.log(Level.INFO, "The RAM of '$title' does not carry the signature of Digimon World 2003.")
        return wrongGame(title)
    }

    private fun wrongGame(title: String) =
        ConnectionResult.Failed(ConnectionCodes.WRONG_GAME, "The content '$title' is not Digimon World 2003.")

    /**
     * No save is loaded yet. At the title screen the three party slots hold the empty marker; earlier, while the game
     * boots, its memory is still zeros, which would read as the same partner three times, and a party cannot repeat one.
     */
    private fun isPartyEmpty(): Boolean {
        val party = addressesRepository.getPartyAddresses()
        val members = party.slots.map { reader.readByte(it.address).toInt() }.filter { it != party.emptySlotId }
        return party.slots.isNotEmpty() && (members.isEmpty() || members.distinct().size != members.size)
    }

    /**
     * A small read outside the page cache, for [GameLoop]'s quick ticks: one request, no retry, [QUICK_READ_TIMEOUT_MS] of
     * patience. Null when the reply is lost or refused; that says nothing about the connection.
     */
    fun readQuick(address: Long, size: Int): ByteArray? = client.tryRead(address, size, QUICK_READ_TIMEOUT_MS)

    /** What to do just before each page request of a cycle; see [PagedMemoryAccessor.beforeRequest]. */
    var betweenRequests: (() -> Unit)?
        get() = accessor.beforeRequest
        set(value) {
            accessor.beforeRequest = value
        }

    /** Whether RetroArch answers `VERSION`; the loop uses it to tell lost datagrams from a RetroArch that is gone. */
    fun isRetroArchAlive(): Boolean = client.version() != null

    /** Forgets the verified session: the next cycle asks `VERSION` and checks the signature again. */
    fun reset() {
        verifiedTitle = null
        rejectedTitle = null
    }

    /** Opens the way for requests again after [close]. */
    fun open() = client.reopen()

    /** Interrupts a request in flight and leaves no socket open. */
    fun close() = client.close()

    companion object {
        /** About 5 s at the loop's cadence. */
        const val WRONG_GAME_RECHECK_CYCLES = 10

        /** The system `GET_STATUS` names before the comma for the game's core. */
        const val PLAYSTATION_SYSTEM = "playstation"

        /** A quick read answers within a frame or two (about 20 ms each); later than this it is not worth waiting for. */
        const val QUICK_READ_TIMEOUT_MS = 60

        private val logger = Logger.getLogger(RetroArchConnector::class.java.name)
    }
}

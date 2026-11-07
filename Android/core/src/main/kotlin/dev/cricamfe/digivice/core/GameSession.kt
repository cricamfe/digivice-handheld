package dev.cricamfe.digivice.core

import dev.cricamfe.digivice.core.events.Event
import dev.cricamfe.digivice.core.events.GameStateStore
import dev.cricamfe.digivice.core.retroarch.PagedMemoryAccessor
import dev.cricamfe.digivice.core.retroarch.RetroArchClient
import dev.cricamfe.digivice.core.retroarch.RetroArchConnector
import java.io.Closeable
import kotlinx.coroutines.CoroutineScope

/** Owns one source's connection and loop; a retired session cannot publish events or be restarted. */
class GameSession internal constructor(
    val source: GameSource,
    private val readPortClosed: () -> Boolean?,
    dispatch: (Event, GameSession) -> Unit,
    createLoop: ((Event) -> Unit) -> GameSessionLoop,
) : GameSessionLoop, Closeable {
    @Volatile private var closed = false
    private val loop = createLoop { event -> if (!closed) dispatch(event, this) }

    val portClosed: Boolean? get() = readPortClosed()

    @Synchronized
    override fun start(scope: CoroutineScope) {
        if (!closed) loop.start(scope)
    }

    @Synchronized
    override fun stop() {
        if (!closed) loop.stop()
    }

    @Synchronized
    override fun requestInitialEvents() {
        if (!closed) loop.requestInitialEvents()
    }

    @Synchronized
    override fun close() {
        if (closed) return
        closed = true
        loop.stop()
    }

    companion object {
        fun create(
            source: GameSource,
            readDefinition: (String) -> String,
            listDefinitions: (String) -> List<String>,
            dispatch: (Event, GameSession) -> Unit,
            positionWanted: () -> Boolean = { true },
            actorsWanted: () -> Boolean = { true },
        ): GameSession = when (source) {
            GameSource.RETROARCH -> {
                val client = RetroArchClient()
                val accessor = PagedMemoryAccessor(client)
                val module = CoreModule(accessor, readDefinition, listDefinitions)
                val connector = RetroArchConnector(client, accessor, module.addressesRepository)
                GameSession(source, { client.portState }, dispatch) { emit ->
                    GameLoop(
                        connector, module, GameStateStore(), emit,
                        positionWanted = positionWanted,
                        actorsWanted = actorsWanted,
                    )
                }
            }
        }
    }
}

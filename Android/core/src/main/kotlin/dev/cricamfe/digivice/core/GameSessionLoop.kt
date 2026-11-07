package dev.cricamfe.digivice.core

import kotlinx.coroutines.CoroutineScope

/** A source of the same Digivice events, regardless of where the game runs. */
interface GameSessionLoop {
    fun start(scope: CoroutineScope)
    fun stop()
    fun requestInitialEvents()
}

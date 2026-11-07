package dev.cricamfe.digivice.core

import dev.cricamfe.digivice.core.events.Event
import dev.cricamfe.digivice.core.events.dto.HealthDto
import dev.cricamfe.digivice.core.events.models.EventType
import dev.cricamfe.digivice.core.events.models.HealthStatus
import dev.cricamfe.digivice.core.retroarch.ConnectionCodes

/** What the events say about whether the overlay has anything to show, for [OverlayPolicy]. */
object Reachability {
    /**
     * `false` if [event] is a health event that says the player is not on the game: RetroArch does not answer, it has no
     * content, or it runs a game of another console, so the overlay should leave the lower screen to the console. `true`
     * for any other health event, where the overlay has something to say: a PlayStation game that is not this one, the
     * title screen, a failed read. Null for an event that says nothing about it.
     */
    fun of(event: Event): Boolean? {
        if (event.type != EventType.HealthChanged) return null
        val health = event.payload as? HealthDto ?: return null
        return !(health.status == HealthStatus.Error && health.errorCode in AwayCodes)
    }

    private val AwayCodes =
        setOf(ConnectionCodes.RETROARCH_UNREACHABLE, ConnectionCodes.NO_CONTENT, ConnectionCodes.OTHER_CONSOLE)
}

package dev.cricamfe.digivice.core.events

import dev.cricamfe.digivice.core.domain.models.State
import dev.cricamfe.digivice.core.events.models.HealthStatus

/**
 * Port of `Backend/Events/States/GameStateStore.cs` (without its `IGameStateStore` interface, which only served the
 * dependency injection of the web host): the last state that was read and what the health of the reading is.
 *
 * Not thread safe, as the original: whoever reads the game owns it. [HealthEventFactory][dev.cricamfe.digivice.core.events.factory.HealthEventFactory]
 * writes the status and the error fields.
 */
class GameStateStore {
    var currentState: State? = null
    var status: HealthStatus = HealthStatus.Loading
    var lastErrorCode: String? = null
    var lastErrorDetail: String? = null

    fun updateState(state: State) {
        currentState = state
    }

    /** Forgets the state; the status is left as it is. */
    fun clearState() {
        currentState = null
    }
}

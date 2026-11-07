package dev.cricamfe.digivice.core.events.factory

import dev.cricamfe.digivice.core.events.Event
import dev.cricamfe.digivice.core.events.GameStateStore
import dev.cricamfe.digivice.core.events.dto.HealthDto
import dev.cricamfe.digivice.core.events.models.EventType
import dev.cricamfe.digivice.core.events.models.HealthStatus

/** Port of `Backend/Events/Factory/HealthEventFactory.cs`. Unlike the other factories it writes: the store's status and error fields. */
object HealthEventFactory {
    /** `CreateSuccess`: the read worked, so there is no error; a `HealthChanged` event only when the status changes. */
    fun success(store: GameStateStore): List<Event> {
        val targetStatus = if (store.currentState != null) HealthStatus.Healthy else HealthStatus.Loading

        store.lastErrorCode = null
        store.lastErrorDetail = null

        if (store.status == targetStatus) return emptyList()

        store.status = targetStatus
        return listOf(create(targetStatus))
    }

    /**
     * `CreateError`: the read failed; the state is forgotten and the error recorded. Clients are told unless they
     * already know (the store was in error and had no state, with this same code).
     *
     * Departure from the C#: a change of [code] while already in error is also announced. The C# only had a handful of
     * codes that never followed one another, but the RetroArch connection goes from `retroarch_unreachable` to
     * `no_content` to `wrong_game` to `waiting_for_save`, and a UI driven by events must see each reason. A changed
     * [detail] alone does not notify.
     */
    fun error(store: GameStateStore, code: String, detail: String? = null): List<Event> {
        val shouldNotifyClients =
            store.status != HealthStatus.Error || store.currentState != null || store.lastErrorCode != code

        store.lastErrorCode = code
        store.lastErrorDetail = detail
        store.clearState()
        store.status = HealthStatus.Error

        if (!shouldNotifyClients) return emptyList()

        return listOf(create(HealthStatus.Error, code, detail))
    }

    /** What a client that connects now must be told about the health: the status the store holds, with its error if any. */
    fun current(store: GameStateStore): Event = create(store.status, store.lastErrorCode, store.lastErrorDetail)

    private fun create(status: HealthStatus, errorCode: String? = null, errorDetail: String? = null) =
        Event(EventType.HealthChanged, HealthDto(status, errorCode, errorDetail))
}

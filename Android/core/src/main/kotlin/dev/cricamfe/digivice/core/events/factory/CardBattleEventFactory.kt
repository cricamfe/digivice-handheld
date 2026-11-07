package dev.cricamfe.digivice.core.events.factory

import dev.cricamfe.digivice.core.domain.models.State
import dev.cricamfe.digivice.core.events.Event
import dev.cricamfe.digivice.core.events.diffing.CardBattleDiffer
import dev.cricamfe.digivice.core.events.dto.extensions.isNotEmpty
import dev.cricamfe.digivice.core.events.dto.CardBattleDto
import dev.cricamfe.digivice.core.events.models.EventType

/** Port of `Backend/Events/Factory/CardBattleEventFactory.cs`: the `CardBattleChanged` event, or none when the section did not change. */
object CardBattleEventFactory {
    fun create(previousState: State, newState: State): List<Event> {
        val dto = CardBattleDiffer.diff(previousState.cardBattle, newState.cardBattle)

        return if (dto.isNotEmpty(CardBattleDto())) listOf(Event(EventType.CardBattleChanged, dto)) else emptyList()
    }
}

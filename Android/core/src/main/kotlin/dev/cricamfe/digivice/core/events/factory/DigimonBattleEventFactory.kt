package dev.cricamfe.digivice.core.events.factory

import dev.cricamfe.digivice.core.domain.models.State
import dev.cricamfe.digivice.core.events.Event
import dev.cricamfe.digivice.core.events.diffing.DigimonBattleDiffer
import dev.cricamfe.digivice.core.events.dto.extensions.isNotEmpty
import dev.cricamfe.digivice.core.events.dto.DigimonBattleDto
import dev.cricamfe.digivice.core.events.models.EventType

/** Port of `Backend/Events/Factory/DigimonBattleEventFactory.cs`: the `DigimonBattleChanged` event, or none when the section did not change. */
object DigimonBattleEventFactory {
    fun create(previousState: State, newState: State): List<Event> {
        val dto = DigimonBattleDiffer.diff(previousState.digimonBattle, newState.digimonBattle)

        return if (dto.isNotEmpty(DigimonBattleDto())) listOf(Event(EventType.DigimonBattleChanged, dto)) else emptyList()
    }
}

package dev.cricamfe.digivice.core.events.converters.journals.quests

import dev.cricamfe.digivice.core.domain.models.journals.quests.Requisite
import dev.cricamfe.digivice.core.events.dto.journals.quests.RequisiteDto
import dev.cricamfe.digivice.core.events.dto.shared.Optional

/** Port of `Backend/Events/Converters/Journals/Quests/RequisiteConverter.cs`. Reads the domain model, never changes it. */
object RequisiteConverter {
    fun toDto(requisite: Requisite): RequisiteDto = RequisiteDto(
        id = requisite.id,
        isDone = Optional.Present(requisite.isDone),
    )
}

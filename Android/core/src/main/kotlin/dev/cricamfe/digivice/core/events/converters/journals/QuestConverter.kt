package dev.cricamfe.digivice.core.events.converters.journals

import dev.cricamfe.digivice.core.domain.models.journals.Quest
import dev.cricamfe.digivice.core.events.converters.journals.quests.RequisiteConverter
import dev.cricamfe.digivice.core.events.converters.journals.quests.StepConverter
import dev.cricamfe.digivice.core.events.dto.journals.QuestDto
import dev.cricamfe.digivice.core.events.dto.shared.Optional

/** Port of `Backend/Events/Converters/Journals/QuestConverter.cs`. Reads the domain model, never changes it. */
object QuestConverter {
    fun toDto(quest: Quest): QuestDto = QuestDto(
        id = quest.id,
        requisites = Optional.Present(quest.requisites.map(RequisiteConverter::toDto)),
        steps = Optional.Present(quest.steps.map(StepConverter::toDto)),
    )
}

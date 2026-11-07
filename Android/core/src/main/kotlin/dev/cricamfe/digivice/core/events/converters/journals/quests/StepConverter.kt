package dev.cricamfe.digivice.core.events.converters.journals.quests

import dev.cricamfe.digivice.core.domain.models.journals.quests.Step
import dev.cricamfe.digivice.core.events.dto.journals.quests.StepDto
import dev.cricamfe.digivice.core.events.dto.shared.Optional

/** Port of `Backend/Events/Converters/Journals/Quests/StepConverter.cs`. Reads the domain model, never changes it. */
object StepConverter {
    fun toDto(step: Step): StepDto = StepDto(
        number = step.number,
        isDone = Optional.Present(step.isDone),
        requisites = Optional.Present(step.requisites.map(RequisiteConverter::toDto)),
    )
}

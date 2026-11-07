package dev.cricamfe.digivice.core.events.diffing.journals.quests

import dev.cricamfe.digivice.core.domain.models.journals.quests.Step
import dev.cricamfe.digivice.core.events.converters.journals.quests.StepConverter
import dev.cricamfe.digivice.core.events.diffing.extensions.changed
import dev.cricamfe.digivice.core.events.diffing.extensions.changedItems
import dev.cricamfe.digivice.core.events.diffing.extensions.hasNoChanges
import dev.cricamfe.digivice.core.events.dto.journals.quests.StepDto

/** Port of `Backend/Events/Diffing/Journals/Quests/StepDiffer.cs`. Only reads the steps. */
object StepDiffer {
    fun diff(previousStep: Step?, newStep: Step): StepDto? {
        if (newStep.hasNoChanges(previousStep)) return null
        if (previousStep == null) return StepConverter.toDto(newStep)

        val isDone = changed(previousStep.isDone, newStep.isDone)
        val requisites = changedItems(
            newStep.requisites.mapNotNull { newRequisite ->
                RequisiteDiffer.diff(previousStep.requisites.firstOrNull { it.id == newRequisite.id }, newRequisite)
            },
        )

        if (!isDone.hasValue && !requisites.hasValue) return null

        return StepDto(number = newStep.number, isDone = isDone, requisites = requisites)
    }
}

package dev.cricamfe.digivice.core.events.diffing.journals

import dev.cricamfe.digivice.core.domain.models.journals.Quest
import dev.cricamfe.digivice.core.events.converters.journals.QuestConverter
import dev.cricamfe.digivice.core.events.diffing.extensions.changedItems
import dev.cricamfe.digivice.core.events.diffing.extensions.hasNoChanges
import dev.cricamfe.digivice.core.events.diffing.journals.quests.RequisiteDiffer
import dev.cricamfe.digivice.core.events.diffing.journals.quests.StepDiffer
import dev.cricamfe.digivice.core.events.dto.journals.QuestDto

/** Port of `Backend/Events/Diffing/Journals/QuestDiffer.cs`. Only reads the quests. */
object QuestDiffer {
    fun diff(previousQuest: Quest?, newQuest: Quest): QuestDto? {
        if (newQuest.hasNoChanges(previousQuest)) return null
        if (previousQuest == null) return QuestConverter.toDto(newQuest)

        val requisites = changedItems(
            newQuest.requisites.mapNotNull { newRequisite ->
                RequisiteDiffer.diff(previousQuest.requisites.firstOrNull { it.id == newRequisite.id }, newRequisite)
            },
        )
        val steps = changedItems(
            newQuest.steps.mapNotNull { newStep ->
                StepDiffer.diff(previousQuest.steps.firstOrNull { it.number == newStep.number }, newStep)
            },
        )

        if (!requisites.hasValue && !steps.hasValue) return null

        return QuestDto(id = newQuest.id, requisites = requisites, steps = steps)
    }
}

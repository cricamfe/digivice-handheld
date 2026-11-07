package dev.cricamfe.digivice.core.domain.assemblers.journals

import dev.cricamfe.digivice.core.domain.assemblers.journals.quests.RequisiteAssembler
import dev.cricamfe.digivice.core.domain.assemblers.journals.quests.StepAssembler
import dev.cricamfe.digivice.core.domain.models.journals.Quest
import dev.cricamfe.digivice.core.memory.resources.journals.QuestResource

/** Port of `Backend/Domain/Assemblers/Journals/QuestAssembler.cs`. */
object QuestAssembler {
    fun assemble(resource: QuestResource): Quest = Quest(
        id = resource.id,
        requisites = resource.requisites.map(RequisiteAssembler::assemble),
        steps = resource.steps.map(StepAssembler::assemble),
    )
}

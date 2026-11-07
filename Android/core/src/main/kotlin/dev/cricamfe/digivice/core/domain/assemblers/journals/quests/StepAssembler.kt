package dev.cricamfe.digivice.core.domain.assemblers.journals.quests

import dev.cricamfe.digivice.core.domain.models.journals.quests.Step
import dev.cricamfe.digivice.core.memory.resources.journals.quests.StepResource

/** Port of `Backend/Domain/Assemblers/Journals/Quests/StepAssembler.cs`. */
object StepAssembler {
    fun assemble(resource: StepResource): Step = Step(
        number = resource.number,
        isDone = resource.value.toInt() != 0,
        requisites = resource.requisites.map(RequisiteAssembler::assemble),
    )
}

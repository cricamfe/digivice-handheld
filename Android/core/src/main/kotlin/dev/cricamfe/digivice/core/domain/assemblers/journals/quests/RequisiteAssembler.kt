package dev.cricamfe.digivice.core.domain.assemblers.journals.quests

import dev.cricamfe.digivice.core.domain.models.journals.quests.Requisite
import dev.cricamfe.digivice.core.memory.resources.journals.quests.RequisiteResource

/** Port of `Backend/Domain/Assemblers/Journals/Quests/RequisiteAssembler.cs`. */
object RequisiteAssembler {
    fun assemble(resource: RequisiteResource): Requisite = Requisite(
        id = resource.id,
        isDone = resource.value.toInt() != 0,
    )
}
